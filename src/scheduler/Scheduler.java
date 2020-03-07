package scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;
import enums.SystemEnumTypes.ElevatorCurrentStatus;
import enums.SystemEnumTypes.MonitoredSchedulerEvent;
import enums.SystemEnumTypes.RequestEvent;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.FloorButtonRequest;
import requests.Request;
import server.Server;



/**
 * The purpose of this class is to schedule the events required to coordinate an
 * elevator system. The scheduler is responsible for controlling: - assigning
 * and maintaining queue's for trip requests received for each elevator -
 * sequences for each elevators operations including: motor on/off up/down,
 * lamps on/off, doors open/close A primary goal of the scheduler is to ensure
 * all trip requests are actioned in a timely manner, ensuring none wait
 * indefinitely. The scheduler maintains a complete state of the elevator
 * subsystem at any given time, this includes: - state of the elevators
 * locations, directions, status (moving/STOP), doors (OPEN/CLOSE)
 *
 */
public class Scheduler implements Runnable, ElevatorEvents {

	private final double monitoredSchedulerDelayFactor = 1.25; // This factor is used to set an appropriate alarm for
																// each monitored scheduler event - this allows a slight
																// delay related to network transmission times in
																// addition to expected execution times
	private String name;
	private Server server;
	private Thread serverThread;
	private Queue<Request> eventsQueue; // This queue contains an ordered list of events as received by the Scheduler
										// from other subsystems
	private HashMap<Class<?>, ArrayList<Double>> eventElapsedTimes; // key -> class type (ie. ElevatorArrivalRequest,
																	// FloorButtonRequest, etc.), value -> list
																	// containing elapsed times to fully handle the
																	// request (milliseconds)
	private boolean debug = false;
	private HashMap<String, Integer> portsByElevatorName; // key -> elevator name, value -> port number
	private HashMap<String, Integer> portsByFloorName; // key -> floor number, value -> port number
	private HashMap<String, Monitor> MonitorByElevatorName; // key -> elevator name, value -> elevator
																			// monitor
	private ArrayList<MakeTrip> pendingMakeTrips;
	private HashMap<String, MonitoredEventTimer> monitoredSchedulerEvents; // key -> subsystemName, value ->
																			// monitoredEventTimer
	private HashMap<String, String> hostByElevatorName;
	private HashMap<String, String> hostByFloorName;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.MonitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingMakeTrips = new ArrayList<MakeTrip>();
		this.monitoredSchedulerEvents = new HashMap<String, MonitoredEventTimer>();
		this.hostByElevatorName = new HashMap<String, String>();
		this.hostByFloorName = new HashMap<String, String>();
		this.eventElapsedTimes = new HashMap<Class<?>, ArrayList<Double>>();

		// Initialize infrastructure configurations (elevators/floors)
		this.init(elevatorConfiguration, floorConfigurations);

		// Create a server (bound to this Instance of ElevatorSubsystem) in a new
		// thread.
		// When this server receives requests, they will be added to the eventsQueue of
		// THIS ElevatorSubsystem instance.
		this.server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	/**
	 * The purpose of this method is to initialize some of the data structures used
	 * to monitor the state of the elevator systems by the scheduler.
	 * 
	 * @param elevatorConfiguration
	 * @param floorConfigurations
	 */
	public void init(HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		// Initialize data structures for elevators
		for (String elevatorName : elevatorConfiguration.keySet()) {
			HashMap<String, String> config = elevatorConfiguration.get(elevatorName);

			this.portsByElevatorName.put(elevatorName, Integer.parseInt(config.get("port")));
			this.hostByElevatorName.put(elevatorName, config.get("host"));

			// Initialize Monitors for each elevator
			this.MonitorByElevatorName.put(elevatorName,
					new Monitor(elevatorName, Integer.parseInt(config.get("startFloor")),
							Integer.parseInt(config.get("startFloor")), Direction.STAY, ElevatorCurrentStatus.STOP,
							ElevatorCurrentDoorStatus.OPEN, floorConfigurations.size(),
							Integer.parseInt(config.get("timeBetweenFloors")),
							Integer.parseInt(config.get("passengerWaitTime")),
							Integer.parseInt(config.get("doorOperationTime"))));

		}

		// Initialize data structures for floors
		for (String floorName : floorConfigurations.keySet()) {
			HashMap<String, String> config = floorConfigurations.get(floorName);

			this.portsByFloorName.put(floorName, Integer.parseInt(config.get("port")));
			this.hostByFloorName.put(floorName, config.get("host"));
		}
	}

	@Override
	/**
	 * Add an event to the eventQueue.
	 */
	public synchronized void receiveEvent(Request request) {
		eventsQueue.add(request);
		this.notifyAll();
	}

	@Override
	/**
	 * Get next event from the eventQueue.
	 */
	public synchronized Request getNextEvent() {
		while (eventsQueue.isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return eventsQueue.poll();
	}

	/**
	 * This method is called by a monitoredEventTimer when the timer is complete.
	 * This means the timer was not cancelled before its deadline was reached. This
	 * method will handle the missed deadline for the monitoredSchedulerEvent.
	 * 
	 * @param subsystemName
	 */
	public synchronized void monitoredEventTimerComplete(String subsystemName) {
		// Check to ensure a monitoredEventTimer was created for this subsystemName
		MonitoredEventTimer monitoredEventTimer = this.monitoredSchedulerEvents.get(subsystemName);
		if (monitoredEventTimer == null) {
			return;
		}

		MonitoredSchedulerEvent monitoredSchedulerEvent = monitoredEventTimer.getMonitoredSchedulerEvent();
		switch (monitoredSchedulerEvent) {
		case ELEVATOR_MOVE:
			Monitor monitor = this.MonitorByElevatorName.get(subsystemName);

			// If an elevator response has not been received for an ELEVATOR_MOVE
			// monitoredEvent, then set the Elevator as OUT_OF_SERVICE
			monitor.updateElevatorStatus(ElevatorCurrentStatus.OUT_OF_SERVICE);
			this.consoleOutput("[RESPONSE NOT RECEIVED FROM " + subsystemName
					+ "] Expected floor arrival notice. Elevator stuck between floors. " + subsystemName
					+ " is OUT OF SERVICE");

			// Rescheduled any pending trips (that have not yet begun, in other words,
			// pickup hasn't occurred) from this elevator.
			ArrayList<MakeTrip> reassignableMakeTrips = monitor.unassignPendingTripRequests();
			for (MakeTrip MakeTrip : reassignableMakeTrips) {
				this.consoleOutput(
						"Reassigning pending trip request " + MakeTrip + " from " + subsystemName + "...");
				this.eventMakeTripReceived(MakeTrip);
			}
			break;
		case ELEVATOR_OPEN_DOOR:
			// Resend elevator door open
			this.consoleOutput("[RESPONSE NOT RECEIVED FROM " + subsystemName + "] Expected door open confirmation");
			this.consoleOutput(RequestEvent.SENT, subsystemName, "Open elevator door.");
			this.sendToServer(new ElevatorDoorRequest(subsystemName, ElevatorCurrentDoorStatus.OPEN),
					this.hostByElevatorName.get(subsystemName), this.portsByElevatorName.get(subsystemName));
			break;
		case ELEVATOR_CLOSE_DOOR:
			// resend elevator door close
			this.consoleOutput("[RESPONSE NOT RECEIVED FROM " + subsystemName + "] Expected door CLOSE confirmation");
			this.consoleOutput(RequestEvent.SENT, subsystemName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(subsystemName, ElevatorCurrentDoorStatus.CLOSE),
					this.hostByElevatorName.get(subsystemName), this.portsByElevatorName.get(subsystemName));
			break;
		}
	}

	/**
	 * Add a MonitoredEventTimer for a MonitoredEvent.
	 * 
	 * @param subsystemName
	 * @param eventTimer
	 */
	private synchronized void addMonitoredEvent(String subsystemName, MonitoredEventTimer eventTimer) {
		this.monitoredSchedulerEvents.put(subsystemName, eventTimer);
	}

	/**
	 * Remove a MonitoredEvent from the MonitoredSchedulerEvents. This will cancel a
	 * MonitoredEventTimer, then remove it from the MonitoredSchedulerEvents. This
	 * is to be called when an expected response for a monitored event from a
	 * subsystem has been received.
	 * 
	 * @param subsystemName
	 */
	private synchronized void removeMonitoredEvent(String subsystemName) {
		MonitoredEventTimer monitoredEventTimer = this.monitoredSchedulerEvents.get(subsystemName);
		if (monitoredEventTimer != null) {
			monitoredEventTimer.cancel();
		}
		this.monitoredSchedulerEvents.remove(subsystemName);
	}

	@Override
	/**
	 * Get the name of this scheduler.
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public void run() {
		this.consoleOutput("Scheduler is online. Waiting for a trip request...");
		while (true) {
			this.handleEvent(this.getNextEvent());
		}
	}

	/**
	 * This method will determine the type of Request and call the appropriate event
	 * handler method for this request.
	 * 
	 * @param event
	 */
	private void handleEvent(Request event) {
		// switch statement corresponding to different "event handlers"
		if (event instanceof FloorButtonRequest) {
			FloorButtonRequest request = (FloorButtonRequest) event;

			this.consoleOutput(RequestEvent.RECEIVED, "Floor " + request.getFloorName(), "Trip request from floor "
					+ request.getFloorName() + " in direction " + request.getDirection() + ".");
			this.eventMakeTripReceived(Integer.parseInt(request.getFloorName()), request.getDirection());
		} else if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;

			// Only handle this event if the ElevatorCurrentStatus is not OUT OF SERVICE
			if (this.MonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator arrival notice at floor " + request.getFloorName() + ".");
				this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator arrival notice at floor " + request.getFloorName() + ".");
			}
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;

			// Only handle this event if the ElevatorCurrentStatus is not OUT OF SERVICE
			if (this.MonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator door is " + request.getRequestAction() + ".");
				if (request.getRequestAction() == ElevatorCurrentDoorStatus.OPEN) {
					this.eventElevatorDoorOPEN(request.getElevatorName());
				} else if (request.getRequestAction() == ElevatorCurrentDoorStatus.CLOSE) {
					this.eventElevatorDoorCLOSE(request.getElevatorName());
				}
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator door is " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;

			// Only handle this event if the ElevatorCurrentStatus is not OUT OF SERVICE
			if (this.MonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != ElevatorCurrentStatus.OUT_OF_SERVICE) {
				if (request.getRequestAction() == Direction.STAY) {
					this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(), "Elevator has STOP.");
					this.eventElevatorSTOP(request.getElevatorName());
				} else {
					this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
							"Elevator is moving " + request.getRequestAction() + ".");
				}
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator is " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;

			// Only handle this event if the ElevatorCurrentStatus is not OUT OF SERVICE
			if (this.MonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Destination request from pickup floor: " + request.getPickupFloor() + " to destination floor: "
								+ request.getDestinationFloor());
				this.eventElevatorDestinationRequest(request.getElevatorName(),
						Integer.parseInt(request.getPickupFloor()), Integer.parseInt(request.getDestinationFloor()));
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Destination request from pickup floor: " + request.getPickupFloor()
								+ " to destination floor: " + request.getDestinationFloor());
			}
		} else if (event instanceof ElevatorWaitRequest) {
			ElevatorWaitRequest request = (ElevatorWaitRequest) event;

			// Only handle this event if the ElevatorCurrentStatus is not OUT OF SERVICE
			if (this.MonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator has completed its wait.");
				this.eventElevatorWaitComplete(request.getElevatorName());
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator has completed its wait.");
			}
		}

		// Set the end time for the request, and add it the event history.
		event.setEndTime();
		this.addCompletedEvent(event);
	}

	/**
	 * 
	 * @param event
	 */
	private void addCompletedEvent(Request event) {
		ArrayList<Double> elapsedTimes = this.eventElapsedTimes.get(event.getClass());
		if (elapsedTimes == null) {
			elapsedTimes = new ArrayList<Double>();
			this.eventElapsedTimes.put(event.getClass(), elapsedTimes);
		}
		elapsedTimes.add(event.getElapsedTime());
		this.eventElapsedTimes.put(event.getClass(), elapsedTimes);
	}

	/**
	 * Calculates the Scheduler's response mean and variance for every each request
	 * type. Displays this information to console. All time values are in
	 * milliseconds.
	 * 
	 */
	public void displaySchedulerResponseTimes() {
		System.out.println("\n\n-----------------------------------------");
		System.out.println("Displaying Scheduler Response Statistics");
		System.out.printf("%-30s %-10s %-22s %-18s %n", "Event Type", "# of Events", "Mean Response(ms)",
				"Variance(ms^2)");
		for (Class<?> eventType : this.eventElapsedTimes.keySet()) {
			ArrayList<Double> elapsedTimes = this.eventElapsedTimes.get(eventType);
			Double mean = this.calculateMean(elapsedTimes);
			Double variance = this.calculateVariance(elapsedTimes, mean);
			System.out.printf("%-30s %10d %22.5f %18.5f %n", eventType.getSimpleName(), elapsedTimes.size(), mean,
					variance);
		}
	}

	/**
	 * Calculates and returns mean average value of a list containing elapsedTimes
	 * 
	 * @param elapsedTimes
	 * @return mean in nanoseconds
	 */
	private Double calculateMean(ArrayList<Double> elapsedTimes) {
		Double responseTotals = 0.0;

		if (elapsedTimes.size() > 0) {
			for (Double elapsedTime : elapsedTimes) {
				responseTotals += elapsedTime;
			}
			return responseTotals / elapsedTimes.size();
		}
		return 0.0;
	}

	/**
	 * Calculates and returns the variance of a list containing elapsedTimes
	 * 
	 * @param elapsedTimes
	 * @param mean
	 * @return
	 */
	private Double calculateVariance(ArrayList<Double> elapsedTimes, Double mean) {
		Double sum = 0.0;

		if (elapsedTimes.size() > 0) {
			for (Double elapsedTime : elapsedTimes) {
				sum += Math.pow(elapsedTime - mean, 2);
			}
			return sum / elapsedTimes.size();
		}

		return 0.0;
	}

	/**
	 * Send a request to port using this object's server.
	 * 
	 * @param request
	 * @param elevatorName
	 */
	private void sendToServer(Request request, String host, int port) {
		this.server.send(request, host, port);
	}

	/**
	 * This method attempts to assign an incoming MakeTrip to one of the
	 * elevators. The first preference is to assign the MakeTrip to an in service
	 * elevator, if the trip is en route. If there are no en-route options, attempt
	 * to find an STAY elevator to service the MakeTrip. If this is not possible
	 * then the MakeTrip will be put in a pending queue.
	 * 
	 * @param pickupFloorNumber
	 * @param destinationFloorNumber
	 * @param direction
	 */
	private void eventMakeTripReceived(int pickupFloorNumber, Direction direction) {
		// Create a MakeTrip object
		MakeTrip MakeTrip = new MakeTrip(pickupFloorNumber, direction);
		this.eventMakeTripReceived(MakeTrip);
	}

	private void eventMakeTripReceived(MakeTrip MakeTrip) {
		Monitor monitor = this.planningSystem(MakeTrip);

		// If an Elevator has been selected for this trip request, determine the next
		// action required for the elevator.
		// 1 - If the elevator is STOP and STAY, then
		// i - if it is at the floor of the trip request, then the floor must be
		// advised, and the elevator needs to be advised to wait for passengers to load
		// ii - otherwise, send a door CLOSE event, when the door CLOSE event is
		// confirmed, the scheduler will determine the next direction for the elevator.
		if (monitor != null) {
			monitor.addTripRequest(MakeTrip);
			this.consoleOutput(
					"Trip request " + MakeTrip + " was assigned to " + monitor.getElevatorName() + ".");
			// If the elevator is currently STOP and STAY, then a door close event must
			// be sent
			// within a short time (before the elevator door is CLOSE).
			if ((monitor.getElevatorStatus() == ElevatorCurrentStatus.STOP)
					&& (monitor.getElevatorDirection() == Direction.STAY)) {

				// Since the elevator is STOP and STAY, check if it is at the floor of the
				// trip request
				// If so, the scheduler must then advise the floor and advise elevator to wait
				// for passengers to load.
				if (monitor.getElevatorFloorLocation() == MakeTrip.getUserinitalLocation()) {
					// Send event to floor that elevator is ready to accept passengers - this will
					// ensure the floor sends the corresponding destination request to the elevator
					// - pushing things forward
					this.consoleOutput(RequestEvent.SENT, "FLOOR " + MakeTrip.getUserinitalLocation(),
							"Elevator " + monitor.getElevatorName() + " has arrived for a pickup/dropoff.");
					this.sendToServer(
							new ElevatorArrivalRequest(monitor.getElevatorName(),
									String.valueOf(MakeTrip.getUserinitalLocation()), monitor.getQueueDirection()),
							this.hostByFloorName.get(MakeTrip.getUserinitalLocation()),
							this.portsByFloorName.get(String.valueOf(MakeTrip.getUserinitalLocation())));

					// Only if this was the first trip added to the queue at this stop, send an
					// elevator wait arrival command, this is to handle the case where an elevator
					// is STOP and STAY and receives
					// two requests for trips before the elevator is done waiting from the first
					// request (as the elevator state would still be STOP and STAY until the wait
					// is over).
					if (monitor.getQueueLength() == 1) {
						// Send a wait at floor command to the elevator - this is to simulate both
						// passengers leaving and entering the elevator
						this.consoleOutput(RequestEvent.SENT, monitor.getElevatorName(),
								"Wait at floor for passengers to load.");
						this.sendToServer(new ElevatorWaitRequest(monitor.getElevatorName()),
								this.hostByElevatorName.get(monitor.getElevatorName()),
								this.portsByElevatorName.get(monitor.getElevatorName()));
						return;
					}
				}

				// Otherwise, since the elevator is STAY and STOP but not at the right floor,
				// it must close its door to start moving in the right direction.
				// Only if this was the first trip added to the queue at this stop, send an
				// elevator close door command, this is to handle the case where an elevator is
				// STOP and STAY and receives
				// two requests for trips before the elevator has CLOSE its door from the
				// previous request (as the elevator state would still be STOP and STAY until
				// the wait is over).
				if (monitor.getQueueLength() == 1) {
					this.consoleOutput(RequestEvent.SENT, monitor.getElevatorName(), "Close elevator door.");
					this.sendToServer(
							new ElevatorDoorRequest(monitor.getElevatorName(), ElevatorCurrentDoorStatus.CLOSE),
							this.hostByElevatorName.get(monitor.getElevatorName()),
							this.portsByElevatorName.get(monitor.getElevatorName()));
				}
			}
		} else {
			// Add this MakeTrip to the pendingMakeTrips queue
			this.pendingMakeTrips.add(MakeTrip);
			this.consoleOutput("Trip request " + MakeTrip
					+ " was unable to be assigned immediately. It has been added to pending requests "
					+ this.pendingMakeTrips + ".");
		}
	}

	/**
	 * 
	 * @param MakeTrip
	 * @return
	 */
	private Monitor planningSystem(MakeTrip MakeTrip) {
		Monitor closestMonitor = null;
		Integer closestElevatorTime = null;

		// Iterate through all elevators to determine whether there is an eligible
		// elevator to handle this trip request,
		// and which would be most optimal.
		for (String elevatorName : MonitorByElevatorName.keySet()) {
			boolean currentElevatorIsMoreFavourable = false;

			Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);
			Integer estimatedElevatorPickupTime = Monitor.estimatePickupTime(MakeTrip);

			// If the estimateElevatorPickupTime for this elevator is null, then this
			// elevator cannot accommodate this MakeTrip at this time
			// skip to next iteration of foreach loop
			if (estimatedElevatorPickupTime == null) {
				continue;
			}

			// If there is not yet a closest eligible elevator, set the current elevator
			// being evaluated as closestElevator
			if (closestElevatorTime == null) {
				currentElevatorIsMoreFavourable = true;
			} else {
				// The comparison between the current elevator being evaluated and the
				// closestElevator depends on whether the closestElevator is STAY or in-service
				if (closestMonitor.getNextElevatorDirection() == Direction.STAY) {
					// The comparison between the current elevator and the closestElevator also
					// depends on whether the current elevator being evaluated is STAY or in-service
					// In the case where the current elevator being evaluated is in-service
					if (Monitor.getNextElevatorDirection() == Direction.STAY) {
						// If the current elevator being evaluated is in-service and has a quicker
						// estimated pickup time
						// then this elevator is more favourable for this trip request than the current
						// closestElevator
						if (estimatedElevatorPickupTime < closestElevatorTime) {
							currentElevatorIsMoreFavourable = true;
						}
					} else {
						// Always favour an in service elevator if possible.
						// if (Math.abs(closestElevatorTime - estimatedElevatorPickupTime) >=
						// closestElevatorTime){
						currentElevatorIsMoreFavourable = true;
						// }
					}
					// In the case where the closestElevator is STAY
				} else {
					// In the case where the current elevator being evaluated is in-service
					if (Monitor.getNextElevatorDirection() == Direction.STAY) {
						// Always favour in service elevator
						/*
						 * //If the current elevator being evaluated is in-service and has an estimated
						 * pickup time that is less than or equal to the current closest elevator's
						 * pickup time //then this elevator is more favourable for this trip request
						 * than the current closestElevator if ((estimatedElevatorPickupTime <=
						 * closestElevatorTime) && (Math.abs(closestElevatorTime -
						 * estimatedElevatorPickupTime) >= estimatedElevatorPickupTime)) {
						 * currentElevatorIsMoreFavourable = true; }
						 */
					} else {
						// If the current elevator being evaluated is STAY and has a quicker estimated
						// pickup time
						// then this elevator is more favourable for this trip request than the current
						// closestElevator
						if (estimatedElevatorPickupTime < closestElevatorTime) {
							currentElevatorIsMoreFavourable = true;
						}
					}
				}
			}

			// Replace closest Elevator with current Elevator if any of the conditions above
			// have been met.
			if (currentElevatorIsMoreFavourable) {
				closestMonitor = Monitor;
				closestElevatorTime = estimatedElevatorPickupTime;
			}
		}

		return closestMonitor;
	}

	/**
	 * 
	 * @param elevatorName
	 * @param pickupFloor
	 * @param destinationFloor
	 */
	private void eventElevatorDestinationRequest(String elevatorName, Integer pickupFloor, Integer destinationFloor) {
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		if (Monitor.addDestination(pickupFloor, destinationFloor)) {
			this.consoleOutput(
					"Destination [" + destinationFloor + "] was successfully added to " + elevatorName + "'s queue.");
		} else {
			this.consoleOutput("Destination [" + destinationFloor + "] was not successfully added to " + elevatorName
					+ "'s queue. Abandoning destination request.");
		}
	}

	/**
	 * When an elevator arrives at a floor, update the Monitor then check if
	 * the elevator needs to stop. If the elevator needs to stop, send a stop
	 * request to the elevator. If the elevator needs to stop because this floor is
	 * a pickup, then a request needs to be sent to the floor to turn off the floor
	 * direction lamp. If not stop is needed, check the Monitor to determine
	 * the next direction for the elevator and send a motor request in that
	 * direction.
	 * 
	 * @param elevatorName
	 * @param floorNumber
	 */
	private void eventElevatorArrivalNotice(String elevatorName, int floorNumber) {
		// Remove the Move request from monitorElevatorEvents for this elevator
		// 'elevatorName'
		// Presumably this is occurring before the monitoredEventTimer has completed.
		this.removeMonitoredEvent(elevatorName);

		// Get the Monitor for this elevator
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		// Update the Monitor with the new floor of the elevator
		Monitor.updateElevatorFloorLocation(floorNumber);

		// Check if this elevator needs to stop at this floor
		if (Monitor.isStopRequired(floorNumber)) {
			this.consoleOutput("Stop is required for " + elevatorName + " at floor " + floorNumber);
			this.consoleOutput(RequestEvent.SENT, elevatorName, "Stop elevator.");
			this.sendToServer(new ElevatorMotorRequest(elevatorName, Direction.STAY),
					this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));
		} else {
			this.consoleOutput("Stop is not required for " + elevatorName + " at floor " + floorNumber);
			// The reason we evaluate this direction again, is because in certain
			// circumstances, the direction may change
			// Example, if the elevator is going down to it's starting floor, but a trip
			// request had been assigned to it before it reached its destination, the
			// elevator needs to change directions
			Direction nextDirection = Monitor.getNextElevatorDirection();
			this.sendElevatorMoveEvent(elevatorName, nextDirection);
		}
	}

	/**
	 * When an elevator stops, take this opportunity to update the Monitor.
	 * If this stop is a destination for a MakeTrip, the corresponding
	 * MakeTrip is completed, and the Monitor needs to be updated to
	 * reflect this If this stop is a pickup for a MakeTrip, the MakeTrip
	 * needs to remove this pickup from it's queue of pickups. but the MakeTrip
	 * still is in service in the Monitor.
	 * 
	 * When this is completed, send an elevator open door event.
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorSTOP(String elevatorName) {
		// Get Monitor for the elevator.
		Monitor monitor = this.MonitorByElevatorName.get(elevatorName);

		// Update elevator status to STOP
		monitor.updateElevatorStatus(ElevatorCurrentStatus.STOP);

		// The Monitor needs to be advised this stop has occurred
		HashSet<MakeTrip> completedTrips = monitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.consoleOutput(
					"The following trips have been completed at this stop by " + elevatorName + ":" + completedTrips);
		}

		// Send an open door event to the elevator
		this.consoleOutput(RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorCurrentDoorStatus.OPEN),
				this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

		// Monitor the Elevator Move request
		MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
				MonitoredSchedulerEvent.ELEVATOR_OPEN_DOOR,
				(int) (monitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
		this.addMonitoredEvent(elevatorName, monitoredEventTimer);

		// Start the monitored event timer
		Thread t = new Thread(monitoredEventTimer);
		t.start();
	}

	/**
	 * When confirmation has been received that the elevator has OPEN its doors,
	 * determine whether this elevator has more trips. If the Monitor is not
	 * empty, then
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorDoorOPEN(String elevatorName) {
		// Remove the Door request from monitorElevatorEvents for this elevator
		// 'elevatorName'
		// Presumably this is occurring before the monitoredEventTimer has completed.
		this.removeMonitoredEvent(elevatorName);

		// Get Monitor for the elevator.
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		// Update current elevator door status
		Monitor.updateElevatorDoorStatus(ElevatorCurrentDoorStatus.OPEN);

		// Checking pending requests now that the elevator has STOP and its doors are
		// open.
		// It's possible trips can now be assigned to this elevator (case where the
		// elevator reaches its destination)
		if (!this.pendingMakeTrips.isEmpty()) {
			HashSet<MakeTrip> assignedPendingRequests = this.assignPendingRequestsToElevator(elevatorName);
			if (!assignedPendingRequests.isEmpty()) {
				this.consoleOutput("The following pending trip requests have been assigned to " + elevatorName + "  : "
						+ assignedPendingRequests);
			}
		}

		// Send notice to floor that elevator has STOP and doors are open
		this.consoleOutput(RequestEvent.SENT, "Floor " + String.valueOf(Monitor.getElevatorFloorLocation()),
				"Elevator " + elevatorName + " has arrived and doors are OPEN.");
		this.sendToServer(
				new ElevatorArrivalRequest(elevatorName, String.valueOf(Monitor.getElevatorFloorLocation()),
						Monitor.getQueueDirection()),
				this.hostByFloorName.get(String.valueOf(Monitor.getElevatorFloorLocation())),
				this.portsByFloorName.get(String.valueOf(Monitor.getElevatorFloorLocation())));

		// Send a wait at floor command to the elevator - this is to simulate both
		// passengers leaving and entering the elevator
		this.consoleOutput(RequestEvent.SENT, elevatorName, "Wait at floor.");
		this.sendToServer(new ElevatorWaitRequest(elevatorName), this.hostByElevatorName.get(elevatorName),
				this.portsByElevatorName.get(elevatorName));
	}

	/**
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorWaitComplete(String elevatorName) {
		// Get Monitor for the elevator.
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		// If the Monitor is waiting for a destination request (Elevator is at a
		// pickup floor and is awaiting for the destination request
		// Continue to wait until the destination request has been received. Send
		// another ElevatorWaitRequest to the elevator.
		// TODO This event should be allowed to be late.
		if (Monitor.isWaitingForDestinationRequest()) {
			this.consoleOutput("Elevator " + elevatorName + ": is at Floor "
					+ Monitor.getElevatorFloorLocation()
					+ " for a pickup, has completed waiting for passengers however has not received a destination request yet.");

			// Send a wait at floor command to the elevator
			this.consoleOutput(RequestEvent.SENT, elevatorName, "Continue to wait at floor...");
			this.sendToServer(new ElevatorWaitRequest(elevatorName), this.hostByElevatorName.get(elevatorName),
					this.portsByElevatorName.get(elevatorName));

			// Are there still more floors to visit? If so then send an ElevatorDoorRequest
			// to close it's doors.
		} else if (!Monitor.isTripQueueEmpty()) {
			this.consoleOutput("There are more floors to visit for this elevator " + elevatorName);

			this.consoleOutput(RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorCurrentDoorStatus.CLOSE),
					this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

			// Monitor the Elevator Move request
			MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
					MonitoredSchedulerEvent.ELEVATOR_CLOSE_DOOR,
					(int) (Monitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
			this.addMonitoredEvent(elevatorName, monitoredEventTimer);

			// Start the monitored event timer
			Thread t = new Thread(monitoredEventTimer);
			t.start();

			// If there are no more floors to visit then need ot determine whether the
			// elevator is on its start floor or not.
			// If on the start floor, wait for the next trip request
			// If not on the start floor, start return trip to the start floor.
		} else {
			Integer currentFloor = Monitor.getElevatorFloorLocation();
			Integer startFloor = Monitor.getElevatorStartingFloorLocation();
			boolean isElevatorOnStartFloor;

			if (currentFloor == startFloor) {
				isElevatorOnStartFloor = true;
			} else {
				isElevatorOnStartFloor = false;
			}

			if (isElevatorOnStartFloor) {
				// Update direction of elevator to STAY
				Monitor.updateElevatorDirection(Direction.STAY);

				this.consoleOutput("There are no available trip requests for " + elevatorName
						+ ", and elevator is already on it's starting floor [" + startFloor
						+ "]. Waiting for next trip request...");
			} else {
				this.consoleOutput("There are no available trip requests for " + elevatorName
						+ ", elevator should return to it's starting floor [" + startFloor + "]");

				this.consoleOutput(RequestEvent.SENT, elevatorName, "Close elevator door.");
				this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorCurrentDoorStatus.CLOSE),
						this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

				// Monitor the Elevator Move request
				MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
						MonitoredSchedulerEvent.ELEVATOR_CLOSE_DOOR,
						(int) (Monitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
				this.addMonitoredEvent(elevatorName, monitoredEventTimer);

				// Start the monitored event timer
				Thread t = new Thread(monitoredEventTimer);
				t.start();
			}
		}
	}

	/**
	 * When the elevator has confirmed that it's door has CLOSE, then determine the
	 * next direction the elevator should go from the Monitor and send a
	 * motor request to the elevator.
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorDoorCLOSE(String elevatorName) {
		// Remove the Door request from monitorElevatorEvents for this elevator
		// 'elevatorName'
		// Presumably this is occurring before the monitoredEventTimer has completed.
		this.removeMonitoredEvent(elevatorName);

		// Get the Monitor for this elevator
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		// Update current elevator door status
		Monitor.updateElevatorDoorStatus(ElevatorCurrentDoorStatus.CLOSE);

		// Get the next direction for this elevator based on the Monitor
		Direction nextDirection = Monitor.getNextElevatorDirection();

		// Update elevator current direction
		Monitor.updateElevatorDirection(nextDirection);

		// send an elevator move event in the next direction it needs to go
		this.sendElevatorMoveEvent(elevatorName, nextDirection);
	}

	/**
	 * Send a motor request to an elevator to go either UP, DOWN, or STAY. If STAY
	 * is specified, this means stop the motor.
	 * 
	 * @param elevatorName
	 * @param direction
	 */
	private void sendElevatorMoveEvent(String elevatorName, Direction direction) {
		Monitor Monitor = this.MonitorByElevatorName.get(elevatorName);

		// Update elevator status to Moving
		Monitor.updateElevatorStatus(ElevatorCurrentStatus.MOVE);
		// Update elevator direction
		Monitor.updateElevatorDirection(direction);

		this.consoleOutput(RequestEvent.SENT, elevatorName, "Move elevator " + direction + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, direction), this.hostByElevatorName.get(elevatorName),
				this.portsByElevatorName.get(elevatorName));

		// Monitor the Elevator Move request
		MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
				MonitoredSchedulerEvent.ELEVATOR_MOVE,
				(int) (Monitor.getTimeBetweenFloors() * this.monitoredSchedulerDelayFactor));
		this.addMonitoredEvent(elevatorName, monitoredEventTimer);

		// Start the monitored event timer
		Thread t = new Thread(monitoredEventTimer);
		t.start();
	}

	/**
	 * Attempt to assign any pending requests to an elevator. If the elevator queue
	 * is empty, assigns it automatically as a first trip request. Then attempt to
	 * see if any subsequent pending trips can be assigned (as an enroute trip to
	 * this elevator).
	 * 
	 * @param elevatorName
	 * @return
	 */
	private HashSet<MakeTrip> assignPendingRequestsToElevator(String elevatorName) {
		Monitor monitor = this.MonitorByElevatorName.get(elevatorName);
		HashSet<MakeTrip> assignedPendingRequests = new HashSet<MakeTrip>();

		// If the elevator has no trips in its queue's, then it should take the first
		// pending request
		if (monitor.isTripQueueEmpty()) {
			MakeTrip firstPriorityPendingRequest = this.pendingMakeTrips.get(0);
			if (monitor.addTripRequest(firstPriorityPendingRequest)) {
				assignedPendingRequests.add(firstPriorityPendingRequest);
				this.pendingMakeTrips.remove(0);
			}
		}

		// Now the elevator, should see if its possible to take any of the other pending
		// trips as en-route trip requests
		Iterator<MakeTrip> iterator = pendingMakeTrips.iterator();
		while (iterator.hasNext()) {
			MakeTrip pendingMakeTrip = iterator.next();
			if (monitor.addTripRequest(pendingMakeTrip)) {
				assignedPendingRequests.add(pendingMakeTrip);
				iterator.remove();
			}
		}
		return assignedPendingRequests;
	}

	/**
	 * Print to console in a specific format.
	 * 
	 * @param output
	 */
	private void consoleOutput(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
				+ this.name + " : " + output);
	}

	/**
	 * Print to console a send/receive event in a specific format.
	 * 
	 * @param event
	 * @param target
	 * @param output
	 */
	private void consoleOutput(RequestEvent event, String target, String output) {
		if (event.equals(RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

	public static void main(String[] args) {
		// This will return a Map of Maps. First key -> elevator Name, Value -> map of
		// all attributes for that elevator (as per config.xml)
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();

		// This will return a Map of Maps. First key -> floor Name, Value -> map of all
		// attributes for that elevator (as per config.xml)
		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();

		// This will return a Map of all attributes for the Scheduler (as per
		// config.xml)
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		// Instantiate the scheduler
		Scheduler scheduler = new Scheduler(schedulerConfiguration.get("name"),
				Integer.parseInt(schedulerConfiguration.get("port")), elevatorConfigurations, floorConfigurations);

		// Spawn and start a new thread for this Scheduler
		Thread schedulerThread = new Thread(scheduler, schedulerConfiguration.get("name"));
		// ElevatorFrame frame = new
		// ElevatorFrame(scheduler.MonitorByElevatorName);
		schedulerThread.start();

		// Sleep for 2.5 minutes to allow for simulation to complete. Then computer
		// Scheduler's average response times
		try {
			Thread.sleep(150000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		scheduler.displaySchedulerResponseTimes();

	}
}

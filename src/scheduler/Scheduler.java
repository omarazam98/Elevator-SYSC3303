package scheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import enums.SystemEnumTypes;
import enums.SystemEnumTypes.MonitoredSchedulerEvent;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.FloorButtonRequest;
import requests.Request;
import server.Server;

/**
 * This class deals with the scheduler system and is responsible for creating
 * instance of it.
 * 
 * It ensure all trip requests are auctioned in a timely manner, ensuring none
 * of them wait indefinitely. The scheduler maintains elevator subsystem state
 * at any given time, this includes: - Elevator locations, directions, status,
 * doors
 *
 * It schedules events required for the complete system. The scheduler is
 * responsible for: - maintaining queue's for trip requests received for
 * elevators - ordering and sorting of elevators operations including: motor
 * state, lamps state, doors state
 */
public class Scheduler implements Runnable, ElevatorEvents {

	private String name;
	private Server server;
	private Thread serverThread;
	private Queue<Request> eventsQueue;
	private boolean debug = false;
	// key will be elevator name and the value will be the port number on which it
	// will operate on
	private HashMap<String, Integer> portsByElevatorName;
	// key will be the current floor number and value will be the port number on
	// which it will operate on
	private HashMap<String, Integer> portsByFloorName;
	// key will be the elevator name and the value will be the monitor of the
	// current elevator
	private HashMap<String, Monitor> elevatorMonitorByElevatorName;
	// will contain a list of requests in queue currently
	private ArrayList<MakeTrip> pendingTripRequests;
	private HashMap<Class<?>, ArrayList<Double>> eventElapsedTimes;
	private HashMap<String, MonitoredEventTimer> monitoredSchedulerEvents; // key -> subsystemName, value ->
																			// monitoredEventTimer
	private HashMap<String, String> hostByElevatorName;
	private HashMap<String, String> hostByFloorName;
	private final double monitoredSchedulerDelayFactor = 1.25;

	/**
	 * The constructor
	 * 
	 * @param name                  the elevator name
	 * @param port                  the port number
	 * @param elevatorConfiguration the elevator configuration
	 * @param floorConfigurations   the floor configuration
	 */
	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.elevatorMonitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingTripRequests = new ArrayList<MakeTrip>();

		this.monitoredSchedulerEvents = new HashMap<String, MonitoredEventTimer>();
		this.hostByElevatorName = new HashMap<String, String>();
		this.hostByFloorName = new HashMap<String, String>();
		this.eventElapsedTimes = new HashMap<Class<?>, ArrayList<Double>>();

		this.init(elevatorConfiguration, floorConfigurations);

		// Creating thread for the current instance of ElevatorSubsystem which will hold
		// an instance of server
		// When a request will be made, it will be added to the eventsQueue of the
		// current ElevatorSubsystem instance.
		this.server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	/**
	 * Initializing the data structures that are being used for the elevator state
	 * monitoring by the scheduler
	 * 
	 * @param elevatorConfiguration the elevator configuration
	 * @param floorConfigurations   the floor configuration
	 */
	public void init(HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		// Initialize data structures for elevators
		for (String elevatorName : elevatorConfiguration.keySet()) {
			HashMap<String, String> config = elevatorConfiguration.get(elevatorName);

			this.portsByElevatorName.put(elevatorName, Integer.parseInt(config.get("port")));
			this.hostByElevatorName.put(elevatorName, config.get("host"));

			// Initialize elevatorMonitors for each elevator
			this.elevatorMonitorByElevatorName.put(elevatorName,
					new Monitor(elevatorName, Integer.parseInt(config.get("startFloor")),
							Integer.parseInt(config.get("startFloor")), SystemEnumTypes.Direction.STAY,
							SystemEnumTypes.ElevatorCurrentStatus.STOP, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN,
							floorConfigurations.size(), Integer.parseInt(config.get("timeBetweenFloors")),
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
	 * Add an event for the queue.
	 */
	public synchronized void receiveEvent(Request request) {
		eventsQueue.add(request);
		this.notifyAll();
	}

	@Override
	/**
	 * Get next event from the queue.
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
			Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(subsystemName);

			// If an elevator response has not been received for an ELEVATOR_MOVE
			// monitoredEvent, then set the Elevator as OUT_OF_SERVICE
			elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE);
			this.toString("[RESPONSE NOT RECEIVED FROM " + subsystemName
					+ "] Expected floor arrival notice. Elevator stuck between floors. " + subsystemName
					+ " is OUT OF SERVICE");

			// Rescheduled any pending trips (that have not yet begun, in other words,
			// pickup hasn't occurred) from this elevator.
			ArrayList<MakeTrip> reassignableTripRequests = elevatorMonitor.unassignPendingTripRequests();
			for (MakeTrip tripRequest : reassignableTripRequests) {
				this.toString("Reassigning pending trip request " + tripRequest + " from " + subsystemName + "...");
				this.eventTripRequestReceived(tripRequest);
			}
			break;
		case ELEVATOR_OPEN_DOOR:
			// Resend elevator door open
			this.toString("[RESPONSE NOT RECEIVED FROM " + subsystemName + "] Expected door open confirmation");
			this.toString(SystemEnumTypes.RequestEvent.SENT, subsystemName, "Open elevator door.");
			this.sendToServer(new ElevatorDoorRequest(subsystemName, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN),
					this.hostByElevatorName.get(subsystemName), this.portsByElevatorName.get(subsystemName));
			break;
		case ELEVATOR_CLOSE_DOOR:
			// resend elevator door close
			this.toString("[RESPONSE NOT RECEIVED FROM " + subsystemName + "] Expected door closed confirmation");
			this.toString(SystemEnumTypes.RequestEvent.SENT, subsystemName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(subsystemName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
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
	 * returns the name of the scheduler
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public void run() {
		this.toString("Scheduler is up and is waiting for trip requests...");
		while (true) {
			this.handleEvent(this.getNextEvent());
		}
	}

	/**
	 * Determines the type of Request and call the event handler method for the
	 * request.
	 * 
	 * @param event the incoming request
	 */
	private void handleEvent(Request event) {
		if (event instanceof FloorButtonRequest) {
			FloorButtonRequest request = (FloorButtonRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, "Floor " + request.getFloorName(),
					"A trip request has been made from floor " + request.getFloorName() + " to go to floor:  "
							+ request.getDirection() + ".");
			this.eventTripRequestReceived(Integer.parseInt(request.getFloorName()), request.getDirection());
		} else if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;

			// Only handle this event if the elevatorStatus is not OUT OF SERVICE
			if (this.elevatorMonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator arrival notice at floor " + request.getFloorName() + ".");
				this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator arrival notice at floor " + request.getFloorName() + ".");
			}
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"State of the elevator door is: " + request.getRequestAction() + ".");
			if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator door is " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;
			// Only handle this event if the elevatorStatus is not OUT OF SERVICE
			if (this.elevatorMonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE) {

				if (request.getRequestAction() == SystemEnumTypes.Direction.STAY) {
					this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
							"Elevator is in free state.");
					this.eventElevatorStopped(request.getElevatorName());
				} else {
					this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
							"Elevator is in the moving state " + request.getRequestAction() + ".");
				}
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Elevator is " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;

			// Only handle this event if the elevatorStatus is not OUT OF SERVICE
			if (this.elevatorMonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Destination request from pickup floor: " + request.getPickupFloor() + " to destination floor: "
								+ request.getDestinationFloor());
				this.eventElevatorDestinationRequest(request.getElevatorName(),
						Integer.parseInt(request.getPickupFloor()), Integer.parseInt(request.getDestinationFloor()));
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"[OUT OF SERVICE - Ignored] Destination request from pickup floor: " + request.getPickupFloor()
								+ " to destination floor: " + request.getDestinationFloor());
			}
		} else if (event instanceof ElevatorWaitRequest) {
			ElevatorWaitRequest request = (ElevatorWaitRequest) event;

			// Only handle this event if the elevatorStatus is not OUT OF SERVICE
			if (this.elevatorMonitorByElevatorName.get(request.getElevatorName())
					.getElevatorStatus() != SystemEnumTypes.ElevatorCurrentStatus.OUT_OF_SERVICE) {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator has completed its wait.");
				this.eventElevatorWaitComplete(request.getElevatorName());
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
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
	 * Sending request to the ports via the server
	 * 
	 * @param request      the request to be sent to the server
	 * @param elevatorName name of the current elevator
	 */
	private void sendToServer(Request request, String host, int port) {
		this.server.send(request, host, port);
	}

	
	/**
	 * This method attempts to assign an incoming tripRequest to one of the
	 * elevators. The first preference is to assign the tripRequest to an in service
	 * elevator, if the trip is en route. If there are no en-route options, attempt
	 * to find an idle elevator to service the tripRequest. If this is not possible
	 * then the tripRequest will be put in a pending queue.
	 * 
	 * @param pickupFloorNumber
	 * @param destinationFloorNumber
	 * @param direction
	 */
	private void eventTripRequestReceived(int pickupFloorNumber, SystemEnumTypes.Direction direction) {
		// Create a TripRequest object
		MakeTrip tripRequest = new MakeTrip(pickupFloorNumber, direction);
		this.eventTripRequestReceived(tripRequest);
	}

	private void eventTripRequestReceived(MakeTrip tripRequest) {
		Monitor elevatorMonitor = this.planningSystem(tripRequest);

		// If an Elevator has been selected for this trip request, determine the next
		// action required for the elevator.
		// 1 - If the elevator is stopped and idle, then
		// i - if it is at the floor of the trip request, then the floor must be
		// advised, and the elevator needs to be advised to wait for passengers to load
		// ii - otherwise, send a door closed event, when the door closed event is
		// confirmed, the scheduler will determine the next direction for the elevator.
		if (elevatorMonitor != null) {
			elevatorMonitor.addTripRequest(tripRequest);
			this.toString(
					"Trip request " + tripRequest + " was assigned to " + elevatorMonitor.getElevatorName() + ".");
			// If the elevator is currently stopped and IDLE, then a door close event must
			// be sent
			// within a short time (before the elevator door is closed).
			if ((elevatorMonitor.getElevatorStatus() == SystemEnumTypes.ElevatorCurrentStatus.STOP)
					&& (elevatorMonitor.getElevatorDirection() == SystemEnumTypes.Direction.STAY)) {

				// Since the elevator is stopped and idle, check if it is at the floor of the
				// trip request
				// If so, the scheduler must then advise the floor and advise elevator to wait
				// for passengers to load.
				if (elevatorMonitor.getElevatorFloorLocation() == tripRequest.getUserinitalLocation()) {
					// Send event to floor that elevator is ready to accept passengers - this will
					// ensure the floor sends the corresponding destination request to the elevator
					// - pushing things forward
					this.toString(SystemEnumTypes.RequestEvent.SENT, "FLOOR " + tripRequest.getUserinitalLocation(),
							"Elevator " + elevatorMonitor.getElevatorName() + " has arrived for a pickup/dropoff.");
					this.sendToServer(
							new ElevatorArrivalRequest(elevatorMonitor.getElevatorName(),
									String.valueOf(tripRequest.getUserinitalLocation()), elevatorMonitor.getQueueDirection()),
							this.hostByFloorName.get(tripRequest.getUserinitalLocation()),
							this.portsByFloorName.get(String.valueOf(tripRequest.getUserinitalLocation())));

					// Only if this was the first trip added to the queue at this stop, send an
					// elevator wait arrival command, this is to handle the case where an elevator
					// is stopped and idle and receives
					// two requests for trips before the elevator is done waiting from the first
					// request (as the elevator state would still be STOPPED and IDLE until the wait
					// is over).
					if (elevatorMonitor.getQueueLength() == 1) {
						// Send a wait at floor command to the elevator - this is to simulate both
						// passengers leaving and entering the elevator
						this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(),
								"Wait at floor for passengers to load.");
						this.sendToServer(new ElevatorWaitRequest(elevatorMonitor.getElevatorName()),
								this.hostByElevatorName.get(elevatorMonitor.getElevatorName()),
								this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
						return;
					}
				}

				// Otherwise, since the elevator is idle and stopped but not at the right floor,
				// it must close its door to start moving in the right direction.
				// Only if this was the first trip added to the queue at this stop, send an
				// elevator close door command, this is to handle the case where an elevator is
				// stopped and idle and receives
				// two requests for trips before the elevator has closed its door from the
				// previous request (as the elevator state would still be STOPPED and IDLE until
				// the wait is over).
				if (elevatorMonitor.getQueueLength() == 1) {
					this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(), "Close elevator door.");
					this.sendToServer(
							new ElevatorDoorRequest(elevatorMonitor.getElevatorName(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
							this.hostByElevatorName.get(elevatorMonitor.getElevatorName()),
							this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
				}
			}
		} else {
			// Add this tripRequest to the pendingTripRequests queue
			this.pendingTripRequests.add(tripRequest);
			this.toString("Trip request " + tripRequest
					+ " was unable to be assigned immediately. It has been added to pending requests "
					+ this.pendingTripRequests + ".");
		}
	}

	/**
	 * The method responsible for the planing the trip
	 * 
	 * @param tripRequest
	 * @return
	 */
	private Monitor planningSystem(MakeTrip tripRequest) {
		Monitor closestMonitor = null;
		Integer closestElevatorTime = null;

		// going through all the elevators to determine if they can handle the trip
		// request,
		// and which will be most optimal.
		for (String elevatorName : elevatorMonitorByElevatorName.keySet()) {
			boolean currentElevatorIsMoreFavourable = false;

			Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
			Integer estimatedElevatorPickupTime = elevatorMonitor.estimatePickupTime(tripRequest);

			// If the estimateElevatorPickupTime for this elevator is null, skip the
			// elevator
			if (estimatedElevatorPickupTime == null) {
				continue;
			}

			// If there is no elevator available, set the current elevator as
			// closestElevator
			if (closestElevatorTime == null) {
				currentElevatorIsMoreFavourable = true;
			} else {
				// The comparison between the current elevator being evaluated and the
				// closestElevator depends on whether the closestElevator is STAY or in-service
				if (closestMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
					if (elevatorMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
						// If current elevator is available and has a quicker
						// estimated pickup time, this elevator is more favorable for the trip
						if (estimatedElevatorPickupTime < closestElevatorTime) {
							currentElevatorIsMoreFavourable = true;
						}
					} else {
						currentElevatorIsMoreFavourable = true;
					}
					// In the case where the closestElevator is STAY
				} else {
					// In the case where the current elevator being evaluated is available
					if (elevatorMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
						// do nothing
					} else {
						// If the current elevator being evaluated is STAY and has a quicker estimated
						// pickup time then this elevator is more favorable for this trip request
						if (estimatedElevatorPickupTime < closestElevatorTime) {
							currentElevatorIsMoreFavourable = true;
						}
					}
				}
			}

			// Replace closest Elevator with current Elevator if any of the conditions above
			// have been met.
			if (currentElevatorIsMoreFavourable) {
				closestMonitor = elevatorMonitor;
				closestElevatorTime = estimatedElevatorPickupTime;
			}
		}

		return closestMonitor;
	}

	/**
	 * The elevator destination request event
	 * 
	 * @param elevatorName     the name
	 * @param pickupFloor      the pickup floor
	 * @param destinationFloor the destination floor
	 */
	private void eventElevatorDestinationRequest(String elevatorName, Integer pickupFloor, Integer destinationFloor) {
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		if (elevatorMonitor.addDestination(pickupFloor, destinationFloor)) {
			this.toString(
					"Destination [" + destinationFloor + "] was successfully added to " + elevatorName + "'s queue.");
		} else {
			this.toString("Destination [" + destinationFloor + "] was not successfully added to " + elevatorName
					+ "'s queue. Abandoning destination request.");
		}
	}

	/**
	 * This method is responsible for updating the monitor and checking if the
	 * elevator needs to stop.
	 * 
	 * @param elevatorName the floor name
	 * @param floorNumber  the floor number
	 */
	private void eventElevatorArrivalNotice(String elevatorName, int floorNumber) {
		// Create an instance of the elevator monitor
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		// Update the monitor with the new floor of the elevator
		elevatorMonitor.updateElevatorFloorLocation(floorNumber);
		// Check if this elevator needs to stop at the current floor
		if (elevatorMonitor.isStopRequired(floorNumber)) {
			this.toString("Stop is required for " + elevatorName + " at floor " + floorNumber);
			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Stop elevator.");
			this.sendToServer(new ElevatorMotorRequest(elevatorName, SystemEnumTypes.Direction.STAY),
					this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));
		} else {
			this.toString("Stop is not required for " + elevatorName + " at floor " + floorNumber);
			SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
			this.sendElevatorMoveEvent(elevatorName, nextDirection);
		}
	}

	/**
	 * This method is responsible for updating the monitor when an elevator stops.
	 * 
	 * @param elevatorName the name of the elevator
	 */
	private void eventElevatorStopped(String elevatorName) {
		// creating an instance monitor
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		// change the elevator status to stop
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);

		// The monitor is updated when the stop occurs
		HashSet<MakeTrip> completedTrips = elevatorMonitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.toString(
					"The following trips have been completed at this stop by " + elevatorName + ":" + completedTrips);
		}

		// Send an open door event to the elevator
		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN),
				this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

		// Monitor the Elevator Move request
		MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
				MonitoredSchedulerEvent.ELEVATOR_OPEN_DOOR,
				(int) (elevatorMonitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
		this.addMonitoredEvent(elevatorName, monitoredEventTimer);

		// Start the monitored event timer
		Thread t = new Thread(monitoredEventTimer);
		t.start();

	}

	/**
	 * This method determines if the current elevator has more trips once a
	 * notification about the elevator door opening has been received
	 * 
	 * @param elevatorName the name of the elevator
	 */
	private void eventElevatorDoorOpened(String elevatorName) {
		// The monitor for the elevator
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		// Updating the elevator door status
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);

		// Checking pending requests for the current elevator once it has stopped and
		// the doors are opened.
		if (!this.pendingTripRequests.isEmpty()) {
			HashSet<MakeTrip> assignedPendingRequests = this.assignPendingRequestsToElevator(elevatorName);
			if (!assignedPendingRequests.isEmpty()) {
				this.toString("The following pending trip requests have been assigned to " + elevatorName + "  : "
						+ assignedPendingRequests);
			}
		}

		// Send notification to floor that elevator has stopped and doors are open
		this.toString(SystemEnumTypes.RequestEvent.SENT,
				"Floor " + String.valueOf(elevatorMonitor.getElevatorFloorLocation()),
				"Elevator " + elevatorName + " has arrived and doors are opened.");
		this.sendToServer(
				new ElevatorArrivalRequest(elevatorName, String.valueOf(elevatorMonitor.getElevatorFloorLocation()),
						elevatorMonitor.getQueueDirection()),
				this.hostByFloorName.get(String.valueOf(elevatorMonitor.getElevatorFloorLocation())),
				this.portsByFloorName.get(String.valueOf(elevatorMonitor.getElevatorFloorLocation())));

		// Update the elevator to wait at a floor
		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Wait at floor.");
		this.sendToServer(new ElevatorWaitRequest(elevatorName), this.hostByElevatorName.get(elevatorName),
				this.portsByElevatorName.get(elevatorName));
	}

	/**
	 * This ethod checks if the wait for the current floor is complete or not
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorWaitComplete(String elevatorName) {
		// Get elevatorMonitor for the elevator.
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		if (elevatorMonitor.isWaitingForDestinationRequest()) {
			this.toString("Elevator " + elevatorName + ": is at Floor " + elevatorMonitor.getElevatorFloorLocation()
					+ " for a pickup, has completed waiting for passengers however has not received a destination request yet.");

			// Update the elevator to wait
			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Continue to wait at floor...");
			this.sendToServer(new ElevatorWaitRequest(elevatorName), this.hostByElevatorName.get(elevatorName),
					this.portsByElevatorName.get(elevatorName));

			// If there are floors to visit, send the ElevatorDoorRequest
			// to close doors
		} else if (!elevatorMonitor.isTripQueueEmpty()) {
			this.toString("There are more floors to visit for this elevator " + elevatorName);

			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
					this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

			// Monitor the Elevator Move request
			MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
					MonitoredSchedulerEvent.ELEVATOR_CLOSE_DOOR,
					(int) (elevatorMonitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
			this.addMonitoredEvent(elevatorName, monitoredEventTimer);

			// Start the monitored event timer
			Thread t = new Thread(monitoredEventTimer);
			t.start();
			// If there are no more floors to visit, determine whether the
			// elevator is on its start floor or not.
		} else {
			Integer currentFloor = elevatorMonitor.getElevatorFloorLocation();
			Integer startFloor = elevatorMonitor.getElevatorStartingFloorLocation();
			boolean isElevatorOnStartFloor;

			if (currentFloor == startFloor) {
				isElevatorOnStartFloor = true;
			} else {
				isElevatorOnStartFloor = false;
			}

			if (isElevatorOnStartFloor) {
				// Update direction of elevator to STAY
				elevatorMonitor.updateElevatorDirection(SystemEnumTypes.Direction.STAY);

				this.toString("There are no available trip requests for " + elevatorName
						+ ", and elevator is already on it's starting floor [" + startFloor
						+ "]. Waiting for next trip request...");
			} else {
				this.toString("There are no available trip requests for " + elevatorName
						+ ", elevator should return to it's starting floor [" + startFloor + "]");

				this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.hostByElevatorName.get(elevatorName), this.portsByElevatorName.get(elevatorName));

				// Monitor the Elevator Move request
				MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
						MonitoredSchedulerEvent.ELEVATOR_CLOSE_DOOR,
						(int) (elevatorMonitor.getDoorOperationTime() * this.monitoredSchedulerDelayFactor));
				this.addMonitoredEvent(elevatorName, monitoredEventTimer);

				// Start the monitored event timer
				Thread t = new Thread(monitoredEventTimer);
				t.start();
			}
		}
	}

	/**
	 * When the elevator has confirmed that it's door are closed, then determine the
	 * next direction the elevator should go from the Monitor and send a motor
	 * request to the elevator.
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorDoorClosed(String elevatorName) {
		// Remove the Door request from monitorElevatorEvents for this elevator
		// 'elevatorName'
		// Presumably this is occurring before the monitoredEventTimer has completed.
		this.removeMonitoredEvent(elevatorName);

		// Get the elevatorMonitor for this elevator
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update current elevator door status
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);

		// Get the next direction for this elevator based on the elevatorMonitor
		SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();

		// Update elevator current direction
		elevatorMonitor.updateElevatorDirection(nextDirection);

		// send an elevator move event in the next direction it needs to go
		this.sendElevatorMoveEvent(elevatorName, nextDirection);
	}

	private void sendElevatorMoveEvent(String elevatorName, SystemEnumTypes.Direction direction) {
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update elevator status to Moving
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		// Update elevator direction
		elevatorMonitor.updateElevatorDirection(direction);

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Move elevator " + direction + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, direction), this.hostByElevatorName.get(elevatorName),
				this.portsByElevatorName.get(elevatorName));

		// Monitor the Elevator Move request
		MonitoredEventTimer monitoredEventTimer = new MonitoredEventTimer(this, elevatorName,
				MonitoredSchedulerEvent.ELEVATOR_MOVE,
				(int) (elevatorMonitor.getTimeBetweenFloors() * this.monitoredSchedulerDelayFactor));
		this.addMonitoredEvent(elevatorName, monitoredEventTimer);

		// Start the monitored event timer
		Thread t = new Thread(monitoredEventTimer);
		t.start();
	}

	/**
	 * Attempt to assign any pending requests to an elevator. If the elevator queue
	 * is empty, assigns it automatically as a first trip request. Then attempt to
	 * see if any subsequent pending trips can be assigned
	 * 
	 * @param elevatorName the name of the elevator
	 */
	private HashSet<MakeTrip> assignPendingRequestsToElevator(String elevatorName) {
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		HashSet<MakeTrip> assignedPendingRequests = new HashSet<MakeTrip>();

		// check if the elevator has any pending requests, if not, then assign it the
		// the first one in the list of pending requests
		if (elevatorMonitor.isTripQueueEmpty()) {
			MakeTrip firstPriorityPendingRequest = this.pendingTripRequests.get(0);
			if (elevatorMonitor.addTripRequest(firstPriorityPendingRequest)) {
				assignedPendingRequests.add(firstPriorityPendingRequest);
				this.pendingTripRequests.remove(0);
			}
		}

		// Now the elevator, should see if its possible to take any of the other pending
		// trips as re-route trip requests
		Iterator<MakeTrip> iterator = pendingTripRequests.iterator();
		while (iterator.hasNext()) {
			MakeTrip pendingTripRequest = iterator.next();
			if (elevatorMonitor.addTripRequest(pendingTripRequest)) {
				assignedPendingRequests.add(pendingTripRequest);
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
	private void toString(String output) {
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
	private void toString(SystemEnumTypes.RequestEvent event, String target, String output) {
		if (event.equals(SystemEnumTypes.RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(SystemEnumTypes.RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

	public static void main(String[] args) {
		// map of maps where key of the parent map is elevator name, value is the
		// attributes of the elevator defined in the config.xml file
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();
		// map of maps where key of the parent map is the floor name and value is the
		// elevator attributes in the config.xml file
		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();
		// scheduler attributes from the config.xml
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		// creating a thread for the scheduler
		Scheduler scheduler = new Scheduler(schedulerConfiguration.get("name"),
				Integer.parseInt(schedulerConfiguration.get("port")), elevatorConfigurations, floorConfigurations);

		// Starting a new thread for this Scheduler
		Thread schedulerThread = new Thread(scheduler, schedulerConfiguration.get("name"));
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

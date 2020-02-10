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
 * elevator system. The scheduler is responsible for: - assigning and
 * maintaining queue's for trip requests received for each elevator - sequences
 * for each elevators operations including: motor on/off up/down, lamps on/off,
 * doors open/close A primary goal of the scheduler is to ensure all trip
 * requests are actioned in a timely manner, ensuring none wait indefinitely.
 * The scheduler maintains a complete state of the elevator subsystem at any
 * given time, this includes: - state of the elevators locations, directions,
 * status (moving/stopped), doors (opened/closed)
 *
 */
public class Scheduler implements Runnable, ElevatorEvents {

	private String name;
	private Server server;
	private Thread serverThread;
	private Queue<Request> eventsQueue;
	private boolean debug = false;
	private HashMap<String, Integer> portsByElevatorName; // key -> elevator name, value -> port number
	private HashMap<String, Integer> portsByFloorName; // key -> floor number, value -> port number
	private HashMap<String, Monitor> elevatorMonitorByElevatorName; // key -> elevator name, value -> elevator monitor
	private ArrayList<MakeTrip> pendingTripRequests;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.elevatorMonitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingTripRequests = new ArrayList<MakeTrip>();

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

			// Initialize elevatorMonitors for each elevator
			this.elevatorMonitorByElevatorName.put(elevatorName,
					new Monitor(elevatorName, Integer.parseInt(config.get("startFloor")),
							Integer.parseInt(config.get("startFloor")), SystemEnumTypes.Direction.STAY,
							SystemEnumTypes.ElevatorCurrentStatus.STOP, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN,
							floorConfigurations.size()));

		}

		// Initialize data structures for floors
		for (String floorName : floorConfigurations.keySet()) {
			HashMap<String, String> config = floorConfigurations.get(floorName);

			this.portsByFloorName.put(floorName, Integer.parseInt(config.get("port")));
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

			this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, "Floor " + request.getFloorName(),
					"Trip request from floor " + request.getFloorName() + " in direction " + request.getDirection()
							+ ".");
			this.eventTripRequestReceived(Integer.parseInt(request.getFloorName()), request.getDirection());
		} else if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;

			this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator arrival notice at floor " + request.getFloorName() + ".");
			this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;

			this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator door is " + request.getRequestAction() + ".");
			if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;

			if (request.getRequestAction() == SystemEnumTypes.Direction.STAY) {
				this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator has stopped.");
				this.eventElevatorStopped(request.getElevatorName());
			} else {
				this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is moving " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;

			this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Destination request from pickup floor: " + request.getPickupFloor() + " to destination floor: "
							+ request.getDestinationFloor());
			this.eventElevatorDestinationRequest(request.getElevatorName(), Integer.parseInt(request.getPickupFloor()),
					Integer.parseInt(request.getDestinationFloor()));
		} else if (event instanceof ElevatorWaitRequest) {
			ElevatorWaitRequest request = (ElevatorWaitRequest) event;

			this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator has completed its wait.");
			this.eventElevatorWaitComplete(request.getElevatorName());
		}
	}

	/**
	 * Send a request to port using this object's server.
	 * 
	 * @param request
	 * @param elevatorName
	 */
	private void sendToServer(Request request, int port) {
		try {
			this.server.send(request, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
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
		// Create a MakeTrip object
		MakeTrip tripRequest = new MakeTrip(pickupFloorNumber, direction);

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
			this.consoleOutput(
					"Trip request " + tripRequest + " was assigned to " + elevatorMonitor.getElevatorName() + ".");
			// If the elevator is currently stopped and STAY, then a door close event must
			// be sent.
			if ((elevatorMonitor.getElevatorStatus() == SystemEnumTypes.ElevatorCurrentStatus.STOP)
					&& (elevatorMonitor.getElevatorDirection() == SystemEnumTypes.Direction.STAY)) {

				// Since the elevator is stopped and idle, check if it is at the floor of the
				// trip request
				// If so, the scheduler must then advise the floor and advise elevator to wait
				// for passengers to load.
				if (elevatorMonitor.getElevatorFloorLocation() == tripRequest.getUserinitalLocation()) {
					// Send event to floor that elevator is ready to accept passengers - this will
					// ensure the floor sends a destination request to the elevator - pushing things
					// forward
					this.consoleOutput(SystemEnumTypes.RequestEvent.SENT,
							"FLOOR " + tripRequest.getUserinitalLocation(),
							"Elevator " + elevatorMonitor.getElevatorName() + " has arrived for a pickup/dropoff.");
					this.sendToServer(new ElevatorArrivalRequest(elevatorMonitor.getElevatorName(),
							String.valueOf(tripRequest.getUserinitalLocation()), elevatorMonitor.getQueueDirection()),
							this.portsByFloorName.get(String.valueOf(tripRequest.getUserinitalLocation())));

					// Send a wait at floor command to the elevator - this is to simulate both
					// passengers leaving and entering the elevator
					this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(),
							"Wait at floor for passengers to load.");
					this.sendToServer(new ElevatorWaitRequest(elevatorMonitor.getElevatorName()),
							this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
					return;
				}

				// Otherwise, since the elevator is idle and stopped but not at the right floor,
				// it must close its door to start moving in the right direction.
				this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(),
						"Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorMonitor.getElevatorName(),
								SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
			}
		} else {
			// Add this tripRequest to the pendingTripRequests queue
			this.pendingTripRequests.add(tripRequest);
			this.consoleOutput("Trip request " + tripRequest
					+ " was unable to be assigned immediately. It has been added to pending requests "
					+ this.pendingTripRequests + ".");
		}
	}

	/**
	 * 
	 * @param tripRequest
	 * @return
	 */
	private Monitor planningSystem(MakeTrip tripRequest) {
		Monitor closestMonitor = null;
		Integer closestElevatorTime = null;

		// Iterate through all elevators to determine whether there is an eligible
		// elevator to handle this trip request,
		// and which would be most optimal.
		for (String elevatorName : elevatorMonitorByElevatorName.keySet()) {
			boolean currentElevatorIsMoreFavourable = false;

			Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
			Integer estimatedElevatorPickupTime = elevatorMonitor.estimatePickupTime(tripRequest);

			// If the estimateElevatorPickupTime for this elevator is null, then this
			// elevator cannot accommodate this tripRequest at this time
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
				// closestElevator depends on whether the closestElevator is Idle or in-service
				if (closestMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
					// The comparison between the current elevator and the closestElevator also
					// depends on whether the current elevator being evaluated is Idle or in-service
					// In the case where the current elevator being evaluated is in-service
					if (elevatorMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
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
					if (elevatorMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
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
				closestMonitor = elevatorMonitor;
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
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		if (elevatorMonitor.addDestination(pickupFloor, destinationFloor)) {
			this.consoleOutput(
					"Destination [" + destinationFloor + "] was successfully added to " + elevatorName + "'s queue.");
		} else {
			this.consoleOutput("Destination [" + destinationFloor + "] was not successfully added to " + elevatorName
					+ "'s queue. Abandoning destination request.");
		}
	}

	/**
	 * When an elevator arrives at a floor, update the elevatorMonitor then check if
	 * the elevator needs to stop. If the elevator needs to stop, send a stop
	 * request to the elevator. If the elevator needs to stop because this floor is
	 * a pickup, then a request needs to be sent to the floor to turn off the floor
	 * direction lamp. If not stop is needed, check the elevatorMonitor to determine
	 * the next direction for the elevator and send a motor request in that
	 * direction.
	 * 
	 * @param elevatorName
	 * @param floorNumber
	 */
	private void eventElevatorArrivalNotice(String elevatorName, int floorNumber) {
		// Get the elevatorMonitor for this elevator
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update the elevatorMonitor with the new floor of the elevator
		elevatorMonitor.updateElevatorFloorLocation(floorNumber);

		// Check if this elevator needs to stop at this floor
		if (elevatorMonitor.isStopRequired(floorNumber)) {
			this.consoleOutput("Stop is required for " + elevatorName + " at floor " + floorNumber);
			this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Stop elevator.");
			this.sendToServer(new ElevatorMotorRequest(elevatorName, SystemEnumTypes.Direction.STAY),
					this.portsByElevatorName.get(elevatorName));
		} else {
			this.consoleOutput("Stop is not required for " + elevatorName + " at floor " + floorNumber);
			// The reason we evaluate this direction again, is because in certain
			// circumstances, the direction may change
			// Example, if the elevator is going down to it's starting floor, but a trip
			// request had been assigned to it before it reached its destination, the
			// elevator needs to change directions
			SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
			this.sendElevatorMoveEvent(elevatorName, nextDirection);
		}
	}

	/**
	 * When an elevator stops, take this opportunity to update the elevatorMonitor.
	 * If this stop is a destination for a tripRequest, the corresponding
	 * tripRequest is completed, and the elevatorMonitor needs to be updated to
	 * reflect this If this stop is a pickup for a tripRequest, the tripRequest
	 * needs to remove this pickup from it's queue of pickups. but the tripRequest
	 * still is in service in the elevatorMonitor.
	 * 
	 * When this is completed, send an elevator open door event.
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorStopped(String elevatorName) {
		// Get elevatorMonitor for the elevator.
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update elevator status to Stopped
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);

		// The elevatorMonitor needs to be advised this stop has occurred
		HashSet<MakeTrip> completedTrips = elevatorMonitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.consoleOutput(
					"The following trips have been completed at this stop by " + elevatorName + ":" + completedTrips);
		}

		// Send an open door event to the elevator
		this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN),
				this.portsByElevatorName.get(elevatorName));

	}

	/**
	 * When confirmation has been received that the elevator has opened its doors,
	 * determine whether this elevator has more trips. If the elevatorMonitor is not
	 * empty, then
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorDoorOpened(String elevatorName) {
		// Get elevatorMonitor for the elevator.
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update current elevator door status
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);

		// Checking pending requests now that the elevator has stopped and its doors are
		// open.
		// It's possible trips can now be assigned to this elevator (case where the
		// elevator reaches its destination)
		if (!this.pendingTripRequests.isEmpty()) {
			HashSet<MakeTrip> assignedPendingRequests = this.assignPendingRequestsToElevator(elevatorName);
			if (!assignedPendingRequests.isEmpty()) {
				this.consoleOutput("The following pending trip requests have been assigned to " + elevatorName + "  : "
						+ assignedPendingRequests);
			}
		}

		// Send notice to floor that elevator has stopped and doors are open
		this.consoleOutput(SystemEnumTypes.RequestEvent.SENT,
				"Floor " + String.valueOf(elevatorMonitor.getElevatorFloorLocation()),
				"Elevator " + elevatorName + " has arrived and doors are opened.");
		this.sendToServer(
				new ElevatorArrivalRequest(elevatorName, String.valueOf(elevatorMonitor.getElevatorFloorLocation()),
						elevatorMonitor.getQueueDirection()),
				this.portsByFloorName.get(String.valueOf(elevatorMonitor.getElevatorFloorLocation())));

		// Send a wait at floor command to the elevator - this is to simulate both
		// passengers leaving and entering the elevator
		this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Wait at floor.");
		this.sendToServer(new ElevatorWaitRequest(elevatorName), this.portsByElevatorName.get(elevatorName));
	}

	/**
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorWaitComplete(String elevatorName) {
		// Get elevatorMonitor for the elevator.
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// If the elevatorMonitor is waiting for a destination request (Elevator is at a
		// pickup floor and is awaiting for the destination request
		// Continue to wait until the destination request has been received. Send
		// another ElevatorWaitRequest to the elevator.
		// TODO This event should be allowed to be late.
		if (elevatorMonitor.isWaitingForDestinationRequest()) {
			this.consoleOutput("Elevator " + elevatorName + ": is at Floor "
					+ elevatorMonitor.getElevatorFloorLocation()
					+ " for a pickup, has completed waiting for passengers however has not received a destination request yet.");

			// Send a wait at floor command to the elevator
			this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Continue to wait at floor...");
			this.sendToServer(new ElevatorWaitRequest(elevatorName), this.portsByElevatorName.get(elevatorName));

			// Are there still more floors to visit? If so then send an ElevatorDoorRequest
			// to close it's doors.
		} else if (!elevatorMonitor.isTripQueueEmpty()) {
			this.consoleOutput("There are more floors to visit for this elevator " + elevatorName);

			this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
					this.portsByElevatorName.get(elevatorName));

			// If there are no more floors to visit then need ot determine whether the
			// elevator is on its start floor or not.
			// If on the start floor, wait for the next trip request
			// If not on the start floor, start return trip to the start floor.
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

				this.consoleOutput("There are no available trip requests for " + elevatorName
						+ ", and elevator is already on it's starting floor [" + startFloor
						+ "]. Waiting for next trip request...");
			} else {
				this.consoleOutput("There are no available trip requests for " + elevatorName
						+ ", elevator should return to it's starting floor [" + startFloor + "]");

				this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorName));
			}
		}
	}

	/**
	 * When the elevator has confirmed that it's door has closed, then determine the
	 * next direction the elevator should go from the Monitor and send a motor
	 * request to the elevator.
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorDoorClosed(String elevatorName) {
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

	/**
	 * Send a motor request to an elevator to go either UP, DOWN, or STAY. If STAY
	 * is specified, this means stop the motor.
	 * 
	 * @param elevatorName
	 * @param direction
	 */
	private void sendElevatorMoveEvent(String elevatorName, SystemEnumTypes.Direction direction) {
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);

		// Update elevator status to Moving
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		// Update elevator direction
		elevatorMonitor.updateElevatorDirection(direction);

		this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Move elevator " + direction + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, direction),
				this.portsByElevatorName.get(elevatorName));
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
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		HashSet<MakeTrip> assignedPendingRequests = new HashSet<MakeTrip>();

		// If the elevator has no trips in its queue's, then it should take the first
		// pending request
		if (elevatorMonitor.isTripQueueEmpty()) {
			MakeTrip firstPriorityPendingRequest = this.pendingTripRequests.get(0);
			if (elevatorMonitor.addTripRequest(firstPriorityPendingRequest)) {
				assignedPendingRequests.add(firstPriorityPendingRequest);
				this.pendingTripRequests.remove(0);
			}
		}

		// Now the elevator, should see if its possible to take any of the other pending
		// trips as en-route trip requests
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
	private void consoleOutput(SystemEnumTypes.RequestEvent event, String target, String output) {
		if (event.equals(SystemEnumTypes.RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(SystemEnumTypes.RequestEvent.RECEIVED)) {
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
		schedulerThread.start();
	}
}

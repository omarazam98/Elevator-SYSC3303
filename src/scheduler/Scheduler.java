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

			// Initialize Monitors for each elevator
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

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator has arrived at the floor: " + request.getFloorName() + ".");
			this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"State of the elevator door is: " + request.getRequestAction() + ".");
			if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;

			if (request.getRequestAction() == SystemEnumTypes.Direction.STAY) {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is in free state.");
				this.eventElevatorStopped(request.getElevatorName());
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is in the moving state " + request.getRequestAction() + ".");
			}
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Destination request from pickup floor: " + request.getPickupFloor() + " to destination floor: "
							+ request.getDestinationFloor());
			this.eventElevatorDestinationRequest(request.getElevatorName(), Integer.parseInt(request.getPickupFloor()),
					Integer.parseInt(request.getDestinationFloor()));
		} else if (event instanceof ElevatorWaitRequest) {
			ElevatorWaitRequest request = (ElevatorWaitRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator completed the waiting.");
			this.eventElevatorWaitComplete(request.getElevatorName());
		}
	}

	/**
	 * Sending request to the ports via the server
	 * 
	 * @param request      the request to be sent to the server
	 * @param elevatorName name of the current elevator
	 */
	private void sendToServer(Request request, int port) {
		try {
			this.server.send(request, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is responsible for assigning an incoming tripRequest to one of
	 * the elevators.
	 * 
	 * @param pickupFloorNumber      the pickup floor
	 * @param destinationFloorNumber the destination floor
	 * @param direction              the direction of motion
	 */
	private void eventTripRequestReceived(int pickupFloorNumber, SystemEnumTypes.Direction direction) {
		// Creates an instance of MakeTrip
		MakeTrip tripRequest = new MakeTrip(pickupFloorNumber, direction);

		Monitor elevatorMonitor = this.planningSystem(tripRequest);

		// Determines the next action for the elevator.
		// If the elevator is stopped and STAY:
		// if the request floor, update the floor, and elevator
		// to wait for passengers. Else, send a door closed event, when the door closed
		// event is
		// confirmed, the scheduler will determine the next direction for the elevator.
		if (elevatorMonitor != null) {
			elevatorMonitor.addTripRequest(tripRequest);
			this.toString("Trip " + tripRequest + " was assigned to " + elevatorMonitor.getElevatorName() + ".");
			// If the elevator is currently stopped and STAY, then a door close event must
			// be sent.
			if ((elevatorMonitor.getElevatorStatus() == SystemEnumTypes.ElevatorCurrentStatus.STOP)
					&& (elevatorMonitor.getElevatorDirection() == SystemEnumTypes.Direction.STAY)) {

				// Since the elevator is stopped and STAY, check if the floor is the trip
				// request
				// If so, the scheduler updates the floor and the elevator to wait
				// for passengers to load.
				if (elevatorMonitor.getElevatorFloorLocation() == tripRequest.getUserinitalLocation()) {
					// Update floor that elevator is accepting passengers
					this.toString(SystemEnumTypes.RequestEvent.SENT, "FLOOR " + tripRequest.getUserinitalLocation(),
							"Elevator " + elevatorMonitor.getElevatorName() + " has arrived for a pickup/dropoff.");
					this.sendToServer(new ElevatorArrivalRequest(elevatorMonitor.getElevatorName(),
							String.valueOf(tripRequest.getUserinitalLocation()), elevatorMonitor.getQueueDirection()),
							this.portsByFloorName.get(String.valueOf(tripRequest.getUserinitalLocation())));

					// Send wait command to the elevator to simulate
					// passengers leaving and entering the elevator
					this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(),
							"Wait at floor for passengers to load.");
					this.sendToServer(new ElevatorWaitRequest(elevatorMonitor.getElevatorName()),
							this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
					return;
				}

				// Else since the elevator is STAY and stopped but not at the right floor,
				// it must close its door to start moving in the right direction.
				this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getElevatorName(),
						"Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorMonitor.getElevatorName(),
								SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorMonitor.getElevatorName()));
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
					this.portsByElevatorName.get(elevatorName));
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
				this.portsByElevatorName.get(elevatorName));

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
				this.portsByFloorName.get(String.valueOf(elevatorMonitor.getElevatorFloorLocation())));

		// Update the elevator to wait at a floor
		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Wait at floor.");
		this.sendToServer(new ElevatorWaitRequest(elevatorName), this.portsByElevatorName.get(elevatorName));
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
			this.sendToServer(new ElevatorWaitRequest(elevatorName), this.portsByElevatorName.get(elevatorName));

			// If there are floors to visit, send the ElevatorDoorRequest
			// to close doors
		} else if (!elevatorMonitor.isTripQueueEmpty()) {
			this.toString("There are more floors to visit for this elevator " + elevatorName);

			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
					this.portsByElevatorName.get(elevatorName));

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
						this.portsByElevatorName.get(elevatorName));
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
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		// obtaining the next direction of motion of the elevator form the monitor
		SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
		elevatorMonitor.updateElevatorDirection(nextDirection);
		// sending a request to move the elevator in the direction it is supposed to go
		this.sendElevatorMoveEvent(elevatorName, nextDirection);
	}

	private void sendElevatorMoveEvent(String elevatorName, SystemEnumTypes.Direction direction) {
		Monitor elevatorMonitor = this.elevatorMonitorByElevatorName.get(elevatorName);
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		elevatorMonitor.updateElevatorDirection(direction);

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Move elevator " + direction + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, direction),
				this.portsByElevatorName.get(elevatorName));
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
	}
}

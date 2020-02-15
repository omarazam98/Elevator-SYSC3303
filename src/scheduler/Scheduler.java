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

import scheduler.Monitor;
import scheduler.MakeTrip;
import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import enums.SystemEnumTypes;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.FloorButtonRequest;
import requests.FloorLampRequest;
import requests.Request;
import server.Server;

/**
 * This class deals with the scheduler system and is responsible for creating
 * instance of it.
 * 
 * It ensure all trip requests are made in a timely manner, ensuring none of
 * them wait indefinitely. The scheduler maintains elevator subsystem state at
 * any given time, this includes: - Elevator locations, directions, status,
 * doors
 *
 * It schedules events required for the complete system. The scheduler is
 * responsible for: - maintaining queue's for trip requests received for
 * elevators - ordering and sorting of elevators operations including: motor
 * state, lamps state, doors state
 *
 *
 */
public class Scheduler implements Runnable, ElevatorEvents {

	private String name;
	private Server server;
	private Thread serverThread;
	private Queue<Request> eventsQueue;
	// key will be elevator name and the value will be the port number on which it
	// will operate on
	private HashMap<String, Integer> portsByElevatorName;
	// key will be the current floor number and value will be the port number on
	// which it will operate on
	private HashMap<String, Integer> portsByFloorName;
	// key will be the elevator name and the value will be the monitor of the
	// current elevator
	private HashMap<String, Monitor> monitorByElevatorName;
	// will contain a list of requests in queue currently
	private ArrayList<MakeTrip> pendingTripRequests;
	private boolean debug = false;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.monitorByElevatorName = new HashMap<String, Monitor>();
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
	 * @param elevatorConfiguration
	 * @param floorConfigurations
	 */
	public void init(HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {

		// Initializing data structures for the elevators
		for (String currentElevator : elevatorConfiguration.keySet()) {
			HashMap<String, String> config = elevatorConfiguration.get(currentElevator);
			// reads in the cofing.xml file to get the information regarding the ports
			this.portsByElevatorName.put(currentElevator, Integer.parseInt(config.get("port")));

			// Initializing monitors for each elevator
			this.monitorByElevatorName.put(currentElevator,
					new Monitor(currentElevator, Integer.parseInt(config.get("startFloor")),
							Integer.parseInt(config.get("startFloor")), SystemEnumTypes.Direction.STAY,
							SystemEnumTypes.ElevatorCurrentStatus.STOP, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN,
							floorConfigurations.size()));

		}
		// Initializing data structures for floors
		for (String floorName : floorConfigurations.keySet()) {
			HashMap<String, String> conf = floorConfigurations.get(floorName);

			this.portsByFloorName.put(floorName, Integer.parseInt(conf.get("port")));
		}

	}

	@Override
	public void run() {
		this.toString("Scheduler is waiting for trip requests...");
		while (true) {
			this.eventHandler(this.getNextEvent());
		}

	}

	private void eventHandler(Request event) {
		if (event instanceof FloorButtonRequest) {
			FloorButtonRequest request = (FloorButtonRequest) event;
			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getFloorName(),
					"A trip request has been made from floor " + request.getFloorName() + " to go to floor:  "
							+ request.getDestinationFloor() + ".");
			this.incomingTripRequest(Integer.parseInt(request.getFloorName()), request.getDirection());

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
		} else if (event instanceof ElevatorWaitRequest) {
			ElevatorWaitRequest request = (ElevatorWaitRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator has completed its wait.");
			this.eventElevatorWaitComplete(request.getElevatorName());
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;

			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(),
					"Destination request from pickup floor: " + request.getPickupFloor() + " to destination floor: "
							+ request.getDestinationFloor());
			this.eventElevatorDestinationRequest(request.getElevatorName(), Integer.parseInt(request.getPickupFloor()),
					Integer.parseInt(request.getDestinationFloor()));
		}

	}

	/**
	 * 
	 * @param elevatorName
	 * @param pickupFloor
	 * @param destinationFloor
	 */
	private void eventElevatorDestinationRequest(String elevatorName, Integer pickupFloor, Integer destinationFloor) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		if (elevatorMonitor.addDestination(pickupFloor, destinationFloor)) {
			this.toString(
					"Destination [" + destinationFloor + "] was successfully added to " + elevatorName + "'s queue.");
		} else {
			this.toString("Destination [" + destinationFloor + "] was not successfully added to " + elevatorName
					+ "'s queue. Abandoning destination request.");
		}
	}

	/**
	 * 
	 * @param elevatorName
	 */
	private void eventElevatorWaitComplete(String elevatorName) {
		// Get elevatorMonitor for the elevator.
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// If the elevatorMonitor is waiting for a destination request (Elevator is at a
		// pickup floor and is awaiting for the destination request
		// Continue to wait until the destination request has been received. Send
		// another ElevatorWaitRequest to the elevator.
		// TODO This event should be allowed to be late.
		if (elevatorMonitor.isWaitingForDestinationRequest()) {
			this.toString("Elevator " + elevatorName + ": is at Floor " + elevatorMonitor.getElevatorFloorLocation()
					+ " for a pickup, has completed waiting for passengers however has not received a destination request yet.");

			// Send a wait at floor command to the elevator
			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Continue to wait at floor...");
			this.sendToServer(new ElevatorWaitRequest(elevatorName), this.portsByElevatorName.get(elevatorName));

			// Are there still more floors to visit? If so then send an ElevatorDoorRequest
			// to close it's doors.
		} else if (!elevatorMonitor.isEmpty()) {
			this.toString("There are more floors to visit for this elevator " + elevatorName);

			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
					this.portsByElevatorName.get(elevatorName));

			// If there are no more floors to visit then need to determine whether the
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
				// Update direction of elevator to IDLE
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

	private Monitor planningSystem(MakeTrip tripRequest) {
		Monitor closestElevatorMonitor = null;
		Integer closestElevatorTime = null;

		// Iterate through all elevators to determine whether there is an eligible
		// elevator to handle this trip request,
		// and which would be most optimal.
		for (String elevatorName : monitorByElevatorName.keySet()) {
			boolean currentElevatorIsMoreFavourable = false;

			Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
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
				if (closestElevatorMonitor.getNextElevatorDirection() == SystemEnumTypes.Direction.STAY) {
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
					// In the case where the closestElevator is IDLE
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
						// If the current elevator being evaluated is IDLE and has a quicker estimated
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
				closestElevatorMonitor = elevatorMonitor;
				closestElevatorTime = estimatedElevatorPickupTime;
			}
		}

		return closestElevatorMonitor;
	}

	private void incomingTripRequest(int currentFloorNumber, SystemEnumTypes.Direction direction) {
		// Create a TripRequest object
		MakeTrip tripRequest = new MakeTrip(currentFloorNumber, direction);

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
			this.toString("Trip request " + tripRequest + " was assigned to " + elevatorMonitor.getCurrentElevatorName()
					+ ".");
			// If the elevator is currently stopped and IDLE, then a door close event must
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
					this.toString(SystemEnumTypes.RequestEvent.SENT, "FLOOR " + tripRequest.getUserinitalLocation(),
							"Elevator " + elevatorMonitor.getCurrentElevatorName()
									+ " has arrived for a pickup/dropoff.");
					this.sendToServer(new ElevatorArrivalRequest(elevatorMonitor.getCurrentElevatorName(),
							String.valueOf(tripRequest.getUserinitalLocation()), elevatorMonitor.getQueueDirection()),
							this.portsByFloorName.get(String.valueOf(tripRequest.getUserinitalLocation())));

					this.portsByFloorName.get(String.valueOf(tripRequest.getUserinitalLocation()));

					// Send a wait at floor command to the elevator - this is to simulate both
					// passengers leaving and entering the elevator
					this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getCurrentElevatorName(),
							"Wait at floor for passengers to load.");
					this.sendToServer(new ElevatorWaitRequest(elevatorMonitor.getCurrentElevatorName()),
							this.portsByElevatorName.get(elevatorMonitor.getCurrentElevatorName()));
					return;
				}

				// Otherwise, since the elevator is idle and stopped but not at the right floor,
				// it must close its door to start moving in the right direction.
				this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorMonitor.getCurrentElevatorName(),
						"Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorMonitor.getCurrentElevatorName(),
								SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorMonitor.getCurrentElevatorName()));
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
	 * This method writes to the console in a particular format
	 * 
	 * @param output
	 */
	private void toString(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
				+ this.name + " : " + output);
	}

	/**
	 * This method is responsible for printing out the send/receive requests in a
	 * specific format.
	 * 
	 * @param received the request that has been received
	 * @param target   the destination where the request is supposed to go
	 * @param output   prints out the output
	 */
	private void toString(enums.SystemEnumTypes.RequestEvent received, String target, String output) {
		if (received.equals(SystemEnumTypes.RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [Successfully sent event to " + target + "] " + output);
		} else if (received.equals(SystemEnumTypes.RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [Received an event from " + target + "] " + output);
		}
	}

	private void eventElevatorStopped(String elevatorName) {
		// Creating an instance of monitor for the current elevator.
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		// Change the elevator state to stopped
		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);
		// Updating the elevator monitor regarding the stopping of the elevator
		HashSet<MakeTrip> completedTrips = elevatorMonitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.toString(
					elevatorName + " has successfully completed the following trips at the stop: " + completedTrips);
		}

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN),
				this.portsByElevatorName.get(elevatorName));
	}

	private void eventElevatorDoorClosed(String elevatorName) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		// obtaining the next direction of motion of the elevator form the monitor
		SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
		elevatorMonitor.updateElevatorDirection(nextDirection);
		// sending a request to move the elevator in the direction it is supposed to go
		this.sendElevatorMoveEvent(elevatorName, nextDirection);

	}

	private void sendElevatorMoveEvent(String elevatorName, SystemEnumTypes.Direction nextDirection) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		elevatorMonitor.updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		elevatorMonitor.updateElevatorDirection(nextDirection);

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Move elevator " + nextDirection + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, nextDirection),
				this.portsByElevatorName.get(elevatorName));

	}

	private void eventElevatorDoorOpened(String elevatorName) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// changing the elevator door status to OPEN
		elevatorMonitor.updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
		// check if the current elevator has any pending requests or not
		if (!this.pendingTripRequests.isEmpty()) {
			HashSet<MakeTrip> assignedPendingRequests = this.assignPendingRequestsToElevator(elevatorName);
			if (!assignedPendingRequests.isEmpty()) {
				this.toString(elevatorName + " has been assignred following trip requests: " + assignedPendingRequests);
			}
		}
		// checking if the current elevator has any trips in queue
		if (!elevatorMonitor.isEmpty()) {
			this.toString(elevatorName + " still has requests in queue");

			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
					this.portsByElevatorName.get(elevatorName));
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
				// Update direction enum to free state i.e., STAY
				elevatorMonitor.updateElevatorDirection(SystemEnumTypes.Direction.STAY);

				this.toString(
						elevatorName + " has no available trip requests" + ", and is currently at its intial floor ["
								+ startFloor + "]. The elevator is waiting for next trip request...");
			} else {
				this.toString(elevatorName + " has no available trip requests"
						+ ", the elevator will be returned to its intital floor [" + startFloor + "]");

				this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
				this.sendToServer(
						new ElevatorDoorRequest(elevatorName, SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorName));
			}
		}

	}

	private HashSet<MakeTrip> assignPendingRequestsToElevator(String elevatorName) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		HashSet<MakeTrip> pendingRequests = new HashSet<MakeTrip>();

		// check if the elevator has any pending requests, if not, then assign it the
		// the first one in the list of pending requests
		if (elevatorMonitor.isEmpty()) {
			MakeTrip highestPriorityPendingRequest = this.pendingTripRequests.get(0);
			if (assignTripToFreeElevator(elevatorName, highestPriorityPendingRequest)) {
				pendingRequests.add(highestPriorityPendingRequest);
				this.pendingTripRequests.remove(0);
			}
		}

		// Checking if the elevator can take up any other requests currently in queue
		// for whom the elevator may not have to de-tour from its path
		Iterator<MakeTrip> iterator = pendingTripRequests.iterator();
		while (iterator.hasNext()) {
			MakeTrip pendingTripRequest = iterator.next();
			if (this.assignTripToInServiceElevator(elevatorName, pendingTripRequest)) {
				pendingRequests.add(pendingTripRequest);
				iterator.remove();
			}
		}
		return pendingRequests;
	}
	private boolean assignTripToInServiceElevator(String elevatorName, MakeTrip tripRequest) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Checks if the elevator will be re-routed for another request in queue. If the
		// elevator is in use and the requested floor is in the
		// same direction, then add this requested trip to the current elevator monitor
		if (!elevatorMonitor.isEmpty() && (elevatorMonitor.getQueueDirection() == tripRequest.getElevatorDirection())) {
			// add the trip request to the queue
			if (elevatorMonitor.addEnRouteTripRequest(tripRequest)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean assignTripToFreeElevator(String elevatorName, MakeTrip requestedTrip) {
		Monitor currentElevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		// check if the elevator is empty and if so, add the trip to the queue
		if (currentElevatorMonitor.isEmpty() && currentElevatorMonitor.addFirstTripRequest(requestedTrip)) {
			return true;
		}
		return false;
	}

	private void eventElevatorArrivalNotice(String elevatorName, int floorNumber) {

		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Updating the status of elevator door
		elevatorMonitor.updateElevatorFloorLocation(floorNumber);

		// Check if the elevator has a stop on the current floor or not
		if (elevatorMonitor.isStopRequired(floorNumber)) {
			this.toString(elevatorName + "needs to stop at floor number: " + floorNumber);

			// Check if this floor is the one to pick people or car and if so, send a
			// message to turn off the floor direction lamp
			if (elevatorMonitor.isPickupFloor(floorNumber)) {
				SystemEnumTypes.Direction queueDirection = (SystemEnumTypes.Direction) elevatorMonitor
						.getQueueDirection();
				this.toString(SystemEnumTypes.RequestEvent.SENT, "FLOOR " + floorNumber,
						elevatorName + " has arrived." + "Turning off " + queueDirection + " direction light.");
				this.sendToServer(new FloorLampRequest(queueDirection, SystemEnumTypes.FloorDirectionLampStatus.OFF),
						this.portsByFloorName.get(String.valueOf(floorNumber)));
			}
			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Stop elevator.");
			this.sendToServer(new ElevatorMotorRequest(elevatorName, SystemEnumTypes.Direction.STAY),
					this.portsByElevatorName.get(elevatorName));
		} else {
			this.toString(elevatorName + " does not need to stop at floor number: " + floorNumber);
			// Checking the direction to see if has changed or not during the trip
			SystemEnumTypes.Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
			this.sendElevatorMoveEvent(elevatorName, nextDirection);
		}

	}

	@Override
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
	public String getName() {
		return this.name;
	}

	@Override
	public synchronized void receiveEvent(Request request) {
		eventsQueue.add(request);
		this.notifyAll();
	}

	public static void main(String[] args) {
		// map of maps where key of the parent map is elevator name, value is the
		// attributes of
		// of the elevator defined in the config.xml file
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();

		// map of maps where key of the parent map is the floor name and value is the
		// elevator attributes in the
		// config.xml file
		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();
		// scheduler attributes from the config.xml
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		Scheduler scheduler = new Scheduler(schedulerConfiguration.get("name"),
				Integer.parseInt(schedulerConfiguration.get("port")), elevatorConfigurations, floorConfigurations);
		// creating a thread for the scheduler
		Thread schedulerThread = new Thread(scheduler, schedulerConfiguration.get("name"));
		schedulerThread.start();
	}

}
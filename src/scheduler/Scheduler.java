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

import info.ElevatorArrivalRequest;
import info.ElevatorDoorRequest;
import info.ElevatorMotorRequest;
import info.FloorButtonRequest;
import info.Request;
import info.RequestType;
import info.SystemEnumTypes;
import scheduler.Monitor;
import info.LampStatus;
import info.ElevatorLampRequest;
import scheduler.MakeTrip;
import elevator.Direction;
import elevator.ElevatorDoorStatus;
import elevator.ElevatorEvents;
import elevator.ElevatorStatus;
import server.Server;

public class Scheduler implements Runnable, ElevatorEvents {

	private String name;
	private Server server;
	private Thread serverThread;
	private Queue<Request> eventsQueue;
	private boolean debug = false;
	// key will refer to the name of the elevator and the value will refer to the
	// port number
	private HashMap<String, Integer> portsByElevatorName;
	// key will refer to the floor number where the elevator is currently at and the
	// value will refer to
	// the port number
	private HashMap<String, Integer> portsByFloorName;
	// key will refer to the name of the elevator and the value will refer to the
	// monitor of the elevator
	private HashMap<String, Monitor> monitorByElevatorName;
	private ArrayList<MakeTrip> pendingTripRequests;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.monitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingTripRequests = new ArrayList<MakeTrip>();

		// Initialize infrastructure configurations (elevators/floors)
		this.init(elevatorConfiguration, floorConfigurations);

		// Create a server (bound to this Instance of ElevatorSubsystem) in a new
		// thread.
		// When this server receives requests, they will be added to the eventsQueue of
		// THIS ElevatorSubsystem instance.

		// this.server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	/**
	 * This method initializes the data structures that are being used for the
	 * elevator state monitoring by the scheduler
	 * 
	 * @param elevatorConfiguration
	 * @param floorConfigurations
	 */
	public void init(HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		// Initialization of data structures
		for (String currentElevator : elevatorConfiguration.keySet()) {
			HashMap<String, String> config = elevatorConfiguration.get(currentElevator);

			this.portsByElevatorName.put(currentElevator, Integer.parseInt(config.get("port")));

			// Initialize monitors for each elevator
			this.monitorByElevatorName.put(currentElevator,
					new Monitor(currentElevator, Integer.parseInt(config.get("startFloor")),
							Integer.parseInt(config.get("startFloor")), Direction.STAY, ElevatorStatus.STOP,
							ElevatorDoorStatus.OPEN, floorConfigurations.size()));

		}
		// Initializing floor data structures
		for (String floorName : floorConfigurations.keySet()) {
			HashMap<String, String> conf = floorConfigurations.get(floorName);

			this.portsByFloorName.put(floorName, Integer.parseInt(conf.get("port")));
		}
	}

	@Override
	public void run() {
		while (true) {
			this.eventHandler(this.getNextEvent());
		}

	}

	private void eventHandler(Request event) {
		// switch statement corresponding to different "event handlers"
/*
		if (nextEvent.getType().equals(RequestType.FLOORBUTTON)) {
			Request request = nextEvent;

			this.toString(RequestEvent.RECEIVED, String.valueOf(request.getFloor()),
					"Trip request from floor " + request.getFloor() + " to " + request.getDestination() + ".");
			this.incomingTripRequest(request.getFloor(),
					Integer.parseInt(request.getDestination()), request.getDirec());
		} else if (nextEvent.getType().equals(RequestType.ELEVATORARRIVAL)) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) nextEvent;

			this.toString(RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator arrival notice at floor " + request.getFloorName() + ".");
			this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
		} else if (nextEvent instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) nextEvent;

			this.toString(RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator door is " + request.getRequestAction() + ".");
			if (request.getRequestAction() == ElevatorDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == ElevatorDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			}
		} else if (nextEvent instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) nextEvent;

			if (request.getRequestAction() == Direction.STAY) {
				this.toString(RequestEvent.RECEIVED, request.getElevatorName(), "Elevator has stopped.");
				this.eventElevatorStopped((String) request.getElevatorName());
			} else {
				this.toString(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is moving " + request.getRequestAction() + ".");
			}
		}
		// TODO Auto-generated method stub
		 *
		 */
		if (event.getType().equals(RequestType.FLOORBUTTON)) {
			FloorButtonRequest request = (FloorButtonRequest) event;
			
			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getFloorName(), "Trip request from floor " + request.getFloorName() + " to " + request.getDestinationFloor() + ".");
			this.eventTripRequestReceived(Integer.parseInt(request.getFloorName()), Integer.parseInt(request.getDestinationFloor()), request.getDirection());
		} else if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
			
			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(), "Elevator arrival notice at floor " + request.getFloorName() + ".");
			this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;
			
			this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(), "Elevator door is " + request.getRequestAction() + ".");
			if (request.getRequestAction() == ElevatorDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == ElevatorDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;

			if (request.getRequestAction() == Direction.STAY) {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(), "Elevator has stopped.");
				this.eventElevatorStopped(request.getElevatorName());
			} else {
				this.toString(SystemEnumTypes.RequestEvent.RECEIVED, request.getElevatorName(), "Elevator is moving " + request.getRequestAction() + ".");
			}
		}

	}

	private void incomingTripRequest(int currentFloorNumber, int destinationFloorNumber, String direction) {
		// Create a TripRequest object
		MakeTrip requestedTrip = new MakeTrip(currentFloorNumber, destinationFloorNumber);

		// check if any elevator in service can accomplish the trip request by
		// re-routing
		for (String elevatorName : monitorByElevatorName.keySet()) {
			if (this.assignTripToInServiceElevator(elevatorName, requestedTrip)) {
				this.toString("The " + elevatorName + " will serve the request: " + requestedTrip);

				// TODO remove this in iteration 2 - as the elevator will manage its own floor
				// buttons.

				// Send event to elevator to light floor button for new destination.
				this.sendToServer(new ElevatorLampRequest(String.valueOf(destinationFloorNumber), LampStatus.ON),
						this.portsByElevatorName.get(elevatorName));
				return;
			}
		}
		// if any elevator in service can not accomplish the trip request by re-routing,
		// then check if there are any elevators that
		// are free.
		for (String elevatorName : monitorByElevatorName.keySet()) {
			Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
			if (this.assignTripToFreeElevator(elevatorName, requestedTrip)) {
				this.toString(elevatorName + " has been assigned the trip request: " + requestedTrip);

				// TODO remove this in iteration 2 - as the elevator will manage its own floor
				// buttons.
				// Send event to elevator to light floor button for new destination.
				this.sendToServer(new ElevatorLampRequest(String.valueOf(destinationFloorNumber), LampStatus.ON),
						this.portsByElevatorName.get(elevatorName));

				if (elevatorMonitor.getElevatorStatus() == ElevatorStatus.STOP) {

					// Close the door by creating an event to close the door.
					this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
					this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorDoorStatus.CLOSE),
							this.portsByElevatorName.get(elevatorName));
				}
				return;
			}
		}

		// To the queue containing the pending requests, add the trip request
		this.pendingTripRequests.add(requestedTrip);
		this.toString("Trip request " + requestedTrip
				+ " was unable to be assigned immediately. It has been added to pending requests "
				+ this.pendingTripRequests + ".");
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

	private boolean assignTripToFreeElevator(String elevatorName, MakeTrip requestedTrip) {
		Monitor currentElevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// check if the elevator is empty and if so, add the trip to the queue
		if (currentElevatorMonitor.isEmpty() && currentElevatorMonitor.addFirstTripRequest(requestedTrip)) {
			return true;
		}
		return false;
	}

	/**
	 * This method writes to the console in a paticular format
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
	 * @param received
	 * @param target
	 * @param output
	 */
	private void toString(info.SystemEnumTypes.RequestEvent received, String target, String output) {
		if (received.equals(SystemEnumTypes.RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + target + "] " + output);
		} else if (received.equals(SystemEnumTypes.RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

	private boolean assignTripToInServiceElevator(String elevatorName, MakeTrip tripRequest) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Checks if the elevator will be re-routed for another request in queue. If the
		// elevator is in use and
		// the requested floor is in the same direction, then add this requested trip to
		// the current elevator monitor

		if (!elevatorMonitor.isEmpty() && (elevatorMonitor.getQueueDirection() == tripRequest.getElevatorDirection())) {
			// Try to add this trip to the tripQueue
			if (elevatorMonitor.addEnRouteTripRequest(tripRequest)) {
				return true;
			}
		}
		return false;
	}

	private void eventElevatorStopped(String elevatorName) {
		// Get monitor for the current elevator.
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Update the elevator state to Stoped
		elevatorMonitor.updateElevatorStatus(ElevatorStatus.STOP);

		// The elevatorMonitor needs to be advised this stop has occurred
		HashSet<MakeTrip> completedTrips = elevatorMonitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.toString(
					"The following trips have been completed at this stop by " + elevatorName + ":" + completedTrips);
		}

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorDoorStatus.OPEN),
				this.portsByElevatorName.get(elevatorName));
	}

	private void eventElevatorDoorClosed(String elevatorName) {
		// this.toString("Confirmation received that " + elevatorName + " has closed its
		// doors.");

		// Get the elevatorMonitor for this elevator
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Update the status of current elevator door
		elevatorMonitor.updateElevatorDoorStatus(ElevatorDoorStatus.CLOSE);

		// Get the next direction for the elevator based on the elevatorMonitor
		Direction nextDirection = elevatorMonitor.getNextElevatorDirection();

		// Update elevator current direction
		elevatorMonitor.updateElevatorDirection(nextDirection);

		// send an elevator move event in the next direction it needs to go
		this.sendElevatorMoveEvent(elevatorName, nextDirection);

	}

	private void sendElevatorMoveEvent(String elevatorName, Direction nextDirection) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Update the elevator status to move
		elevatorMonitor.updateElevatorStatus(ElevatorStatus.MOVE);
		// Update elevator direction
		elevatorMonitor.updateElevatorDirection(nextDirection);

		this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Move elevator " + nextDirection + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, nextDirection),
				this.portsByElevatorName.get(elevatorName));

	}

	private void eventElevatorDoorOpened(String elevatorName) {
		// this.toString("Confirmation received that " + elevatorName + " has
		// opened its doors.");

		// create instance of elevator monitor
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Update the status of current elevator door
		elevatorMonitor.updateElevatorDoorStatus(ElevatorDoorStatus.OPEN);

		// check if there are any pending requests or not
		if (!this.pendingTripRequests.isEmpty()) {
			HashSet<MakeTrip> assignedPendingRequests = this.assignPendingRequestsToElevator(elevatorName);
			if (!assignedPendingRequests.isEmpty()) {
				this.toString("The following pending trip requests have been assigned to " + elevatorName + "  : "
						+ assignedPendingRequests);
			}
		}

		// Check if the elevator has unvisited floors or not
		if (!elevatorMonitor.isEmpty()) {
			this.toString("There are more floors to visit for this elevator " + elevatorName);

			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
			this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorDoorStatus.CLOSE),
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
				elevatorMonitor.updateElevatorDirection(Direction.STAY);

				this.toString(
						elevatorName + " has no available trip requests" + ", and is currently at its intial floor ["
								+ startFloor + "]. The elevator is waiting for next trip request...");
			} else {
				this.toString(elevatorName + " has no available trip requests"
						+ ", the elevator will be returned to its intital floor [" + startFloor + "]");

				this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Close elevator door.");
				this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorDoorStatus.CLOSE),
						this.portsByElevatorName.get(elevatorName));
			}
		}

	}

	private HashSet<MakeTrip> assignPendingRequestsToElevator(String elevatorName) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		HashSet<MakeTrip> pendingRequests = new HashSet<MakeTrip>();

		// check if the elevator has any pending requests, if not, then assign it the
		// the first one
		// in the list of pending requests
		if (elevatorMonitor.isEmpty()) {
			MakeTrip highestPriorityPendingRequest = this.pendingTripRequests.get(0);
			if (assignTripToFreeElevator(elevatorName, highestPriorityPendingRequest)) {
				pendingRequests.add(highestPriorityPendingRequest);
				this.pendingTripRequests.remove(0);
			}
		}

		// Checking if the elevator can take up any other requests currently in queue
		// for whom
		// the elevator may not have to de-tour from its path
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

	private void eventElevatorArrivalNotice(String elevatorName, int floorNumber) {

		// create instance of elevator monitor
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Update the status of current elevator door
		elevatorMonitor.updateElevatorFloorLocation(floorNumber);

		// Check if the elevator has a stop on the current floor or not
		if (elevatorMonitor.isStopRequired(floorNumber)) {
			this.toString(elevatorName + "needs to stop at floor number: " + floorNumber);

			// Check if this floor is the one to pick people or car and if so, send a
			// message
			// to turn off the floor direction lamp
			if (elevatorMonitor.isPickupFloor(floorNumber)) {
				Direction queueDirection = (Direction) elevatorMonitor.getQueueDirection();
				this.toString(SystemEnumTypes.RequestEvent.SENT, "FLOOR " + floorNumber,
						"Turn off " + queueDirection + " direction lamp as " + elevatorName + " has arrived.");
				this.sendToServer(new FloorLampRequest(queueDirection, LampStatus.OFF),
						this.portsByFloorName.get(String.valueOf(floorNumber)));
			}
			this.toString(SystemEnumTypes.RequestEvent.SENT, elevatorName, "Stop elevator.");
			this.sendToServer(new ElevatorMotorRequest(elevatorName, Direction.STAY),
					this.portsByElevatorName.get(elevatorName));
		} else {
			this.toString(elevatorName + " does not need to stop at floor number: " + floorNumber);
			// Checking the direction to see if has changed or not during the
			// trip
			Direction nextDirection = elevatorMonitor.getNextElevatorDirection();
			this.sendElevatorMoveEvent(elevatorName, nextDirection);
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

	@Override
	public void receiveEvent(Request event) {
		eventsQueue.add(event);
		this.notifyAll();

	}

	@Override
	public Request getNextEvent() {
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

}

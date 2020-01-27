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
import info.RequestEvent;
import info.LampStatus;
import info.ElevatorLampRequest;
import scheduler.Monitor;
import scheduler.TripRequest;
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
	private ArrayList<TripRequest> pendingTripRequests;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.monitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingTripRequests = new ArrayList<TripRequest>();

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
	 * This method initializes the data structures that are bring used for the
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

	private void eventHandler(Request nextEvent) {
		// switch statement corresponding to different "event handlers"

		if (nextEvent instanceof FloorButtonRequest) {
			FloorButtonRequest request = (FloorButtonRequest) nextEvent;

			this.toString(RequestEvent.RECEIVED, request.getFloorName(),
					"Trip request from floor " + request.getFloorName() + " to " + request.getDestinationFloor() + ".");
			this.incomingTripRequest(Integer.parseInt(request.getFloorName()),
					Integer.parseInt(request.getDestinationFloor()), request.getDirec());
		} else if (nextEvent instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) nextEvent;

			this.toString(RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator arrival notice at floor " + request.getFloorName() + ".");
			this.eventElevatorArrivalNotice(request.getElevatorName(), Integer.parseInt(request.getFloorName()));
		} else if (nextEvent instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) nextEvent;

			this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
					"Elevator door is " + request.getRequestAction() + ".");
			if (request.getRequestAction() == ElevatorDoorStatus.OPEN) {
				this.eventElevatorDoorOpened(request.getElevatorName());
			} else if (request.getRequestAction() == ElevatorDoorStatus.CLOSE) {
				this.eventElevatorDoorClosed(request.getElevatorName());
			}
		} else if (nextEvent instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) nextEvent;

			if (request.getRequestAction() == Direction.STAY) {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(), "Elevator has stopped.");
				this.eventElevatorStopped((String) request.getElevatorName());
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is moving " + request.getRequestAction() + ".");
			}
		}
		// TODO Auto-generated method stub

	}

	private void incomingTripRequest(int currentFloorNumber, int destinationFloorNumber, Direction direction) {
		// Create a TripRequest object
		TripRequest requestedTrip = new TripRequest(currentFloorNumber, destinationFloorNumber);

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
					this.toString(RequestEvent.SENT, elevatorName, "Close elevator door.");
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

	private boolean assignTripToFreeElevator(String elevatorName, TripRequest requestedTrip) {
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
	 * @param event
	 * @param target
	 * @param output
	 */
	private void toString(RequestEvent event, String target, String output) {
		if (event.equals(RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

	private boolean assignTripToInServiceElevator(String elevatorName, TripRequest tripRequest) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);

		// Determine whether this trip request is en route for this elevators trip
		// request queue.
		// If this elevator is currently in service (elevatorMonitor is not empty), and
		// the tripRequest is in the same direction as the elevatorMonitor,
		// then attempt to add this tripRequest to the elevator's elevatorMonitor
		if (!elevatorMonitor.isEmpty() && (elevatorMonitor.getQueueDirection() == tripRequest.getDirection())) {
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
		HashSet<TripRequest> completedTrips = elevatorMonitor.stopOccurred();
		if (!completedTrips.isEmpty()) {
			this.toString(
					"The following trips have been completed at this stop by " + elevatorName + ":" + completedTrips);
		}

		this.consoleOutput(RequestEvent.SENT, elevatorName, "Open elevator door.");
		this.sendToServer(new ElevatorDoorRequest(elevatorName, ElevatorDoorStatus.OPEN),
				this.portsByElevatorName.get(elevatorName));
	}

	private void eventElevatorDoorClosed(Object elevatorName) {
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

	private void sendElevatorMoveEvent(Object elevatorName, Direction nextDirection) {
		Monitor elevatorMonitor = this.monitorByElevatorName.get(elevatorName);
		
		//Update the elevator status to move
		elevatorMonitor.updateElevatorStatus(ElevatorStatus.MOVE);
		//Update elevator direction 
		elevatorMonitor.updateElevatorDirection(nextDirection);
		
		this.consoleOutput(RequestEvent.SENT, elevatorName, "Move elevator " + nextDirection + ".");
		this.sendToServer(new ElevatorMotorRequest(elevatorName, nextDirection), this.portsByElevatorName.get(elevatorName));
		
	}

	private void eventElevatorDoorOpened(Object elevatorName) {
		// TODO Auto-generated method stub

	}

	private void eventElevatorArrivalNotice(String elevatorName, int parseInt) {
		// TODO Auto-generated method stub

	}

	private void consoleOutput(RequestEvent received, Object floorName, String string) {
		// TODO Auto-generated method stub

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

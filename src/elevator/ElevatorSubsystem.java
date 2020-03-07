package elevator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;
import enums.SystemEnumTypes.ElevatorCurrentStatus;
import enums.SystemEnumTypes.Fault;
import enums.SystemEnumTypes.RequestEvent;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.Request;
import server.Server;

/**
 * This is the main class for the elevator subsystem and all other classes
 * communicate with it This class is responsible for the elevator system
 * management
 * 
 *
 */
public class ElevatorSubsystem implements Runnable, ElevatorEvents {
	// class variables
	private Server server;
	private Thread serverThread;
	private String name;
	private int travelTime;
	private int passengerTime;
	private int doorTime;
	private ElevatorState state;
	private Queue<Request> eventsQueue;
	private boolean debug = false;
	private int schedulerPort;
	private boolean destinationRequestFlag = false;
	private boolean motorFaultFlag = false;
	private boolean doorFaultFlag = false;
	private InetAddress host;

	/**
	 * The constructor
	 * 
	 * @param name          the elevator name
	 * @param port          the elevator port
	 * @param start         the starting floor of the elevator
	 * @param schedulerPort the scheduler port
	 * @param totalNum      the total number of the floors
	 * @param travelTime    the time elevator need to travel
	 * @param passengerTime the time passenger get in and off the elevator
	 * @param doorTime      the time elevator open/close the door
	 */

	public ElevatorSubsystem(String name, int port, int startFloor, int schedulerPort, int maxFloor, int travelTime,
			int passengerTime, int doorTime, String host) {
		this.name = name;
		this.travelTime = travelTime;
		this.passengerTime = passengerTime;
		this.doorTime = doorTime;
		this.eventsQueue = new LinkedList<Request>();
		this.state = new ElevatorState(startFloor, startFloor, Direction.STAY, ElevatorCurrentStatus.STOP,
				ElevatorCurrentDoorStatus.OPEN, maxFloor, travelTime, passengerTime, doorTime);
		this.schedulerPort = schedulerPort;
		try {
			this.host = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// Create a server (bound to this Instance of ElevatorSubsystem) in a new
		// thread.
		// When this server receives requests, they will be added to the eventsQueue of
		// THIS ElevatorSubsystem instance.
		server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	/**
	 * receive request event from the event queue
	 */
	public synchronized void receiveEvent(Request event) {
		eventsQueue.add(event);
		this.notifyAll();
	}

	/**
	 * get the next request from event queue
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
	 * getd the name of the elevator e.g., E1, E2 etc
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * thread run
	 */
	@Override
	public void run() {
		this.toString(this.name + " is online. Waiting for a command from Scheduler...");
		while (true) {
			this.handleRequest(this.getNextEvent());
		}
	}

	/*
	 * Deals with the requests and decide the elevator movements
	 */
	private void handleRequest(Request event) {
		// switch statement corresponding to different "event handlers"
		if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
			this.toString("Sending arrival notice.");
			this.sendToServer(request);
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;
			if (request.getRequestAction() == ElevatorCurrentDoorStatus.OPEN) {
				this.toString(RequestEvent.RECEIVED, "Scheduler", "Open elevator doors.");
				this.doorOpen();
			} else if (request.getRequestAction() == ElevatorCurrentDoorStatus.CLOSE) {
				this.toString(RequestEvent.RECEIVED, "Scheduler", "Close elevator doors.");
				this.doorClose();
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;
			if (request.getRequestAction() == Direction.STAY) {
				this.toString(RequestEvent.RECEIVED, "Scheduler", "Stop elevator.");
				this.elevatorStop();
			} else if (request.getRequestAction() == Direction.UP) {
				this.toString(RequestEvent.RECEIVED, "Scheduler", "Move elevator up.");
				this.elevatorUp();
			} else if (request.getRequestAction() == Direction.DOWN) {
				this.toString(RequestEvent.RECEIVED, "Scheduler", "Move elevator down.");
				this.elevatorDown();
			}
		} else if (event instanceof ElevatorDestinationRequest) {
			ElevatorDestinationRequest request = (ElevatorDestinationRequest) event;
			this.toString(RequestEvent.RECEIVED, "Floor " + request.getPickupFloor(),
					"Destination request to floor " + request.getDestinationFloor());
			this.destinationRequest(request);
		} else if (event instanceof ElevatorWaitRequest) {
			this.toString(RequestEvent.RECEIVED, "Scheduler", "Waiting For Passengers");
			this.handleWaitForPassengers();
		}
	}

	/**
	 * toggles lamp state dependent on floor provided
	 * 
	 * @param floor the floor
	 * @param b     the lamp state
	 */
	private void toggleLamp(int floor, Boolean b) {
		this.state.toggleLamp(floor, b);
	}

	/*
	 * Handle elevator to stop
	 */
	private void elevatorStop() {
		this.state.setDirection(Direction.STAY);
		this.state.setStatus(ElevatorCurrentStatus.STOP);
		this.toString("Turn off floor " + this.state.getCurrentFloor() + " button lamp if on.");
		this.toggleLamp(this.state.getCurrentFloor(), false);
		ElevatorMotorRequest request = new ElevatorMotorRequest(this.name, Direction.STAY);
		this.toString(RequestEvent.SENT, "Scheduler", "STOP at " + this.state.getCurrentFloor() + ".");
		this.sendToServer(request);
	}

	/*
	 * Deals with the elevator move up event
	 */
	private void elevatorUp() {
		if (this.state.getDoorStatus() != ElevatorCurrentDoorStatus.OPEN) {
			this.state.setDirection(Direction.UP);
			this.state.setStatus(ElevatorCurrentStatus.MOVE);
			this.toString("Elevator motor set to move up. Simulating travel time...");
			// check if fault
			if (!this.motorFaultFlag) {
				try {
					Thread.sleep(this.travelTime);
				} catch (java.lang.InterruptedException e) {
					e.printStackTrace();
				}
				this.state.setCurrentFloor(this.state.getCurrentFloor() + 1);
				this.toString(RequestEvent.SENT, "Scheduler",
						"Arriving at floor " + this.state.getCurrentFloor() + ".");
				ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
						Integer.toString(this.state.getCurrentFloor()), this.state.getDirection());
				this.sendToServer(request);
			}
		}
	}

	/*
	 * deals with the elevator move down event
	 */
	private void elevatorDown() {
		if (this.state.getDoorStatus() != ElevatorCurrentDoorStatus.OPEN) {
			this.state.setDirection(Direction.DOWN);
			this.state.setStatus(ElevatorCurrentStatus.MOVE);
			this.toString("Elevator motor set to move down. Simulating travel time...");
			// check if fault
			if (!this.motorFaultFlag) {
				try {
					Thread.sleep(this.travelTime);
				} catch (java.lang.InterruptedException e) {
					e.printStackTrace();
				}
				this.state.setCurrentFloor(this.state.getCurrentFloor() - 1);
				this.toString(RequestEvent.SENT, "Scheduler",
						"Arriving at floor " + this.state.getCurrentFloor() + ".");
				ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
						Integer.toString(this.state.getCurrentFloor()), this.state.getDirection());
				this.sendToServer(request);
			} else {
				this.motorFaultFlag = false;
			}

		}

	}

	/*
	 * Deals with the elevator door open event
	 */
	private void doorOpen() {
		this.toString("Elevator opening doors...");
		// check if fault
		if (!this.doorFaultFlag) {
			try {
				Thread.sleep(this.doorTime);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setDoorStatus(ElevatorCurrentDoorStatus.OPEN);
			this.toString(RequestEvent.SENT, "Scheduler", "Doors are OPEN.");
			ElevatorDoorRequest request = new ElevatorDoorRequest(this.name, ElevatorCurrentDoorStatus.OPEN);
			this.sendToServer(request);
		} else {
			this.doorFaultFlag = false;
		}
	}

	/*
	 * Deals with the elevator door close event 
	 */
	private void doorClose() {
		this.toString("Elevator closing doors...");
		if (!this.doorFaultFlag) {
			try {
				Thread.sleep(this.doorTime);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setDoorStatus(ElevatorCurrentDoorStatus.CLOSE);
			this.toString(RequestEvent.SENT, "Scheduler", "Doors are CLOSE.");
			ElevatorDoorRequest request = new ElevatorDoorRequest(this.name, ElevatorCurrentDoorStatus.CLOSE);
			this.sendToServer(request);
		} else {
			this.doorFaultFlag = false;
		}
	}

	  /*
	    * Deals with elevator loading passengers
	    */
	private void handleWaitForPassengers() {
		this.toString("Elevator Waiting while loading/unloading passengers...");
		try {
			Thread.sleep(this.passengerTime);
		} catch (java.lang.InterruptedException e) {
			e.printStackTrace();
		}
		// creates temp queue
		Queue<Request> tmp = new LinkedList<Request>();
		int size = this.eventsQueue.size();
		// loop through entire queue
		for (int count = 0; count < size; count++) {
			Request head = eventsQueue.poll();// check to see if head is A destination request
			if (head instanceof ElevatorDestinationRequest) {
				eventsQueue.offer(head);
				this.destinationRequestFlag = true;
			} else {
				tmp.offer(head);
			}
		} // arrange queue so that destination requests are at the front
		eventsQueue.addAll(tmp);
		if (!this.destinationRequestFlag) { // send event if no more destination requests
			ElevatorWaitRequest request = new ElevatorWaitRequest(this.name);
			this.sendToServer(request);
		}
	}

	/**
	 * Deals with the destination requests
	 * @param request the request that is being made
	 */
	private void destinationRequest(ElevatorDestinationRequest request) {
		this.toggleLamp(Integer.parseInt(request.getDestinationFloor()), true);
		// check if fault in request and set appropriate flag
		if (request.getFault() != null) {
			if (request.getFault() == Fault.MOTOR) {
				this.motorFaultFlag = true;
			} else {
				this.doorFaultFlag = true;
			}
		}
		this.toString(RequestEvent.SENT, "Scheduler", "Destination request to " + request.getDestinationFloor());
		this.sendToServer(request);
		boolean tempflag = false;
		if (this.destinationRequestFlag) {
			// This works because the collection has been ordered to ensure all
			// ElevatorDestinationRequests to the front
			Request head = eventsQueue.peek();
			if (!(head instanceof ElevatorDestinationRequest)) {
				tempflag = true;
			}
			if (tempflag) { // if no more destination requests send wait event
				ElevatorWaitRequest sendRequest = new ElevatorWaitRequest(this.name);
				this.toString(RequestEvent.SENT, "Scheduler", "Wait complete.");
				this.sendToServer(sendRequest);
				this.destinationRequestFlag = false;
			}
		}
	}

	/*
	 * Send request to server
	 */
	private void sendToServer(Request request) {
		this.server.send(request, this.host.getHostAddress(), this.schedulerPort);
	}

	/*
	 * Prints the elevator movement with accurate time
	 */
	private void toString(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
				+ this.name + " : " + output);
	}

	/*
	 * Prints the requests as the jump between the server and elevator subsystem
	 * with accurate time
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

	/**
	 * The main method responsible for creating an instance of the entire
	 * ElevatorSubSystem
	 */
	public static void main(String[] args) {
		// This will return a Map of Maps. First key -> elevator Name, Value -> map of
		// all attributes for that elevator (as per config.xml)
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();

		// This will return a Map of all attributes for the Scheduler (as per
		// config.xml)
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();

		int tempfloor = 0;
		for (@SuppressWarnings("unused") String floorName : floorConfigurations.keySet()) {
			// find amount of floors
			tempfloor += tempfloor;
		}

		// Iterate through each elevator and create an instance of an ElevatorSubsystem
		for (String elevatorName : elevatorConfigurations.keySet()) {
			// Get the configuration for this particular 'elevatorName'
			HashMap<String, String> elevatorConfiguration = elevatorConfigurations.get(elevatorName);

			// Create an instance of ElevatorSubsystem for this 'elevatorName'
			ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem(elevatorName,
					Integer.parseInt(elevatorConfiguration.get("port")),
					Integer.parseInt(elevatorConfiguration.get("startFloor")),
					Integer.parseInt(schedulerConfiguration.get("port")), tempfloor,
					Integer.parseInt(elevatorConfiguration.get("timeBetweenFloors")),
					Integer.parseInt(elevatorConfiguration.get("passengerWaitTime")),
					Integer.parseInt(elevatorConfiguration.get("doorOperationTime")),
					schedulerConfiguration.get("host"));

			// Spawn and start a new thread for this ElevatorSubsystem instance
			Thread elevatorSubsystemThread = new Thread(elevatorSubsystem, elevatorName);
			elevatorSubsystemThread.start();
		}

	}

}

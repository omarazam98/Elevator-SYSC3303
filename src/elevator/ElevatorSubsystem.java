package elevator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import requests.ElevatorArrivalRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorLampRequest;
import requests.ElevatorMotorRequest;
import requests.Request;
import server.Server;
import enums.SystemEnumTypes;
import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;
import enums.SystemEnumTypes.ElevatorCurrentStatus;
import enums.SystemEnumTypes.RequestEvent;

public class ElevatorSubsystem implements Runnable, ElevatorEvents {
	// class variables
	private Server server;
	private Thread serverThread;
	private Queue<Request> events;
	private ElevatorState state;
	private boolean debug = false;
	private int schedulerPort;
	private String name;

	public ElevatorSubsystem(String name, int port, int start, int schedulerPort, int totalNum) {
		this.name = name;
		this.events = new LinkedList<Request>();
		this.state = new ElevatorState(start, start, Direction.STAY,ElevatorCurrentStatus.STOP, ElevatorCurrentDoorStatus.OPEN, totalNum);
		this.schedulerPort = schedulerPort;

		// Create a server (bound to this Instance of ElevatorSubsystem) in a new thread.
		// When this server receives requests, they will be added to the queue "events" of
		// This ElevatorSubsystem instance.
		server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	// receive request event from the event queue
	public synchronized void receiveEvent(Request event) {
		events.add(event);
		this.notifyAll();
	}

	// get the next request from event queue
	public synchronized Request getNextEvent() {
		while (events.isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return events.poll();
	}

	public String getName() {
		return this.name;
	}

	
	// toggles lamp state dependent on floor provided
	private void toggleLamp(int floor, Boolean b) {
		this.state.toggleLamp(floor, b);
	}

	/*
	 * Handle elevator to stop
	 */
	private void elevatorStop() {
		this.state.setDirection(Direction.STAY);
		this.state.setStatus(ElevatorCurrentStatus.STOP);
		this.Output("Turn off floor " + this.state.getCurrentFloor() + " button lamp if on.");
		this.state.toggleLamp(this.state.getCurrentFloor(), false);
		ElevatorMotorRequest request = new ElevatorMotorRequest(this.name, Direction.STAY);
		this.Output(RequestEvent.SENT, "Scheduler", "Stopped at " + this.state.getCurrentFloor() + ".");
		this.sendServer(request);
	}
	
	/*
	 * Handle elevator to move up
	 */
	private void elevatorUp() {
		if (this.state.getDoorStatus() != SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN) {
			this.state.setDirection(SystemEnumTypes.Direction.UP);
			this.state.setStatus(ElevatorCurrentStatus.MOVE);
			this.Output("Elevator motor set to move up. Simulating travel time...");
			try {
				Thread.sleep(5000);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setCurrentFloor(this.state.getCurrentFloor() + 1);
			this.Output(RequestEvent.SENT, "Scheduler",
					"Arriving at floor " + this.state.getCurrentFloor() + ".");
			ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
					Integer.toString(this.state.getCurrentFloor()));
			this.sendServer(request);
		}
	}

	/*
	 * Handle elevator to move down
	 */
	private void elevatorDown() {
		if (this.state.getDoorStatus() != ElevatorCurrentDoorStatus.OPEN) {
			this.state.setDirection(Direction.DOWN);
			this.state.setStatus(ElevatorCurrentStatus.MOVE);
			this.Output("Elevator motor set to move down. Simulating travel time...");
			try {
				Thread.sleep(5000);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setCurrentFloor(this.state.getCurrentFloor() - 1);
			this.Output(RequestEvent.SENT, "Scheduler",
					"Arriving at floor " + this.state.getCurrentFloor() + ".");
			ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
					Integer.toString(this.state.getCurrentFloor()));
			this.sendServer(request);
		}

	}
	
	/*
	 * Handle elevator door to open
	 */
	private void doorOpen() {
		this.state.setDoorStatus(ElevatorCurrentDoorStatus.OPEN);
		this.Output(RequestEvent.SENT, "Scheduler", "Doors are opened.");
		ElevatorDoorRequest request = new ElevatorDoorRequest(this.name,
				ElevatorCurrentDoorStatus.OPEN);
		this.sendServer(request);
	}
	
	/*
	 * Handle elevator door to close
	 */
	private void doorClose() {
		this.state.setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		this.Output(RequestEvent.SENT, "Scheduler", "Doors are closed.");
		ElevatorDoorRequest request = new ElevatorDoorRequest(this.name,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		this.sendServer(request);
	}
	
	/*
	 * Send request to server
	 */
	private void sendServer(Request request) {
		try {
			this.server.send(request, InetAddress.getLocalHost(), this.schedulerPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Out print the elevator movement with accurate time
	 */
	private void Output(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
				+ this.name + " : " + output);
	}
	/*
	 * Out print the transmission of request between server and elevator subsystem with accurate time
	 */
	private void Output(RequestEvent event, String receiver, String output) {
		if (event.equals(RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + receiver + "] " + output);
		} else if (event.equals(RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + receiver + "] " + output);
		}
	}
	/*
	 * Handle the request and decide the elevator movement
	 */
	private void handleRequest(Request event) {
		
		// switch statement corresponding to different kinds of request
		if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
			this.Output("Sending arrival notice.");
			this.sendServer(request);
		} else if (event instanceof ElevatorDoorRequest) {
			ElevatorDoorRequest request = (ElevatorDoorRequest) event;
			if (request.getRequestAction() == ElevatorCurrentDoorStatus.OPEN) {
				this.Output(RequestEvent.RECEIVED, "Scheduler", "Open elevator doors.");
				this.doorOpen();
			} else if (request.getRequestAction() == ElevatorCurrentDoorStatus.CLOSE) {
				this.Output(RequestEvent.RECEIVED, "Scheduler", "Close elevator doors.");
				this.doorClose();
			}
		} else if (event instanceof ElevatorMotorRequest) {
			ElevatorMotorRequest request = (ElevatorMotorRequest) event;
			if (request.getRequestAction() == Direction.STAY) {
				this.Output(RequestEvent.RECEIVED, "Scheduler", "Stop elevator.");
				this.elevatorStop();
			} else if (request.getRequestAction() == Direction.UP) {
				this.Output(RequestEvent.RECEIVED, "Scheduler", "Move elevator up.");
				this.elevatorUp();
			} else if (request.getRequestAction() == Direction.DOWN) {
				this.Output(RequestEvent.RECEIVED, "Scheduler", "Move elevator down.");
				this.elevatorDown();
			}
		} else if (event instanceof ElevatorLampRequest) {
			ElevatorLampRequest request = (ElevatorLampRequest) event;
			this.Output(RequestEvent.RECEIVED, "Scheduler",
					"Turn on floor " + request.getElevatorButton() + " button lamp.");
			toggleLamp(Integer.parseInt(request.getElevatorButton()), true);
		}
	}

	// thread run
		@Override
		public void run() {
			while (true) {
				this.handleRequest(this.getNextEvent());
			}
		}
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

		int temp = 0;
		for (String floor : floorConfigurations.keySet()) {
			// find amount of floors
			temp+= temp;
		}

		// Iterate through each elevator and create an instance of an ElevatorSubsystem
		for (String elevator : elevatorConfigurations.keySet()) {
			// Get the configuration for this particular 'elevatorName'
			HashMap<String, String> elevatorConfiguration = elevatorConfigurations.get(elevator);

			// Create an instance of ElevatorSubsystem for this 'elevatorName'
			ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem(elevator,
					Integer.parseInt(elevatorConfiguration.get("port")),
					Integer.parseInt(elevatorConfiguration.get("startFloor")),
					Integer.parseInt(schedulerConfiguration.get("port")), temp);

			// Spawn and start a new thread for this ElevatorSubsystem instance
			Thread elevatorSubsystemThread = new Thread(elevatorSubsystem, elevator);
			elevatorSubsystemThread.start();
		}

	}

}
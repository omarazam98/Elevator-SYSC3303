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
	private HashMap<String, Monitor> elevatorMonitorByElevatorName;
	private ArrayList<TripRequest> pendingTripRequests;

	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration,
			HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.elevatorMonitorByElevatorName = new HashMap<String, Monitor>();
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
		// Intialization of data structures
		for (String currentElevator : elevatorConfiguration.keySet()) {
			HashMap<String, String> config = elevatorConfiguration.get(currentElevator);

			this.portsByElevatorName.put(currentElevator, Integer.parseInt(config.get("port")));

			// Initialize monitors for each elevator
			this.elevatorMonitorByElevatorName.put(currentElevator,
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

			this.consoleOutput(RequestEvent.RECEIVED, request.getFloorName(),
					"Trip request from floor " + request.getFloorName() + " to " + request.getDestinationFloor() + ".");
			this.eventTripRequestReceived(Integer.parseInt((String) request.getFloorName()),
					Integer.parseInt(request.getDestinationFloor()), request.getDirec());
		} else if (nextEvent instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) nextEvent;

			this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
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
				this.eventElevatorStopped(request.getElevatorName());
			} else {
				this.consoleOutput(RequestEvent.RECEIVED, request.getElevatorName(),
						"Elevator is moving " + request.getRequestAction() + ".");
			}
		}
		// TODO Auto-generated method stub

	}

	private void eventTripRequestReceived(int parseInt, int parseInt2, String direc) {
		// TODO Auto-generated method stub

	}

	private void eventElevatorStopped(Object elevatorName) {
		// TODO Auto-generated method stub

	}

	private void eventElevatorDoorClosed(Object elevatorName) {
		// TODO Auto-generated method stub

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

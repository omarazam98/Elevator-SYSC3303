package elevator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.ElevatorDoorRequest;
import requests.ElevatorLampRequest;
import requests.ElevatorMotorRequest;
import requests.ElevatorWaitRequest;
import requests.Request;
import server.Server;
import enums.SystemEnumTypes;
import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;
import enums.SystemEnumTypes.ElevatorCurrentStatus;
import enums.SystemEnumTypes.RequestEvent;

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
	private Queue<Request> events;
	private ElevatorState state;
	private boolean debug = false;
	private int schedulerPort;
	private String name;
	private int travelTime;
	private int passengerTime;
	private int doorTime;
	private boolean destinationRequestFlag = false;
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
	public ElevatorSubsystem(String name, int port, int start, int schedulerPort, int totalNum, int travelTime,int passengerTime,int doorTime, String host) {
		
		this.name = name;
		this.events = new LinkedList<Request>();
		this.travelTime = travelTime;
		this.passengerTime = passengerTime;
		this.doorTime = doorTime;
		this.state = new ElevatorState(start, start, Direction.STAY, ElevatorCurrentStatus.STOP,
				ElevatorCurrentDoorStatus.OPEN, totalNum, travelTime, passengerTime, doorTime);
		this.schedulerPort = schedulerPort;
		try {
			this.host =  InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Creating a server for current instance of the ElevatorSubSystem in a new
		// thread.
		// When this server receives requests, they will be added to the queue for the
		// current
		// SlevatorSubsystem instance.
		server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}
    public ElevatorSubsystem(String name,int port) {
    	this.name = name;
    	this.events = new LinkedList<Request>();
    	server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
    }
	/**
	 * receive request event from the event queue
	 */
	public synchronized void receiveEvent(Request event) {
		events.add(event);
		this.notifyAll();
	}

	/**
	 * get the next request from event queue
	 */
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

	// get the name of the elevator e.g., E1, E2 etc
	public String getName() {
		return this.name;
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
		this.toString(RequestEvent.SENT, "Scheduler", "Stopped at " + this.state.getCurrentFloor() + ".");
		this.sendServer(request);
	}

	/*
	 * Deals with the elevator move up event
	 */
	private void elevatorUp() {
		if (this.state.getDoorStatus() != SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN) {
			this.state.setDirection(SystemEnumTypes.Direction.UP);
			this.state.setStatus(ElevatorCurrentStatus.MOVE);
			this.toString("Elevator motor set to move up. Simulating travel time...");
			try {
				
				Thread.sleep(this.travelTime);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setCurrentFloor(this.state.getCurrentFloor() + 1);
			this.toString(RequestEvent.SENT, "Scheduler", "Arriving at floor " + this.state.getCurrentFloor() + ".");
			ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
					Integer.toString(this.state.getCurrentFloor()),this.state.getDirection());
			this.sendServer(request);
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
			try {
				Thread.sleep(this.travelTime);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
			this.state.setCurrentFloor(this.state.getCurrentFloor() - 1);
			this.toString(RequestEvent.SENT, "Scheduler", "Arriving at floor " + this.state.getCurrentFloor() + ".");
			ElevatorArrivalRequest request = new ElevatorArrivalRequest(this.name,
					Integer.toString(this.state.getCurrentFloor()),this.state.getDirection());
			this.sendServer(request);
		}

	}

	/*
	 * Deals with the elevator door open event
	 */
	private void doorOpen() {
		
		this.toString("Elevator is opening the door...");
		try {
			Thread.sleep(this.doorTime);
		}catch(java.lang.InterruptedException e) {
			e.printStackTrace();
		}
				
		this.state.setDoorStatus(ElevatorCurrentDoorStatus.OPEN);
		this.toString(RequestEvent.SENT, "Scheduler", "Doors are opened.");
		ElevatorDoorRequest request = new ElevatorDoorRequest(this.name, ElevatorCurrentDoorStatus.OPEN);
		this.sendServer(request);
	}
	

	/*
	 * Deals with the elevator door close event
	 */
	private void doorClose() {
		this.toString("Elevator is closing the door...");
		try {
			Thread.sleep(this.doorTime);
		}catch(java.lang.InterruptedException e) {
			e.printStackTrace();
		}
			
		this.state.setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		this.toString(RequestEvent.SENT, "Scheduler", "Doors are closed.");
		ElevatorDoorRequest request = new ElevatorDoorRequest(this.name,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		this.sendServer(request);
	}
   /*
    * Deals with elevator loading passengers
    */
	private void waitforPassengers(){
 		this.toString("Elevator is waiting for passengers to enter...");
 		try {
 			Thread.sleep(this.passengerTime);
 		} catch (java.lang.InterruptedException e) {
 			e.printStackTrace();
 		}
 		Queue<Request> tmp = new LinkedList<Request>();
 		int size = this.events.size();

 		for (int count = 0; count < size; count++) {
 			Request head = events.poll();
 			if (head instanceof ElevatorDestinationRequest){
 				events.offer(head);
 				this.destinationRequestFlag=true;
 			}
 			else {
 				tmp.offer(head);
 			}
 		}
 		events.addAll(tmp);
 		if(!this.destinationRequestFlag){
 			ElevatorWaitRequest request = new ElevatorWaitRequest(this.name);
 			this.sendServer(request);
 		}
 	}

 	private void destinationRequest(ElevatorDestinationRequest request){
 		this.toggleLamp(Integer.parseInt(request.getDestinationFloor()), true);
 		this.sendServer(request);
 		boolean tempflag = false;
 		int size = this.events.size();
 		if(this.destinationRequestFlag) {
 			for (int count = 0; count < size; count++) {
 				Request head = events.peek();
 				if (head instanceof ElevatorDestinationRequest) {
 					tempflag = true;
 				}
 			}
 			if (tempflag) {
 				ElevatorWaitRequest sendRequest = new ElevatorWaitRequest(this.name);
 				this.sendServer(sendRequest);
 				this.destinationRequestFlag = false;
 			}
 		}
 	}
	/*
	 * Send request to server
	 */
	private void sendServer(Request request) {
		try {
			this.server.send(request, this.host.getHostAddress(), this.schedulerPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	private void toString(RequestEvent event, String receiver, String output) {
		if (event.equals(RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT SENT TO " + receiver + "] " + output);
		} else if (event.equals(RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] "
					+ this.name + " : [EVENT RECEIVED FROM " + receiver + "] " + output);
		}
	}

	/*
	 * Deals with the requests and decide the elevator movements
	 */
	private void handleRequest(Request event) {

		// switch statement corresponding to different kinds of request
		if (event instanceof ElevatorArrivalRequest) {
			ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
			this.toString("Sending arrival notice.");
			this.sendServer(request);
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
			this.toString(RequestEvent.RECEIVED, "floor", "destination request to:"+ request.getDestinationFloor());
			this.destinationRequest(request);
		}
		else if (event instanceof ElevatorWaitRequest) {
			this.toString(RequestEvent.RECEIVED, "Scheudler", "Waiting for Passengers");
			this.waitforPassengers();
		}
	}

	@Override
	/**
	 * thread run
	 */
	public void run() {
		while (true) {
			this.handleRequest(this.getNextEvent());
		}
	}

	/**
	 * The main method responsible for creating an instance of the entire
	 * ElevatorSubSystem
	 */
	public static void main(String[] args) {
		// This will return a Map of Maps. Parent key is elevator Name, and the value is
		// map of
		// attributes for that particular elevator instance in accordance to the
		// config.xml file
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();

		// This will return a Map of all attributes for the Scheduler in accorddance to
		// the config.xml)
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();

		int temp = 0;
		for (String floor : floorConfigurations.keySet()) {
			// find amount of floors
			temp += temp;
		}

		// Iterate through each elevator and create an instance of an ElevatorSubsystem
		for (String elevator : elevatorConfigurations.keySet()) {
			// Get the configuration for this particular 'elevatorName'
			HashMap<String, String> elevatorConfiguration = elevatorConfigurations.get(elevator);

			// Create an instance of ElevatorSubsystem for this 'elevatorName'
			ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem(elevator, Integer.parseInt(elevatorConfiguration.get("port")),
					Integer.parseInt(elevatorConfiguration.get("startFloor")), Integer.parseInt(schedulerConfiguration.get("port")),temp,
					Integer.parseInt(elevatorConfiguration.get("timeBetweenFloors")), Integer.parseInt(elevatorConfiguration.get("passengerWaitTime")),
					Integer.parseInt(elevatorConfiguration.get("doorOperationTime")), schedulerConfiguration.get("host"));
					
			// Spawn and start a new thread for this ElevatorSubsystem instance
			Thread elevatorSubsystemThread = new Thread(elevatorSubsystem, elevator);
			elevatorSubsystemThread.start();
		}

	}

}
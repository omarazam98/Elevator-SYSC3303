package main.scheduler;

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

import server.Server;
public class Scheduler implements Runnable {
	
	private String name;
	private Server server;
	private Thread serverThread;
	//private Queue<Request> eventsQueue;
	private boolean debug = false;
	private HashMap<String, Integer> portsByElevatorName;										//key -> elevator name, value -> port number
	private HashMap<String, Integer> portsByFloorName;											//key -> floor number, value -> port number
	private HashMap<String, Monitor> elevatorMonitorByElevatorName;						//key -> elevator name, value -> elevator monitor
	private ArrayList<TripRequest> pendingTripRequests;
	
	public Scheduler(String name, int port, HashMap<String, HashMap<String, String>> elevatorConfiguration, HashMap<String, HashMap<String, String>> floorConfigurations) {
		this.name = name;
		//this.eventsQueue = new LinkedList<Request>();
		this.portsByElevatorName = new HashMap<String, Integer>();
		this.portsByFloorName = new HashMap<String, Integer>();
		this.elevatorMonitorByElevatorName = new HashMap<String, Monitor>();
		this.pendingTripRequests = new ArrayList<TripRequest>();
		
		//Initialize infrastructure configurations (elevators/floors)
		//this.init(elevatorConfiguration, floorConfigurations);
		
		//Create a server (bound to this Instance of ElevatorSubsystem) in a new thread.
		//When this server receives requests, they will be added to the eventsQueue of THIS ElevatorSubsystem instance.
		//this.server = new Server(this, port, this.debug);
		serverThread = new Thread(server, name);
		serverThread.start();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}

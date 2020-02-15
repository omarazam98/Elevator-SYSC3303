package client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sun.javafx.scene.traversal.Direction;

import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import enums.SystemEnumTypes;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.FloorButtonRequest;
import requests.FloorLampRequest;
import requests.Request;
import server.Server;

/**
 * This class is responsible for creating the trip requests for passengers.
 * These request will be used by the elevator system. The floor subsystem will
 * be responsible for: - reading data from the file - creating and sending
 * requests to the elevator - toggling the lamp (button light) when uses press
 * it
 *
 */
public class FloorSubSystem implements Runnable, ElevatorEvents {

	private Server server;
	private String floorNum;
	private Queue<Request> eventRequest;
	private Queue<FloorButtonRequest> upReqList;        //Divides the button requests to up and down
	private Queue<FloorButtonRequest> downReqList;
	private HashMap<String, Integer> elevatorPorts;
	private int schedulerPort;
	private final boolean debug = false;
	private SystemEnumTypes.FloorDirectionLampStatus floorLamp_UP;
	private SystemEnumTypes.FloorDirectionLampStatus floorLamp_DOWN;

	/**
	 * Constructor
	 * 
	 * @param floorNum      the floor number
	 * @param floorPort     the port on which the floor will operate
	 * @param schedulerPort the scheduler port
	 */
	public FloorSubSystem(String floorNum, int floorPort, int schedulerPort, HashMap<String, HashMap<String, String>> elevatorConfig) {

		this.floorNum = floorNum;
		this.eventRequest = new LinkedList<Request>();
		this.elevatorPorts = new HashMap<String, Integer>();
		this.schedulerPort = schedulerPort;
		this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;
		this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;
		
		this.upReqList = new LinkedList<FloorButtonRequest>();
		this.downReqList = new LinkedList<FloorButtonRequest>();
		
		//server for floorSubsystem
		server = new Server(this, floorPort, this.debug);
		Thread serverThread = new Thread(server, floorNum);
		serverThread.start();
		
		//initialize elevator ports list
		for(String elevator: elevatorConfig.keySet()) {
			this.elevatorPorts.put(elevator, Integer.parseInt(elevatorConfig.get(elevator).get("port")));
		}
	}

	@Override
	public synchronized Request getNextEvent() {
		while (eventRequest.isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return eventRequest.poll();
	}

	@Override
	public String getName() {
		return floorNum;
	}

	/**
	 * when up/down button is pressed change the state accordingly
	 * 
	 * @param floorLamp the the light on the button (UP or DOWN)
	 * @param lamp      direction of motion
	 * @return
	 */
	public boolean toggleFloorLamp(String floorLamp, String status) {
		if (floorLamp.equals("UP")) {
			if(status.equals("ON")) {
				this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.ON;
			}
			else {
				this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;
			}
		}
		if (floorLamp.equals("DOWN")) {
			if(status.equals("ON")) {
				this.floorLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.ON;
			}
			else {
				this.floorLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.OFF;
			}
		}
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Tuning "
				+ floorLamp + " button lamp " + status + ".");
		return true;
	}

	/**
	 * this method is responsible for reading in the coming request form the file
	 * 
	 * @param fileName
	 * @return a list with all the requests that are read form the file
	 */

	public static List<FloorButtonRequest> readingInputReq(String fileName) {
		List<FloorButtonRequest> requests = new LinkedList<FloorButtonRequest>();

		InputStream fileRead = FloorSubSystem.class.getClass().getResourceAsStream(fileName);
		try {
			fileRead = new FileInputStream(fileName);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileRead));

			String inputReq = null;
			while ((inputReq = bufferedReader.readLine()) != null) {
				String[] inputInfo = inputReq.split(" ");

				SystemEnumTypes.Direction direction = SystemEnumTypes.Direction.valueOf(inputInfo[2]);
				FloorButtonRequest newReq = new FloorButtonRequest(inputInfo[0], inputInfo[1], direction, inputInfo[3]);
				requests.add(newReq);
			}

			fileRead.close();
			bufferedReader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return requests;
	}

	// receives requests send by Server, Scheduler, etc
	@Override
	public synchronized void receiveEvent(Request request) {
		eventRequest.add(request);
		this.notifyAll();
	}

	/**
	 * Determines the type of Request and calls.
	 * 
	 * @param request the incoming request
	 */
	private void handleRequest(Request request) {
		if (request instanceof FloorButtonRequest) {
			try {
				FloorButtonRequest currRequest = (FloorButtonRequest) request;
				this.server.send(currRequest, InetAddress.getLocalHost(), schedulerPort);
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S"))
						+ "] Button " + currRequest.getDirection() + " at floor " + floorNum + " has been pressed.");
				this.toggleFloorLamp(currRequest.getDirection().toString(), "ON"); // Turn on button lamp
				
				if(((FloorButtonRequest) request).getDirection().equals(Direction.UP)) {
					this.upReqList.add((FloorButtonRequest)request);
				}
				else if(((FloorButtonRequest) request).getDirection().equals(Direction.DOWN)) {
					this.downReqList.add((FloorButtonRequest)request);
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} else if (request instanceof ElevatorArrivalRequest) { 
			ElevatorArrivalRequest currRequest = (ElevatorArrivalRequest) request;

			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor "
					+ floorNum + " : [EVENT RECEIVED FROM Scheduler ] Shut off " + currRequest.getDirec()
					+ " direction lamp.");
			
			toggleFloorLamp(currRequest.getDirec(), "OFF"); // Turn off button lamp
			
			//send request corresponding to direction to elevator
			if(currRequest.getDirec().equals(Direction.UP)) {
				for(FloorButtonRequest fbReq: this.upReqList) {
					ElevatorDestinationRequest desRequest = new ElevatorDestinationRequest(floorNum, fbReq.getDestination(), currRequest.getElevatorName());
					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor "
							+ floorNum + " : [EVENT SEND TO Elevator " + currRequest.getElevatorName() + "] moving to floor : " + fbReq.getDestinationFloor());
					try {
						server.send(desRequest, InetAddress.getLocalHost(), this.elevatorPorts.get(currRequest.getElevatorName()));
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}
				this.upReqList.clear();
			}
			else if(currRequest.getDirec().equals(Direction.DOWN)) {
				for(FloorButtonRequest fbReq: this.downReqList) {
					ElevatorDestinationRequest desRequest = new ElevatorDestinationRequest(floorNum, fbReq.getDestination(), currRequest.getElevatorName());
					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor "
							+ floorNum + " : [EVENT SEND TO Elevator " + currRequest.getElevatorName() + "] moving to floor : " + fbReq.getDestinationFloor());
					try {
						server.send(desRequest, InetAddress.getLocalHost(), this.elevatorPorts.get(currRequest.getElevatorName()));
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}
				this.downReqList.clear();
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			this.handleRequest(getNextEvent());
		}
	}

	/**
	 * This method is responsible for creating an instance of the floor subsystem
	 */
	public static void main(String[] args) throws ParseException {
		List<FloorSubSystem> floors = new LinkedList<FloorSubSystem>();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");

		// Map of all attributes for the Scheduler
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration
				.getSchedulerConfiguration();

		// Map of <floor Name, <attributes for that floor>> 
		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
				.getAllFloorSubsytemConfigurations();
		
		// Map of <ElevatorName, <attributes of elevator>>
		HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration
				.getAllElevatorSubsystemConfigurations();

		// Iterate through each floor and create an instance of an floorSubsystem
		for (String floorName : floorConfigurations.keySet()) {
		
			// Create an instance of floorSubsystem for this 'floorName'
			FloorSubSystem floorSubsystem = new FloorSubSystem(floorName,
					Integer.parseInt(floorConfigurations.get(floorName).get("port")),
					Integer.parseInt(schedulerConfiguration.get("port")),
					elevatorConfigurations);
			floors.add(floorSubsystem);

			// Spawn and start a new thread for this floorSubsystem instance
			Thread floorSubsystemThread = new Thread(floorSubsystem, floorName);
			floorSubsystemThread.start();
		}

		List<FloorButtonRequest> requests = readingInputReq("src/resources/requests.txt"); // Retrieve all requests from
																							// input file

		// Sort requests based on time to be sent
		Collections.sort(requests, new Comparator<FloorButtonRequest>() {
			@Override
			public int compare(FloorButtonRequest r1, FloorButtonRequest r2) {
				Date r1Time = null;
				Date r2Time = null;
				try {
					r1Time = sdf.parse(r1.getButtonPressTime());
					r2Time = sdf.parse(r2.getButtonPressTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}

				if (r1Time.after(r2Time))
					return 1;
				else if (r1Time.before(r2Time))
					return -1;
				else
					return 0;
			}
		});

		long lastTime = 0;

		for (FloorButtonRequest currRequest : requests) { // Loop over requests
			for (FloorSubSystem currFloor : floors) { // Loop over floors
				if (currFloor.getName().equalsIgnoreCase(currRequest.getFloorName())) { // If request is meant for the
																						// current floor

					long currReqTime = (sdf.parse(currRequest.getButtonPressTime())).getTime(); // Get time of request

					// Measure time between last request and current, and sleep for the time
					// difference
					if (lastTime != 0) {
						long timeDiff = currReqTime - lastTime;
						try {
							Thread.sleep(timeDiff);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// Send request to floor to be sent to scheduler
					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S"))
							+ "] Request details // Time:" + currRequest.getButtonPressTime() + "  Floor Name: "
							+ currRequest.getFloorName() + "  Direction: " + currRequest.getDirection()
							+ "  Dest Floor: " + currRequest.getDestinationFloor());
					currFloor.receiveEvent(currRequest);
					lastTime = currReqTime;
				}
			}
		}
	}
}
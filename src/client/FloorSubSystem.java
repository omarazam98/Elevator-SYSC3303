package client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import enums.SystemEnumTypes;
import requests.FloorButtonRequest;
import requests.FloorLampRequest;
import requests.Request;
import server.Server;

//each floor has one floorSubsystem
public class FloorSubSystem implements Runnable, ElevatorEvents{
	
	private Server server;
	private String floorNum;				   
	private Queue<Request> eventRequest;
	private int schedulerPort;				  //which scheduler will work
	private final boolean debug = false;
	private SystemEnumTypes.FloorDirectionLampStatus floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;          //up button have been pressed
	private SystemEnumTypes.FloorDirectionLampStatus floorLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.OFF;                         //direction of elevator
	

	//port used to specify each floorSubSystem in a list of floors
	public FloorSubSystem(String floorNum, int floorPort, int schedulerPort) {
		
		eventRequest = new LinkedList<Request>();
			
		this.schedulerPort = schedulerPort;
		server = new Server(this, floorPort, debug);
		Thread serverThread = new Thread(server, floorNum);
		serverThread.start();
	}
	
	@Override
	public Request getNextEvent() {
		while(eventRequest.isEmpty()) {
			try {
				wait();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		return eventRequest.poll();
	}

	@Override
	public String getName() {
		return floorNum;
	}
	
	//when up/down button is pressed
	public boolean toggleFloorLamp(String floorLamp, String lamp) {
		if(floorLamp.equals("UP")) this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.ON;
		if(floorLamp.equals("DOWN")) this.floorLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.ON;
		else if(floorLamp.equals("OFF")) {
			if(lamp.equals("UP")) this.floorLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;
			if(lamp.equals("DOWN")) this.floorLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.OFF;
		}
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor lamp on floor" + floorNum + ": " + floorLamp_UP.toString() + ", " + floorLamp_DOWN.toString());
		return true;
	}
	
	//the format of input string: "time floor floorButton CarButton"
	public static List<FloorButtonRequest> readingInputReq(String fileName) {
		List<FloorButtonRequest> requests = new LinkedList<FloorButtonRequest>();
		
		FileInputStream fileRead;
		try {
			fileRead = new FileInputStream(fileName);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileRead));
			
			String inputReq = null;
			while((inputReq = bufferedReader.readLine()) != null) {
				String[] inputInfo = inputReq.split(" ");
				
				SystemEnumTypes.Direction direction = SystemEnumTypes.Direction.valueOf(inputInfo[2]);
				
				FloorButtonRequest newReq = new FloorButtonRequest(inputInfo[0], inputInfo[1], direction, inputInfo[3]);
				requests.add(newReq);
			}
			
			fileRead.close();
			bufferedReader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch( IOException e) {
			e.printStackTrace();
		}
		
		
		return requests;
	}
	
	//requests send by Server, Scheduler, etc
	//should be solved when running thread
	@Override
	public synchronized void receiveEvent(Request request) {
		this.eventRequest.add(request);
		this.notifyAll();
	}
	
	private void handleRequest(Request request) {
		if(request instanceof FloorButtonRequest) {
			try {
				this.toggleFloorLamp(request.getDirec(), request.getDirec()); //Turn on button lamp
				this.server.send(request, InetAddress.getLocalHost(), schedulerPort);
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Button " + request.getDirec() + " at floor " + floorNum + " has been pressed.");
			} catch (UnknownHostException e) {
              e.printStackTrace();
          }
		}
		 else if (request instanceof FloorLampRequest) { // If event received is a FloorLampRequest
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor "
						+ floorNum + " : [EVENT RECEIVED FROM Scheduler ] Shut off " + request.getDirec() + " direction lamp.");
				toggleFloorLamp(request.getDirec(), "OFF"); // Turn off button lamp
		 }
	}

	@Override
	public void run() {
		while(true) {
			this.handleRequest(getNextEvent());
		}
	}
	
	public static void main(String[] args) throws ParseException {
		List<FloorSubSystem> floors = new LinkedList<FloorSubSystem>();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");

		// This will return a Map of all attributes for the Scheduler (as per
		// config.xml)
		HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

		// This will return a Map of Maps. First key -> floor Name, Value -> map of
		// all attributes for that floor (as per config.xml)
		HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration.getAllFloorSubsytemConfigurations();

		// Iterate through each floor and create an instance of an floorSubsystem
		for (String floorName : floorConfigurations.keySet()) {
			// Get the configuration for this particular 'floorName'
			HashMap<String, String> floorConfiguration = floorConfigurations.get(floorName);

			// Create an instance of floorSubsystem for this 'floorName'
			FloorSubSystem floorSubsystem = new FloorSubSystem(floorName,
					Integer.parseInt(floorConfiguration.get("port")),
					Integer.parseInt(schedulerConfiguration.get("port")));
			floors.add(floorSubsystem);

			// Spawn and start a new thread for this floorSubsystem instance
			Thread floorSubsystemThread = new Thread(floorSubsystem, floorName);
			floorSubsystemThread.start();
		}

		List<FloorButtonRequest> requests = readingInputReq("resources/requests.txt"); // Retrieve all requests from input file

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
				if (currFloor.getName().equalsIgnoreCase(currRequest.getFloorName())) { // If request is meant for the current floor
					
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
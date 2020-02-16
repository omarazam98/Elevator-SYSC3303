package client;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
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
import java.util.Locale;
import java.util.Queue;

import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import enums.SystemEnumTypes;
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.FloorButtonRequest;
import requests.Request;
import server.Server;



/**
 * The purpose of this class is to create trip requests for passengers to use the elevator system.
 * The floor is responsible for:
 * 	- reading requests from an input file
 *  - creating trip requests to be sent to the elevator
 *  - turning on and off button lamps when uses press them for a request
 */
public class FloorSubSystem implements Runnable, ElevatorEvents {


    private Server server;
    private String floorNum;
    //private Queue<FloorButtonRequest> pickupQueue;                          //Queue of requests to be sent
    private int schedulerPort;
    private final boolean debug = false;
    private final static String requestsFile = "src/resources/requests.txt";
    private SystemEnumTypes.FloorDirectionLampStatus buttonLamp_UP;         //Button lamp for UP button
    private SystemEnumTypes.FloorDirectionLampStatus buttonLamp_DOWN;       //Button lamp for DOWN button
    private Queue<FloorButtonRequest> upReqList;                              //Queue of requests to be sent to elevator taking UP requests
    private Queue<FloorButtonRequest> downReqList;                            //Queue of requests to be sent to elevator taking DOWN requests
    private HashMap<String, Integer> elevatorPorts;                   //Map of ports for each elevator
	private Queue<Request> eventsQueue;
	
    /**
     * Constructor for floor
     *
     * @param name
     * @param port
     * @param schedulerPort
     * @param elevatorConfiguration
     */
    public FloorSubSystem(String floorNum, int port, int schedulerPort, HashMap<String, HashMap<String, String>> elevatorConfiguration) {
        this.floorNum = floorNum;
        this.upReqList = new LinkedList<FloorButtonRequest>();
        this.downReqList = new LinkedList<FloorButtonRequest>();
        this.schedulerPort = schedulerPort;
        this.buttonLamp_UP = SystemEnumTypes.FloorDirectionLampStatus.OFF;
        this.buttonLamp_DOWN = SystemEnumTypes.FloorDirectionLampStatus.OFF;
        this.elevatorPorts = new HashMap<String, Integer>();
		this.eventsQueue = new LinkedList<Request>();

        // Create a server for FloorSubsystem in a new thread.
        server = new Server(this, port, this.debug);
        Thread serverThread = new Thread(server, floorNum);
        serverThread.start();

        //Initialize elevators
        for (String elevatorName : elevatorConfiguration.keySet()) {
            HashMap<String, String> config = elevatorConfiguration.get(elevatorName);
            this.elevatorPorts.put(elevatorName, Integer.parseInt(config.get("port")));
        }
    }

    /**
     * Add an event to the pickupQueue.
     *
     * @param event
     */
    public synchronized void receiveEvent(Request event) {
		eventsQueue.add(event);
        this.notifyAll();                        
    }

    /**
     * Get next event from the pickupQueue.
     *
     * @return next request
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
     * Get the name of this floor
     *
     * @return name of floor
     */
    public String getName() {
        return this.floorNum;
    }

    /**
     * Gets direction enum from string
     *
     * @param s string of direction
     * @return SystemEnumTypes.Direction status
     */
    private static SystemEnumTypes.Direction getDirectionFromString(String s) {
        switch (s.toLowerCase()) {
            case "up":
                return SystemEnumTypes.Direction.UP;
            case "down":
                return SystemEnumTypes.Direction.DOWN;
            default:
                return SystemEnumTypes.Direction.STAY;
        }
    }
    
    /**
     * Converts time in a string to a Date object, and returns it.
     *
     * @param dateString
     * @return Date
     */
    private static Date convertTime(String dateString) {
        DateFormat format = new SimpleDateFormat("hh:mm:ss.SSS", Locale.ENGLISH);
        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Turns floors up/down button lamps on/off
     *
     * @param direction Button lamp with this direction to be modified
     * @param lampStatus Set button lamp to this status
     */
    private void toggleFloorButtonLamp(SystemEnumTypes.Direction direction, SystemEnumTypes.FloorDirectionLampStatus lampStatus) {
        this.consoleOutput("Turning " + direction.toString() + " button lamp " + lampStatus.toString() + ".");
        if (direction == SystemEnumTypes.Direction.UP)
            buttonLamp_UP = lampStatus;
        else if (direction == SystemEnumTypes.Direction.DOWN)
            buttonLamp_DOWN = lampStatus;
    }  

    /**
     * Reads input file at directory to grab requests to be sent to scheduler
     *
     * @return List of requests
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

				SystemEnumTypes.Direction direction = getDirectionFromString(inputInfo[2]);
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

    @Override
    public void run() {
        while (true) {
            this.handleEvent(this.getNextEvent());
        }
    }

    /**
     * This method will determine the type of Request and call the appropriate event handler method for this request.
     * @param event the received event
     */
    private void handleEvent(Request event) {
        //switch statement corresponding to different "event handlers"
        if (event instanceof FloorButtonRequest) {      //If event received is a FloorButtonRequest
            FloorButtonRequest request = (FloorButtonRequest) event;
            
            this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, "Simulated Passenger", "Trip request going " + request.getDirection() + " to " + request.getDestinationFloor());

            if (request.getDirection() == SystemEnumTypes.Direction.UP){
            	upReqList.add(request);
            } else if (request.getDirection() == SystemEnumTypes.Direction.DOWN){
            	downReqList.add(request);
            }

            try {
                //Sends request to scheduler
                this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, "Scheduler", "Trip request going " + request.getDirection());
                this.server.send(request, InetAddress.getLocalHost(), schedulerPort);
                toggleFloorButtonLamp(request.getDirection(), SystemEnumTypes.FloorDirectionLampStatus.ON);   //Turn button lamp on for direction in request
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else if (event instanceof ElevatorArrivalRequest) { //If event received is a ElevatorArrivalRequest
            ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
            this.consoleOutput(SystemEnumTypes.RequestEvent.RECEIVED, "Scheduler" , "Elevator " + request.getElevatorName() + " has arrived. Elevator is headed " + request.getDirection() + ".");
            if (request.getDirection() != SystemEnumTypes.Direction.STAY) {
            	toggleFloorButtonLamp(request.getDirection(), SystemEnumTypes.FloorDirectionLampStatus.OFF);  //Turn off button lamp since Elevator has arrived
            }
            sendRequestsToElevator(request);    //Elevator is arriving, send it trip requests
        }
    }

    /**
     * Method to send the arriving elevator all trip requests for the direction it will be travelling
     *
     * @param request
     */
    private void sendRequestsToElevator (ElevatorArrivalRequest request) {
        if (request.getDirection() == SystemEnumTypes.Direction.UP){    //If Elevator will be going up
            for (FloorButtonRequest currFloorButtonRequest : upReqList){  //Loop through the queue of trip requests going up
                ElevatorDestinationRequest currER = new ElevatorDestinationRequest(this.getName(), currFloorButtonRequest.getDestinationFloor(), request.getElevatorName());    //Create elevator destination request based on data from the queue
                this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, request.getElevatorName(), "Destination request to floor " + currFloorButtonRequest.getDestinationFloor());
                try {
                	//Send the request to the elevator arriving
                	this.server.send(currER, InetAddress.getLocalHost(), this.elevatorPorts.get(request.getElevatorName()));   
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            //after sending all requests, clear queue
            upReqList.clear(); 
        } else if (request.getDirection() == SystemEnumTypes.Direction.DOWN) {    //If elevator will be going down
            for (FloorButtonRequest currFloorButtonRequest : downReqList){    //Loop through the queue of trip requests going down
                ElevatorDestinationRequest currER = new ElevatorDestinationRequest(this.getName(), currFloorButtonRequest.getDestinationFloor(), request.getElevatorName());    //Create elevator destination request based on data from the queue
                this.consoleOutput(SystemEnumTypes.RequestEvent.SENT, request.getElevatorName(), "Destination request to floor" + currFloorButtonRequest.getDestinationFloor());
                try {
                	//Send the request to the elevator arriving
                	this.server.send(currER, InetAddress.getLocalHost(), this.elevatorPorts.get(request.getElevatorName()));   
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            //after sending all requests, clear queue
            downReqList.clear(); 
        }
    }

    /**
     * Prints text with preset beginning and given string
     *
     * @param output string to be printed
     */
    private void consoleOutput(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : " + output);
	}

	private void consoleOutput(SystemEnumTypes.RequestEvent event, String target, String output) {
		if (event.equals(SystemEnumTypes.RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(SystemEnumTypes.RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

    public static void main(String[] args) {
        List<FloorSubSystem> floors = new LinkedList<FloorSubSystem>();

        // Map of all attributes for the Scheduler
        HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

        // Map of <ElevatorName, <attributes of elevator>>
        HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration.getAllElevatorSubsystemConfigurations();

        // Map of <floor Name, <attributes for that floor>> 
        HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
                .getAllFloorSubsytemConfigurations();

        // Iterate through each floor and create an instance of an floorSubsystem
        for (String floorName : floorConfigurations.keySet()) {
        	
            // Get the configuration for this particular 'floorName'
            HashMap<String, String> floorConfiguration = floorConfigurations.get(floorName);

            // Create an instance of floorSubsystem for this 'floorName'
            FloorSubSystem floorSubsystem = new FloorSubSystem(floorName,
                    Integer.parseInt(floorConfiguration.get("port")), Integer.parseInt(schedulerConfiguration.get("port")), elevatorConfigurations);
            floors.add(floorSubsystem);

            // Spawn and start a new thread for this floorSubsystem instance
            Thread floorSubsystemThread = new Thread(floorSubsystem, floorName);
            floorSubsystemThread.start();
        }

        //Retrieve all requests from input file
        List<FloorButtonRequest> requests = readingInputReq(requestsFile);   
        
        //Sort requests based on time to be sent
        Collections.sort(requests, new Comparator<FloorButtonRequest>() {
            @Override
            public int compare(FloorButtonRequest r1, FloorButtonRequest r2) {
                Date r1Time = convertTime(r1.getButtonPressTime());
                Date r2Time = convertTime(r2.getButtonPressTime());

                if (r1Time.after(r2Time))
                    return 1;
                else if (r1Time.before(r2Time))
                    return -1;
                else
                    return 0;
            }
        });

        long lastTime = 0;

        // for every floor in each request
        for (FloorButtonRequest currRequest : requests) {   
            for (FloorSubSystem currFloor : floors) {   
                if (currFloor.getName().equalsIgnoreCase(currRequest.getFloorName())) { 
                	//If request is meant for the current floor
                    long currReqTime = (convertTime(currRequest.getButtonPressTime().toString())).getTime();  
                    //Get time of request

                    if (lastTime != 0) {
                        try {
                            Thread.sleep(currReqTime - lastTime);
                            //sleep for the time difference
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //Send request to floor
                    System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Request details // Time:" + currRequest.getTime() + "  Floor Name: " + currRequest.getFloorName() + "  SystemEnumTypes.Direction: " + currRequest.getDirection() + "  Dest Floor: " + currRequest.getDestinationFloor());
                    currFloor.receiveEvent(currRequest);
                    lastTime = currReqTime;
                }
            }
        }
    }
}

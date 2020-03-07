package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import requests.ElevatorArrivalRequest;
import requests.ElevatorDestinationRequest;
import requests.FloorButtonRequest;
import requests.Request;
import server.Server;
import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.Fault;
import enums.SystemEnumTypes.FloorDirectionLampStatus;
import enums.SystemEnumTypes.RequestEvent;



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
    private int schedulerPort;
    private String schedulerHost;
    private final boolean debug = false;
    private final static String requestsFile = "resources/requests.txt";
    private FloorDirectionLampStatus buttonLamp_UP;                                       //Button lamp for UP button
    private FloorDirectionLampStatus buttonLamp_DOWN;                                     //Button lamp for DOWN button
    private Queue<FloorButtonRequest> upReqList;                              //Queue of requests to be sent to elevator taking UP requests
    private Queue<FloorButtonRequest> downReqList;                            //Queue of requests to be sent to elevator taking DOWN requests
    private HashMap<String, Integer> elevatorPorts;                   //Map of ports for each elevator
    private HashMap<String,String> elevatorHosts;
	private Queue<Request> eventsQueue;
	
    /**
     * Constructor for floor
     *
     * @param name
     * @param port
     * @param schedulerPort
     * @param elevatorConfiguration
     */
    private FloorSubSystem(String name, int port, int schedulerPort, String schedulerHost, HashMap<String, HashMap<String, String>> elevatorConfiguration) {
        //Set fields
        this.floorNum = name;
        this.upReqList = new LinkedList<FloorButtonRequest>();
        this.downReqList = new LinkedList<FloorButtonRequest>();
        this.schedulerPort = schedulerPort;
        this.schedulerHost = schedulerHost;
        this.buttonLamp_UP = FloorDirectionLampStatus.OFF;
        this.buttonLamp_DOWN = FloorDirectionLampStatus.OFF;
        this.elevatorPorts = new HashMap<String, Integer>();
        this.elevatorHosts = new HashMap<String,String>();
		this.eventsQueue = new LinkedList<Request>();

		// Create a server for FloorSubsystem in a new thread.
		server = new Server(this, port, this.debug);
        Thread serverThread = new Thread(server, name);
        serverThread.start();

      //Initialize elevators
        for (String elevatorName : elevatorConfiguration.keySet()) {
            HashMap<String, String> config = elevatorConfiguration.get(elevatorName);
            this.elevatorPorts.put(elevatorName, Integer.parseInt(config.get("port")));
            this.elevatorHosts.put(elevatorName, config.get("host"));
        }
    }

    /**
     * Add an event to the eventsQueue.
     *
     * @param event
     */
    @Override
    public synchronized void receiveEvent(Request event) {
		eventsQueue.add(event);
        this.notifyAll();                        //Notify all listeners
    }

    /**
     * Get next event from the eventsQueue.
     *
     * @return next request
     */
    @Override
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
     * @return Direction status
     */
    private static Direction getDirectionFromString(String s) {
        switch (s.toLowerCase()) {
            case "up":
                return Direction.UP;
            case "down":
                return Direction.DOWN;
            default:
                return Direction.STAY;
        }
    }


    /**
     * Gets fault enum from string
     *
     * @param s string of fault
     * @return Fault status
     */
    private static Fault getFaultFromString(String s) {
        switch (s.toLowerCase()) {
            case "door":
                return Fault.DOOR;
            case "motor":
                return Fault.MOTOR;
            default:
                return null;
        }
    }

    /**
     * Send a request to port using this object's server.
     *
     * @param request
     * @param port
     */
    private void sendToServer(Request request, String host, int port) {
            this.server.send(request, host, port);
    }

    /**
     * Turns floors up/down button lamps on/off
     *
     * @param direction Button lamp with this direction to be modified
     * @param FloorDirectionLampStatus Set button lamp to this status
     */
    private void toggleFloorButtonLamp(Direction direction, FloorDirectionLampStatus FloorDirectionLampStatus) {
        this.toString("Turning " + direction.toString() + " button lamp " + FloorDirectionLampStatus.toString() + ".");
        if (direction == Direction.UP)
            buttonLamp_UP = FloorDirectionLampStatus;
        else if (direction == Direction.DOWN)
            buttonLamp_DOWN = FloorDirectionLampStatus;
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
     * Reads input file at directory to grab requests to be sent to scheduler
     *
     * @return List of requests
     */
    private static List<FloorButtonRequest> readInputFromFile() {
        FileReader input = null;
        try {
            String requestsFilePath = new File(FloorSubSystem.class.getClassLoader().getResource(requestsFile).getFile()).getAbsolutePath().replace("%20", " "); //Retrieves input file
            input = new FileReader(requestsFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader bufRead = new BufferedReader(input);
        String myLine;

        List<FloorButtonRequest> requests = new LinkedList<FloorButtonRequest>();   //List of requests

        try {
            while ((myLine = bufRead.readLine()) != null) { //Loops through each line in file
                String[] info = myLine.split(" ");  //Splits line based on a space

                //Retrieve data from each line
                String time = info[0];
                String floorName = info[1];
                Direction direction = getDirectionFromString(info[2]);
                String destinationFloor = info[3];
                Fault floorFault;
                if (info.length != 5)
                    floorFault = null;
                else
                    floorFault = getFaultFromString(info[4]);

                //Create floor button request with retrieved data, and add to ongoing list
                FloorButtonRequest currRequest = new FloorButtonRequest(time, floorName, direction, destinationFloor, floorFault);
                requests.add(currRequest);
            }
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
            
            this.toString(RequestEvent.RECEIVED, "Simulated Passenger", "Trip request going " + request.getDirection() + " to " + request.getDestinationFloor());

            if (request.getDirection() == Direction.UP){
                upReqList.add(request);
            } else if (request.getDirection() == Direction.DOWN){
                downReqList.add(request);
            }
                //Sends request to scheduler
                this.toString(RequestEvent.SENT, "Scheduler", "Trip request going " + request.getDirection());
                this.server.send(request, schedulerHost, schedulerPort);
                toggleFloorButtonLamp(request.getDirection(), FloorDirectionLampStatus.ON);   //Turn button lamp on for direction in request
        } else if (event instanceof ElevatorArrivalRequest) { //If event received is a ElevatorArrivalRequest
            ElevatorArrivalRequest request = (ElevatorArrivalRequest) event;
            this.toString(RequestEvent.RECEIVED, "Scheduler" , "Elevator " + request.getElevatorName() + " has arrived. Elevator is headed " + request.getDirection() + ".");
            if (request.getDirection() != Direction.STAY) {
            	toggleFloorButtonLamp(request.getDirection(), FloorDirectionLampStatus.OFF);  //Turn off button lamp since Elevator has arrived
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
        if (request.getDirection() == Direction.UP){    //If Elevator will be going up
            for (FloorButtonRequest currFloorButtonRequest : upReqList){  //Loop through the queue of trip requests going up
                ElevatorDestinationRequest currER = new ElevatorDestinationRequest(this.getName(), currFloorButtonRequest.getDestinationFloor(), request.getElevatorName(), currFloorButtonRequest.getFault());    //Create elevator destination request based on data from the queue
                this.toString(RequestEvent.SENT, request.getElevatorName(), "Destination request to floor " + currFloorButtonRequest.getDestinationFloor());
                sendToServer(currER, this.elevatorHosts.get(request.getElevatorName()), this.elevatorPorts.get(request.getElevatorName()));    //Send the request to the elevator arriving
            }
            upReqList.clear(); //Clear requests from queue, since they've been sent
        } else if (request.getDirection() == Direction.DOWN) {    //If elevator will be going down
            for (FloorButtonRequest currFloorButtonRequest : downReqList){    //Loop through the queue of trip requests going down
                ElevatorDestinationRequest currER = new ElevatorDestinationRequest(this.getName(), currFloorButtonRequest.getDestinationFloor(), request.getElevatorName(), currFloorButtonRequest.getFault());    //Create elevator destination request based on data from the queue
                this.toString(RequestEvent.SENT, request.getElevatorName(), "Destination request to floor" + currFloorButtonRequest.getDestinationFloor());
                sendToServer(currER, this.elevatorHosts.get(request.getElevatorName()), this.elevatorPorts.get(request.getElevatorName()));    //Send the request to the elevator arriving
            }
            downReqList.clear();   //Clear requests from queue, since they've been sent
        }
    }

    /**
     * Prints text with preset beginning and given string
     *
     * @param output string to be printed
     */
    private void toString(String output) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : " + output);
	}

	private void toString(RequestEvent event, String target, String output) {
		if (event.equals(RequestEvent.SENT)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : [EVENT SENT TO " + target + "] " + output);
		} else if (event.equals(RequestEvent.RECEIVED)) {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor " + this.floorNum + " : [EVENT RECEIVED FROM " + target + "] " + output);
		}
	}

    public static void main(String[] args) {
        List<FloorSubSystem> floors = new LinkedList<FloorSubSystem>();

        //This will return a Map of all attributes for the Scheduler (as per config.xml)
        HashMap<String, String> schedulerConfiguration = ElevatorSystemConfiguration.getSchedulerConfiguration();

        //This will return a Map of Maps. First key -> elevator Name, Value -> map of all attributes for that elevator (as per config.xml)
        HashMap<String, HashMap<String, String>> elevatorConfigurations = ElevatorSystemConfiguration.getAllElevatorSubsystemConfigurations();

        // This will return a Map of Maps. First key -> floor Name, Value -> map of
        // all attributes for that floor (as per config.xml)
        HashMap<String, HashMap<String, String>> floorConfigurations = ElevatorSystemConfiguration
                .getAllFloorSubsytemConfigurations();

        // Iterate through each floor and create an instance of an FloorSubSystem
        for (String floorName : floorConfigurations.keySet()) {
            // Get the configuration for this particular 'floorName'
            HashMap<String, String> floorConfiguration = floorConfigurations.get(floorName);

            // Create an instance of FloorSubSystem for this 'floorName'
            FloorSubSystem FloorSubSystem = new FloorSubSystem(floorName,
                    Integer.parseInt(floorConfiguration.get("port")), Integer.parseInt(schedulerConfiguration.get("port")), schedulerConfiguration.get("host"), elevatorConfigurations);
            floors.add(FloorSubSystem);

            // Spawn and start a new thread for this FloorSubSystem instance
            Thread FloorSubSystemThread = new Thread(FloorSubSystem, floorName);
            FloorSubSystemThread.start();
        }


        List<FloorButtonRequest> requests = readInputFromFile();    //Retrieve all requests from input file

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

        for (FloorButtonRequest currRequest : requests) {   //Loop over requests
            for (FloorSubSystem currFloor : floors) {   //Loop over floors
                if (currFloor.getName().equalsIgnoreCase(currRequest.getFloorName())) { //If request is meant for the current floor
                    long currReqTime = (convertTime(currRequest.getButtonPressTime())).getTime();  //Get time of request

                    //Measure time between last request and current, and sleep for the time difference
                    if (lastTime != 0) {
                        long timeDiff = currReqTime - lastTime;
                        try {
                            Thread.sleep(timeDiff);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //Send request to floor to be sent to scheduler
                    Fault requestFault = currRequest.getFault();
                    String requestFaultString;
                    if (requestFault == null){
                        requestFaultString = "None";
                    }
                    else{
                        requestFaultString = requestFault.toString();
                    }
                    System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Request details // Time:" + currRequest.getButtonPressTime() + "  Floor Name: " + currRequest.getFloorName() + "  Direction: " + currRequest.getDirection() + "  Dest Floor: " + currRequest.getDestinationFloor() + "  Fault: " + requestFaultString);
                    currFloor.receiveEvent(currRequest);
                    lastTime = currReqTime;
                }
            }
        }
    }

}

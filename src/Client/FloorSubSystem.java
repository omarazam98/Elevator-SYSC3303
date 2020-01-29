package Client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import info.LampStatus;
import info.Request;
import info.RequestType;

//each floor has one floorSubsystem
public class FloorSubSystem implements Runnable, ElevatorSystemComponent{
	private Scheduler scheduler;
	private Server server;
	private String floorNum;
	private int floorPort;				   
	private int topFloor;
	private Queue<Request> buttonRequest_UP;
	private Queue<Request> buttonRequest_DOWN;
	private Queue<Request> eventRequest;
	private int schedulerPort;				  //which scheduler will work
	private final boolean debug = false;
	private LampStatus floorLamp_UP;          //up button have been pressed
	private LampStatus floorLamp_DOWN;
	private LampStatus directLamp;         //the arrival of elevator and direction of elevator
	

	//port used to specify each floorSubSystem in a list of floors
	public FloorSubSystem(String floorNum, int floorPort, int topFloor, int schedulerPort) {
		if(floorPort != topFloor) buttonRequest_UP = new LinkedList<Request>();
		if(floorPort != 0) buttonRequest_DOWN = new LinkedList<Request>();
		
		this.floorPort = floorPort;
		this.topFloor = topFloor;
		floorLamp_UP.setStr("OFF");
		floorLamp_DOWN.setStr("OFF");
		directLamp.setStr("OFF");
		this.schedulerPort = schedulerPort;
		server = new Server(this, floorPort, debug);
	}
	
	//when up/down button is pressed
	public boolean toggleFloorLamp(String floorLamp, String lamp) {
		if(Integer.parseInt(floorNum) == 0 && floorLamp.equals("DOWN")) return false;
		if(Integer.parseInt(floorNum) == topFloor && floorLamp.equals("UP")) return false;
		if(floorLamp.equals("UP")) this.floorLamp_UP.setStr(floorLamp);
		if(floorLamp.equals("DOWN")) this.floorLamp_DOWN.setStr(floorLamp);
		else if(floorLamp.equals("OFF")) {
			if(lamp.equals("UP")) this.floorLamp_UP.setStr(floorLamp);
			if(lamp.equals("DOWN")) this.floorLamp_DOWN.setStr(floorLamp);
		}
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Floor lamp on floor" + floorNum + ": " + floorLamp_UP.toString() + ", " + floorLamp_DOWN.toString());
		return true;
	}
	
	//set by scheduler
	//depends on the direction of elevator
	public void toggleDirecLamp(String direcLamp) {
		this.directLamp.setStr(direcLamp);
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Direction lamp on floor" + floorNum + ": " + direcLamp);
	}
	
	public String getFloorNumber() {
		return floorNum;
	}
	
	public synchronized Queue<Request> getUpRequests() {
		return this.buttonRequest_UP;
	}
	
	public synchronized Queue<Request> getDownRequests(){
		return this.buttonRequest_DOWN;
	}
	
	
	//the format of input string: "time floor floorButton CarButton"
	public boolean readingInputReq(String fileName) {
		
		FileInputStream fileRead = new FileInputStream(fileName);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileRead));
		
		String inputReq = null;
		while((inputReq = bufferedReader.readLine()) != null) {
			String[] inputInfo = inputReq.split(" ");
			
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");
			Date date = sdf.parse(inputInfo[0]);
			
			int floor = Integer.parseInt(inputInfo[1]);
			int carButton = Integer.parseInt(inputInfo[3]);
			String direction = inputInfo[2];
			
			if(floor == 0 && direction.equals(LampStatus.DOWN)) return false;
			if(floor == topFloor && direction.equals(LampStatus.UP)) return false;
			
			Request newReq = new Request(date, floor, direction, carButton, RequestType.FLOORBUTTON);
			if(direction.equals(LampStatus.UP)) buttonRequest_UP.add(newReq);
			if(direction.equals(LampStatus.DOWN))buttonRequest_DOWN.add(newReq);
			toggleFloorLamp(direction, direction); //set the floor lamp
		}
		
		fileRead.close();
		bufferedReader.close();
		
		return true;
	}
	
	//requests send by Server, Scheduler, etc
	//should be solved when running thread
	public synchronized void receiveEvents(Request request) {
		this.eventRequest.add(request);
		this.notifyAll();
	}
	
	public void handleRequest(Request request) {
		if(request.equals(RequestType.FLOORBUTTON)) {
			try {
				this.toggleDirecLamp(request.getDirec());
				this.server.send(request, InetAddress.getLocalHost(), schedulerPort);
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Button " + request.getDirec() + " at floor " + floorNum + " has been pressed.");
			} catch (UnknownHostException e) {
              e.printStackTrace();
          }
		}
		else if(request.equals(RequestType.ELEVATORARRIVAL)) {
			//send corresponding requests (up/down) to elevator
			if(request.getDirec().equals("UP")) {
				//elevator.getRequest(buttonRequest_UP);
				this.toggleFloorLamp("UP", "OFF");
			}
			else if(request.getDirec().contentEquals("DOWN")) {
				//elevator.getRequest(buttonRequest_DOWN);
				this.toggleFloorLamp("DOWN", "OFF");
			}
			
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.S")) + "] Elevator has arrived at floor " + floorNum + ", the direction of elevator is: " + request.getDirec());
		}
	}
	
	//when elevator arrives
	//send the requirement list to elevator
	//clear all the requests of that floor
	private Queue<Request> elevatorArrive(String direction) {
		Queue<Request> tempList = new LinkedList<Request>();

		if(direction.equals("UP")){
			tempList = buttonRequest_UP;
			buttonRequest_UP.clear();
		}
		if(direction.equals("DOWN")) {
			tempList = buttonRequest_DOWN;
			buttonRequest_DOWN.clear();
		}
		return tempList;
	}

	@Override
	public void run() {

		while(true) {
			while(eventRequest.isEmpty()) {
				try {
					wait();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			this.handleRequest(this.eventRequest.poll());
		}
	}
	

}


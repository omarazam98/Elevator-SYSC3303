package Client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import info.LampStatus;
import info.Request;
import info.RequestType;

//each floor has one floorSubsystem
public class FloorSubSystem implements Runnable{
	private Scheduler scheduler;
	private Server server;
	private int floor;				   //0 is the ground floor
	private int topFloor;
	private Queue<Request> buttonRequest_UP;
	private Queue<Request> buttonRequest_DOWN;
	private Queue<Request> eventRequest;
	//private int schedulerPort;
	private LampStatus floorLamp_UP;          //up button have been pressed
	private LampStatus floorLamp_DOWN;
	private LampStatus directLamp;         //the arrival of elevator and direction of elevator
	
	/**
	 * 
	 * @param level
	 */
	public FloorSubSystem(int floor, int topFloor) {
		if(floor != topFloor) buttonRequest_UP = new LinkedList<Request>();
		if(floor != 0) buttonRequest_DOWN = new LinkedList<Request>();
		this.floor = floor;
		this.topFloor = topFloor;
		floorLamp_UP.setStr("OFF");
		floorLamp_DOWN.setStr("OFF");
		directLamp.setStr("OFF");
		//server = new Server();
	}
	
	//when up/down button is pressed
	public boolean toggleFloorLamp(String floorLamp, String lamp) {
		if(floor == 0 && floorLamp.equals("DOWN")) return false;
		if(floor == topFloor && floorLamp.equals("UP")) return false;
		if(floorLamp.equals("UP")) this.floorLamp_UP.setStr(floorLamp);
		if(floorLamp.equals("DOWN")) this.floorLamp_DOWN.setStr(floorLamp);
		else if(floorLamp.equals("OFF")) {
			if(lamp.equals("UP")) this.floorLamp_UP.setStr(floorLamp);
			if(lamp.equals("DOWN")) this.floorLamp_DOWN.setStr(floorLamp);
		}
		System.out.println("Floor lamp on level" + floor + ": " + floorLamp_UP.toString() + ", " + floorLamp_DOWN.toString());
		return true;
	}
	
	//set by scheduler
	//depends on the direction of elevator
	public void toggleDirecLamp(String direcLamp) {
		this.directLamp.setStr(direcLamp);
		System.out.println("Direction lamp on level" + floor + ": " + direcLamp);
	}
	
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
			//scheduler.getReq(newReq);
			//send request to scheduler
		}
		
		fileRead.close();
		bufferedReader.close();
		
		return true;
	}
	
	//requests send by Server, Scheduler, etc
	//should be solved when running thread
	public void receiveEvents(Request request) {
		this.eventRequest.add(request);
	}
	
	public void handleRequest(Request request) {
		if(request.equals(RequestType.FLOORDIRECTIONLAMP)) {
			this.toggleDirecLamp(request.getDirec());
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
			
			System.out.println("Elevator has arrived at level " + floor + ", the direction of elevator is: " + request.getDirec());
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

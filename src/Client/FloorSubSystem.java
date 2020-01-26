package Client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import info.Request;
import scheduler.Scheduler;

//each floor has one floorSubsystem
public class FloorSubSystem implements Runnable{
	private Scheduler scheduler;
	private int floor;				   //0 is the ground floor
	private int topFloor;
	private Queue<Request> requestList;
	//private int schedulerPort;
	private String floorLamp;          //up/down button have been pressed
	boolean floorLampOn;
	private String directLamp;         //the arrival of elevator and direction of elevator
	
	/**
	 * 
	 * @param level
	 */
	public FloorSubSystem(int floor, int topFloor) {
		requestList = new LinkedList<Request>();
		this.floor = floor;
		this.topFloor = topFloor;
	}
	
	//when up/down button is pressed
	public boolean toggleFloorLamp(String floorLamp, boolean isOn) {
		if(floor == 0 && floorLamp.equals("DOWN")) return false;
		if(floor == topFloor && floorLamp.equals("UP")) return false;
		this.floorLamp = floorLamp;
		System.out.println("Floor lamp: " + floorLamp);
		this.floorLampOn = isOn;
		return true;
	}
	
	//set by scheduler
	//depends on the direction of elevator
	public void toggleDirecLamp(String direcLamp) {
		this.directLamp = direcLamp;
		System.out.println("Direction lamp: " + direcLamp);
	}
	
	public boolean readingInputReq(String inputFile) {
		String[] inputInfo = inputFile.split(" ");
		
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.mmm");
		Date date = sdf.parse(inputInfo[0]);
		
		int floor = Integer.parseInt(inputInfo[1]);
		int carButton = Integer.parseInt(inputInfo[3]);
		String direction = inputInfo[2];
		
		if(floor == 0 && direction.equals("DOWN")) return false;
		if(floor == topFloor && floorLamp.equals("UP")) return false;
		
		Request newReq = new Request(date, floor, direction, carButton);
		requestList.add(newReq);
		toggleFloorLamp(direction, true); //set the floor lamp
		//scheduler.getReq(newReq);
		//send request to scheduler
		return true;
	}
	
	//when elevator arrives
	//send the requirement list to elevator
	//clear all the requests of that floor
	public Queue<Request> elevatorArrive() {
		Queue<Request> tempList = requestList;
		requestList.clear();
		return tempList;
	}

	@Override
	public void run() {
		while(true) {
			if(floorLampOn) {
				//get the floor of elevator
				//print
			}	
		}
	}
	

}

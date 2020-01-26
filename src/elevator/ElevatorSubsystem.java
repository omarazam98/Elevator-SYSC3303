package elevator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import info.Request;
import scheduler.Scheduler;



public class ElevatorSubsystem implements Runnable{
	
    private Scheduler scheduler;
    private Queue<Request> eventQueue;
    private Direction direction;
    private ElevatorDoorStatus doorstatus;
    private ElevatorStatus status;
    private int currentfloor;
    private ArrayList <Integer> destination;
    private ArrayList <Boolean> lamp;
    private int totalNum;
    
    
    public ElevatorSubsystem (Scheduler scheduler){
    	this.scheduler = scheduler;
    	this.eventQueue = new LinkedList<Request>();
    	this.direction = null;
    	this.doorstatus = ElevatorDoorStatus.CLOSE;
    	this.status = status.STOP;
    	this.currentfloor = 1;
    	this.destination = new ArrayList<Integer>();
    	this.lamp = new ArrayList<Boolean>();
    	this.totalNum = 0;
    	
    	
    }
    public void receiveEvent(Request r) {
    	eventQueue.add(r);
    	int i = r.getCarButton();
    	destination.add(i-1, i);
    	lampOn(i);
    	
    }
    public Request getNextEvent() {
    	Request r = eventQueue.poll();
    	int i = r.getCarButton();
    	destination.remove(i-1);
    	lampOff(i);
    	return r;
    }
    
    public void setTotalNum(int totalNum) {
    	this.totalNum = totalNum;
    	this.lamp = new ArrayList<Boolean>(totalNum);
    	this.destination = new ArrayList<Integer>(totalNum);
    	for(int i=0;i<lamp.size();i++) {
    		lamp.add(i,false);
    	}
    }
    public void lampOn(int i) {
    	//System.out.println("The floor" +destination.get(i) )
    	lamp.set(i-1, true);
    }
    public void lampOff(int i) {
    	lamp.set(i-1, false);
    }
    public void doorOpen() {
    	doorstatus = ElevatorDoorStatus.OPEN;
    	System.out.println("Door is open");
    }
    public void doorClose() {
    	doorstatus = ElevatorDoorStatus.CLOSE;
    	System.out.println("Door is close");
    }
    public void run() {
        
    }
	

}

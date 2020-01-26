package main.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;


/**
 * This class is responsible to maintain the current state of the elevator and queue containing information 
 * regarding the trips. Everytime the elevator changes its state, the class shall be updated.
 * The class also determines if the elevator can accept a trip request based Each monitor
 *  will be responsible for a single Elevator.
 *
 */
public class Monitor {
	private String currentELevatorName;
	private LinkedHashSet<TripRequest> requestInQueue;
	private HashSet<Integer> destinationFloor;
	private HashSet<Integer> requestFloor;	
	//private Direction queueDirection;
	private ArrayList<TripRequest> tripRequestCompletionSuccess;
	//private ElevatorState elevatorCurrentState;
	
	//public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation, Direction currentElevatorDirection, ElevatorStatus currentElevatorStatus, ElevatorDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors) {
		
	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation, Integer totalNumberOfFloors) {
		this.currentELevatorName = elevatorName;
		this.requestInQueue = new LinkedHashSet<TripRequest>();
		this.destinationFloor = new HashSet<Integer>();
		this.requestFloor = new HashSet<Integer>();
		this.tripRequestCompletionSuccess = new ArrayList<TripRequest>();
		//this.queueDirection = Direction.IDLE;
		//this.elevatorCurrentState = new ElevatorState(
		//		elevatorStartFloorLocation,
			//	currentElevatorFloorLocation,
				//currentElevatorDirection,
			//	currentElevatorStatus,
		//		currentElevatorDoorStatus,
			//	totalNumberOfFloors);
	}
}

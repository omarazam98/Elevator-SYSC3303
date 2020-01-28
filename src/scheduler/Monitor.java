package scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import elevator.Direction;
import elevator.ElevatorDoorStatus;
import elevator.ElevatorState;
import elevator.ElevatorStatus;


/**
 * This class is responsible to maintain the current state of the elevator and queue containing information 
 * regarding the trips. Everytime the elevator changes its state, the class shall be updated.
 * The class also determines if the elevator can accept a trip request based Each monitor
 *  will be responsible for a single Elevator.
 *
 */
public class Monitor {
	private String currentELevatorName;
	private LinkedHashSet<MakeTrip> requestInQueue;
	private HashSet<Integer> destinationFloor;
	private HashSet<Integer> requestFloor;	
	private Direction queueDirection;
	private ArrayList<MakeTrip> tripRequestCompletionSuccess;
	private ElevatorState elevatorCurrentState;
	
	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation, Direction currentElevatorDirection, ElevatorStatus currentElevatorStatus, ElevatorDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors) {
		
		this.currentELevatorName = elevatorName;
		this.requestInQueue = new LinkedHashSet<MakeTrip>();
		this.destinationFloor = new HashSet<Integer>();
		this.requestFloor = new HashSet<Integer>();
		this.tripRequestCompletionSuccess = new ArrayList<MakeTrip>();
		this.queueDirection = Direction.STAY;
	//	this.elevatorCurrentState = new ElevatorState(elevatorStartFloorLocation, currentElevatorFloorLocation, currentElevatorDirection, currentElevatorStatus, currentElevatorDoorStatus, totalNumberOfFloors);
	}

	public Object getElevatorStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean addFirstTripRequest(MakeTrip requestedTrip) {
		// TODO Auto-generated method stub
		return false;
	}

	public void updateElevatorDirection(Direction nextDirection) {
		// TODO Auto-generated method stub
		
	}

	public Direction getNextElevatorDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateElevatorDoorStatus(ElevatorDoorStatus close) {
		// TODO Auto-generated method stub
		
	}

	public void updateElevatorStatus(ElevatorStatus stop) {
		// TODO Auto-generated method stub
		
	}

	public HashSet<MakeTrip> stopOccurred() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getQueueDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean addEnRouteTripRequest(MakeTrip tripRequest) {
		// TODO Auto-generated method stub
		return false;
	}

	public Integer getElevatorFloorLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer getElevatorStartingFloorLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateElevatorFloorLocation(int floorNumber) {
		// TODO Auto-generated method stub
		
	}

	public boolean isStopRequired(int floorNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isPickupFloor(int floorNumber) {
		// TODO Auto-generated method stub
		return false;
	}
}

package elevator;

import java.util.HashMap;

import enums.SystemEnumTypes;

public class ElevatorState {

	private int startFloor;
	private int currentFloor;
	private SystemEnumTypes.Direction direction;
	private SystemEnumTypes.ElevatorCurrentStatus status;
	private SystemEnumTypes.ElevatorCurrentDoorStatus doorStatus;
	private int totalNum;// change name
	private HashMap<Integer, Boolean> lamps;

	public ElevatorState(int start, int current, SystemEnumTypes.Direction direction,
			SystemEnumTypes.ElevatorCurrentStatus status, SystemEnumTypes.ElevatorCurrentDoorStatus doorStatus,
			int totalNum) {

		this.startFloor = start;
		this.currentFloor = current;
		this.direction = direction;
		this.status = status;
		this.doorStatus = doorStatus;
		this.totalNum = totalNum;// change name
		this.lamps = new HashMap<Integer, Boolean>();

		for (int i = 1; i <= this.totalNum; i++) {
			lamps.put(i, false);
		}

	}

	public int getStartFloor() {
		return this.startFloor;
	}

	public void setStartFloor(int start) {
		this.startFloor = start;

	}

	public int getCurrentFloor() {
		return this.currentFloor;
	}

	public void setCurrentFloor(int i) {
		this.currentFloor = i;
	}

	public SystemEnumTypes.Direction getDirection() {
		return this.direction;
	}

	public void setDirection(SystemEnumTypes.Direction direction) {
		this.direction = direction;
	}

	public SystemEnumTypes.ElevatorCurrentStatus getCurrentStatus() {
		return this.status;
	}

	public void setStatus(SystemEnumTypes.ElevatorCurrentStatus status) {
		this.status = status;
	}

	public SystemEnumTypes.ElevatorCurrentDoorStatus getDoorStatus() {
		return doorStatus;
	}

	public void setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus doorStatus) {
		this.doorStatus = doorStatus;
	}

	public int getHighestFloor() {
		return this.totalNum;// change name
	}

	public void toggleLamp(int floor, boolean toggle) {
		lamps.put(floor, toggle);// change name
	}

}
package elevator;

import java.util.HashMap;



import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorStatus;
import enums.SystemEnumTypes.ElevatorDoorStatus;


public class ElevatorState {

	private int startFloor;
	private int currentFloor;
	private Direction direction;
	private ElevatorStatus status;
	private ElevatorDoorStatus doorStatus;
	private int totalNum;
	private HashMap<Integer, Boolean> lamps;

	public ElevatorState(int start, int current, Direction direction,
			ElevatorStatus status, ElevatorDoorStatus doorStatus,
			int totalNum) {

		this.startFloor = start;
		this.currentFloor = current;
		this.direction = direction;
		this.status = status;
		this.doorStatus = doorStatus;
		this.totalNum = totalNum;
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

	public Direction getDirection() {
		return this.direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public ElevatorStatus getCurrentStatus() {
		return this.status;
	}

	public void setStatus(ElevatorStatus status) {
		this.status = status;
	}

	public ElevatorDoorStatus getDoorStatus() {
		return doorStatus;
	}

	public void setDoorStatus(ElevatorDoorStatus doorStatus) {
		this.doorStatus = doorStatus;
	}

	public int getHighestFloor() {
		return this.totalNum;
	}

	public void toggleLamp(int floor, boolean toggle) {
		lamps.put(floor, toggle);
	}
}
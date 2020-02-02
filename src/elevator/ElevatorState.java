package elevator;

import java.util.HashMap;

import enums.SystemEnumTypes.Direction;
import enums.SystemEnumTypes.ElevatorCurrentStatus;
import enums.SystemEnumTypes.ElevatorCurrentDoorStatus;

/**
 * This class is responsible for determining the current elevator state including:
 * - the direction it is going
 * - the state of the doors
 * - the doors status
 *
 */
public class ElevatorState {

	private int startFloor;
	private int currentFloor;
	private Direction direction;
	private ElevatorCurrentStatus status;
	private ElevatorCurrentDoorStatus doorStatus;
	private int totalNum;
	private HashMap<Integer, Boolean> lamps;

	/**
	 * Constructor
	 * @param start the floor on which the elevator is starting its journey
	 * @param current the current floor of the elevator
	 * @param direction the direction in which the elevator is going
	 * @param status the current status
	 * @param doorStatus the state of the doors
	 * @param totalNum the total number of floors
	 */
	public ElevatorState(int start, int current, Direction direction, ElevatorCurrentStatus status,
			ElevatorCurrentDoorStatus doorStatus, int totalNum) {

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

	// get start floor
	public int getStartFloor() {
		return this.startFloor;
	}

	// set start floor
	public void setStartFloor(int start) {
		this.startFloor = start;

	}

	// get current floor
	public int getCurrentFloor() {
		return this.currentFloor;
	}

	// set current floor
	public void setCurrentFloor(int i) {
		this.currentFloor = i;
	}

	// get current elevator direction
	public Direction getDirection() {
		return this.direction;
	}

	// set current elevator direction
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	// get elevator status is moving or stay
	public ElevatorCurrentStatus getCurrentStatus() {
		return this.status;
	}

	// set elevator status is moving or stay
	public void setStatus(ElevatorCurrentStatus status) {
		this.status = status;
	}

	// get elevator door status is close or open
	public ElevatorCurrentDoorStatus getDoorStatus() {
		return doorStatus;
	}

	// set elevator door status is close or open
	public void setDoorStatus(ElevatorCurrentDoorStatus doorStatus) {
		this.doorStatus = doorStatus;
	}

	// get the highest floor
	public int getHighestFloor() {
		return this.totalNum;
	}

	// set the highest floor
	public void setHighestFloor(int i) {
		this.totalNum = i;
	}

	public void toggleLamp(int floor, boolean toggle) {
		lamps.put(floor, toggle);
	}
}
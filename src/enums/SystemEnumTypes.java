package enums;

public class SystemEnumTypes {
	public enum RequestEvent {
		SENT, RECEIVED
	}

	/*
	 * public enum DoorState { OPENED, CLOSED }
	 */
	public enum FloorDirectionLampStatus {
		ON, OFF
	}

	public enum Direction {
		UP, DOWN, STAY
	}

	public enum ElevatorStatus {
		MOVE, STOP
	}

	public enum ElevatorDoorStatus {
		OPEN, CLOSE
	}
	/*
	 * public enum RequestType {
	 * FLOOREDIRECTIONLAMP,FLOORBUTTON,ELEVATORARRIVAL,//for floor
	 * ELEVATORLAMP,ELEVATORDOOR,ELEVATORMOTOR// for elevator
	 * 
	 * }
	 */

}

package enums;

public class SystemEnumTypes {
	public enum RequestEvent {
		SENT, RECEIVED;
	}

	/*
	 * public enum DoorState { OPENED, CLOSED }
	 */
	public enum FloorDirectionLampStatus {
		ON, OFF;
	}

	public enum Direction {
		UP, DOWN, STAY;
	}

	public enum ElevatorCurrentStatus {
		MOVE, STOP;
	}

	public enum ElevatorCurrentDoorStatus {
		OPEN, CLOSE;
	}

}

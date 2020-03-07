package enums;

/**
 * This class is a class of enums and contains all the enums that are used
 * across the project *
 */
public class SystemEnumTypes {
	public enum RequestEvent {
		SENT, RECEIVED;
	}

	public enum FloorDirectionLampStatus {
		ON, OFF;
	}
	
	public enum Direction {
		UP, DOWN, STAY;
	}

	public enum ElevatorCurrentStatus {
		MOVE, STOP, OUT_OF_SERVICE;
	}

	public enum ElevatorCurrentDoorStatus {
		OPEN, CLOSE;
	}
	
	public enum MonitoredSchedulerEvent {
		ELEVATOR_MOVE,
		ELEVATOR_OPEN_DOOR,
		ELEVATOR_CLOSE_DOOR
	}
	public enum Fault {
		DOOR,
		MOTOR
	}

}

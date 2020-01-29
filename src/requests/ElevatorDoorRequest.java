package requests;

import enums.SystemEnumTypes;

public class ElevatorDoorRequest extends Request {

	/**
	 * Name of the elevator's doors to open
	 */
	private String ElevatorName;
	private SystemEnumTypes.ElevatorCurrentDoorStatus RequestAction;

	/**
	 * Type of request for parsing purposes
	 */
	private static byte[] RequestType = new byte[] { 1, 3 };

	/**
	 * Scheduler calls this to give the elevator an action
	 * 
	 * @param name
	 * @param status
	 */
	public ElevatorDoorRequest(String name, SystemEnumTypes.ElevatorCurrentDoorStatus action) {
		this.setRequestType(RequestType);
		this.ElevatorName = name;
		this.RequestAction = action;
	}

	public String getElevatorName() {
		return ElevatorName;
	}

	public void setElevatorName(String elevatorName) {
		ElevatorName = elevatorName;
	}

	public SystemEnumTypes.ElevatorCurrentDoorStatus getRequestAction() {
		return RequestAction;
	}

	public void setRequestAction(SystemEnumTypes.ElevatorCurrentDoorStatus requestAction) {
		RequestAction = requestAction;
	}

	public static byte[] getRequestType() {
		return RequestType;
	}

}

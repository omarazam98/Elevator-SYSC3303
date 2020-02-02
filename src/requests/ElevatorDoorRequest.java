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
	 */
	public ElevatorDoorRequest(String name, SystemEnumTypes.ElevatorCurrentDoorStatus action) {
		this.setRequestType(RequestType);
		this.ElevatorName = name;
		this.RequestAction = action;
	}

	/**
	 * gets the name of the elevator name
	 */
	public String getElevatorName() {
		return ElevatorName;
	}

	/**
	 * sets the elevator name
	 */
	public void setElevatorName(String elevatorName) {
		ElevatorName = elevatorName;
	}

	/**
	 * gets the Request Action
	 */
	public SystemEnumTypes.ElevatorCurrentDoorStatus getRequestAction() {
		return RequestAction;
	}

	/**
	 * sets the request Action
	 */
	public void setRequestAction(SystemEnumTypes.ElevatorCurrentDoorStatus requestAction) {
		RequestAction = requestAction;
	}

	/**
	 * get the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

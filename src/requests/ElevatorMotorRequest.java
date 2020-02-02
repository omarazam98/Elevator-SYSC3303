package requests;

import enums.SystemEnumTypes;

public class ElevatorMotorRequest extends Request {
	// Action to give to scheduler or motor, respectively

	private SystemEnumTypes.Direction RequestAction;

	// Name of the motor's elevator
	private String ElevatorName;

	// Type of request for parsing purposes
	private static byte[] RequestType = new byte[] { 1, 5 };

	/**
	 * Scheduler calls this to give the elevator a motor action, and the elevator
	 * sends it back as confirmation
	 * 
	 * @param name   RequestAction
	 * @param action the direction in which the elevator is supposed to move
	 */
	public ElevatorMotorRequest(String name, SystemEnumTypes.Direction action) {
		this.setRequestType(RequestType);
		this.ElevatorName = name;
		this.RequestAction = action;
	}

	/**
	 * gets the RequestAction
	 */
	public SystemEnumTypes.Direction getRequestAction() {
		return RequestAction;
	}

	/**
	 * sets the request action
	 */
	public void setRequestAction(SystemEnumTypes.Direction requestAction) {
		RequestAction = requestAction;
	}

	/**
	 * gets the elevator name
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
	 * gets the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

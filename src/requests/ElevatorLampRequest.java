package requests;

import enums.SystemEnumTypes;

public class ElevatorLampRequest extends LampRequest {

	/**
	 * The name of the floor button in the elevator
	 */
	private String ElevatorButton;

	/**
	 * The type of the request
	 */
	private static byte[] RequestType = new byte[] { 1, 4 };

	/**
	 * Create a request to change the status of a button lamp of a given elevator
	 * 
	 * @param button the elevator button
	 * @param status the elevaotr current state
	 */
	public ElevatorLampRequest(String button, SystemEnumTypes.FloorDirectionLampStatus status) {
		super(status);
		this.setRequestType(RequestType);
		this.ElevatorButton = button;
	}

	/*
	 * @return the current elevator button
	 */
	public String getElevatorButton() {
		return ElevatorButton;
	}

	/**
	 * sets the elevator button
	 */
	public void setElevatorButton(String elevatorButton) {
		ElevatorButton = elevatorButton;
	}

	/**
	 * gets the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

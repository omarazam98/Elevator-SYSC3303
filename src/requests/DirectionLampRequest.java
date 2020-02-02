package requests;

import enums.SystemEnumTypes;

/**
 * This class deals with the requests for the elevator lamp
 */
public class DirectionLampRequest extends LampRequest {

	private SystemEnumTypes.Direction LampDirection;
	// request type for parsing purposes

	private static byte[] RequestType = new byte[] { 1, 1 };

	/**
	 * Creates a request to change the status of the direction lamp of a given floor
	 * 
	 * @param direction the direction that the elevator is currently moving in
	 * @param status    the current status of lamp
	 */
	public DirectionLampRequest(SystemEnumTypes.Direction direction, SystemEnumTypes.FloorDirectionLampStatus status) {
		super(status);
		this.setRequestType(RequestType);
		this.LampDirection = direction;
	}

	/**
	 * returns the the lamp light direction
	 */
	public SystemEnumTypes.Direction getLampDirection() {
		return LampDirection;
	}

	/**
	 * sets the the lamp light direction
	 */
	public void setLampDirection(SystemEnumTypes.Direction lampDirection) {
		LampDirection = lampDirection;
	}

	/**
	 * returns the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

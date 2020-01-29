package requests;

import enums.SystemEnumTypes;

public class DirectionLampRequest extends LampRequest {

	/**
	 * Direction of the Floor's elevator up/down indicators
	 */
	private SystemEnumTypes.Direction LampDirection;

	/**
	 * Type of request for parsing purposes
	 */
	private static byte[] RequestType = new byte[] { 1, 1 };

	/**
	 * Create a request to change the status of the direction lamp of a given floor
	 * 
	 * @param direction the direction that the elevator is currently travelling in
	 * @param status    the status of the lamp
	 */
	public DirectionLampRequest(SystemEnumTypes.Direction direction, SystemEnumTypes.FloorDirectionLampStatus status) {
		super(status);
		this.setRequestType(RequestType);
		this.LampDirection = direction;
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@link DirectionLampRequest#LampDirection}
	 */
	public SystemEnumTypes.Direction getLampDirection() {
		return LampDirection;
	}

	/**
	 * {@link DirectionLampRequest#LampDirection}
	 */
	public void setLampDirection(SystemEnumTypes.Direction lampDirection) {
		LampDirection = lampDirection;
	}

	/**
	 * {@link DirectionLampRequest#RequestType}
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

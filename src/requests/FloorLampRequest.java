package requests;

import enums.SystemEnumTypes;

/**
 * This class deals with the FloorLamp requests
 *
 */
public class FloorLampRequest extends LampRequest {

	// direction of the floor's button to light up
	private SystemEnumTypes.Direction Direction;

	// request type for parsing purposes

	private static byte[] RequestType = new byte[] { 1, 7 };

	/**
	 * 
	 * @param Direction the direction of the floor lamp
	 * @param status    floor lamp current status
	 */
	public FloorLampRequest(SystemEnumTypes.Direction Direction, SystemEnumTypes.FloorDirectionLampStatus status) {

		super(status);
		this.setRequestType(RequestType);
		this.Direction = Direction;
	}

	/**
	 * gets the direction of the floor lamp
	 */
	public SystemEnumTypes.Direction getDirection() {
		return Direction;
	}

	/**
	 * sets the direction of the floor lamp
	 */
	public void setDirection(SystemEnumTypes.Direction direction) {
		Direction = direction;
	}

	/**
	 * gets the request type
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}

}

package requests;

import enums.SystemEnumTypes;

/**
 * This class pass info of light up floor lamp
 * 
 *
 */
public class FloorLampRequest extends LampRequest {

	// direction of the floor's button to light up
	private SystemEnumTypes.Direction Direction;

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

	public SystemEnumTypes.Direction getDirection() {
		return Direction;
	}

	public void setDirection(SystemEnumTypes.Direction direction) {
		Direction = direction;
	}

	public static byte[] getRequestType() {
		return RequestType;
	}

}

package requests;

import enums.SystemEnumTypes;
import enums.SystemEnumTypes.FloorDirectionLampStatus;

public class FloorLampRequest extends LampRequest {
	
	/**
	 * Direction of the floor's button to light up
	 */
	private SystemEnumTypes.Direction Direction;
	
	/**
	 * Type of request for parsing purposes
	 */
	private static byte[] RequestType = new byte[] {1,7};

	/**
	 * 
	 * @param Direction {@link FloorLampRequest#Direction}
	 * @param status {@link LampRequest#getCurrentStatus()}
	 */
	public FloorLampRequest(SystemEnumTypes.Direction Direction, SystemEnumTypes.FloorDirectionLampStatus status) {

		super(status);
		this.setRequestType(RequestType);
		this.Direction = Direction;
	}

	/**
	 * {@link FloorLampRequest#Direction}
	 */
	public SystemEnumTypes.Direction getDirection() {
		return Direction;
	}

	/**
	 * {@link FloorLampRequest#Direction}
	 */
	public void setDirection(SystemEnumTypes.Direction direction) {
		Direction = direction;
	}


	/**
	 * {@link FloorLampRequest#RequestType}
	 */
	public static byte[] getRequestType() {
		return RequestType;
	}
	
}

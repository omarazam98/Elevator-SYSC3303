package requests;

import enums.SystemEnumTypes;

/**
 * This class used to pass info to deal with event of floor button pressed
 *
 */
public class FloorButtonRequest extends Request {

	private String buttonPressTime;
	private String currentFloorName;
	private SystemEnumTypes.Direction ButtonPressed; //used to sign which button is pressed (UP/DOWN)
	private String DestinationFloor;
	
	private static byte[] RequestType = new byte[] {1, 6};

	/**
	 * Constructor
	 * 
	 * @param time             the request time
	 * @param FloorName        the floor name
	 * @param Direction        the direction to move
	 * @param destinationFloor the destination floor
	 */
	public FloorButtonRequest(String time, String FloorName, SystemEnumTypes.Direction Direction,
			String destinationFloor) {
		this.setRequestType(RequestType);
		this.buttonPressTime = time;
		this.currentFloorName = FloorName;
		this.ButtonPressed = Direction;
		this.DestinationFloor = destinationFloor;
	}

	public String getButtonPressTime() {
		return this.buttonPressTime;
	}

	public void setButtonPressTime(String time) {
		this.buttonPressTime = time;
	}

	public String getFloorName() {
		return currentFloorName;
	}

	public void setFloorName(String floorName) {
		currentFloorName = floorName;
	}

	public SystemEnumTypes.Direction getDirection() {
		return ButtonPressed;
	}

	public void setDirection(SystemEnumTypes.Direction direction) {
		ButtonPressed = direction;
	}

	public String getDestinationFloor() {
		return DestinationFloor;
	}

	public void setDestinationFloor(String destinationFloor) {
		DestinationFloor = destinationFloor;
	}

	public static byte[] getRequestType() {
		return RequestType;
	}
}

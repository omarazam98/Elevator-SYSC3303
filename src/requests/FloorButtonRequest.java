package requests;

import enums.SystemEnumTypes;

public class FloorButtonRequest extends Request {

	// the time for the button press
	private String buttonPressTime;
	// the name for the current floor e.g., 1 ,2 etc
	private String currentFloorName;
	// the direction of the pressed button
	private SystemEnumTypes.Direction pressedButtonDirection;
	// the final floor where the user wants to go
	private String DestinationFloor;
	// for parsing the input form the configuraiton file
	private static byte[] RequestType = new byte[] { 1, 6 };

	/**
	 * 
	 * @param time             {@link FloorButtonRequest#buttonPressTime}
	 * @param FloorName        {@link FloorButtonRequest#currentFloorName}
	 * @param Direction        {@link FloorButtonRequest#pressedButtonDirection}
	 * @param destinationFloor {@link FloorButtonRequest#DestinationFloor}
	 */
	public FloorButtonRequest(String time, String FloorName, SystemEnumTypes.Direction Direction,
			String destinationFloor) {
		this.setRequestType(RequestType);
		this.buttonPressTime = time;
		this.currentFloorName = FloorName;
		this.pressedButtonDirection = Direction;
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
		return pressedButtonDirection;
	}

	public void setDirection(SystemEnumTypes.Direction direction) {
		pressedButtonDirection = direction;
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

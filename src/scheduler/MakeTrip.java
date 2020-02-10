package scheduler;

import enums.SystemEnumTypes;

/**
 * The TripRequest will model a trip request. It includes a pickup floor, destination floor and a direction.
 *
 */
public class MakeTrip {
	private int userinitalLocation;
	private int userFinalLocation;
	private SystemEnumTypes.Direction direction;
	private boolean hasDestination;
	
	public MakeTrip(int pickupFloor, SystemEnumTypes.Direction direction) {
		this.userinitalLocation = pickupFloor;
		this.hasDestination = false;
		this.direction = direction;
	}
	
	public boolean hasDestination() {
		return this.hasDestination;
	}
	/**
	 * Get the pickup floor.
	 * @return
	 */
	public int getUserinitalLocation() {
		return this.userinitalLocation;
	}
	
	/**
	 * Get the destination floor.
	 * @return
	 */
	public int getUserFinalLocation() {
		return this.userFinalLocation;
	}
	
	/**
	 * Set the destination floor.
	 * @param destinationFloor
	 */
	public void setDestinationFloor(int destinationFloor) {
		this.userFinalLocation = destinationFloor;
		this.hasDestination = true;
	}
	
	/**
	 * Get the direction.
	 * @return
	 */
	public SystemEnumTypes.Direction getElevatorDirection() {
		return this.direction;
	}
	
	/**
	 * A way to compare trip request objects. This is used to prevent duplicate trip requests in any set collection.
	 * @param tripRequest
	 * @return
	 */
	public boolean equals(MakeTrip tripRequest) {
		if ((this.userinitalLocation == tripRequest.getUserinitalLocation()) && (this.userFinalLocation == tripRequest.getUserFinalLocation()) && (this.direction == tripRequest.getElevatorDirection()) ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Coordinate notation to depict a trip request ex -> (pickup, destination)
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		sb.append(this.userinitalLocation);
		sb.append(",");
		sb.append(this.direction);
		sb.append(",");
		if (this.hasDestination) {
			sb.append(this.userFinalLocation);
		} else {
			sb.append("?");
		}
		sb.append(")");

		return sb.toString();
	}
}

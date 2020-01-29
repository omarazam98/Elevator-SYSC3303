package scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import elevator.Direction;
import elevator.ElevatorDoorStatus;
import elevator.ElevatorState;
import elevator.ElevatorStatus;
import enums.SystemEnumTypes;

/**
 * This class is responsible to maintain the current state of the elevator and
 * queue containing information regarding the trips. Every time the elevator
 * changes its state, the class shall be updated. The class also determines if
 * for a single Elevator.
 *
 */
public class Monitor {
	private String currentELevatorName;
	private LinkedHashSet<MakeTrip> requestInQueue;
	private HashSet<Integer> destinationFloor;
	private HashSet<Integer> requestFloor;
	private SystemEnumTypes.Direction queueDirection;
	private ArrayList<MakeTrip> tripRequestCompletionSuccess;
	private ElevatorState elevatorCurrentState;

	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation,
			Direction currentElevatorDirection, ElevatorStatus currentElevatorStatus,
			ElevatorDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors) {

		this.currentELevatorName = elevatorName;
		this.requestInQueue = new LinkedHashSet<MakeTrip>();
		this.destinationFloor = new HashSet<Integer>();
		this.requestFloor = new HashSet<Integer>();
		this.tripRequestCompletionSuccess = new ArrayList<MakeTrip>();
		this.queueDirection = SystemEnumTypes.Direction.STAY;
		this.elevatorCurrentState = new ElevatorState(elevatorStartFloorLocation, currentElevatorFloorLocation,
				currentElevatorDirection, currentElevatorStatus, currentElevatorDoorStatus, totalNumberOfFloors);
	}

	public Object getElevatorStatus() {
		return this.elevatorCurrentState.getCurrentStatus();

	}

	public boolean isEmpty() {
		return this.requestInQueue.isEmpty();
	}

	/**
	 * Add a first trip to the queue. This is to add a trip to an idle elevator.
	 * 
	 * @param tripRequest
	 * @return
	 */
	public boolean addFirstTripRequest(MakeTrip tripRequest) {
		if (this.isEmpty()) {
			this.requestInQueue.add(tripRequest);
			this.queueDirection = tripRequest.getElevatorDirection();
			this.destinationFloor.add(tripRequest.getUserFinalLocation());
			this.requestFloor.add(tripRequest.getUserinitalLocation());
			return true;
		}
		return false;
	}

	public void updateElevatorDirection(SystemEnumTypes.Direction nextDirection) {
		this.elevatorCurrentState.setDirection(nextDirection);
	}

	public SystemEnumTypes.Direction getNextElevatorDirection() {
		SystemEnumTypes.Direction nextDirection = null;
		// The elevator is assigned the STAY state if there are no trips in queue and it
		// is
		// determined if the elevator parent floor is up or down with reference to
		// current floor
		if (this.isEmpty()) {
			if (this.elevatorCurrentState.getCurrentFloor() > this.elevatorCurrentState.getStartFloor()) {
				nextDirection = SystemEnumTypes.Direction.DOWN;
			} else if (this.elevatorCurrentState.getCurrentFloor() < this.elevatorCurrentState.getStartFloor()) {
				nextDirection = SystemEnumTypes.Direction.UP;
			} else {
				nextDirection = SystemEnumTypes.Direction.STAY;
			}
		} else {
			// TODO once getting other components, change the switch to if and else case
			switch (this.queueDirection) {
			case UP:
				if (this.elevatorCurrentState.getCurrentFloor() > this.getLowestScheduledFloor()) {
					nextDirection = SystemEnumTypes.Direction.DOWN;
				} else {
					nextDirection = SystemEnumTypes.Direction.UP;
				}
				break;
			case DOWN:
				if (this.elevatorCurrentState.getCurrentFloor() < this.getLowestScheduledFloor()) {
					nextDirection = SystemEnumTypes.Direction.UP;
				} else {
					nextDirection = SystemEnumTypes.Direction.DOWN;
				}
				break;
			}
		}
		return nextDirection;
	}

	private int getLowestScheduledFloor() {
		HashSet<Integer> allStops = new HashSet<Integer>();
		allStops.addAll(requestFloor);
		allStops.addAll(destinationFloor);

		Iterator<Integer> iterator = allStops.iterator();
		Integer currentLowestFloor = iterator.next();
		while (iterator.hasNext()) {
			Integer nextFloorToCompare = iterator.next();
			if (nextFloorToCompare < currentLowestFloor) {
				currentLowestFloor = nextFloorToCompare;
			}
		}

		return currentLowestFloor;
	}

	public void updateElevatorDoorStatus(ElevatorDoorStatus updatedDoorState) {
		this.elevatorCurrentState.setDoorStatus(updatedDoorState);

	}

	public void updateElevatorStatus(ElevatorStatus updatedElevatorStatus) {
		this.elevatorCurrentState.setDoorStatus(updatedElevatorStatus);

	}

	public HashSet<MakeTrip> stopOccurred() {
		HashSet<MakeTrip> completedTrips = new HashSet<MakeTrip>();
		// If current floor refers to the destination floor, the stop is removed from
		// the queue
		// for both the destination as well as the requestInQueue
		if (this.isDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
			if (this.removeDestinationFloor(this.elevatorCurrentState.getCurrentFloor())) {
				completedTrips = this.removeTripsWithDestinationFloor(this.elevatorCurrentState.getCurrentFloor());
			}
			// change the state to STAY if there are no more floors in th queue to visit
			if (this.isEmpty()) {
				this.queueDirection = SystemEnumTypes.Direction.STAY;
			}
		}
		// If current floor is the requestFloor i.e., from which the request is made;
		// remove it
		// from the queue.
		if (this.isPickupFloor(this.elevatorCurrentState.getCurrentFloor())) {
			this.isPickupFloor(this.elevatorCurrentState.getCurrentFloor());
		}
		return completedTrips;
	}

	private boolean isDestinationFloor(int currentFloor) {
		if (this.destinationFloor.contains(currentFloor)) {
			return true;
		}
		return false;
	}

	private boolean removeDestinationFloor(int currentFloor) {
		if (this.destinationFloor.contains(currentFloor)) {
			this.destinationFloor.remove(currentFloor);
			return true;
		}
		return false;
	}

	private HashSet<MakeTrip> removeTripsWithDestinationFloor(int currentFloor) {
		HashSet<MakeTrip> completedTrips = new HashSet<MakeTrip>();

		Iterator<MakeTrip> iterator = this.requestInQueue.iterator();
		while (iterator.hasNext()) {
			MakeTrip tripRequest = iterator.next();
			if (currentFloor == tripRequest.getUserFinalLocation()) {
				this.tripRequestCompletionSuccess.add(tripRequest);
				completedTrips.add(tripRequest);
				iterator.remove();
			}
		}
		return completedTrips;
	}

	public Object getQueueDirection() {
		return this.queueDirection;
	}

	public boolean addEnRouteTripRequest(MakeTrip tripRequest) {
		if (!this.isEmpty()) {
			// Trip is only made if it is in the direction of the elevator motion and is
			// added to the queue except if the next direction after the current one is same
			if ((this.queueDirection == tripRequest.getElevatorDirection())
					&& ((this.elevatorCurrentState.getDirection() == this.queueDirection)
							|| (this.getNextElevatorDirection() == this.queueDirection))) {

				// If request floor is elevator current location, and the elevator is in
				// STOP position and the doors are open, only then add it to queue
				if (this.elevatorCurrentState.getCurrentFloor() == tripRequest.getUserinitalLocation()) {
					// if the above conditions are not satisfied, do not add request to the queue.
					if ((this.elevatorCurrentState.getCurrentStatus() != SystemEnumTypes.ElevatorCurrentStatus.STOP)
							|| (this.elevatorCurrentState
									.getCurrentStatus() != SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN)) {
						return false;
					}
				} else {
					// Check if the elevator has passed the request floor already or not and if so,
					// do not add it to the request queue.
					// TODO after getting code, try to convert switch to if and else statements
					switch (this.queueDirection) {
					case UP:
						if (this.elevatorCurrentState.getCurrentFloor() > tripRequest.getUserinitalLocation()) {
							return false;
						}
						break;
					case DOWN:
						if (this.elevatorCurrentState.getCurrentFloor() < tripRequest.getUserFinalLocation()) {
							return false;
						}
						break;
					}
				}

				// if none of the above candidness are not satisfied, add the request to the
				// queue.
				requestInQueue.add(tripRequest);
				this.destinationFloor.add(tripRequest.getUserFinalLocation());
				if (this.elevatorCurrentState.getCurrentFloor() != tripRequest.getUserinitalLocation()) {
					this.requestFloor.add(tripRequest.getUserinitalLocation());
				}
				return true;
			}
		}
		return false;
	}

	public Integer getElevatorFloorLocation() {
		return this.elevatorCurrentState.getCurrentFloor();
	}

	public Integer getElevatorStartingFloorLocation() {
		return this.elevatorCurrentState.getStartFloor();
	}

	public void updateElevatorFloorLocation(int floorNumber) {
		this.elevatorCurrentState.setCurrentFloor(floorNumber);

	}

	public boolean isStopRequired(int floorNumber) {
		if ((this.isDestinationFloor(floorNumber) && (this.elevatorCurrentState.getDirection() == this.queueDirection))
				|| ((this.queueDirection == SystemEnumTypes.Direction.STAY)
						&& (this.elevatorCurrentState.getCurrentFloor() == this.elevatorCurrentState.getStartFloor()))
				|| (this.isPickupFloor(floorNumber))) {
			return true;
		}

		return false;
	}

	public boolean isPickupFloor(int floorNumber) {
		if (this.requestFloor.contains(floorNumber)) {
			return true;
		}
		return false;
	}

	public String toString() {
		String s = "";
		s += "[" + "Elevator name: " + this.currentELevatorName + "\n" + "Current floor: "
				+ this.elevatorCurrentState.getCurrentFloor() + "\n" + "Current direction: "
				+ this.elevatorCurrentState.getDirection() + "\n" + "Current elevator status: "
				+ this.elevatorCurrentState.getCurrentStatus() + "\n" + "Current door status: "
				+ this.elevatorCurrentState.getDoorStatus() + "\n" + "Trip request queue: ";

		s += "[";
		Iterator<MakeTrip> queueIterator = this.requestInQueue.iterator();
		while (queueIterator.hasNext()) {
			MakeTrip tripRequest = queueIterator.next();
			s += tripRequest.toString();
			if (queueIterator.hasNext()) {
				s += ",";
			}
		}
		s += "]\n" + "Floor pickups remaining: " + this.requestFloor.toString() + "\n"
				+ "Floor destinations remaining: " + this.destinationFloor.toString() + "\n" + "Completed trips: ";
		s += "[";
		Iterator<MakeTrip> completedTripsIterator = this.tripRequestCompletionSuccess.iterator();
		while (completedTripsIterator.hasNext()) {
			MakeTrip tripRequest = completedTripsIterator.next();
			s += tripRequest.toString();
			if (completedTripsIterator.hasNext()) {
				s += ",";
			}
		}
		s += "]" + "]\n";
		return s;
	}
}

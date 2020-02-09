package scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import elevator.ElevatorState;
import enums.SystemEnumTypes;

/**
 * This class is responsible to maintain the current state of the elevator and a
 * queue containing information regarding the trips. Every time the elevator
 * changes its state, the class shall be updated. The class also determines if
 * for a single Elevator.
 *
 */
public class Monitor {
	private String currentElevatorName;
	private LinkedHashSet<MakeTrip> requestInQueue;
	private HashSet<Integer> destinationFloor;
	private HashSet<Integer> requestFloor;
	private SystemEnumTypes.Direction queueDirection;
	private ArrayList<MakeTrip> tripRequestCompletionSuccess;
	private ElevatorState elevatorCurrentState;

	// the constructor
	public Monitor(String elevatorName, Integer elevatorStartFloorLocation, Integer currentElevatorFloorLocation,
			SystemEnumTypes.Direction currentElevatorDirection,
			SystemEnumTypes.ElevatorCurrentStatus currentElevatorStatus,
			SystemEnumTypes.ElevatorCurrentDoorStatus currentElevatorDoorStatus, Integer totalNumberOfFloors) {

		this.currentElevatorName = elevatorName;
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
	 * Add a first trip to the queue of an idle elevator.
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

	/**
	 * updates the elevator direction
	 */
	public void updateElevatorDirection(SystemEnumTypes.Direction nextDirection) {
		this.elevatorCurrentState.setDirection(nextDirection);
	}

	@SuppressWarnings("incomplete-switch")
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
			switch (this.queueDirection) {
			case UP:
				if (this.elevatorCurrentState.getCurrentFloor() > this.getLowestScheduledFloor()) {
					nextDirection = SystemEnumTypes.Direction.DOWN;
				} else {
					nextDirection = SystemEnumTypes.Direction.UP;
				}
				break;
			case DOWN:
				if (this.elevatorCurrentState.getCurrentFloor() < this.getHighestScheduledFloor()) {
					nextDirection = SystemEnumTypes.Direction.UP;
				} else {
					nextDirection = SystemEnumTypes.Direction.DOWN;
				}
				break;
			}
		}
		return nextDirection;
	}

	/**
	 * gets the highest floor
	 */
	private Integer getHighestScheduledFloor() {
		HashSet<Integer> allStops = new HashSet<Integer>();
		allStops.addAll(requestFloor);
		allStops.addAll(destinationFloor);

		Iterator<Integer> iterator = allStops.iterator();
		Integer currentHighestFloor = iterator.next();
		while (iterator.hasNext()) {
			Integer nextFloorToCompare = iterator.next();
			if (nextFloorToCompare > currentHighestFloor) {
				currentHighestFloor = nextFloorToCompare;
			}
		}

		return currentHighestFloor;
	}

	/**
	 * gets the lowest floor
	 */
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

	public boolean isWaitingForDestinationRequest() {
		if (this.queueDirection != SystemEnumTypes.Direction.STAY) {
			for (MakeTrip tripRequest : this.requestInQueue) {
				if (tripRequest.getUserinitalLocation() == this.elevatorCurrentState.getCurrentFloor()) {
					if (tripRequest.hasDestination() == false) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * This method assumes the proper timing of the destination request (once the
	 * elevator is at a floor to do pickup).
	 * 
	 * @param destinationFloor
	 * @return
	 */
	@SuppressWarnings("incomplete-switch")
	public boolean addDestination(Integer pickupFloor, Integer destinationFloor) {
		boolean destinationFloorValid = false;
		// Check whether the elevator can take this destinationFloor given the elevators
		// current state (the elevator must be in service of the queue (not travelling
		// towards the first pickup floor)
		// The destination must not require the elevator to change directions from its
		// current location.
		switch (this.queueDirection) {
		case UP:
			if (destinationFloor > this.elevatorCurrentState.getCurrentFloor()) {
				destinationFloorValid = true;
			}
			break;
		case DOWN:
			if (destinationFloor < this.elevatorCurrentState.getCurrentFloor()) {
				destinationFloorValid = true;
			}
			break;
		}
		// TODO mod if things break.....
		// If the destination floor is valid, add it to the destination floors queue and
		// add it to its corresponding tripRequest
		if (destinationFloorValid) {
			this.destinationFloor.add(destinationFloor);
			for (MakeTrip tripRequest : requestInQueue) {
				if ((tripRequest.getUserinitalLocation() == pickupFloor) && (!tripRequest.hasDestination())) {
					tripRequest.setDestinationFloor(destinationFloor);
					break;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * updates the elevator door status
	 */
	public void updateElevatorDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus updatedDoorState) {
		this.elevatorCurrentState.setDoorStatus(updatedDoorState);

	}

	/**
	 * updates the elevaotr status
	 */
	public void updateElevatorStatus(SystemEnumTypes.ElevatorCurrentStatus updatedElevatorStatus) {
		this.elevatorCurrentState.setStatus(updatedElevatorStatus);

	}

	/**
	 * checks if the elevator is stopped or not
	 */
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
			this.removePickupFloor(this.elevatorCurrentState.getCurrentFloor());
		}
		return completedTrips;
	}

	/**
	 * This method adds the request to the elevator queue if it is in service
	 * currently or else it makes it the current elevator default request
	 * 
	 * @param tripRequest
	 * @return
	 */
	public boolean addTripRequest(MakeTrip tripRequest) {
		if (this.isEmpty()) {
			return this.addFirstTripRequest(tripRequest);
		} else {
			return this.addEnRouteTripRequest(tripRequest);
		}
	}

	/**
	 * deletes the pickup floor
	 */
	private boolean removePickupFloor(int floor) {
		if (this.requestFloor.contains(floor)) {
			this.requestFloor.remove(floor);
			return true;
		}
		return false;
	}

	/**
	 * checks if the current floor is the destination floor or not
	 */
	private boolean isDestinationFloor(int currentFloor) {
		if (this.destinationFloor.contains(currentFloor)) {
			return true;
		}
		return false;
	}

	/**
	 * removes the destinaiton floor
	 */
	private boolean removeDestinationFloor(int currentFloor) {
		if (this.destinationFloor.contains(currentFloor)) {
			this.destinationFloor.remove(currentFloor);
			return true;
		}
		return false;
	}

	/**
	 * removes the trips with the destination floor reached
	 */
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

	public SystemEnumTypes.Direction getQueueDirection() {
		return this.queueDirection;
	}

	public SystemEnumTypes.Direction getElevatorDirection() {
		return this.elevatorCurrentState.getDirection();
	}

	/**
	 * if an elevaor is moving in the same direction as towards which a button is
	 * pressed; then it adds it to the queue else it continues its journey
	 */
	@SuppressWarnings("incomplete-switch")
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
									.getDoorStatus() != SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN)) {
						return false;
					}
				} else {
					// Check if the elevator has passed the request floor already or not and if so,
					// do not add it to the request queue.
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

	@SuppressWarnings("incomplete-switch")
	public Integer estimatePickupTime(MakeTrip tripRequest) {
		int averageTravelTimePerFloor = 5;
		int averageTimePerStop = 9;
		if (this.isEmpty()) {
			return (Math.abs(this.elevatorCurrentState.getCurrentFloor() - tripRequest.getUserinitalLocation())
					* averageTravelTimePerFloor);
		} else if (this.addEnRouteTripRequest(tripRequest)) {
			int interimStops = 0;
			HashSet<Integer> allFloors = new HashSet<Integer>();
			allFloors.addAll(this.requestFloor);
			allFloors.addAll(this.destinationFloor);
			switch (this.queueDirection) {
			case UP:
				// Check if any of the scheduled floor stops are in between the elevator's
				// current floor and the tripRequests floor, if so this is an interim stop
				for (Integer floor : allFloors) {
					if ((this.elevatorCurrentState.getCurrentFloor() < floor)
							&& (floor < tripRequest.getUserinitalLocation())) {
						interimStops++;
					}
				}
				break;
			case DOWN:
				// Check if any of the scheduled floor stops are in between the elevator's
				// current floor and the tripRequests floor, if so this is an interim stop
				for (Integer floor : allFloors) {
					if ((this.elevatorCurrentState.getCurrentFloor() > floor)
							&& (floor < tripRequest.getUserinitalLocation())) {
						interimStops++;
					}
				}
				break;
			}
			return ((Math.abs(this.elevatorCurrentState.getCurrentFloor() - tripRequest.getUserinitalLocation())
					* averageTravelTimePerFloor) + (interimStops * averageTimePerStop));
		}
		return null;
	}

	/**
	 * gets the elevator floor locaiton
	 * 
	 * @return
	 */
	public Integer getElevatorFloorLocation() {
		return this.elevatorCurrentState.getCurrentFloor();
	}

	/**
	 * gets the elevator starting location
	 */
	public Integer getElevatorStartingFloorLocation() {
		return this.elevatorCurrentState.getStartFloor();
	}

	/**
	 * 
	 * @return the current elevator name
	 */
	public String getCurrentElevatorName() {
		return currentElevatorName;
	}

	/**
	 * updates the elevator floor locaiton
	 */
	public void updateElevatorFloorLocation(int floorNumber) {
		this.elevatorCurrentState.setCurrentFloor(floorNumber);

	}

	/**
	 * checks if the elevator is supposed to stop or not
	 */
	public boolean isStopRequired(int floorNumber) {
		if ((this.isDestinationFloor(floorNumber) && (this.elevatorCurrentState.getDirection() == this.queueDirection))
				|| ((this.queueDirection == SystemEnumTypes.Direction.STAY)
						&& (this.elevatorCurrentState.getCurrentFloor() == this.elevatorCurrentState.getStartFloor()))
				|| (this.isPickupFloor(floorNumber))) {
			return true;
		}

		return false;
	}

	/**
	 * checks if the current floor is the pick up floor or not
	 */
	public boolean isPickupFloor(int floorNumber) {
		if (this.requestFloor.contains(floorNumber)) {
			return true;
		}
		return false;
	}

	/**
	 * prints to the console in a specific format
	 */
	public String toString() {
		String s = "";
		s += "[" + "Elevator name: " + this.currentElevatorName + "\n" + "Current floor: "
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

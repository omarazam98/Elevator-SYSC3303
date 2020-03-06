package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import elevator.ElevatorState;
import enums.SystemEnumTypes;
/**
 * 
 * Test class for ElevatorState
 *
 */
public class ElevatorStateTest {
	private ElevatorState elestate;
	
	@Before//initial set up
	public void setUp() throws Exception {
		elestate = new ElevatorState(3, 9, SystemEnumTypes.Direction.UP, SystemEnumTypes.ElevatorCurrentStatus.MOVE,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE, 12);
	}

	@Test//set and get startfloor
	public void testSetStartFloor() {
		assertEquals(elestate.getStartFloor(), 3);
		elestate.setStartFloor(5);
		assertEquals(elestate.getStartFloor(), 5);
	}

	@Test//set and get current floor
	public void testSetCurrentFloor() {
		assertEquals(elestate.getCurrentFloor(), 9);
		elestate.setCurrentFloor(7);
		assertEquals(elestate.getCurrentFloor(), 7);
	}

	@Test//set and get direction
	public void testSetDirection() {
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.UP);
		elestate.setDirection(SystemEnumTypes.Direction.DOWN);
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.DOWN);
	}

	@Test//set and get status
	public void testSetStatus() {
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		elestate.setStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.STOP);
	}

	@Test//set and get door status
	public void testSetDoorStatus() {
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		elestate.setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
	}

	@Test//set and get highest floor
	public void testSetHighestFloor() {
		assertEquals(elestate.getHighestFloor(), 12);
		elestate.setHighestFloor(15);
		assertEquals(elestate.getHighestFloor(), 15);
	}

}
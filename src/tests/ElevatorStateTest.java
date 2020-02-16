package tests;

import static org.junit.Assert.assertEquals;
/**
 * Test class for elevator state
 */

import org.junit.Before;
import org.junit.Test;

import elevator.ElevatorState;
import enums.SystemEnumTypes;

public class ElevatorStateTest {
	private ElevatorState elestate;
	
	@Before//initial constructor
	public void setUp() throws Exception {
		elestate = new ElevatorState(3, 9, SystemEnumTypes.Direction.UP, SystemEnumTypes.ElevatorCurrentStatus.MOVE,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE, 12);
	}

	@Test//test get and set for startfloor
	public void testSetStartFloor() {
		assertEquals(elestate.getStartFloor(), 3);
		elestate.setStartFloor(5);
		assertEquals(elestate.getStartFloor(), 5);
	}
	@Test//test get and set for CurrentFloor
	public void testSetCurrentFloor() {
		assertEquals(elestate.getCurrentFloor(), 9);
		elestate.setCurrentFloor(7);
		assertEquals(elestate.getCurrentFloor(), 7);
	}

	@Test//test get and set for Direction
	public void testSetDirection() {
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.UP);
		elestate.setDirection(SystemEnumTypes.Direction.DOWN);
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.DOWN);
	}

	@Test//test get and set status
	public void testSetStatus() {
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		elestate.setStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.STOP);
	}

	@Test//test get and set doorstatus
	public void testSetDoorStatus() {
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		elestate.setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
	}

	@Test//test get and set HighestFloor
	public void testSetHighestFloor() {
		assertEquals(elestate.getHighestFloor(), 12);
		elestate.setHighestFloor(15);
		assertEquals(elestate.getHighestFloor(), 15);
	}

}
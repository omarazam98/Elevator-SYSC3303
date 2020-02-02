package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import elevator.ElevatorState;
import enums.SystemEnumTypes;

public class ElevatorStateTest {
	private ElevatorState elestate;
	
	@Before
	public void setUp() throws Exception {
		elestate = new ElevatorState(3, 9, SystemEnumTypes.Direction.UP, SystemEnumTypes.ElevatorCurrentStatus.MOVE,
				SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE, 12);
	}

	@Test
	public void testSetStartFloor() {
		assertEquals(elestate.getStartFloor(), 3);
		elestate.setStartFloor(5);
		assertEquals(elestate.getStartFloor(), 5);
	}

	@Test
	public void testSetCurrentFloor() {
		assertEquals(elestate.getCurrentFloor(), 9);
		elestate.setCurrentFloor(7);
		assertEquals(elestate.getCurrentFloor(), 7);
	}

	@Test
	public void testSetDirection() {
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.UP);
		elestate.setDirection(SystemEnumTypes.Direction.DOWN);
		assertEquals(elestate.getDirection(), SystemEnumTypes.Direction.DOWN);
	}

	@Test
	public void testSetStatus() {
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.MOVE);
		elestate.setStatus(SystemEnumTypes.ElevatorCurrentStatus.STOP);
		assertEquals(elestate.getCurrentStatus(), SystemEnumTypes.ElevatorCurrentStatus.STOP);
	}

	@Test
	public void testSetDoorStatus() {
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
		elestate.setDoorStatus(SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
		assertEquals(elestate.getDoorStatus(), SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
	}

	@Test
	public void testSetHighestFloor() {
		assertEquals(elestate.getHighestFloor(), 12);
		elestate.setHighestFloor(15);
		assertEquals(elestate.getHighestFloor(), 15);
	}

}
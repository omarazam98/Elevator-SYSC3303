package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorMotorRequest;

public class ElevatorMotorRequestTest {

	private ElevatorMotorRequest ElevatorMotorReq;

	@Before
	public void setUp() throws Exception {
		ElevatorMotorReq = new ElevatorMotorRequest("Name", SystemEnumTypes.Direction.UP);
	}

	@Test
	public void testGetSetDirection() {
		assertEquals(ElevatorMotorReq.getRequestAction(), SystemEnumTypes.Direction.UP);
		ElevatorMotorReq.setRequestAction(SystemEnumTypes.Direction.DOWN);
		assertEquals(ElevatorMotorReq.getRequestAction(), SystemEnumTypes.Direction.DOWN);
	}
	@Test
    public void testGetSetName() {
    		assertEquals(ElevatorMotorReq.getElevatorName(), "Name");
    		ElevatorMotorReq.setElevatorName("3303");
    		assertEquals(ElevatorMotorReq.getElevatorName(), "3303");
    	}
}
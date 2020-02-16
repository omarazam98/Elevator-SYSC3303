package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorMotorRequest;
/**
 * 
 * Test class for elevator motor request class
 *
 */
public class ElevatorMotorRequestTest {

	private ElevatorMotorRequest ElevatorMotorReq;

	@Before //initial setup
	public void setUp() throws Exception {
		ElevatorMotorReq = new ElevatorMotorRequest("Name", SystemEnumTypes.Direction.UP);
	}

	@Test //test get and set for Direction
	public void testGetSetDirection() {
		assertEquals(ElevatorMotorReq.getRequestAction(), SystemEnumTypes.Direction.UP);
		ElevatorMotorReq.setRequestAction(SystemEnumTypes.Direction.DOWN);
		assertEquals(ElevatorMotorReq.getRequestAction(), SystemEnumTypes.Direction.DOWN);
	}
	@Test //test get and set for name
    public void testGetSetName() {
    		assertEquals(ElevatorMotorReq.getElevatorName(), "Name");
    		ElevatorMotorReq.setElevatorName("3303");
    		assertEquals(ElevatorMotorReq.getElevatorName(), "3303");
    	}
}
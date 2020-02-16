package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorLampRequest;

public class ElevatorLampRequestTest {

	private ElevatorLampRequest ElevatorLampReq;
	private static byte[] RequestType = new byte[] { 1, 4 };

	@Before
	public void setUp() throws Exception {
		ElevatorLampReq = new ElevatorLampRequest('', SystemEnumTypes.FloorDirectionLampStatus.ON);
	}

	@Test
	public void testSetButton() {
		ElevatorLampReq.setElevatorButton('14');
		assertEquals(ElevatorLampReq.getElevatorButton(), '14');
	}
	@Test
    public void testGetRequestType() {
    		assertEquals(ElevatorLampReq.getRequestType(), RequestType);
    	}
}
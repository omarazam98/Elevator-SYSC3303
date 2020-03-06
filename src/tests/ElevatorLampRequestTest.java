package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorLampRequest;
/**
 * 
 * Test class for elevator lamp request class
 *
 */
public class ElevatorLampRequestTest {

	private ElevatorLampRequest ElevatorLampReq;

	@Before//initial setup
	public void setUp() throws Exception {
		ElevatorLampReq = new ElevatorLampRequest("", SystemEnumTypes.FloorDirectionLampStatus.ON);
	}

	@Test//test set and get button
	public void testSetButton() {
		ElevatorLampReq.setElevatorButton("14");
		assertEquals(ElevatorLampReq.getElevatorButton(), "14");
	}
}
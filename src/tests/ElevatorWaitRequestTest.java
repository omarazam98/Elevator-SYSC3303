package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import requests.ElevatorWaitRequest;
/**
 * 
 * Test class for ElevatorWaitRequest
 *
 */
public class ElevatorWaitRequestTest {

    private ElevatorWaitRequest ElevatorWaitReq;

    @Before//initial setup()
    public void setUp() throws Exception {
        ElevatorWaitReq = new ElevatorWaitRequest("ElevatorName");
    }

    @Test//set and get name
    public void testGetSetName() {
        assertEquals(ElevatorWaitReq.getElevatorName(), "ElevatorName");
        ElevatorWaitReq.setElevatorName("3303");
        assertEquals(ElevatorWaitReq.getElevatorName(), "3303");
    }
}
package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import requests.ElevatorWaitRequest;

public class ElevatorWaitRequestTest {

    private ElevatorWaitRequest ElevatorWaitReq;

    @Before
    public void setUp() throws Exception {
        ElevatorWaitReq = new ElevatorWaitRequest("ElevatorName");
    }

    @Test
    public void testGetSetName() {
        assertEquals(ElevatorWaitReq.getElevatorName(), "ElevatorName");
        ElevatorWaitReq.setElevatorName("3303");
        assertEquals(ElevatorWaitReq.getElevatorName(), "3303");
    }
}
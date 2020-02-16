package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorDoorRequest;
/**
 * 
 * Test class for ElevatorDoorRequest
 *
 */
public class ElevatorDoorRequestTest {

    private ElevatorDoorRequest ElevatorDoorReq;

    @Before//initial setup 
    public void setUp() throws Exception {
        ElevatorDoorReq = new ElevatorDoorRequest("NAME", SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
    }

    @Test//test set and get status
    public void testGetSetStatus() {
        assertEquals(ElevatorDoorReq.getRequestAction(), SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
        ElevatorDoorReq.setRequestAction(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
        assertEquals(ElevatorDoorReq.getRequestAction(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
    }

    @Test//test get and set name
    public void testGetSetName() {
        assertEquals(ElevatorDoorReq.getElevatorName(), "NAME");
        ElevatorDoorReq.setElevatorName("DOOR");
        assertEquals(ElevatorDoorReq.getElevatorName(), "DOOR");
    }
}
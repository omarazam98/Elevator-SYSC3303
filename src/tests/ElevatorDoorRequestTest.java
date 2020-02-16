package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorDoorRequest;

public class ElevatorDoorRequestTest {

    private ElevatorDoorRequest ElevatorDoorReq;

    @Before
    public void setUp() throws Exception {
        ElevatorDoorReq = new ElevatorDoorRequest("NAME", SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
    }

    @Test
    public void testGetSetStatus() {
        assertEquals(ElevatorDoorReq.getRequestAction(), SystemEnumTypes.ElevatorCurrentDoorStatus.OPEN);
        ElevatorDoorReq.setRequestAction(SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
        assertEquals(ElevatorDoorReq.getRequestAction(), SystemEnumTypes.ElevatorCurrentDoorStatus.CLOSE);
    }

    @Test
    public void testGetSetName() {
        assertEquals(ElevatorDoorReq.getElevatorName(), "NAME");
        ElevatorDoorReq.setElevatorName("DOOR");
        assertEquals(ElevatorDoorReq.getElevatorName(), "DOOR");
    }
}
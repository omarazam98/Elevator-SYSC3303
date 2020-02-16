package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorArrivalRequest;

public class ElevatorArrivalRequestTest {

    private ElevatorArrivalRequest ElevatorArrivalReq;
    static String floorName = "Lobby";
    static String elevatorName = "Main";

    @Before
    public void setUp() throws Exception {
        ElevatorArrivalReq = new ElevatorArrivalRequest( elevatorName, floorName, SystemEnumTypes.Direction.UP);
    }

    @Test
    public void TestGetSetNames() {
        assertEquals(ElevatorArrivalReq.getElevatorName(),  elevatorName);
        ElevatorArrivalReq.setElevatorName("Secondary");
        assertEquals(ElevatorArrivalReq.getElevatorName(), "Secondary");

        assertEquals(ElevatorArrivalReq.getFloorName(), floorName);
        ElevatorArrivalReq.setFloorName("Pent House");
        assertEquals(ElevatorArrivalReq.getFloorName(), "Pent House");
    }

    @Test
    public void testGetSetDirection() {
        assertEquals(ElevatorArrivalReq.getDirection(), SystemEnumTypes.Direction.UP);
        ElevatorArrivalReq.setDirection(SystemEnumTypes.Direction.DOWN);
        assertEquals(ElevatorArrivalReq.getDirection(), SystemEnumTypes.Direction.DOWN);
    }
}
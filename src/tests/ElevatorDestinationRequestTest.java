package tests;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.ElevatorDestinationRequest;

public class ElevatorDestinationRequestTest {

    private ElevatorDestinationRequest ElevatorDestinationReq;
    private String pickupFloor = "1";
    static String destName = "Lobby";
    static String elevatorName = "Main";

    @Before
    public void setUp() throws Exception {
        ElevatorDestinationReq = new ElevatorDestinationRequest(pickupFloor, destName, elevatorName);
    }

    @Test
    public void TestGetSetNames() {
        assertEquals(ElevatorDestinationReq.getDestinationFloor(), destName);
        ElevatorDestinationReq.setDestinationFloor("Pent House");
        assertEquals(ElevatorDestinationReq.getDestinationFloor(), "Pent House");

        assertEquals(ElevatorDestinationReq.getElevatorName(), elevatorName);
        ElevatorDestinationReq.setElevatorName("Secondary");
        assertEquals(ElevatorDestinationReq.getElevatorName(), "Secondary");
    }

    @Test
    public void TestGetSetPickupFloor() {
        assertEquals(ElevatorDestinationReq.getPickupFloor(), pickupFloor);
        ElevatorDestinationReq.setPickupFloor("3");
        assertEquals(ElevatorDestinationReq.getPickupFloor(), "3");
    }
}
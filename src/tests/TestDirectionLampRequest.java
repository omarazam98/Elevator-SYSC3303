package tests;

import static org.junit.Assert.*;
/**
 * Test class for DirectionLampRequest
 */
import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.DirectionLampRequest;

public class TestDirectionLampRequest {

    private DirectionLampRequest DirectionLampReq;

    @Before//initial setup()
    public void setUp() throws Exception {
        DirectionLampReq = new DirectionLampRequest(SystemEnumTypes.Direction.UP, SystemEnumTypes.FloorDirectionLampStatus.ON);
    }

    @Test//set and get direction
    public void testGetSetDirection() {
        assertEquals(SystemEnumTypes.Direction.UP , DirectionLampReq.getLampDirection());
        DirectionLampReq.setLampDirection(SystemEnumTypes.Direction.DOWN);
        assertEquals(SystemEnumTypes.Direction.DOWN , DirectionLampReq.getLampDirection());
    }
}
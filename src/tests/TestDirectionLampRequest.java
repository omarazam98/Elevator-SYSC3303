package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.DirectionLampRequest;

public class TestDirectionLampRequest {

    private DirectionLampRequest DirectionLampReq;

    @Before
    public void setUp() throws Exception {
        DirectionLampReq = new DirectionLampRequest(SystemEnumTypes.Direction.UP, SystemEnumTypes.FloorDirectionLampStatus.ON);
    }

    @Test
    public void testGetSetDirection() {
        assertEquals(SystemEnumTypes.Direction.UP , DirectionLampReq.getLampDirection());
        DirectionLampReq.setLampDirection(SystemEnumTypes.Direction.DOWN);
        assertEquals(SystemEnumTypes.Direction.DOWN , DirectionLampReq.getLampDirection());
    }
}
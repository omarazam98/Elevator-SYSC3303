package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import enums.SystemEnumTypes;
import requests.LampRequest;
/**
 * 
 * Test class for LampRequest
 *
 */
public class LampRequestTest {

    private LampRequest LampReq;

    @Before//initial setup()
    public void setUp() throws Exception {
        LampReq = new LampRequest(SystemEnumTypes.FloorDirectionLampStatus.ON);
    }

    @Test//set and get status
    public void testGetSetStatus() {
        assertEquals( LampReq.getCurrentStatus(), SystemEnumTypes.FloorDirectionLampStatus.ON);
        LampReq.setCurrentStatus(SystemEnumTypes.FloorDirectionLampStatus.OFF);
        assertEquals( LampReq.getCurrentStatus(), SystemEnumTypes.FloorDirectionLampStatus.OFF);
    }
}
  
package tests;
import org.junit.runner.RunWith;
//test all the test class
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
@RunWith(Suite.class)
@SuiteClasses({
	
	ElevatorArrivalRequestTest.class,
	ElevatorDestinationRequestTest.class,
	ElevatorDoorRequestTest.class,
	ElevatorLampRequestTest.class,
	ElevatorMotorRequestTest.class,
	ElevatorStateTest.class,
	ElevatorWaitRequestTest.class,
	FloorButtonRequestTest.class,
	FloorLampRequestTest.class,
	FloorSubSystemTest.class,
	LampRequestTest.class,
	TestDirectionLampRequest.class,
	MonitorTest.class,
	RequestTest.class,
	MutIntTest.class,
	MakeTripTest.class,	

})

public class Tests {}
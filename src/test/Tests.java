package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
@RunWith(Suite.class)
@SuiteClasses({
MonitorTest.class,
FloorSubSystemTest.class,
TestElevatorSubsystem.class,
TestScheduler.class,
RequestTest.class
})

public class Tests 
{
	
}
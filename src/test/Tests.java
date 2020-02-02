package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
@RunWith(Suite.class)
@SuiteClasses({
MonitorTest.class,
RequestTest.class,
MutIntTest.class,
MakeTripTest.class,
FloorSubSystemTest.class,
//TestElevatorSubsystem.class,
//TestScheduler.class,
})

public class Tests {}
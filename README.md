# Elevator-SYSC3303
## Group Members:
- Sayyid Kamran Sagheir 
- Omar Azam 
- Abdulla Al-wazzan 
- Tiantian Lin 
- Ruixuan Ni 

##Introduction
This project is a simulation of elevators and the control systems.

This project can be executed by running the main method in scheduler.java. The FloorSubsystem will then get requests from 
the requests.txt file, which simulated the passengers press "Up"/"Down" buttons. The threads of ElevatorSubsystem, Scheduler
and FloorSubsystem will then deal with these requests and keeps print out the events and outputs.

## Deliverables

scheduler
*MakeTrip.java: deals with the trip requests made, contains current and destination location
*Monitor.java: used to update the current state of elevator and stores list of trips'information
*Scheduler.java: accepting requests and sending events and commands(requests) as reponds

server
*Server.java: uses socket to receive and send packets on specific port

info
*Helper.java: transfer between requests and datagram packet
*MutInt.java

enums
*SystemEnumTypes.java: a collection of enums used to define the states of lamps, requests and directions
Author: Sayyid Kamran Sagheir   

requests
*DirectionLampRequest.java
*ElevatorArrivalRequest.java
*ElevatorDoorRequest.java
*ElevatorLampRequest.java
*ElevatorMotorRequest.java
*FloorButtonRequest.java
*FloorLampRequest.java
*LampRequest.java
*Request.java: This class and the other request classes are used to transmit informations between threads
Author: Sayyid Kamran Sagheir, Tiantian Lin, Ruixuan Ni

elevator
*ElevatorEvents.java: an interface for subsystems
*ElevatorState.java: used to store the states of elevator, including moving, direction, floor, door and lamps
*ElevatorSubsystem.java: used to simulate the moving of elevator
*ElevatorSystemConfiguration.java: configure elevator subsystems by reading input xml files
Author: Tiantian Lin

floor 
*FloorSubsystem.java: responsible for reading input requirements files, sending and receiving requests to/from server
Author: Ruixuan Ni

unit tests
*RequestTest.java
*FloorSubsystemTest.java
Author: Abdulla Al-wazzan
*ServerTest.java
*SchedulerTest.java
*ElevatorSubsystemTest.java
Author: Omar Azam 

UML diagrams
Author: Abdulla Al-wazzan 

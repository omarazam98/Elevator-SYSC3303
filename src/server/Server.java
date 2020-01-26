package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import main.ElevatorSystemComponent;

public class Server implements Runnable{


	private ElevatorSystemComponent elevatorSystemComponent;
	private DatagramSocket receiveSocket;
	private DatagramSocket sendSocket;
	private String role;
	private boolean debug;
	
	public Server(ElevatorSystemComponent elevatorSystemComponent, int port, boolean debug) {
		this.elevatorSystemComponent = elevatorSystemComponent;
		this.role = elevatorSystemComponent.getName() + "_server";
		this.debug = debug;
		try {
			//Instantiate a socket to be used for receiving packets on specific port.
			this.receiveSocket = new DatagramSocket(port);
			//Instantiate a socket to be used for sending and receiving packets
			this.sendSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}

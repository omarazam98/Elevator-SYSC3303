package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import elevator.ElevatorEvents;
import elevator.ElevatorSystemConfiguration;
import info.Helper;
import requests.Request;

/**
 * This class is responsible for creating DatagramSocket server responsible for:
 * - sending the data - receiving the data - getting the data in the bytes
 * format - printing out the details of the data packet received
 *
 */
public class Server implements Runnable {

	private DatagramSocket receiveSocket;
	private DatagramSocket sendSocket;
	private String role;
	private ElevatorEvents elevatorSystemComponent;
	private boolean debug;

	public Server(ElevatorEvents elevatorSystemComponent, int port, boolean debug) {
		this.elevatorSystemComponent = elevatorSystemComponent;
		this.role = elevatorSystemComponent.getName() + "_server";
		this.debug = debug;
		try {
			// create an instance of a socket to be used for receiving packets on specific
			// port.
			this.receiveSocket = new DatagramSocket(port);
			// create an instance of a socket to be used for sending and receiving packets
			this.sendSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method sends a request packet and accepts a Request object. It uses the
	 * Helper class to convert this into a packet and sends it using the
	 * 'sendSocket'. It prints the details of every packet being sent using the
	 * printPacketEventDetails method. The sockets stays open even when the sent is
	 * complete
	 */
	public void send(Request request, String host, Integer port) {

		DatagramPacket packet = null;
		try {
			packet = Helper.CreateRequest(request);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		InetAddress hostAddress = null;
		try {
			hostAddress = InetAddress.getByName(host);
		} catch (Exception E) {
			
		}
		
		//Set destination of packet
		packet.setAddress(hostAddress);
		packet.setPort(port);
		
		if(this.debug) {
			printPacketEventDetails(ElevatorSystemConfiguration.SEND_PACKET_EVENT, packet, this.sendSocket);
		}
		
		//Send packet using sendSocket
		try {
			this.sendSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		

	}

	/**
	 * This method receives a packet and waits indefinitely. It also prints the
	 * details using the printPacketEventDetails() method
	 * 
	 */
	public DatagramPacket receive(DatagramSocket socket) {
		DatagramPacket packet = null;
		try {
			packet = receive(socket, 0);
		} catch (Exception e) {
			// Since no timeout was specified, we know this is not a socket time out
			// exception
			System.out.println("Unhandled Exception Occurred. Exiting.");
			// System.exit(1);
		}
		return packet;
	}

	/**
	 * This method is responsible for receiving a packet with a specific timeout. It
	 * waits till the time is over and prints the details about the received message
	 * using the printPacketEventDetails() method
	 * 
	 * @param socket
	 * @param timeout - in milliseconds
	 * @return
	 */
	public DatagramPacket receive(DatagramSocket socket, int timeout) throws IOException {
		// Wait for 'packet' on 'socket'
		DatagramPacket packet = waitForPacket(socket, timeout);
		if (this.debug) {
			printPacketEventDetails(ElevatorSystemConfiguration.RECEIVE_PACKET_EVENT, packet, socket);
		}

		return packet;
	}

	/**
	 * This method is responsible for waiting for a packet
	 */
	private DatagramPacket waitForPacket(DatagramSocket socket, int timeout) throws IOException {
		// Construct a DatagramPacket for receiving packets up to 100 bytes long (the
		// length of the byte array).
		byte data[] = new byte[ElevatorSystemConfiguration.DEFAULT_PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		// Block until a packet is received on socket.
		socket.setSoTimeout(timeout);
		if (this.debug) {
			if (timeout == 0) {
				System.out.println(this.role + ": Waiting for a packet on port " + socket.getLocalPort() + "... \n");
			} else {
				System.out.println(this.role + ": Waiting " + timeout / 1000 + "s for a packet on port "
						+ socket.getLocalPort() + "... \n");
			}
		}
		socket.receive(packet);

		return packet;
	}

	/**
	 * THis method prints details about the packet.It shows the event description
	 * and the role of the Host which caused the event, the IP address and port on
	 * which the packet was received, the port the packet is sent on(when sending),
	 * the IP address and source port of the sender and the port the packet is
	 * received on(when receiving), the length of the packet in terms of bytes, the
	 * string representation of the packet and the byte representation
	 */
	private void printPacketEventDetails(boolean packetEvent, DatagramPacket packet, DatagramSocket socket) {
		int len = packet.getLength();
		if (packetEvent == ElevatorSystemConfiguration.SEND_PACKET_EVENT) {
			System.out.println(this.role + ": Sending Packet");
			System.out.println("To Host: " + packet.getAddress());
			System.out.println("To Host port: " + packet.getPort());
			System.out.println("Sent using port: " + socket.getLocalPort());
		} else if (packetEvent == ElevatorSystemConfiguration.RECEIVE_PACKET_EVENT) {
			System.out.println(this.role + ": Received Packet");
			System.out.println("From Host: " + packet.getAddress());
			System.out.println("From Host port: " + packet.getPort());
			System.out.println("Received on Port: " + socket.getLocalPort());
		}
		System.out.println("Packet length: " + len);
		System.out.println("Packet contains (String): " + new String(packet.getData(), 0, len));
		System.out.println("Packet contains (bytes): " + getPacketDataBytesAsString(packet));
		System.out.println();
	}

	/**
	 * This method converts the bytes to string format
	 */
	private String getPacketDataBytesAsString(DatagramPacket packet) {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = packet.getData();
		sb.append("[");
		for (int i = 0; i < packet.getLength(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(buffer[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	public void close() {
		receiveSocket.close();
	}

	@Override
	public void run() {
		while (true) {

			// Wait indefinitely to receive the next packet.
			DatagramPacket packet = null;
			try {
				packet = receive(receiveSocket, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// turn the packet received into a Request and add it to the
			// elevatorSystemComponent's queue.
			try {
				Request request = Helper.ParseRequest(packet);
				request.setStartTime();
				elevatorSystemComponent.receiveEvent(Helper.ParseRequest(packet));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

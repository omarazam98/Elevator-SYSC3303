package info;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Request {
	private Date time;
	private int floor;
	private String direction;
	private int carButton;
	
	public Request(Date time, int floor, String direction, int carButton) {
		this.time = time;
		this.floor = floor;
		this.direction = direction;
		this.carButton = carButton;
	}
	
	public void setFloor(int floor) {
		this.floor = floor;
	}
	
	public void setDirec(String direction) {
		if(direction.equals("UP") || direction.equals("DOWN")) {
			this.direction = direction;
		}
	}
	
	public void setCarButton(int carButton) {
		this.carButton = carButton;
	}
	
	public Date getTime() {
		return time;
	}
	
	public int getFloor() {
		return floor;
	}
	
	public String getDirec() {
		return direction;
	}
	
	public int getCarButton() {
		return carButton;
	}

}

package enums;

public enum LampStatus {
	UP("UP"), DOWN("DOWN"), OFF("OFF");
	
	private String str;
	
	LampStatus(String str){
		this.setStr(str);
	}
	
	public LampStatus getStatus(String str) {
		switch(str) {
		case "UP": return UP;
		case "DOWN": return DOWN;
		default: return OFF;
		}
	}

	public void setStr(String str) {
		this.str = str;
	}
	
	@Override
	public String toString() {
		return str;
	}
}
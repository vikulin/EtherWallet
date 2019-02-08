package org.vikulin.etherpush;

public class Data {
	
	public enum Encryption{
		NOT_ENCRYPTED,
		TYPE1
	}
	
	
	
	public Data(String type, String object) {
		this.type = type;
		this.object = object;
	}
	
	private String type;

	private String object;

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}

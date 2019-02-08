package org.vikulin.etherpush;

public class Transaction extends Message implements SignedMessage {
	
	public Transaction(Long timestamp, String from, String to, String value) {
		super(Code.TRANSACTION_IN, timestamp, from, to);
		this.value = value;
	}
		
	private String value;
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public byte[] getPreSignData() {
		return (String.valueOf(getCode())+Long.toString(getTimestamp())+":"+getFrom()+":"+getTo()+":"+value).getBytes();
	}
}

package org.vikulin.etherpush;

import org.web3j.crypto.ECKeyPair;

import java.io.UnsupportedEncodingException;

public class Message implements Code {

	private int code;
	private SignatureWrapper signature;
	private Long timestamp;
	private String from;
	private String to;
	
	public Message(){
		
	}
	
	public Message(int code, Long timestamp, String from, String to) {
		this.timestamp = timestamp;
		this.code = code;
		this.from = from;
		this.to = to;
	}
	
	public int getCode() {
		return code;
	}

	public SignatureWrapper getSignature() {
		return signature;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	public String getFrom() {
		return from;
	}
	public String getTo() {
		return to;
	}

	public Message sign(ECKeyPair keyPair) throws UnsupportedEncodingException {
		signature = new SignatureWrapper(Sign.signMessage(getPreSignData(), keyPair));
		return this;
	}

	public byte[] getPreSignData() throws UnsupportedEncodingException {
		return null;
	}

}

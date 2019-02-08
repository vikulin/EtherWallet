package org.vikulin.etherpush;

import org.spongycastle.util.encoders.Base64;

public class Invite extends Message implements SignedMessage {
	
	private boolean isDefaultMessage;
	private String message;
	/**
	 * PGP public key
	 */
	private String publicKey;

	public Invite(Long timestamp, boolean isDefaultMessage, String from, String to, String message, String publicKey) {
		super(Code.INVITE, timestamp, from, to);
		this.isDefaultMessage = isDefaultMessage;
		this.message = Base64.toBase64String(message.getBytes());
		this.publicKey = publicKey;
	}
	
	public boolean isDefaultMessage() {
		return isDefaultMessage;
	}

	public String getMessage() {
		return new String(Base64.decode(message));
	}

	public String getPublicKey(){
		return publicKey;
	}

	public byte[] getPreSignData(){
		return (String.valueOf(getCode())+Long.toString(getTimestamp())+":"+ message+":"+getFrom()+":"+getTo()+":"+publicKey).getBytes();
	}

}
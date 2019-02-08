package org.vikulin.etherpush;

import org.spongycastle.util.encoders.Hex;

public class SignatureWrapper {
	
	private String r;
	private String s;
	private byte v;

	public SignatureWrapper(SignatureData data) {
		v = data.getV();
		byte[] rByteArray = data.getR();
		byte[] sByteArray = data.getS();
		r = Hex.toHexString(rByteArray);
		s = Hex.toHexString(sByteArray);
	}
	
	public SignatureData toSignatureData(){
		return new SignatureData(v, Hex.decode(r), Hex.decode(s));
	}

	public String getR() {
		return r;
	}

	public String getS() {
		return s;
	}

	public byte getV() {
		return v;
	}

}

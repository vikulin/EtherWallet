package org.vikulin.etherpush;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Hex;
import org.vikulin.etherpush.data.PGPExampleUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.vikulin.etherpush.data.PGP.decryptStream;
import static org.vikulin.etherpush.data.PGP.encryptStream;

public class TextMessage extends Message implements SignedMessage {

	private String message;
	/**
	 * This key verifies destination address
	 */
	private String publicKey;

	private String publicKeyHash;

	public TextMessage(Long timestamp, String from, String to, String message, String publicKey, String pgpPublicKey) throws NoSuchAlgorithmException, IOException, PGPException, NoSuchProviderException {
		super(Code.TEXT_MESSAGE, timestamp, from, to);
        //Unencrypted message
		//this.message = Base64.toBase64String(message.getBytes());
		this.publicKey = publicKey;
        this.publicKeyHash = Hex.toHexString(MessageDigest.getInstance("MD5").digest(publicKey.getBytes("UTF-8")));
        InputStream publicKeyStream = new ByteArrayInputStream(Hex.decode(pgpPublicKey));
        PGPPublicKey encKey = PGPExampleUtil.readPublicKey(publicKeyStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encryptStream(out, (publicKeyHash+":"+message).getBytes("UTF-8"), "encrypted_stream", encKey, false, true);
        this.message = Hex.toHexString(out.toByteArray());
    }

	public String getMessage(String privateKey, String password) throws Exception {
        decrypt(privateKey, password);
		cut();
		return message;
	}

	public String getMessage() {
		return message;
	}

	public String getPublicKey(){
		return publicKey;
	}

	public String setPublicKey(String publicKey){
		return this.publicKey=publicKey;
	}

	public void setPublicKeyHash(String publicKeyHash){
		this.publicKeyHash=publicKeyHash;
	}

	public byte[] getPreSignData() throws UnsupportedEncodingException {
		return (String.valueOf(getCode())+Long.toString(getTimestamp())+":"+ message+":"+getFrom()+":"+getTo()+":"+publicKeyHash).getBytes("UTF-8");
	}

	private final void decrypt(String privateKey, String password) throws Exception {
        InputStream privateKeyStream = new ByteArrayInputStream(Hex.decode(privateKey));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        decryptStream(new ByteArrayInputStream(Hex.decode(this.message)), privateKeyStream, password.toCharArray(), out);
        this.message = out.toString("UTF-8");
    }

    private final void cut(){
		this.message = this.message.substring(message.indexOf(':') + 1, message.length());
	}
}

package org.vikulin.etherpush;

import java.io.UnsupportedEncodingException;

public interface SignedMessage {
	
	byte[] getPreSignData() throws UnsupportedEncodingException;

}

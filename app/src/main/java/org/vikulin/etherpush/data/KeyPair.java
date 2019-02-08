package org.vikulin.etherpush.data;

/**
 * Created by vadym on 28.04.17.
 */

public class KeyPair {

    public KeyPair(String privateKey, String publicKey){
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    private String privateKey;

    private String publicKey;

}

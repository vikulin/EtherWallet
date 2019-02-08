package org.vikulin.etherpush.data;

/**
 * Created by vadym on 02.05.17.
 */

public class EncryptCredentials {

    private String password;
    private String pgpPublicKey;
    private String key_name;
    private String address;
    private boolean blacklisted;

    public EncryptCredentials(String address, String key_name, String pgpPublicKey, String password){
        this.address = address;
        this.key_name = key_name;
        this.pgpPublicKey = pgpPublicKey;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getPgpPublicKey() {
        return pgpPublicKey;
    }

    public String getKeyName() {
        return key_name;
    }

    public String getAddress() {
        return address;
    }
}

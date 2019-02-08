package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GenerateAddressPair extends Pair {

    private String address;

    private String extraId;

    public GenerateAddressPair(String from, String to, String address) {
        super(from, to);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getExtraId() {
        return extraId;
    }

}

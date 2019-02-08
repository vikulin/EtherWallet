package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GenerateAddressRequest extends ChangellyRequest {

    public GenerateAddressRequest(Long id, GenerateAddressPair params) {
        super(id, "generateAddress");
        this.params = params;
    }

    private GenerateAddressPair params;

    public GenerateAddressPair getParams() {
        return params;
    }

}

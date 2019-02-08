package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GetMinAmountRequest extends ChangellyRequest{

    private Pair params;

    public GetMinAmountRequest(Long id, Pair params) {
        super(id, "getMinAmount");
        this.params = params;
    }

    public Pair getParams() {
        return params;
    }

}

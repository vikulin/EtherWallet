package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GetExchangeAmountRequest extends ChangellyRequest {

    private GetExchangeAmountPair params;

    public GetExchangeAmountRequest(Long id, GetExchangeAmountPair params) {
        super(id, "getExchangeAmount");
        this.params = params;
    }

    public Pair getParams() {
        return params;
    }


}

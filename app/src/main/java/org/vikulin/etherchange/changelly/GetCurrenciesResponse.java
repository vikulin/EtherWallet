package org.vikulin.etherchange.changelly;

import java.io.Serializable;
import java.util.List;

/**
 * Created by vadym on 20.12.17.
 */

public class GetCurrenciesResponse implements Serializable {

    private String jsonrpc;
    private Long id;
    private List<String> result;
    private Error error;

    public Long getId() {
        return id;
    }

    public List<String> getResult() {
        return result;
    }

    public Error getError() {
        return error;
    }
}

package org.vikulin.etherwallet.adapter.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadym on 30.07.17.
 */

public class EthplorerResponse {

    private List<Token> tokens = new ArrayList();

    private Eth ETH;

    public List<Token> getTokens(){
        return tokens;
    }

    public Eth getEth(){
        return ETH;
    }
}

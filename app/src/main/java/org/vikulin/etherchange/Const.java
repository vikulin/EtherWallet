package org.vikulin.etherchange;

/**
 * Created by vadym on 03.04.17.
 */

public class Const {

    public interface AdvCash{
        String DECIMAL_FORMAT="#0.00";
        String success_url = "https://www.changer.com/advcash_success.html";
        String fail_url = "https://www.changer.com/advcash_fail.html";
        String success_url_method = "GET";
        String success = "advcash_success";
        String transactionIdKey = "ac_transfer";
        String sci_url = "https://wallet.advcash.com/sci/";
    }

    public interface PM{
        String DECIMAL_FORMAT="#0.00";
        String success_url = "https://www.changer.com/pm_success.html";
        String fail_url = "https://www.changer.com/pm_fail.html";
        String success_url_method = "GET";
        String success = "pm_success";
        String transactionIdKey = "PAYMENT_BATCH_NUM";
        String sci_url = "https://perfectmoney.is/api/step1.asp";
    }

    public interface OKPAY{
        String DECIMAL_FORMAT="#0.00";
        String success_url = "https://www.changer.com/okpay_success.html";
        String fail_url = "https://www.changer.com/okpay_fail.html";
        String success_url_method = "GET";
        String success = "okpay_success";
        String transactionIdKey = "ok_txn_id";
        String sci_url = "https://checkout.okpay.com/";
    }

    public interface PAYEER{
        String DECIMAL_FORMAT="#0.00";
        String success_url = "https://www.changer.com/payeer_success.html";
        String fail_url = "https://www.changer.com/payeer_fail.html";
        String success_url_method = "GET";
        String success = "payeer_success";
        String transactionIdKey = "ok_txn_id";
        String sci_url = "https://payeer.com/merchant/";
    }

    public interface BTCE {
        String DECIMAL_FORMAT="#0.00";
        String success_url = "https://www.changer.com/btce_success.html";
        String fail_url = "https://www.changer.com/btce_fail.html";
        String success_url_method = "GET";
        String success = "btce_success";
        String transactionIdKey = "ok_txn_id";
        String sci_url = "https://btc-e.com/tapi";
    }

}

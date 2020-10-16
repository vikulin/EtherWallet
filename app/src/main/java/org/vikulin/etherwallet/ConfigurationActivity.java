package org.vikulin.etherwallet;

import android.app.Activity;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import static org.vikulin.etherwallet.DrawerActivity.LANGUAGE;

public class ConfigurationActivity extends ListActivity {

    public static final String POLICY = "policy";
    public static String[] languages = new String[]{
            "Deutsch",
            "Ελληνικά",
            "English",
            "Español",
            "Suomi",
            "Français",
            "Magyar",
            "Bahasa Indonesia",
            "ಕನ್ನಡ",
            "Italiano",
            "日本語",
            "Nederlands",
            "Norsk Bokmål",
            "Polski",
            "Português",
            "Русский",
            "Türkçe",
            "Tiếng Việt",
            "中文"
    };

    public static String[] language_codes = new String[]{
            "de",
            "el",
            "en",
            "es",
            "fi",
            "fr",
            "hu",
            "id",
            "kn",
            "it",
            "ja",
            "nl",
            "no",
            "pl",
            "pt",
            "ru",
            "tr",
            "vi",
            "zh"
    };

    public static final String pp = "<p class=\"western\" align=\"center\" style=\"margin-bottom: 0.14in; line-height: 115%; page-break-inside: avoid; page-break-after: avoid\">\n" +
            "<font color=\"#000000\"><font size=\"4\" style=\"font-size: 14pt\"><b>Privacy\n" +
            "policy</b></font></font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>1.\tIntroduction</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">1.1\tWe are committed to\n" +
            "safeguarding the privacy of our EtherWallet application users; in\n" +
            "this policy we explain how we will treat your personal information.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font color=\"#000000\"><font size=\"2\" style=\"font-size: 10pt\"><b>2.\tCredit</b></font></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">2.1\tThis document was created\n" +
            "using a template from SEQ Legal (<a href=\"http://www.seqlegal.com/\"><font color=\"#0000ff\"><u>http://www.seqlegal.com</u></font></a>).</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font color=\"#000000\"><font size=\"2\" style=\"font-size: 10pt\"><b>3.\tCollecting\n" +
            "personal information</b></font></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">3.1\tWe may collect, store and\n" +
            "use the following kinds of personal information: </font>\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tInformation about your\n" +
            "mobile device including your IP address, hardware type, operating\n" +
            "system.</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tDebug information such as\n" +
            "stack traces.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">3.2\tWe never ask for personal\n" +
            "or private information like ID or credit card numbers.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>4.\tUsing\n" +
            "personal information</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">4.1\tPersonal information will\n" +
            "be used for the purposes specified in this policy or on the relevant\n" +
            "pages of the website.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">4.2\tWe may use your personal\n" +
            "information to: </font>\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tAdminister our application\n" +
            "and business;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tPersonalize our\n" +
            "application for you;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(c)\tEnable your use of the\n" +
            "services available in our application;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(d)\tSend statements, invoices\n" +
            "and payment reminders to you, and collect payments from you;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(e)\tSend you non-marketing\n" +
            "commercial communications;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(f)\tSend you email\n" +
            "notifications that you have specifically requested;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(g)\tSend you our email\n" +
            "newsletter, if you have requested it (you can inform us at any time\n" +
            "if you no longer require the newsletter);</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(h)\tProvide third parties with\n" +
            "statistical information about our users (but those third parties will\n" +
            "not be able to identify any individual user from that information);</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(i)\tKeep our application\n" +
            "secure and prevent fraud</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<br/>\n" +
            "<br/>\n" +
            "\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">4.3\tWe will not, without your\n" +
            "express consent, supply your personal information to any third party\n" +
            "for the purpose of their or any other third party's direct marketing.</font></p>\n" +
            "<p align=\"left\" style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">4.4\tAll our application\n" +
            "financial transactions are handled through Ethereum network service\n" +
            "provider, <a href=\"http://infura.io/\"><i>http://infura.io/</i></a>.\n" +
            "You can review the provider's privacy policy at\n" +
            "<a href=\"http://infura.io/privacy.html\"><i>http://infura.io/privacy.html</i></a>.\n" +
            "We do not share information with our payment services provider even\n" +
            "in case of refunding payments and dealing with complaints and queries\n" +
            "relating to such payments and refunds.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>5.\tDisclosing\n" +
            "personal information</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">5.1\tWe may disclose your\n" +
            "personal information: </font>\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tto the extent that we are\n" +
            "required to do so by law;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tin connection with any\n" +
            "ongoing or prospective legal proceedings;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(c)\tin order to establish,\n" +
            "exercise or defend our legal rights (including providing information\n" +
            "to others for the purposes of fraud prevention and reducing credit\n" +
            "risk);</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">5.2\tExcept as provided in this\n" +
            "policy, we will not provide your personal information to third\n" +
            "parties.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>6.\tInternational\n" +
            "walletList transfers</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">6.1\tInformation that we\n" +
            "collect may be stored and processed in and transferred between any of\n" +
            "the countries in which we operate in order to enable us to use the\n" +
            "information in accordance with this policy.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">6.2\tPersonal information that\n" +
            "you publish in our application may be available, via the Internet,\n" +
            "around the world. We cannot prevent the use or misuse of such\n" +
            "information by others.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">6.3\tYou expressly agree to the\n" +
            "transfers of personal information described in this Section 6.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>7.\tRetaining\n" +
            "personal information</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">7.1\tThis Section 7 sets out\n" +
            "our walletList retention policies and procedure, which are designed to help\n" +
            "ensure that we comply with our legal obligations in relation to the\n" +
            "retention and deletion of personal information.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">7.2\tPersonal information that\n" +
            "we process for any purpose or purposes shall not be kept for longer\n" +
            "than is necessary for that purpose or those purposes.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">7.3\tNotwithstanding the other\n" +
            "provisions of this Section 7, we will retain documents (including\n" +
            "electronic documents) containing personal walletList: </font>\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tto the extent that we are\n" +
            "required to do so by law;</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tif we believe that the\n" +
            "documents may be relevant to any ongoing or prospective legal\n" +
            "proceedings; and</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(c)\tin order to establish,\n" +
            "exercise or defend our legal rights (including providing information\n" +
            "to others for the purposes of fraud prevention and reducing credit\n" +
            "risk).</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>8.\tSecurity\n" +
            "of personal information</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">8.1\tWe will take reasonable\n" +
            "technical and organizational precautions to prevent the loss, misuse\n" +
            "or alteration of your personal information.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">8.2\tWe will store all the\n" +
            "personal information you provide on our secure (password- and\n" +
            "firewall-protected) servers.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">8.3\tAll electronic financial\n" +
            "transactions entered into through our website will be protected by\n" +
            "encryption technology.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">8.4\tYou acknowledge that the\n" +
            "transmission of information over the Internet is inherently insecure,\n" +
            "and we cannot guarantee the security of walletList sent over the Internet.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">8.5\tYou are responsible for\n" +
            "keeping the password you use for accessing our accounts confidential;\n" +
            "we will not ever ask you for your password.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>9.\tAmendments</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">9.1\tWe may update this policy\n" +
            "from time to time by publishing a new version on our website.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">9.2\tYou should check this page\n" +
            "occasionally to ensure you are happy with any changes to this policy.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">9.3\tWe may notify you of\n" +
            "changes to this policy by email or through the private messaging\n" +
            "system on our website.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>10.\tYour\n" +
            "rights</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">10.1\tYou may instruct us to\n" +
            "provide you with any personal information we hold about you;\n" +
            "provision of such information will be subject to: </font>\n" +
            "</p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tthe payment of a fee\n" +
            "(currently fixed at GBP 50); and</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tthe supply of appropriate\n" +
            "evidence of your identity [(for this purpose, we will usually accept\n" +
            "a photocopy of your passport certified by a solicitor or bank plus an\n" +
            "original copy of a utility bill showing your current address)].</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">10.2\tWe may withhold personal\n" +
            "information that you request to the extent permitted by law.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">10.3\tYou may instruct us at\n" +
            "any time not to process your personal information for marketing\n" +
            "purposes.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">10.4\tIn practice, you will\n" +
            "usually either expressly agree in advance to our use of your personal\n" +
            "information for marketing purposes, or we will provide you with an\n" +
            "opportunity to opt out of the use of your personal information for\n" +
            "marketing purposes.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>11.\tThird\n" +
            "party websites</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">11.1\tOur website includes\n" +
            "hyperlinks to, and details of, third party websites.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">11.2\tWe have no control over,\n" +
            "and are not responsible for, the privacy policies and practices of\n" +
            "third parties.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font size=\"2\" style=\"font-size: 10pt\"><b>12.\tUpdating\n" +
            "information</b></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">12.1\tPlease let us know if the\n" +
            "personal information that we hold about you needs to be corrected or\n" +
            "updated.</font></p>\n" +
            "<p class=\"western\" style=\"margin-bottom: 0.14in; line-height: 115%\"><font color=\"#000000\"><font size=\"2\" style=\"font-size: 10pt\"><b>1</b></font></font><font color=\"#000000\"><font size=\"2\" style=\"font-size: 10pt\"><b>3</b></font></font><font color=\"#000000\"><font size=\"2\" style=\"font-size: 10pt\"><b>.\tOur\n" +
            "details</b></font></font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">13.1\tThis application and\n" +
            "website is owned and operated by <i>Vadym Vikulin</i>.</font></p>\n" +
            "<p style=\"margin-left: 0.42in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">13.2\tYou can contact me:</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(a)\tBy email:\n" +
            "vadym.vikulin@gmail.com</font></p>\n" +
            "<p style=\"margin-left: 0.83in; text-indent: -0.42in; margin-bottom: 0.14in; line-height: 115%\">\n" +
            "<font size=\"2\" style=\"font-size: 10pt\">(b)\tBy telephone:\n" +
            "+380980987585</font></p>";

    private static SharedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        final String language = preferences.getString(LANGUAGE, null);
        updateAndroidSecurityProvider(this);
        if(language == null) {
            final ListView lv = getListView();
            lv.setTextFilterEnabled(true);
            setListAdapter(new ArrayAdapter<>(this, R.layout.list_language_item, languages));
            lv.setOnItemClickListener((parent, view, position, id) -> {
                String language1 = languages[position];
                preferences.edit().putString(LANGUAGE, language1).commit();
                if(preferences.getString(POLICY, null) == null) {
                    lv.setVisibility(View.GONE);
                    finish();
                    startActivity(getIntent());
                    //showPolicy(position);
                } else {
                    run(position);
                }
            });
        } else {
            if(preferences.getString(POLICY, null) == null) {
                showPolicy(language);
            } else {
                run(language);
            }
        }
    }

    private void run(int position){
        Intent intent = new Intent(ConfigurationActivity.this, AccountListActivity.class);
        String language = languages[position];
        intent.putExtra(LANGUAGE, language);
        startActivity(intent);
        finish();
    }

    private void updateAndroidSecurityProvider(Activity callingActivity) {

        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            // Thrown when Google Play Services is not installed, up-to-date, or enabled
            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
            GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0);
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e("SecurityException", "Google Play Services not available.");
        }
    }

    private void run(String language){
        Intent intent = new Intent(ConfigurationActivity.this, AccountListActivity.class);
        intent.putExtra(LANGUAGE, language);
        startActivity(intent);
        finish();
    }

    private void showPolicy(final String language){
        View view = LayoutInflater.from(this).inflate(R.layout.policy_layout,null);
        TextView msg=(TextView)view.findViewById(R.id.policy);

        msg.setText(Html.fromHtml(pp));
        AlertDialog.Builder ab = new AlertDialog.Builder(ConfigurationActivity.this);
        ab.setTitle("EtherWallet")
                .setIcon(R.mipmap.eth)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Accept", (dialogInterface, i) -> {
                    preferences.edit().putString(POLICY, "accepted").commit();
                    run(language);
                });
        ab.show();
    }

    private void showPolicy(final int position){
        View view = LayoutInflater.from(this).inflate(R.layout.policy_layout,null);
        TextView msg=(TextView)view.findViewById(R.id.policy);

        msg.setText(Html.fromHtml(pp));
        AlertDialog.Builder ab = new AlertDialog.Builder(ConfigurationActivity.this);
        ab.setTitle("Privacy policy")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Accept", (dialogInterface, i) -> {
                    preferences.edit().putString(POLICY, "accepted").commit();
                    run(position);
                });
    }

}
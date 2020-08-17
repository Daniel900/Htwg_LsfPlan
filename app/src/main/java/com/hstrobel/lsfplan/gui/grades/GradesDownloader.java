package com.hstrobel.lsfplan.gui.grades;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.hstrobel.lsfplan.Constants;
import com.hstrobel.lsfplan.GlobalState;
import com.hstrobel.lsfplan.R;
import com.hstrobel.lsfplan.model.CryptoUtils;
import com.hstrobel.lsfplan.model.Utils;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class GradesDownloader implements Runnable {
    Context context = null;
    GradesListFragment fragment;
    Activity activity;
    Handler h = new Handler();

    String user = "";
    String pw = "";
    String JSESSIONID = null;
    String ASI;
    String notenURL;
    String[] Grades;
    ArrayList<String[]> grades;

    public GradesDownloader(Context context, Activity activity, GradesListFragment fragment){
        this.context=context;
        this.fragment = fragment;
        GlobalState state = GlobalState.getInstance();
        boolean useKeystore = state.settings.getBoolean(Constants.PREF_FLAG_KEYSTORE, true);
        if (useKeystore) {
            user = CryptoUtils.getStoreField(context, Constants.PREF_LOGIN_USER);
            pw = CryptoUtils.getStoreField(context, Constants.PREF_LOGIN_PASSWORD);

        } else {
            user = state.settings.getString(Constants.PREF_LOGIN_USER, "");
            pw = state.settings.getString(Constants.PREF_LOGIN_PASSWORD, "");
            if (!user.equals("")) {
                user = new String(Base64.decode(user, Base64.DEFAULT));
                pw = new String(Base64.decode(pw, Base64.DEFAULT));
            }
        }
    }

    public void requestGrades(){
        h.post(this);
    }

    @Override
    public void run() {
        try {
            if (user != "" && pw != "") {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        grades = downloadGrades();
                    }
                });
                t.start();
                while(t.isAlive()){}
                fragment.gradesDownloadedCallback(grades);
            } else {
                showLoginForm();
            }
        }catch (Exception e){

        }
    }

    private void login(){
        try {
            String url = Utils.getLoginUrlGrades(context, GlobalState.getInstance().getCollege());
            Connection connection = Utils.setupAppConnection(url, context)
                    .data("username", user)
                    .data("password", pw)
                    .data("submit", "Anmeldung");
            connection.post();
            Connection.Response response = connection.response();
            Log.d("Grades", String.valueOf(response.method()));

            //get is successfull because it will redirect. if it fails we get a modified post response.
            if (response.method() == Connection.Method.GET) {
                //yay
                for (String key : response.cookies().keySet()) {
                    Log.d("Grades", String.format("%s : %s", key, response.cookie(key)));
                    if (key.equals("JSESSIONID")) {
                        JSESSIONID = response.cookie(key);
                    }
                }
            }

        } catch (Exception ex) {
            Log.e("Grades", "Login failed ", ex);
        }
    }


    private void getNotenURL(){
        // Making HTTP request
        try {
            Connection con = Utils.setupAppConnection(Utils.getPruefungsverwaltungURL(context, GlobalState.getInstance().getCollege()), context);
            con.cookie("JSESSIONID", JSESSIONID);
            Document doc = con.get();


            Elements elements = doc.select("a");
            for (Element a : elements) {
                if(a.hasAttr("href")){
                    if(a.attr("href").contains("notenspiegel")){
                        notenURL =  a.attr("href");
                    }
                }
            }

        } catch (Exception ex) {
            Log.d("Grades", "getNotenURL: " + ex.toString());
        }
    }
    private ArrayList<String[]> downloadGrades(){
        login();
        getNotenURL();
        try {
            Connection con = Utils.setupAppConnection(notenURL, context);
            con.cookie("JSESSIONID", JSESSIONID);
            Document doc = con.get();
            Grades = new String[1];
            ArrayList<String[]> list = new ArrayList<String[]>();
            Elements elements = doc.select("tr");
            for(Element element : elements){
                ArrayList<String> row = new ArrayList<String>();
                    for(Node child : element.childNodes()){
                        String value = getChildValue(child);
                        if(!value.contains("\n")) {
                            row.add(value);
                        }
                    }
                list.add(listToArray(row));
            }
            return list;
        } catch (Exception e){
            Log.d("Grades", "downloadGrades: " + e.toString());
        }
        return null;
    }

    private String getChildValue(Node element){
        if(element.childNodeSize() > 0){
            return getChildValue(element.childNode(0));
        } else {
            String returnValue = ((TextNode)element).getWholeText();
            return returnValue;
        }
    }

    private String[] listToArray(ArrayList<String> list){
        String[] array = new String[list.size()];
        for(int a = 0; a < list.size(); a++){
            array[a] = list.get(a);
        }
        return array;
    }

    private void showLoginForm() {
        GlobalState state = GlobalState.getInstance();
        // Create Object of Dialog class
        final Dialog login = new Dialog(context);
        // Set GUI of login screen
        login.setContentView(R.layout.login_dialog);
        login.setTitle("Enter HTWG Login Data");

        // Init button of login GUI
        final CheckBox box = login.findViewById(R.id.checkSave);
        final Button btnLogin = login.findViewById(R.id.btnLogin);
        final Button btnCancel = login.findViewById(R.id.btnCancel);
        final EditText txtUsername = login.findViewById(R.id.txtUsername);
        final EditText txtPassword = login.findViewById(R.id.txtPassword);

        boolean autoSave = state.settings.getBoolean(Constants.PREF_LOGIN_AUTOSAVE, true);
        boolean useKeystore = state.settings.getBoolean(Constants.PREF_FLAG_KEYSTORE, true);

        box.setChecked(autoSave);
        if (autoSave) {
            String user = "", pw = "";

            if (useKeystore) {
                user = CryptoUtils.getStoreField(context, Constants.PREF_LOGIN_USER);
                pw = CryptoUtils.getStoreField(context, Constants.PREF_LOGIN_PASSWORD);

            } else {
                user = state.settings.getString(Constants.PREF_LOGIN_USER, "");
                pw = state.settings.getString(Constants.PREF_LOGIN_PASSWORD, "");
                if (!user.equals("")) {
                    user = new String(Base64.decode(user, Base64.DEFAULT));
                    pw = new String(Base64.decode(pw, Base64.DEFAULT));
                }
            }

            txtUsername.setText(user);
            txtPassword.setText(pw);
        }


        // Attached listener for login GUI button
        btnLogin.setOnClickListener(v -> {
            String user = txtUsername.getText().toString().trim();
            String pw = txtPassword.getText().toString().trim();

            if (!user.isEmpty() && !pw.isEmpty()) {
                login.dismiss();
                //login
                downloadGrades();
            }


            SharedPreferences.Editor editor = state.settings.edit();
            editor.putBoolean(Constants.PREF_LOGIN_AUTOSAVE, box.isChecked());
            if (box.isChecked()) {
                //Well, at least it's no cleartext ;)
                if (useKeystore) {
                    CryptoUtils.setStoreField(context, Constants.PREF_LOGIN_USER, user);
                    CryptoUtils.setStoreField(context, Constants.PREF_LOGIN_PASSWORD, pw);
                } else {
                    editor.putString(Constants.PREF_LOGIN_USER, Base64.encodeToString(user.getBytes(), Base64.DEFAULT));
                    editor.putString(Constants.PREF_LOGIN_PASSWORD, Base64.encodeToString(pw.getBytes(), Base64.DEFAULT));
                }

            }
            editor.apply();
        });
        btnCancel.setOnClickListener(v -> login.dismiss());

        // Make dialog box visible.
        login.show();
    }
}

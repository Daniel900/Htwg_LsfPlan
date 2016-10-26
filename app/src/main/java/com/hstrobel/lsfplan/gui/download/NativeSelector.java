package com.hstrobel.lsfplan.gui.download;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.hstrobel.lsfplan.Globals;
import com.hstrobel.lsfplan.R;
import com.hstrobel.lsfplan.gui.download.network.ICSLoader;
import com.hstrobel.lsfplan.gui.download.network.LoginProcess;
import com.hstrobel.lsfplan.model.Utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class NativeSelector extends AbstractWebSelector {
    public static final int TIMEOUT = 10 * 1000;
    private static final String MAGIC_WORD_LOGIN = "#LOGIN#";
    private static final String TAG = "LSF";
    PlanExportLoader exportLoader = null;
    PlanOverviewLoader overviewLoader = null;
    LoginProcess loginProcess = null;
    private NativeSelector local;
    private ExpandableListView listView;
    private CourseListAdapter listAdapter;
    private ProgressBar spinner;
    private CourseGroup selectedCourseGroup = null;
    private CourseGroup.Course selectedCourse = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_web_selector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadExportUrl();
            }
        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        local = this;
        listView = (ExpandableListView) findViewById(R.id.listView);
        listAdapter = new CourseListAdapter(this);
        listView.setAdapter(listAdapter);

        spinner = (ProgressBar) findViewById(R.id.progressBarHtml);

        loadOverview();

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            private View lastHighlight = null;

            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.background, typedValue, true);

                v.setBackgroundResource(R.color.orange);
                if (lastHighlight != null)
                    lastHighlight.setBackgroundColor(typedValue.data);
                lastHighlight = v;


                int group_index = parent.getFlatListPosition(ExpandableListView
                        .getPackedPositionForGroup(groupPosition));
                int child_index = parent.getFlatListPosition(ExpandableListView
                        .getPackedPositionForChild(groupPosition, childPosition));


                selectedCourseGroup = (CourseGroup) parent.getItemAtPosition(group_index);
                selectedCourse = (CourseGroup.Course) parent.getItemAtPosition(child_index);
                return true;
            }

        });
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (exportLoader != null) exportLoader.cancel(true);
        if (overviewLoader != null) overviewLoader.cancel(true);
        if (loginProcess != null) loginProcess.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_html_web, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            Globals.cachedPlans = null;
            loadOverview();
        }

        return super.onOptionsItemSelected(item);
    }


    private void loadExportUrl() {
        Log.d(TAG, "loadExportUrl");
        if (selectedCourse == null)
            return;

        if (selectedCourse.URL.equals(MAGIC_WORD_LOGIN)) {
            showLoginForm();
        } else {
            //Tracking
            logDownload();

            //UI
            enableLoading();

            exportLoader = new PlanExportLoader();
            exportLoader.execute(selectedCourse.URL);
        }
    }

    private void logDownload() {
        //Max 36 chars ^^
        String content = (selectedCourseGroup.name + "_" + selectedCourse.name).substring(0, 35);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, Globals.CONTENT_DL);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, content);
        Globals.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        Globals.firebaseAnalytics.setUserProperty(Globals.FB_PROP_CATEGORY, selectedCourseGroup.name);
        Globals.firebaseAnalytics.setUserProperty(Globals.FB_PROP_SPECIFIC, selectedCourse.name);
    }

    public void loginCallback(String loginCookie) {
        if (loginCookie == null) {
            Toast.makeText(this, "Login failed. Check your username/password.", Toast.LENGTH_LONG).show();
            disableLoading();
            return;
        }

        exportLoader = new PlanExportLoader();
        exportLoader.execute(Utils.getPersonalPlanUrl(this, Globals.getCollege()), loginCookie);
    }

    private void exportCallback(String url) {
        if (url == null) {
            disableLoading();
            Toast.makeText(this, "Download failed! Check your Connection", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "exportCallback");
        Globals.icsLoader = new ICSLoader(this, url);
        new Thread(Globals.icsLoader).start();
    }

    private void loadOverview() {
        Log.d(TAG, "loadOverview");

        if (Globals.cachedPlans != null) {
            // got a saved version
            overviewCallback(Globals.cachedPlans);
        } else {
            enableLoading();

            String url = Utils.getCoursesOverviewUrl(this, Globals.getCollege());
            overviewLoader = new PlanOverviewLoader();
            overviewLoader.execute(url);
        }
    }

    private void overviewCallback(List<CourseGroup> results) {
        if (results == null) {
            Toast.makeText(this, "Download failed! Check your Connection", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "overviewCallback");
        Globals.cachedPlans = results;

        listAdapter.clear();
        listAdapter.addPlanGroup(getString(R.string.html_login_head));
        listAdapter.addPlanItem(getString(R.string.html_login_head), getString(R.string.html_login_sub), MAGIC_WORD_LOGIN);

        for (CourseGroup group : results) {
            listAdapter.addPlanGroup(group.name);
            for (CourseGroup.Course item : group.items) {
                listAdapter.addPlanItem(group.name, item.name, item.URL);
            }
        }
        listView.invalidateViews();

        disableLoading();
    }


    private void showLoginForm() {
        // Create Object of Dialog class
        final Dialog login = new Dialog(this);
        // Set GUI of login screen
        login.setContentView(R.layout.login_dialog);
        login.setTitle("Enter HTWG Login Data");

        // Init button of login GUI
        final CheckBox box = (CheckBox) login.findViewById(R.id.checkSave);
        final Button btnLogin = (Button) login.findViewById(R.id.btnLogin);
        final Button btnCancel = (Button) login.findViewById(R.id.btnCancel);
        final EditText txtUsername = (EditText) login.findViewById(R.id.txtUsername);
        final EditText txtPassword = (EditText) login.findViewById(R.id.txtPassword);

        boolean autoSave = Globals.settings.getBoolean("loginAutoSave", true);
        box.setChecked(autoSave);
        if (autoSave) {
            String user = Globals.settings.getString("loginUser", "");
            String pw = Globals.settings.getString("loginPassword", "");
            if (!user.equals("")) {
                user = new String(Base64.decode(user, Base64.DEFAULT));
                pw = new String(Base64.decode(pw, Base64.DEFAULT));
            }

            txtUsername.setText(user);
            txtPassword.setText(pw);
        }


        // Attached listener for login GUI button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = txtUsername.getText().toString().trim();
                String pw = txtPassword.getText().toString().trim();

                if ((user.length() > 0) && (pw.length() > 0)) {
                    login.dismiss();
                    //login
                    enableLoading();

                    loginProcess = new LoginProcess(local);
                    loginProcess.execute(user, pw);
                }

                SharedPreferences.Editor editor = Globals.settings.edit();
                editor.putBoolean("loginAutoSave", box.isChecked());
                if (box.isChecked()) {
                    editor.putString("loginUser", Base64.encodeToString(user.getBytes(), Base64.DEFAULT));
                    editor.putString("loginPassword", Base64.encodeToString(pw.getBytes(), Base64.DEFAULT));
                }
                editor.apply();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login.dismiss();
            }
        });

        // Make dialog box visible.
        login.show();
    }

    private void enableLoading() {
        spinner.setVisibility(View.VISIBLE);
        listView.setEnabled(false);
    }

    private void disableLoading() {
        listView.setEnabled(true);
        spinner.setVisibility(View.GONE);
    }

    public class PlanOverviewLoader extends AsyncTask<String, String, List<CourseGroup>> {
        @Override
        protected List<CourseGroup> doInBackground(String... params) {
            // Making HTTP request
            List<CourseGroup> list = new LinkedList<>();
            CourseGroup group;
            CourseGroup.Course item;

            try {

                Document doc = Jsoup.parse(new URL(params[0]), 5000);

                Elements tableRows = doc.select("tr");
                for (Element row : tableRows) {
                    if (row.children().size() != 3) continue; //skip head row
                    Elements columns = row.children();

                    //course name
                    Element courseURL = columns.get(0).child(0); //row 0 --> a class --> inner text
                    if (Globals.DEBUG) Log.d(TAG, courseURL.text());
                    group = new CourseGroup(courseURL.text());

                    //course semesters
                    for (Element ele : columns.get(1).children()) {
                        if (ele.tagName() != "a") continue;
                        if (Globals.DEBUG)
                            Log.d(TAG, String.format("%s : %s", ele.text(), ele.attr("href")));
                        item = new CourseGroup.Course(ele.text(), ele.attr("href"));
                        group.items.add(item);
                    }
                    //course everthing
                    Element allURL = columns.get(2).child(0); //row 0 --> a class --> inner text
                    if (Globals.DEBUG)
                        Log.d(TAG, String.format("%s : %s", allURL.text(), allURL.attr("href")));
                    item = new CourseGroup.Course(allURL.text(), allURL.attr("href"));
                    group.items.add(item);

                    list.add(group);
                }

            } catch (Exception ex) {
                Log.e(TAG, "FAIL DL: ", ex);
                list = null;
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<CourseGroup> result) {
            super.onPostExecute(result);
            overviewCallback(result);
        }
    }

    public class PlanExportLoader extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // Making HTTP request
            String url = null;
            try {
                Connection con = Jsoup.connect(params[0]);
                if (params.length > 1) {
                    con.cookie("JSESSIONID", params[1]);
                }
                con.userAgent("LSF APP");
                con.timeout(TIMEOUT);
                Document doc = con.get();


                Elements images = doc.select("img");
                for (Element imgs : images) {
                    if (imgs.hasAttr("src")) {
                        if (imgs.attr("src").equals("/QIS/images//calendar_go.gif")) {
                            //found export ;)
                            Log.d(TAG, imgs.parent().attr("href"));
                            url = imgs.parent().attr("href");
                            break;
                        }
                    }
                }

            } catch (Exception ex) {
                Log.e(TAG, "FAIL DL: ", ex);
                url = null;
            }
            return url;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            exportCallback(result);
        }
    }


}
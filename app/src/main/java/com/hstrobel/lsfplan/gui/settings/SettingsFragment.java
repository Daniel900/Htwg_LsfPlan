package com.hstrobel.lsfplan.gui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;

import com.hstrobel.lsfplan.BuildConfig;
import com.hstrobel.lsfplan.Constants;
import com.hstrobel.lsfplan.GlobalState;
import com.hstrobel.lsfplan.R;
import com.hstrobel.lsfplan.model.NotificationUtils;
import com.hstrobel.lsfplan.model.calender.CalenderUtils;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsFragment;

import net.fortuna.ical4j.model.component.VEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by Henry on 28.09.2017.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "LSF";
    private boolean notifyChanged = false;
    private GlobalState state = GlobalState.getInstance();

    //TODO: kill magic values :/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the settings from an XML resource
        addPreferencesFromResource(R.xml.settings);

        //general info update
        onSharedPreferenceChanged(state.settings, "");
        //update dep state
        onSharedPreferenceChanged(state.settings, "skipWeekend");

        Preference myPref = findPreference("reset");
        myPref.setOnPreferenceClickListener(preference -> {
            SharedPreferences.Editor editor = state.settings.edit();
            editor.clear();
            editor.commit();
            state.initialized = false;

            if (getActivity() != null) {
                PreferenceManager.setDefaultValues(getActivity().getApplicationContext(), R.xml.settings, true);
                NavUtils.navigateUpFromSameTask(getActivity());
            }
            return true;
        });


        myPref = findPreference("about");
        myPref.setOnPreferenceClickListener(preference -> {
            LibsFragment frag = new LibsBuilder()
                    .withActivityStyle(Libs.ActivityStyle.LIGHT)
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .withFields(R.string.class.getFields())
                    .withAboutDescription("Created by Henry Strobel (hstrobel.dev@gmail.com)\n " + (BuildConfig.DEBUG ? "DEBUG" : ""))
                    .fragment();

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, frag)
                    .addToBackStack(null)
                    .commit();
            return true;
        });

        ListPreference newpref = (ListPreference) findPreference("college_pref");
        newpref.setTitle(R.string.pref_set_college);
        newpref.setSummary("");
        newpref.setEnabled(true);
        newpref.setEntries(new String[]{"HTWG", "UNI"});
        newpref.setEntryValues(new String[]{String.valueOf(Constants.MODE_HTWG), String.valueOf(Constants.MODE_UNI_KN)});
        newpref.setOnPreferenceChangeListener((preference, o) -> {
            if (o != null) {
                state.setCollege(Integer.parseInt((String) o));
                state.cachedPlans = null;
            }
            return true;
        });


        myPref = findPreference("btnShowBriefing");
        myPref.setOnPreferenceClickListener(preference -> {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);

            List<VEvent> list = CalenderUtils.getEventsForDay(GlobalState.getInstance().myCal, cal);
            CalenderUtils.sortEvents(list);

            NotificationUtils.showBriefingNotification(list, getActivity());

            return true;
        });


        PreferenceCategory credits = (PreferenceCategory) findPreference("credits");
        if (!BuildConfig.DEBUG) {
            credits.removePreference(findPreference("dev_options"));
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "notfiyTime":
                if (TextUtils.isEmpty(state.settings.getString("notfiyTime", "15"))) {
                    //prevent empty field
                    state.settings.edit().putString("notfiyTime", "0").apply();
                }
            case "enableNotifications":
                state.InitNotifications(getActivity());

                notifyChanged = true;
                break;
            case "skipWeekend":
                findPreference("skipWeekendDaysWithoutEvents").setEnabled(sharedPreferences.getBoolean(key, false));
                break;
        }

        Preference myPref = findPreference("notfiyTime");
        int time = Integer.parseInt(state.settings.getString("notfiyTime", "15"));
        myPref.setSummary(String.format(getString(R.string.pref_description_timeSetter), time));

        ListPreference sPref = (ListPreference) findPreference("soundMode");
        if (sPref.getEntry() != null) sPref.setSummary(sPref.getEntry());

        DateFormat d = SimpleDateFormat.getDateTimeInstance();
        long time_load = state.settings.getLong("ICS_DATE", Integer.MAX_VALUE);
        GregorianCalendar syncTime = new GregorianCalendar();
        syncTime.setTimeInMillis(time_load);

        myPref = findPreference("enableRefresh");
        myPref.setSummary(String.format(getString(R.string.pref_description_refresh), d.format(syncTime.getTime())));
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyChanged = false;

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        if (notifyChanged) {
            String info = String.format("%s_%s", String.valueOf(state.settings.getBoolean("enableNotifications", false)), state.settings.getString("notfiyTime", "15"));
        }
    }
}

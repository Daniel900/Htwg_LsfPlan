package com.hstrobel.lsfplan.classes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.hstrobel.lsfplan.MainActivity;
import com.hstrobel.lsfplan.classes.gui.PlanGroup;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Created by Henry on 09.11.2015.
 */
public class Globals {
    public static boolean DEBUG = false;
    public static MainActivity mainActivity;

    public static ICSLoader icsLoader = null;
    public static InputStream icsFileStream = null;
    public static String icsFile = null; //ICS Calender as text

    public static SharedPreferences mSettings;
    public static boolean initialized = false;
    public static boolean updated = false;
    public static boolean changed = false;


    public static Calendar myCal = null;
    public static List<PlanGroup> cachedPlans = null;

    public static void InitCalender(Context c, boolean initNotification) throws IOException, ParserException {
        //App Startup, load everthing from our settings file
        if (!initialized) {
            //Try to load from file
            mSettings = PreferenceManager.getDefaultSharedPreferences(c);
            myCal = null;
            if (mSettings.getBoolean("gotICS", false)) {
                //found something!
                icsFile = mSettings.getString("ICS_FILE", "");
                updated = true;
            }
            initialized = true;
        }

        //Update the Calender object with the n(new) fileStream
        if (updated) {
            icsFileStream = IOUtils.toInputStream(icsFile, "UTF-8");
            if (icsFileStream != null) {
                CalendarBuilder builder = new CalendarBuilder();
                myCal = builder.build(icsFileStream);
            }
            updated = false;
            if (initNotification){
                InitNotifications(c);
            }
        }

        //Start the background service that downloads a new caleander from time to time
        Intent mServiceIntent = new Intent(c, SyncService.class);
        mServiceIntent.setData(Uri.parse(""));
        c.startService(mServiceIntent);
    }

    public static void InitNotifications(final Context c) {
        ComponentName receiver = new ComponentName(c, BootReceiver.class);
        PackageManager pm = c.getPackageManager();

        if (Globals.mSettings.getBoolean("enableNotifications", false)) {
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            new Thread(new Runnable() {
                public void run() {
                    AlarmReceiver.ScheduleNextEventNot(c);
                }
            }).start();

        } else {
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            AlarmReceiver.CancelNextEventNot(c);
        }
    }


    public static void SetNewCalendar(Context act) throws IOException, ParserException {
        updated = true;
        InitCalender(act, false);

        SharedPreferences.Editor editor = Globals.mSettings.edit();
        editor.putBoolean("gotICS", true);
        editor.putString("ICS_FILE", Globals.icsFile);
        editor.putLong("ICS_DATE", java.util.Calendar.getInstance().getTimeInMillis());
        editor.putString("ICS_URL", Globals.icsLoader.url);
        editor.apply();

        //Set to null to show that noting is download --> used by SyncService
        icsLoader = null;
    }

    public static void Save() throws IOException, ValidationException {
        if (changed) {
            Writer w = new StringWriter();
            CalendarOutputter output = new CalendarOutputter();
            output.output(myCal, w);
            icsFile = w.toString();

            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString("ICS_FILE", Globals.icsFile);
            editor.apply();
        }

    }

    public static boolean isDownloadValid() {
        return Globals.icsLoader.file == null || !Globals.icsLoader.file.startsWith("BEGIN:VCALENDAR");
    }


}

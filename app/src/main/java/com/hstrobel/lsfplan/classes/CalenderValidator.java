package com.hstrobel.lsfplan.classes;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Henry on 16.11.2015.
 */
public class CalenderValidator {
    public static boolean CorrectEvents() throws IOException {
        /*
        TO IGNORE
        BEGIN:VEVENT
        DTSTART;TZID=Europe/Berlin:T094500
        DTEND;TZID=Europe/Berlin:T111500
        RRULE:FREQ=WEEKLY;UNTIL=20160129T235900Z;INTERVAL=1;BYDAY=TU
        EXDATE:
        LOCATION:F - 109
        DTSTAMP:20151115T174812Z
        UID:150582264882
        DESCRIPTION:
        SUMMARY:14220920 - Rechnerarchitekturen
        CATEGORIES:Vorlesung/Übung
        END:VEVENT
         */
        //Globals.icsFileStream.reset();
        Log.d("LSF", "CorrectEvents");
        Globals.icsFileStream = IOUtils.toInputStream(Globals.loader.file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(Globals.icsFileStream));
        StringBuilder builder = new StringBuilder();
        StringBuilder eventBuilder = null;
        boolean vaildEvent = true;
        boolean ignoredEvent = false;

        String line;
        while ((line = reader.readLine()) != null ) {
            if (eventBuilder != null){
                eventBuilder.append(line).append("\n");
                if (line.startsWith("END:VEVENT")){
                    //event ende
                    if (vaildEvent){
                        builder.append(eventBuilder.toString());
                    } else {
                        ignoredEvent = true;
                        Log.d("LSF", "Ignored event");
                    }
                    eventBuilder = null;
                    vaildEvent = true;
                } else if (line.startsWith("DTSTART;TZID=Europe/Berlin:T")){
                    vaildEvent = false;
                } else if (line.startsWith("DTEND;TZID=Europe/Berlin:T")){
                    vaildEvent = false;
                }
            } else if (line.startsWith("BEGIN:VEVENT")){
                eventBuilder = new StringBuilder();
                eventBuilder.append(line).append("\n");
            } else {
                builder.append(line).append("\n");
            }
        }

        Globals.icsFile = builder.toString();
        return ignoredEvent;
    }
}

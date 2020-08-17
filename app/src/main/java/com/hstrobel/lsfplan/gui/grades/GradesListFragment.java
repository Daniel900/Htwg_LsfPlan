package com.hstrobel.lsfplan.gui.grades;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.hstrobel.lsfplan.R;

import java.util.ArrayList;


public class GradesListFragment extends Fragment {
    private ListAdapter listAdapter;
    private Context context;
    private Activity activity;

    public void setGradesListFragmentValues(Activity activity){
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grades_listview, container, false);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateContent();
    }


    private void updateContent() {
        GradesDownloader gd = new GradesDownloader(context, activity, this);
        gd.requestGrades();
    }

    public void gradesDownloadedCallback(ArrayList<String[]> Grades){
        TableLayout tl = this.getView().findViewById(R.id.gradesTable);
        tl.removeAllViews();
        for(String[] row : Grades){
            TableRow tr = new TableRow(context);
            for(int a = 0; a < row.length; a++) {
                TextView tv = new TextView(context);
                tv.setText(row[a]);
                tr.addView(tv);
            }
            tl.addView(tr);
        }
    }


}
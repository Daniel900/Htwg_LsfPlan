package com.hstrobel.lsfplan.gui.grades;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hstrobel.lsfplan.R;

/**
 * Created by Henry on 28.09.2017.
 */
public class Grades extends AppCompatActivity{
    public GradesListFragment listFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_grades);
        listFragment = new GradesListFragment();
        listFragment.setGradesListFragmentValues(this);
        getFragmentManager().beginTransaction().replace(R.id.mainFragmentContainer, listFragment).commit();
    }
}

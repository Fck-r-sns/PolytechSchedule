package com.binary_machinery.avalonschedule;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.binary_machinery.avalonschedule.data.GlobalEnvironment;
import com.binary_machinery.avalonschedule.data.Schedule;
import com.binary_machinery.avalonschedule.data.ScheduleRecord;
import com.binary_machinery.avalonschedule.tools.DbProvider;
import com.binary_machinery.avalonschedule.tools.ScheduleStorager;
import com.binary_machinery.avalonschedule.tools.ScheduleUpdater;

import java.util.Calendar;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class ScheduleActivity extends AppCompatActivity {

    int m_nearestCoursePosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        GlobalEnvironment env = GlobalEnvironment.getInstance();
        if (env.dbProvider == null) {
            env.dbProvider = new DbProvider(this);
        }
        restoreScheduleFromDb();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_go_to_nearest_course:
                scrollToNearestCourse();
                break;
            case R.id.menu_update:
                updateSchedule();
                break;
            case R.id.menu_changes:
                showChanges();
                break;
            case R.id.menu_settings:
                showSettings();
                break;
            case R.id.menu_about:
                showAbout();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void restoreScheduleFromDb() {
        DbProvider dbProvider = GlobalEnvironment.getInstance().dbProvider;
        ScheduleStorager storager = new ScheduleStorager(dbProvider);
        Schedule schedule = storager.restoreSchedule();
        List<ScheduleRecord> records = schedule.getRecords();
        m_nearestCoursePosition = findNearestCourse(records);
        printScheduleRecords(records);
    }

    private void scrollToNearestCourse() {
        ListView list = (ListView) findViewById(R.id.scheduleList);
        list.smoothScrollToPosition(m_nearestCoursePosition);
    }

    private void updateSchedule() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCES_NAME, MODE_PRIVATE);
        String sourceUrl = prefs.getString(Constants.PREF_URL, "");
        Toast.makeText(this, R.string.task_loading_in_process, Toast.LENGTH_SHORT).show();
        Observable.just(sourceUrl)
                .concatMap(ScheduleUpdater::get)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        schedule -> {
                            List<ScheduleRecord> records = schedule.getRecords();
                            printScheduleRecords(records);
                        },
                        throwable -> Toast.makeText(this, getString(R.string.task_loading_failed) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show(),
                        () -> Toast.makeText(this, R.string.task_loading_finished, Toast.LENGTH_SHORT).show()
                );
    }

    private void showChanges() {
        Intent settingsIntent = new Intent(this, ChangesActivity.class);
        startActivity(settingsIntent);
    }

    private void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void showAbout() {
        // TODO: implement
    }

    private void printScheduleRecords(List<ScheduleRecord> records) {
        ListView list = (ListView) findViewById(R.id.scheduleList);
        ListAdapter adapter = new RecordsListAdapter(this, records, m_nearestCoursePosition);
        list.setAdapter(adapter);
    }

    private static int findNearestCourse(List<ScheduleRecord> records) {
        int nearestCoursePosition = 0;
        long currentTime = Calendar.getInstance().getTimeInMillis();
        while (records.get(nearestCoursePosition).date.getTime() < currentTime) {
            ++nearestCoursePosition;
        }
        return nearestCoursePosition;
    }
}

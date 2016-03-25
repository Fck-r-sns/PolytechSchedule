package com.binary_machinery.avalonschedule.tools;

import com.binary_machinery.avalonschedule.data.GlobalEnvironment;
import com.binary_machinery.avalonschedule.data.Schedule;
import com.binary_machinery.avalonschedule.data.ScheduleMetadata;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by fckrsns on 11.03.2016.
 */
public class ScheduleUpdater {
    public static Observable<Schedule> get(String sourceUrl) {
        GlobalEnvironment env = GlobalEnvironment.getInstance();
        ScheduleStorager storager = new ScheduleStorager(env.dbProvider);
        return Observable.just(sourceUrl)
                .concatMap(ScheduleLoader::load)
                .concatMap(ScheduleParser::parse)
                .map(records -> {
                    Schedule schedule = new Schedule();
                    ScheduleMetadata metadata = new ScheduleMetadata();
                    metadata.url = sourceUrl;
                    schedule.setMetadata(metadata);
                    schedule.setRecords(records);
                    return schedule;
                })
                .subscribeOn(Schedulers.io())
                .map(schedule -> {
                    Schedule oldSchedule = storager.restoreSchedule();
                    ScheduleComparator cmp = new ScheduleComparator();
                    boolean equals = cmp.compare(oldSchedule, schedule);
                    env.deletedRecords = cmp.getDeletedRecords();
                    env.addedRecords = cmp.getAddedRecords();
                    if (!equals) {
                        storager.storeSchedule(schedule);
                        storager.storeDeletedRecords(env.deletedRecords);
                        storager.storeAddedRecords(env.addedRecords);
                    }
                    return schedule;
                });
    }
}
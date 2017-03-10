package kr.ac.kaist.kse.ic.obd2datacollector.dao;

import com.j256.ormlite.field.DatabaseField;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kimauk on 2016. 11. 8..
 */

public class LogEntry {
    @DatabaseField(generatedId= true)
    long id;

    @DatabaseField(index = true)
    long timestamp;

    @DatabaseField
    int level;

    @DatabaseField
    int domain;

    @DatabaseField
    String msg;

    LogEntry() {
    }

    public LogEntry(long timestamp, int level, int domain, String msg) {
        this.timestamp = timestamp;
        this.level = level;
        this.domain = domain;
        this.msg = msg;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.S");
        sb.append(dateFormatter.format(new Date(timestamp))).append(",\t").append(level);
        sb.append(",\t").append(domain).append(",\t");
        sb.append(",\t").append(msg);
        return sb.toString();
    }
}

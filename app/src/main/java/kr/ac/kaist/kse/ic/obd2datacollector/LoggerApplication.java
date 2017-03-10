package kr.ac.kaist.kse.ic.obd2datacollector;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import kr.ac.kaist.kse.ic.obd2datacollector.dao.DatabaseHelper;
import kr.ac.kaist.kse.ic.obd2datacollector.dao.LogEntry;

/**
 * Created by kimauk on 2017. 3. 10..
 */

public abstract class LoggerApplication extends Application{

    public static final int DOMAIN_LOGGER = 0;
    public static final int DOMAIN_APPLICATION = 1;
    public static final int DOMAIN_SERVICE = 2;


    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_ERROR = 0;
    private static final int MAX_NUMBER_OF_LOGS_IN_MEMORY = 100;


    private static boolean writing = false;

    private static DatabaseHelper databaseHelper = null;
    private static final List<LogEntry> logEntriesToInsert = new ArrayList<>();

    private static final String TAG = LoggerApplication.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.close();
    }

    @Override
    public void onTerminate() {
        tryToWrite(true);

        if (getDatabaseHelper() != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
        super.onTerminate();
    }

    public void copyLogTo(String toDirectory){
        Validate.notNull(toDirectory);
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        Validate.isTrue(externalStorageDirectory.canWrite());

        final String databaseName = getDatabaseHelper().getDatabaseName();
        File toFile = new File(Environment.getExternalStorageDirectory().toString()+"/"+toDirectory, databaseName);

        Validate.isTrue(toFile.isFile());
        tryToWrite(true);
        try {
            final File fromFile = getDatabasePath(databaseName);

            FileChannel src = new FileInputStream(fromFile).getChannel();
            FileChannel dst = new FileOutputStream(toFile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            info("Database has been copied to %s",  toFile.toString());
        } catch (Exception e) {
            error(e, "Error while copying log database to %s", toFile.toString());
            e.printStackTrace();
        }
    }

    private DatabaseHelper getDatabaseHelper(){
        return databaseHelper;
    }

    private static void tryToWrite(boolean force) {
        if (!writing) {
            writing = true;
            if (logEntriesToInsert.size() > MAX_NUMBER_OF_LOGS_IN_MEMORY || force)
                writeToDB();
            writing = false;
        }
    }

    // Write to persistence DB
    private static void writeToDB() {
        try {
            final Dao<LogEntry, Long> logEntryDao = databaseHelper.getLogEntryDao();
            final int numOfLogs = logEntriesToInsert.size();
            logEntryDao.callBatchTasks(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (LogEntry logEntry : logEntriesToInsert) {
                        logEntryDao.create(logEntry);
                    }
                    info("%s logs has log entries are successfully recorded.", ""+numOfLogs);
                    logEntriesToInsert.clear();
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            error(e, "Error while writing log entries");
        }
    }

    private static void info(String format, String... args){
        i(System.currentTimeMillis(), DOMAIN_LOGGER, format, args);
    }

    private static void error(Throwable throwable, String format, String... args){
        e(throwable, DOMAIN_LOGGER, format, args);
    }

    // Create a log entry
    private static LogEntry createLogEntry(long timestamp, int level, int domain, String format, Object... args) {
        if(args != null)
            format = String.format(format, args);
        LogEntry newLogEntry = new LogEntry(timestamp, level, domain, format);
        logEntriesToInsert.add(newLogEntry);
        tryToWrite(false);
        return newLogEntry;
    }

    public static void i(long timestamp, int domain, String format, Object... args) {
        Log.i(TAG, createLogEntry(timestamp, LEVEL_INFO, domain, format, args).toString());
    }

    public static void e(Throwable throwable, int domain, String format, String... args) {
        Log.e(TAG, createLogEntry(System.currentTimeMillis(), LEVEL_ERROR, domain, format, args).toString());
    }
}

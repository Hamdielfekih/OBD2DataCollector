package kr.ac.kaist.kse.ic.obd2datacollector.dao;

/**
 * Created by kimauk on 2016. 11. 8..
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    public static final String INITIAL_DATABASE_NAME = "msbanddata.db";

    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1; // ensure 1 <= DATABASE_VERSION


    private Dao<LogEntry, Long> logDao = null;

    public DatabaseHelper(Context context) {
        super(context, INITIAL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, LogEntry.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade from oldVersion=[" + oldVersion + "] to newVersion=[" + newVersion + "]");

        try {
            while (++oldVersion <= newVersion) {
                switch (oldVersion) {
                    case 1: {
                        TableUtils.dropTable(connectionSource, LogEntry.class, true);
                        Log.i(TAG, "table dropped");
                        onCreate(db, connectionSource);
                        break;
                    }
                    case 2: {
                        Log.i(TAG, "test upgrade");
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<LogEntry, Long> getLogEntryDao() throws SQLException {
        if (logDao == null) {
            logDao = getDao(LogEntry.class);
        }
        return logDao;
    }

    @Override
    public void close() {
        super.close();
        logDao = null;
    }
}
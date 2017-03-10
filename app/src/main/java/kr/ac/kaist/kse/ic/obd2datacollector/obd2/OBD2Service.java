package kr.ac.kaist.kse.ic.obd2datacollector.obd2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;

import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import kr.ac.kaist.kse.ic.obd2datacollector.LoggerApplication;
import kr.ac.kaist.kse.ic.obd2datacollector.OBD2DataLogger;

/**
 * Created by kimauk on 2017. 2. 21..
 */

public class OBD2Service extends Service{

    private static OBD2Service mThis;

    public class LocalBinder extends Binder {
        public OBD2Service getService(){
            return OBD2Service.this;
        }
    }

    private final IBinder binder = new OBD2Service.LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    private BluetoothSocket sock = null;

    private static final String TAG = OBD2Service.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    private BluetoothAdapter btAdapter;

    public boolean initialize(){
        mThis = this;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(getBluetoothAdapter() == null)
            return false;
        getCommandJobWorker().start();
        return true;
    }

    private BluetoothAdapter getBluetoothAdapter(){
        return btAdapter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }

    public void close(){
        getCommandJobWorker().interrupt();

        jobsQueue.removeAll(jobsQueue); // TODO is this safe?
        isRunning = false;
        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

        // kill service
        stopSelf();
    }



    public static OBD2Service getInstance() {
        return mThis;
    }

    public static boolean isRunning() {
        return isRunning;
    }
    private static boolean isRunning;

    public boolean connect(String address){
        Validate.notEmpty(address);
        final BluetoothDevice dev = getBluetoothAdapter().getRemoteDevice(address);
        Validate.notNull(dev);
        btAdapter.cancelDiscovery();


        isRunning = true;
        try {
            sock = BluetoothManager.connect(dev);
        } catch (Exception e) {
            close();
            OBD2DataLogger.e(e, LoggerApplication.DOMAIN_SERVICE,"Not running.");
            return false;
        }

        queueJob(new ObdCommandJob(new ObdResetCommand()));
        queueJob(new ObdCommandJob(new EchoOffCommand()));

        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(62)));

        // Get protocol from preferences
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf("AUTO"))));

        // Job for returning dummy data
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

        queueCounter = 0L;

        return true;
    }

    private Thread getCommandJobWorker(){
        return t;
    }

    protected Long queueCounter = 0L;
    protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
    Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                executeQueue();
            } catch (InterruptedException e) {
                t.interrupt();
            }
        }
    });

    public boolean isQueueEmpty() {
        return jobsQueue.isEmpty();
    }

    public void queueJob(ObdCommandJob job) {
        job.getCommand().useImperialUnits(false); //TODO:

        queueCounter++;

        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
        }
    }

    private void executeQueue() throws InterruptedException{
        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();

                if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                    job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                } else
                    OBD2DataLogger.i(System.currentTimeMillis(), LoggerApplication.DOMAIN_SERVICE,"Job state was not new, so it shouldn't be in queue. BUG ALERT!");

            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED);
                }
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                }
            }
            if (job != null) {
                final ObdCommand obdCommand = job.getCommand();
                OBD2DataLogger.i(System.currentTimeMillis(), OBD2DataLogger.DOMAIN_SERVICE,"pid = %s, %s", obdCommand.getName(), Arrays.toString(obdCommand.getRawByteArrayData()));
            }
        }
    }

}

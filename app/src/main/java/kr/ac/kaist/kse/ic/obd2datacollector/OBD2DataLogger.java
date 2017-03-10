package kr.ac.kaist.kse.ic.obd2datacollector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.view.Gravity;
import android.widget.Toast;

import kr.ac.kaist.kse.ic.obd2datacollector.obd2.OBD2Service;

/**
 * Created by kimauk on 2017. 3. 11..
 */

public class OBD2DataLogger extends LoggerApplication {
    // Register the BroadcastReceiver
    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        if (mBtAdapter == null) {
            toastMessage( "Bluetooth is not supported");
            System.exit(0);
        }

        startOBD2Service();

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            toastMessage( "Bluetooth LE is not supported");
        }

        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableIntent);
        }
    }

    public BluetoothAdapter mBtAdapter = null;
    public static BluetoothManager mBluetoothManager;

    private OBD2Service obd2Service = null;

    private final ServiceConnection obd2ServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            obd2Service = ((OBD2Service.LocalBinder) service).getService();
            if (!obd2Service.initialize()) {
                toastMessage("Initialize OBD2Service failed");
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            obd2Service = null;
        }
    };

    private void toastMessage(String msg){
        i(System.currentTimeMillis(), DOMAIN_APPLICATION, msg);
        Toast m = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        m.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 300);
        m.show();
    }

    private void startOBD2Service() {
        boolean f;
        Intent bindIntent = new Intent(this, OBD2Service.class);
        startService(bindIntent);
        f = bindService(bindIntent, obd2ServiceConnection, Context.BIND_AUTO_CREATE);
        if (!f) {
            toastMessage("Bind to OBD2Service failed");
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (mBtAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        startOBD2Service();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    default:
                        break;
                }
            }
        }
    };

    @Override
    public void onTerminate() {
        OBD2Service.getInstance().close();
        unbindService(obd2ServiceConnection);
        stopService(new Intent(this, OBD2Service.class));
        super.onTerminate();
    }
}

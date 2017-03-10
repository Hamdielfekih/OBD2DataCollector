package kr.ac.kaist.kse.ic.obd2datacollector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.github.pires.obd.commands.ObdCommand;

import kr.ac.kaist.kse.ic.obd2datacollector.obd2.OBD2Service;
import kr.ac.kaist.kse.ic.obd2datacollector.obd2.ObdCommandJob;
import kr.ac.kaist.kse.ic.obd2datacollector.obd2.ObdConfig;

public class MainActivity extends AppCompatActivity {
    private final static String OBD2_DEVICE_NAME = "OBD";
    private static final long queueDelayTime = 100;
    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            OBD2Service service = OBD2Service.getInstance();
            if (service.isRunning() && service.isQueueEmpty()) {
                for (ObdCommand Command : ObdConfig.getCommands()) {
                    service.queueJob(new ObdCommandJob(Command));
                }
            }
            // run again in period defined in preferences
            new Handler().postDelayed(mQueueCommands, queueDelayTime);
        }
    };
    private final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private Switch switchOBD2;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (getBluetoothAdapter().getState()) {
                    case BluetoothAdapter.STATE_ON:
                        switchOBD2.setEnabled(true);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        switchOBD2.setEnabled(false);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    private boolean collectOBD2Data() {
        for (BluetoothDevice bluetoothDevice : getBluetoothAdapter().getBondedDevices())
            if (bluetoothDevice.getName().contains(OBD2_DEVICE_NAME) && bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                return OBD2Service.getInstance().connect(bluetoothDevice.getAddress());
            }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }




        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        switchOBD2 = (Switch) findViewById(R.id.switch_obd2);
        switchOBD2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!collectOBD2Data())
                                turnOffSwitchOBD2();
                            else
                                new Handler().post(mQueueCommands);
                        }
                    });
                }
            }
        });

        if (getBluetoothAdapter().getState() == BluetoothAdapter.STATE_OFF) {
            switchOBD2.setEnabled(false);
        }

        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }


    private void turnOffSwitchOBD2() {
        switchOBD2.setChecked(false);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}

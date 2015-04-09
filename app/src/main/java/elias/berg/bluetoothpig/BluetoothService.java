package elias.berg.bluetoothpig;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Eli on 3/24/2015.
 */
public class BluetoothService
{
    private static final long UUID_MSB = 0x42899803;
    private static final long UUID_LSB = 0x84560628;
    private static final UUID uuid = new UUID(UUID_MSB, UUID_LSB);

    private static BluetoothService BLUETOOTH_SERVICER = null;
    private static BluetoothAdapter mBluetoothAdapter;
    private static boolean supportsBluetoothLE = false;
    private static Context appContext;

    private static String username = "unknown user";

    private static BluetoothSocket socket;

    public static void initialize( Activity a )
    {
        if ( BLUETOOTH_SERVICER == null )
        {
            BLUETOOTH_SERVICER = new BluetoothService( a );

            if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
                supportsBluetoothLE = true;
        }
    }

    private BluetoothService( Activity a )
    {
        appContext = a.getApplicationContext();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public static void setName(String name)
    {
        username = name;
        mBluetoothAdapter.setName(name);
    }

    public static String getName()
    {
        return username;
    }

    /* MUST BE RUN ON SEPARATE THREAD */
    public static BluetoothServerSocket getServerSocket()
    {
        BluetoothServerSocket server = null;
        try {
            server = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(username, uuid);
        } catch (IOException e ) {
            Log.e("", "Error setting socket as server.");
        }

        return server;
    }

    /* GETS THE SOCKET FROM A CHOSEN DEVICE */
    public static void setSocket(BluetoothDevice device)
    {
        mBluetoothAdapter.cancelDiscovery();
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e ) {
            Log.e("", "Error setting socket.");
        }
    }

    /* SET SOCKET FROM SERVER */
    public static void setSocket(BluetoothSocket s)
    {
        mBluetoothAdapter.cancelDiscovery();
        socket = s;
    }

    public static BluetoothSocket getSocket()
    {
        return socket;
    }

    public static boolean bluetoothEnabled()
    {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            return false;
        return true;
    }

    public static boolean isDiscovering()
    {
        if (mBluetoothAdapter == null)
            return false;

        if (mBluetoothAdapter.isDiscovering())
            return true;
        return false;
    }

    public static void cancelDiscovery()
    {
        mBluetoothAdapter.cancelDiscovery();
    }

    /**
     * Static class for receiving standard Bluetooth connections.
     */
    public static class HostDetector extends BroadcastReceiver
    {
        private Activity activity;
        private ArrayList<BluetoothDevice> devices;
        private ArrayAdapter<String> adapter;
        private ProgressDialog mProcessDialogue;

        public HostDetector(Activity activity, ArrayList<BluetoothDevice> devices, ArrayAdapter<String> adapter)
        {
            this.activity = activity;
            this.devices = devices;
            this.adapter = adapter;
        }

        public void scan()
        {
            devices.clear();

            mProcessDialogue = new ProgressDialog(activity);
            mProcessDialogue.setMessage("Scanning...");
            mProcessDialogue.setCancelable(false);
            mProcessDialogue.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mBluetoothAdapter.cancelDiscovery();
                        }
                    });

            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            activity.registerReceiver(this, filter);
            mBluetoothAdapter.startDiscovery();
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                mProcessDialogue.show();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                mProcessDialogue.dismiss();
                for (int i = 0; i < devices.size(); i++)
                    adapter.add(devices.get(i).getName());
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action) && mBluetoothAdapter.isDiscovering())
            {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                /* Make sure this discovered device has the right UUID */
                boolean isHost = false;
                //ParcelUuid[] uuids = device.getUuids();

                if ( device == null )
                    return;

                if ( device.getName().contains("HOST: ") )
                    isHost = true;

                //if ( uuids == null )
                //{
                //    System.err.println("Error with UUIDs");
                //    return;
                //}

                //for ( int i = 0; i < uuids.length; i++ )
                //{
                //    if (uuids[i].getUuid().equals(uuid))
                //    {
                //        isHost = true;
                //        break;
                //    }
                //}

                /* Hosts will have a unique UUID */
                if ( !isHost )
                    return;

                devices.add( device );

                Toast.makeText(activity, "Found device " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Static class for receiving standard Bluetooth connections.
     *
     * USED FOR IF NO USERS CONNECT WITHIN THE TWO MINUTE TIMEOUT
     */
    public static class HostAdvertiser extends BroadcastReceiver
    {
        private HostActivity activity;

        public HostAdvertiser(HostActivity activity)
        {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
            {
                Toast.makeText(activity, "NO PLAYERS JOINED...", Toast.LENGTH_SHORT).show();
                //activity.killServer();
            }
        }
    }
}

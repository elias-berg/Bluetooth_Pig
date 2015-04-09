package elias.berg.bluetoothpig;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;


public class HostActivity extends Activity
{
    private final int DISCOVERY_REQUEST_BLUETOOTH = 0;

    private BluetoothService.HostAdvertiser advertiser;

    private Thread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);


        BluetoothService.setName( "HOST: " + BluetoothService.getName() );

        //advertiser = new BluetoothService.HostAdvertiser( this );
        //IntentFilter filter = new IntentFilter();
        //filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //registerReceiver(advertiser, filter);

        /* starts the server */
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
                DISCOVERY_REQUEST_BLUETOOTH);
    }

    /*public void killServer()
    {
        try {

            serverThread.interrupt();

        } catch ( IOException e ) {
            System.err.println(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        finish();
    }*/

    @Override
    protected void onStart()
    {
        super.onStart();
        //startServer();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        //startServer();
    }

    @Override
    protected void onDestroy()
    {
        //unregisterReceiver(advertiser);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == DISCOVERY_REQUEST_BLUETOOTH)
        {
            boolean isDiscoverable = resultCode > 0;

            if (isDiscoverable)
            {
                serverThread = new Thread(new Runnable() {

                    public void run() {

                        try {
                            BluetoothServerSocket server = BluetoothService.getServerSocket();

                            // this line is blocking
                            BluetoothSocket socket = server.accept(120000); //120 sec

                            server.close();

                            startGame( socket );

                        } catch (IOException e) {

                            System.err.println("BLUETOOTH " + e.getMessage());
                        }
                    }
                });
                serverThread.start();
            }
        }
        else
            Toast.makeText(this, "Unable to host without allowing permission.", Toast.LENGTH_SHORT).show();
    }

    private void startGame( BluetoothSocket socket )
    {
        BluetoothService.setSocket( socket );

        Intent startGame = new Intent(this, GameActivity.class);
        startGame.putExtra("First", true);

        startActivity( startGame );
        finish();
    }
}

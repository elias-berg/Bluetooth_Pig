package elias.berg.bluetoothpig;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class JoinActivity extends ListActivity
{
    private int selected = -1;

    private Button mScanButton;
    private BluetoothService.HostDetector receiver = null;
    private ArrayList<BluetoothDevice> hosts = null;

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    private ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        hosts = new ArrayList<BluetoothDevice>();

        mScanButton = (Button) findViewById(R.id.scan_button);
        mScanButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
                hosts.clear();
                receiver.scan();
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        setListAdapter(adapter);

        receiver = new BluetoothService.HostDetector( this, hosts, adapter );
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver( receiver );
        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        if ( BluetoothService.isDiscovering() )
            BluetoothService.cancelDiscovery();
        super.onPause();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        final BluetoothDevice b = hosts.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Play with " + b.getName() + "?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.e("", "CLICKED YES TO PLAY HOST");
                        startGame(b);
                    }
                });
        builder.create();
        builder.show();
    }

    private void startGame( BluetoothDevice b )
    {
        Log.e("", "About to connect...");
        BluetoothService.setSocket(b);

        Intent startGame = new Intent(this, GameActivity.class);
        startGame.putExtra("First", false);

        startActivity( startGame );
        finish();
    }

}

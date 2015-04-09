package elias.berg.bluetoothpig;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

/**
 * A login screen that offers the user to enter a user name to play other local players in the
 * game of pig.
 *
 * The user either chooses to host the game or join a game.
 */
public class LoginActivity extends Activity
{
    private final static int REQUEST_ENABLE_BT = 0;

    // UI references.
    private EditText mUserName;
    private Button mJoinButton;
    private Button mHostButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find the host and join buttons.
        mJoinButton = (Button) findViewById(R.id.join_button);
        mHostButton = (Button) findViewById(R.id.host_button);

        BluetoothService.initialize( this );

        // Set up the user name form.
        mUserName = (EditText) findViewById(R.id.username);

        setGUI();
    }

    private void setGUI()
    {
        boolean bluetoothEnabled = BluetoothService.bluetoothEnabled();

        if (bluetoothEnabled) {
            mJoinButton.setEnabled(true);
            mHostButton.setEnabled(true);
            // Set a listener to start the correct activity; join or hosting activities
            mJoinButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    hostORjoin( JoinActivity.class );
                }
            });
            mHostButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    hostORjoin( HostActivity.class );
                }
            });
        } else {
            // Don't let the user play if bluetooth isn't supported.
            Toast.makeText(this, "BLUETOOTH MUST BE ENABLED", Toast.LENGTH_LONG).show();
            mJoinButton.setEnabled(false);
            mHostButton.setEnabled(false);
        }
    }

    private boolean validName()
    {
        String name = mUserName.getText().toString();
        return name.length() > 0;
    }

    /**
     * Starts either the host or join activity.
     */
    private void hostORjoin(Class<?> hostjoin)
    {
        if ( !validName() )
            Toast.makeText(this, "Invalid name. Must be ([a-z0-9])+", Toast.LENGTH_SHORT).show();
        else
        {
            BluetoothService.setName( mUserName.getText().toString() );
            Intent intent = new Intent(this, hostjoin);
            startActivity(intent);
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        setGUI();
    }
}




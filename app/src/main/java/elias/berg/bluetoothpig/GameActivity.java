package elias.berg.bluetoothpig;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * Created by Eli on 4/2/2015.
 */
public class GameActivity extends Activity
{
    private static final int READ_NAME = 1;
    private static final int READ_SCORE = 2;

    private BluetoothSocket socket;
    private boolean turn, hold;

    private InputStream reader;
    private OutputStream writer;

    private TextView mPlayername, mPlayerscore, mOpponentname, mOpponentscore, mRolledVal;
    private Button mRoll, mHold;
    private int score, turnScore;

    private Random die;

    private Thread playThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent i = getIntent();
        socket = BluetoothService.getSocket();
        turn = (boolean) i.getBooleanExtra("First", false);

        if ( turn )
            Toast.makeText(this, "You go first.", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "You go second.", Toast.LENGTH_SHORT).show();

        /* textviews */
        mPlayername = (TextView) findViewById(R.id.playername);
        mPlayerscore = (TextView) findViewById(R.id.playerscore);
        mOpponentname = (TextView) findViewById(R.id.opponentname);
        mOpponentscore = (TextView) findViewById(R.id.opponentscore);
        mRolledVal = (TextView) findViewById(R.id.rolled);

        if ( BluetoothService.getName().contains("HOST: ") )
            BluetoothService.setName(BluetoothService.getName().substring(6));
        mPlayername.setText(BluetoothService.getName());

        mPlayerscore.setText("0");
        mOpponentscore.setText("0");

        /* buttons */
        mRoll = (Button) findViewById(R.id.roll_btn);
        mRoll.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roll();
            }
        });

        mHold = (Button) findViewById(R.id.hold_btn);
        mHold.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hold();
            }
        });

        setButtons(turn);

        die = new Random();
        score = 0;

        playThread = new Thread( new Runnable() {
            @Override
            public void run()
            {
                playGame();
            }
        });
        playThread.start();
    }

    /* Rolls the dice */
    private void roll()
    {
        int r = (die.nextInt(12) % 6) + 1;
        if ( r < 0 )
            r *= -1;

        if ( r == 1 )
        {
            turnScore = 0;
            Toast.makeText(this, "Rolled a 1!", Toast.LENGTH_SHORT).show();
            hold();
        }
        else
            turnScore += r;

        mRolledVal.setText(new Integer(turnScore).toString());
    }

    /* Ends the player's turn */
    private void hold()
    {
        score += turnScore;
        turnScore = 0;
        mPlayerscore.setText(new Integer(score).toString());
        mRolledVal.setText("0");

        setButtons(false);
        hold = true;
    }

    /* Activates the buttons according to whether it's the user's turn or not */
    private void setButtons(boolean b)
    {
        mRoll.setEnabled(b);
        mHold.setEnabled(b);
    }

    /* The thread function that plays the actual game */
    private void playGame()
    {
        try {
            /* The host is already connected */
            if ( !turn )
                socket.connect();

            reader = socket.getInputStream();
            writer = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;

            if ( turn )
            {
                Log.e("", "Sending name then getting name...");
                writer.write(BluetoothService.getName().getBytes());

                bytes = reader.read(buffer);
                mHandler.obtainMessage(READ_NAME, bytes, -1, buffer).sendToTarget();
            }
            else
            {
                Log.e("", "Getting name then sending name...");
                bytes = reader.read(buffer);
                mHandler.obtainMessage(READ_NAME, bytes, -1, buffer).sendToTarget();

                writer.write(BluetoothService.getName().getBytes());
            }

            /* THE ACTUAL GAME IS PLAYED HERE */
            while ( score < 100 )
            {
                if ( !turn )
                {
                    buffer = new byte[1024];
                    bytes = reader.read(buffer);
                    mHandler.obtainMessage(READ_SCORE, bytes, -1, buffer).sendToTarget();
                    turn = true;
                }
                else if ( turn )
                {
                    hold = false;
                    while ( !hold ){Thread.sleep(1000);}
                    writer.write(new String(""+score).getBytes());
                    turn = false;
                }
            }

        } catch (Exception e) {
            Log.e("", e.getMessage());
        } finally {
            finish();
        }
    }

    protected void onDestroy()
    {
        String msg;
        if ( score >= 100 )
            msg = "You won!";
        else
            msg = "You lost!";
        Toast.makeText(this, "Game over! " + msg, Toast.LENGTH_SHORT).show();
        Log.e("", "End of game!");

        try {
            socket.close();
            Thread.sleep(1000);
        } catch (Exception e) {
            Log.e("", "Error ending game: " + e.getMessage());
        }
    }

    /* Handles messages sent from the opponent */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] readBuf;
            String readMessage;
            switch (msg.what) {
                case READ_NAME:
                    readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    readMessage = new String(readBuf, 0, msg.arg1);
                    mOpponentname.setText(readMessage);
                    break;
                case READ_SCORE:
                    readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    readMessage = new String(readBuf, 0, msg.arg1);
                    mOpponentscore.setText(readMessage);
                    setButtons(true);

                    if ( Integer.parseInt(readMessage) >= 100 )
                        endGame();
                    break;
            }
        }
    };

}

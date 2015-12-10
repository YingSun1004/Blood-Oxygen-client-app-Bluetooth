package se.arvidbodkth.laboration3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Created by arvidbodin on 07/12/15.
 */
public class BluetoothIOTask extends AsyncTask<Void, String, String> {

    private static final UUID STANDARD_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final byte[] FORMAT = {0x02, 0x70, 0x04, 0x02, 0x02, 0x00, 0x78, 0x03};
    private static final byte ACK = 0x06; // ACK from Nonin sensor

    private MainActivity activity;
    private BluetoothDevice noninDevice;
    private BluetoothAdapter adapter;
    private Context context;
    private InputStream is;
    private OutputStream os;
    private BluetoothSocket socket;

    public BluetoothIOTask(MainActivity activity, BluetoothDevice noninDevice, Context context) {
        this.context = context;
        this.activity = activity;
        this.noninDevice = noninDevice;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.socket = null;

    }


    @Override
    protected String doInBackground(Void... v) {
        String output = "";

        // an ongoing discovery will slow down the connection
        adapter.cancelDiscovery();
        socket = null;
        System.out.println(noninDevice.getName() + noninDevice);
        try {
            socket = noninDevice
                    .createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
            socket.connect();

            is = socket.getInputStream();
            os = socket.getOutputStream();

            os.write(FORMAT);
            os.flush();
            byte[] reply = new byte[1];
            is.read(reply);

            //If the replay was an ack read the next frame.
            if (reply[0] == ACK) {

                while (!isCancelled()) {
                    //Send request for format.
                    byte[] frame = new byte[5];
                    is.read(frame);

                    //Get the plath value
                    int pleth = unsignedByteToInt(frame[2]);

                    //The resulution is 10bit so we need 2 bits from one byte.
                    //If the bit 0 in the status byte is set to 1
                    //read the next frame.
                    if ((frame[1] & 0x01) == 1) {

                        //Get the 2 LSB from the 4th frame
                        byte mSB =  (byte)(frame[3] & 0x03);
                        //Get the next frame
                        is.read(frame);
                        //Get all the bits in the next byte.
                        byte lSB = (frame[3]);

                        //Convert the bits to ints.
                        int pulsMSB = unsignedByteToInt(mSB);
                        int pulsLSB = unsignedByteToInt(lSB);

                        int puls = pulsMSB*128 + pulsLSB;

                        output = "Puls: " + puls + ";" + pleth;
                        publishProgress(output);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
                is.close();
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return output;
    }


    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        try {
            socket.close();
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        activity.displayData(values[0]);

    }

    @Override
    protected void onPostExecute(String output) {
        super.onPostExecute(output);

    }

}
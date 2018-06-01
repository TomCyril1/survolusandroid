package com.led_on_off.led;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import static java.lang.Math.sqrt;
import static java.lang.Thread.sleep;


public class ledControl extends ActionBarActivity {

   // Button btnOn, btnOff, btnDis;
    ImageButton On, Off, Discnt, Abt;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public boolean activation_gps,activation_parachute,activation_anti_casse,activation_flash,en_chute;
  //  public capteur accelerometre;
    private SensorManager mSensorManager;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Appartient a l'activity

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device
       // accelerometre = new capteur();

        String txt = new String();

        mSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgets
        On = (ImageButton)findViewById(R.id.on);
        Off = (ImageButton)findViewById(R.id.off);
        Discnt = (ImageButton)findViewById(R.id.discnt);
        Abt = (ImageButton)findViewById(R.id.abt);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        On.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });


    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Erreur");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                activation_parachute=false;
                display("Mode Parachute Activation désactivation...");

                btSocket.getOutputStream().write("0".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                activation_parachute=true;
                display("Mode Parachute Activation...");

                btSocket.getOutputStream().write("1".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    public  void about(View v)
    {
        if(v.getId() == R.id.abt)
        {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connection...", "Attendez svp!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }


    //etat de l'application On resume/onPause
    @Override
    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);


    }


    //Récupération des données de positionnnement accélérometre
    private final SensorEventListener mSensorListener = new SensorEventListener()
    {

        public void onSensorChanged(SensorEvent se)
        {
            if(activation_parachute==true)
            {
                float x = se.values[0];
                float y = se.values[1];
                float z = se.values[2];
                Position(x, y , z);
            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    //Affichage des données de positonnement
    public void Position(float iX, float iY, float iZ)
    {
        //tz.setText(" "+iZ);
        //tx.setText(" "+iX);
        //ty.setText(" "+iY);

        //on détecte la chute en meme temps
        detect_chute(iX,iY,iZ);
    }

    // Determination de la chute ou pas
    public void detect_chute(float iX, float iY, float iZ)
    {
       // EditText inputField = (EditText) findViewById(R.id.status);
        double total = sqrt(iX*iX+iY*iY+iZ*iZ);

       // detect.setText(" Mesure : "+total);
        if (total < 3)
        {
            //on active le booleen chute a utiliser pour le gps ( enveras la position chaque x secondes)
            en_chute=true;

            // Verifie si l'anticasse est activée
            if (activation_anti_casse) {

                Vibrator vib=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(10000);
            }
            Toast.makeText(getApplicationContext(), "VOTRE TELEPHONE TOMBE !", Toast.LENGTH_LONG).show();

            //detect.setText(" Chute détectée !!");

            // Discution  arduino ici  pour lancer le signal de déploiement du parachute
            if (btSocket!=null)
            {
                try
                {
                    activation_parachute=true;
                    display("Déploiement du parachute ...");

                    btSocket.getOutputStream().write("0".toString().getBytes());
                    //activation_parachute=false;
                    sleep(4000);

                    btSocket.getOutputStream().write("1".toString().getBytes());
                }
                catch (IOException e)
                {e.printStackTrace();
                    msg("Erreur de déploiement");
                }
                catch (InterruptedException e) {

                }
            }
        }
        else
        {
            en_chute=false;
        }
    }

    public void display(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();

            }
        });
    }


    protected void onPause()
    {
        super.onPause();
        if(activation_parachute)
        {
            mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

}

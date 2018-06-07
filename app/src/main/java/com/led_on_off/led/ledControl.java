package com.led_on_off.led;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.lang.Math.sqrt;
import static java.lang.Thread.sleep;


public class ledControl extends AppCompatActivity implements LocationListener {
    LocationManager locationManager;
    Menu menu;
    TextView locationText;
    TextView longitest;
    TextView latitest;
static String macadr;
static String slatitude;
static String slongitude;
    // Button btnOn, btnOff, btnDis;
    ImageButton On, Off, Discnt, Abt;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public boolean activation_gps, activation_parachute, activation_anti_casse, activation_flash, en_chute;
    //  public capteur accelerometre;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
// variable id utilisateur
        Intent intent = getIntent();
        String resultat = "";
        if (intent != null) {

            resultat = intent.getStringExtra("resultat");
        }
        String url = "http://survolus.com:81/getnomprenom.php";
        new AsyncLogin().execute(url, "id", resultat);
        //call the widgets
        On = (ImageButton) findViewById(R.id.on);
        Off = (ImageButton) findViewById(R.id.off);
        Discnt = (ImageButton) findViewById(R.id.discnt);
        Abt = (ImageButton) findViewById(R.id.abt);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        On.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnLed();      //method to turn on
            }
        });

        Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffLed();   //method to turn off
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

        //  Envoi du signal vers BDD
        locationText = (TextView)findViewById(R.id.position);
        longitest = (TextView)findViewById(R.id.test);
        latitest = (TextView)findViewById(R.id.testlati);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);


    }
    getLocation();



    }
    //getlocation
    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, (LocationListener) this);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        locationText.setText("Latitude: " + location.getLatitude() + "\n Longitude: " + location.getLongitude());
          slatitude = String.valueOf(location.getLatitude());
          slongitude = String.valueOf(location.getLongitude());
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            locationText.setText(locationText.getText() + "\n"+addresses.get(0).getAddressLine(0)+", "+
                    addresses.get(0).getAddressLine(1)+", "+addresses.get(0).getAddressLine(2));
        }catch(Exception e)
        {

        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(ledControl.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
    @Override
    public void onProviderEnabled(String provider) {

    }
    ////////




    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Erreur");
            }
        }
        finish(); //return to the first layout

    }

    private void turnOffLed() {
        if (btSocket != null) {
            try {
                activation_parachute = false;
                display("Mode Parachute Activation désactivation...");

                btSocket.getOutputStream().write("0".toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void turnOnLed() {
        if (btSocket != null) {
            try {
                activation_parachute = true;
                display("Mode Parachute Activation...");

                btSocket.getOutputStream().write("1".toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void about(View v) {
        if (v.getId() == R.id.abt) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        this.menu = menu;
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
        if (id == R.id.logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // deconection de l'utilisateur
    private void logout() {
        Disconnect();
        Intent intent = new Intent(ledControl.this, LoginActivity.class);


        startActivity(intent);
        ledControl.this.finish();


    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ledControl.this, "Connection...", "Attendez svp!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection echoué. Désactivez puis réactivez votre bluetooth.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }


    //etat de l'application On resume/onPause
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);


    }


    //Récupération des données de positionnnement accélérometre
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent se) {
            if (activation_parachute == true) {
                float x = se.values[0];
                float y = se.values[1];
                float z = se.values[2];
                Position(x, y, z);
            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    //Affichage des données de positonnement
    public void Position(float iX, float iY, float iZ) {
        //tz.setText(" "+iZ);
        //tx.setText(" "+iX);
        //ty.setText(" "+iY);

        //on détecte la chute en meme temps
        detect_chute(iX, iY, iZ);
    }

    // Determination de la chute ou pas
    public void detect_chute(float iX, float iY, float iZ) {
        // EditText inputField = (EditText) findViewById(R.id.status);
        double total = sqrt(iX * iX + iY * iY + iZ * iZ);

        // detect.setText(" Mesure : "+total);
        if (total < 3) {
            //on active le booleen chute a utiliser pour le gps ( enveras la position chaque x secondes)
            en_chute = true;

            // Verifie si l'anticasse est activée
            if (activation_anti_casse) {

                Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(10000);
            }
            Toast.makeText(getApplicationContext(), "VOTRE TELEPHONE TOMBE !", Toast.LENGTH_LONG).show();


            //detect.setText(" Chute détectée !!");

            // Discution  arduino ici  pour lancer le signal de déploiement du parachute
            if (btSocket != null) {
                try {
                    activation_parachute = true;
                    display("Déploiement du parachute ...");

                    btSocket.getOutputStream().write("0".toString().getBytes());
                    //activation_parachute=false;
                    sleep(4000);

                    btSocket.getOutputStream().write("1".toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    msg("");
                } catch (InterruptedException e) {

                }
            }
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);


            }



        }
        //le telephone est tombé et  il envoi la loca
        if ( en_chute=true && total >3) {

            get_localisation();
            en_chute = false;
        }
    }

    public void get_localisation()
    {
        //  Envoi du signal vers BDD
        getLocation();
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String MAC = info.getMacAddress();
        String url = "http://survolus.com:81/chute.php";
        longitest.setText(slongitude);
        latitest.setText(macadr);
        //slatitude = "2222";
        // slongitude ="6444";
        macadr= MAC.toString();



        new AsyncModule().execute(url,"MAC",macadr,"latitude",slatitude,"longitude",slongitude);

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

    //////////////////////////  Recuperation du nom de l'utilisateur
    private class AsyncLogin extends AsyncTask<String, String, String>
    {
        public static final int CONNECTION_TIMEOUT=10000;
        public static final int READ_TIMEOUT=15000;


        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread


        }
        @Override
        protected String doInBackground(String... params) {
            try {

                // Enter URL address where your php file resides
                url = new URL(params[0]);

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter(params[1], params[2]);
                //.appendQueryParameter(params[2], params[3]);
                String query = builder.build().getEncodedQuery();

                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method
                    return(result.toString());




                }else{

                    return("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }


        }
        protected void onPostExecute(String result) {

            //this method will be running on UI thread



            // if(result.equalsIgnoreCase("true"))
            if(result != null)
            {
                TextView textView2 = (TextView) findViewById(R.id.Nom);
                textView2.setText("Bonjour "+ result);
                menu.findItem(R.id.action_settings).setTitle(result);



            } else if (result.equalsIgnoreCase("exception") || result.equalsIgnoreCase("unsuccessful")) {

                Toast.makeText(ledControl.this, "OOPs! Something went wrong. Connection Problem.", Toast.LENGTH_LONG).show();

            }
        }

    }


    ///////////////

}

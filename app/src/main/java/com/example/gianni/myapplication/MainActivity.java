package com.example.gianni.myapplication;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,NsdHelper.PeerFounded{

    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    Handler updateConversationHandler;

    /*
     *   A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
     *   Each Handler instance is associated with a single thread and that thread's message queue
     */

    ChatConnection mConnection;
    ListOfPeer mListOfPeer;

    ServerSocket serverSocket;

    String mIp;

    ServerSocket sk=null;//socket for register service

    TextView textbox;

    EditText logName;

    String mUsername;

    List<String> user;
    ArrayAdapter<String> adapter;
    ListView listOfUsers;

    public static final String TAG = "Activity";
//------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //I create the activity UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

     //   new ClientAsyncTask("192.168.1.2").execute();

        new FileServerAsyncTask(this,null).execute(); //This server is for accept incoming chat request

        mListOfPeer=new ListOfPeer();

        user = new ArrayList<String>();

        logName= (EditText) findViewById(R.id.LogeditText);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,user);

        listOfUsers=(ListView) findViewById(R.id.listView);
        assert listOfUsers != null;
        listOfUsers.setAdapter(adapter);

        textbox=(TextView) findViewById(R.id.textView2);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Sync "+mUsername+" with peers ", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if(mNsdHelper != null){
                    mNsdHelper.stopDiscovery(); //close old discovery
                    mNsdHelper.discoverServices(); //reopen discovery
                }

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        // Create an instance of the Login dialog fragment and show it


        final Dialog dialog = new Dialog( this );
        dialog.setTitle("Log");
        dialog.setContentView( R.layout.dialog_signin);

        dialog.setTitle( "Enter Username" );
        logName=(EditText) dialog.findViewById(R.id.LogeditText);

        /*Handle button login*/
        Button b=(Button) dialog.findViewById(R.id.Logbutton);
        b.setOnClickListener(new View.OnClickListener() {

            //Assign a listener to click event
            @Override
            public void onClick(View v) {
                if(logName.getText().toString().equals("")){

                    Toast.makeText(MainActivity.this, "The name inserted is not valid!", Toast.LENGTH_LONG).show();

                    //Do nothing because the username is wrong
                }else {
                    Toast.makeText(MainActivity.this, "Logged as: " + logName.getText() + " ", Toast.LENGTH_LONG).show();
                    mUsername = new String(logName.getText().toString());
                    dialog.dismiss();//this close the dialog frame
                }
            }
        });

        try {
            dialog.show();
            }catch(Exception e){
            //Handle error
        }


        /*
        *   Handle the WiFi part
        */

        /*
        * N.B. Context class is an Interface to global information about an application environment. This is an abstract class whose implementation is provided by the Android system.
        * It allows access to application-specific resources and classes, as well as up-calls for application-level operations
        * such as launching activities, broadcasting and receiving intents, etc.
        *
        * */
        /*
        * I have to check if wifi is on
        * */

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {

            /*Check is wifi is enabled*/
            Toast.makeText(MainActivity.this, "Activating WiFi..", Toast.LENGTH_LONG).show();

            //I activate the wifi
            wifiManager.setWifiEnabled(true);
        } else {
            /*WiFi is already active*/
            Toast.makeText(MainActivity.this, "WiFi active..", Toast.LENGTH_LONG).show();

        }


        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        textbox.append(Formatter.formatIpAddress(wifiInfo.getIpAddress()));


        //----------------------------------------------------
        // Create the NsdServiceInfo object, and populate it.

        mUpdateHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                String chatLine = msg.getData().getString("msg");
                addChatLine(chatLine);
            }
        };

        //mConnection = new ChatConnection(mUpdateHandler);


        try {
            if(sk==null)
                sk=new ServerSocket(0); // if sk != null a connection is established
        } catch (IOException e) {
            e.printStackTrace();
        }

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd(); //callback for register

      /*Register the service*/
        if (sk.getLocalPort() > -1)
        {

            mNsdHelper.registerService(sk.getLocalPort()); //try to register service

        }
        else
        {
            Log.e(TAG, "ServerSocket isn't bound.");
        }

        /*Discover the available services on the network*/
        mNsdHelper.discoverServices();
        mIp=Formatter.formatIpAddress(wifiInfo.getIpAddress());

        addChatLine("\n\n\nMy Ip: "+mIp);

        listOfUsers.setOnItemClickListener(new AdapterView.OnItemClickListener(){


            @Override
            public void onItemClick(AdapterView<?> adattatore, final View componente, int pos, long id){
                // recupero il titolo memorizzato nella riga tramite l'ArrayAdapter
                final String titoloriga = (String) adattatore.getItemAtPosition(pos);
                ClientAsyncTask cl=new ClientAsyncTask("192.168.1.2");
                Toast.makeText(MainActivity.this, "Ho cliccato sull'elemento"+pos+" con titolo" + titoloriga, Toast.LENGTH_LONG).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    cl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
                else
                    cl.execute((Void[])null);



            }

        });



        /*-------This part is for receiving client name from client----------*/
        //updateConversationHandler = new Handler();

        //this.serverThread = new Thread(new ServerThread());

        //this.serverThread.start();
    }

    public void addChatLine(String line)
    {
        textbox.append("\n" + line);
    }
//-------------------------------------------------------------------------


//-------------------------------------------------------------------------

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mNsdHelper != null)
        {
            mNsdHelper.discoverServices();
        }
    }


    @Override
    protected void onPause()
    {
        if (mNsdHelper != null)
        {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        if(mNsdHelper != null)
            mNsdHelper.tearDown();

        if(mConnection != null)
            mConnection.tearDown();

        super.onDestroy();
    }

//-------------------------------------------------------------------------


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

//---------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//----------------------------------------------------------------------------

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


//------------------------------------------------------------------------------

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

/*----------Called by NsdHelper when i'm connected to the peer-----------*/
    @Override
    public void onPeerFounded(final String info,final String host,final int port){


        //I must use this because this function is called by NsdHelper instance and this is not able to change Ui interface
        //for this reason i must launch this thread
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                Log.e(TAG,"Ho trovato un peer!!!!!!");
//              textbox.append(info);


                    if(!mListOfPeer.lookup(host) && !host.contains(mIp)) {

                        //If host.contains(mIp) means that this service run on my devices

                        Log.e(TAG, "New Peer trovato, lo aggiungo in hash table");
                        //Insert a new peer in the list if it's note present
                        mListOfPeer.insert(host, new Peer(host, "name"));
                        user.add(info);
                        adapter.notifyDataSetChanged(); //update adapter
                    }



                try {
                    sk.close();//I close the socket used for discovery
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //new ServerAsyncTask(host).execute(host);


            }
        });

    }

/*-----------------------*/






    /*-----------------------*/

    class ClientAsyncTask extends AsyncTask<Void,Void,Void> {


        private String address;

        public ClientAsyncTask( String address) {

            Log.d(TAG,"Init ClientAsyncTask Constructor");
            this.address=address;
        }

        @Override
        protected Void doInBackground(Void ... params) {

            Log.d(TAG,"Start communication with:"+this.address+": "+8888);
            //Client socket
            Socket socket = new Socket(); //Socket for transmitting the user-name on the server

            try {
                /**
                 * Create a client socket with the host,
                 * port, and timeout information.
                 */
                socket.bind(null);
                Log.d(TAG,"Start communication with:"+this.address+": "+8888);
                socket.connect((new InetSocketAddress(this.address, 8888)), 500);
                Log.d(TAG,"Start communication with:"+this.address+": "+8888);

                OutputStream outputStream = socket.getOutputStream();

//                outputStream.write(new byte[]{Byte.valueOf("Gianni")});//Send a name to server

                outputStream.close();

            }catch (FileNotFoundException e) {
                Log.d(TAG,e.getMessage());
                //catch logic
            } catch (IOException e) {
                //catch logic
                Log.d(TAG,e.getMessage());
            }

            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */

            finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            //catch logic
                        }
                    }
                }
            }


            return null;
        }


    }


        class FileServerAsyncTask extends AsyncTask {

        private Context context;
        private TextView statusText;

        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

            @Override
            protected Object doInBackground(Object[] params) {
                try {

                    /**
                     * Create a server socket and wait for client connections. This
                     * call blocks until a connection is accepted from a client
                     */
                    ServerSocket serverSocket = new ServerSocket(8888);
                    for (; ; ) {
                        Log.d(TAG, "Wait for communication");
                        Socket client = serverSocket.accept();
                        Log.d(TAG, "Accept communication from:");

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                new android.support.v7.app.AlertDialog.Builder(context)


                                        //I inflate the box of custom title
                                        .setTitle("Are you sure?")
                                        .setMessage("Accept new chat connection?")
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // continue with insert
                                                }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // do nothing
                                            }
                                        })


                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }

                        });



                    }        //Toast.makeText(MainActivity.this,"Connessione da parte di:"+client.getInetAddress().toString(),Toast.LENGTH_LONG).show();
                          }catch(IOException e){
                            Log.e(TAG, e.getMessage());
                            return null;

                }

            }
        }



}










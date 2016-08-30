package com.example.gianni.myapplication;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by gianni on 22/07/16.
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,NsdHelper.PeerFounded{

    NsdHelper mNsdHelper;

    ListOfPeer mListOfPeer;

    Context context=this;

    String mIp;

    ServerSocket sk=null;//socket for register service

    TextView textbox;

    EditText logName;

    String mUsername;

    EditText newPeerIpi;

    Dialog dialog1; //Dialog for add manually new Peer in the list

    List<String> user;
    List<String> addr;
    CustomListAdapter adapter;
    ListView listOfUsers;

    Key pubKey;

    Key privKey;

    byte[] cipherText;

    byte[] mID=new byte[20];



    public static final String TAG = "Activity";
//------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //I create the activity UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //creating public_private key pair and ID for the peer

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/P2P_key");
        dir.mkdirs();
        File pub_file = new File(dir, "pub.key"); //file which contain public key
        File pr_file = new File(dir, "pr.key"); //file which contain private key
        File myId = new File(dir,"id.bin"); //file which contain the ID of the peer


        if(!pub_file.isFile() && !pr_file.isFile() || !myId.isFile()) {

            /*In this case i have to generate a new public and private key pair because is the first run for the app*/

            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

                generator.initialize(1024);

                KeyPair pair = generator.generateKeyPair();


                pubKey = pair.getPublic();

                privKey = pair.getPrivate();

                    /* save the public key in a file */
                byte[] key = pubKey.getEncoded();
                FileOutputStream keyfos = new FileOutputStream(pub_file);
                keyfos.write(key);
                keyfos.close();

                    /* save the private key in a file */
                byte[] prkey = privKey.getEncoded();
                FileOutputStream keypr = new FileOutputStream(pr_file);
                keypr.write(prkey);
                keypr.close();

                    /*Save my ID*/
                SecureRandom random = new SecureRandom();
                random.nextBytes(mID);
                FileOutputStream midstream = new FileOutputStream(myId);
                midstream.write(mID);
                midstream.close();




            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            /*Public_Private Key set already generated, i have to load them from files*/

            try {
                byte[] key_pub = new byte[1024];
                FileInputStream inpub = new FileInputStream(pub_file);
                inpub.read(key_pub);
                inpub.close();

                byte[] key_pr = new byte[1024];
                FileInputStream inpr = new FileInputStream(pr_file);
                inpr.read(key_pr);
                inpr.close();

                KeyFactory kf = KeyFactory.getInstance("RSA"); // or "EC" or whatever
                privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(key_pr));
                pubKey = kf.generatePublic(new X509EncodedKeySpec(key_pub));


                //Fetch mID from the file
                FileInputStream midstream = new FileInputStream(myId);
                midstream.read(mID);
                midstream.close();

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }

        }

        new FileServerAsyncTask(this,null).execute(); //This server is for accept incoming chat request

        mListOfPeer=new SingletonListOfPeer().getSingleton();

        user = new ArrayList<String>();
        addr = new ArrayList<String>();


        logName= (EditText) findViewById(R.id.LogeditText);

       // adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,user);
        adapter= new CustomListAdapter(this,user,addr,null);

        listOfUsers=(ListView) findViewById(R.id.listView);
        assert listOfUsers != null;
        listOfUsers.setAdapter(adapter);

        textbox=(TextView) findViewById(R.id.textView2);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Adding new Peer..", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                //** Create an instance of the NewPeer insert dialog fragment and show it **//
                dialog1 = new Dialog(context);
                dialog1.setTitle("Add new Peer");
                dialog1.setContentView( R.layout.dialog_add_new_peer);

                dialog1.setTitle( "Enter Ip of Peer to insert in the list" );
                 newPeerIpi=(EditText) dialog1.findViewById(R.id.Ipaddr);

        /*Handle button insert*/
                Button b1=(Button) dialog1.findViewById(R.id.peerbutton);
                b1.setOnClickListener(new View.OnClickListener() {

                    //Assign a listener to click event
                    @Override
                    public void onClick(View v) {

                        Log.d(TAG,"I want insert new peer");
                        //Fetch the userName inserted
                        String newPeerIp=new String(newPeerIpi.getText().toString());
                        onPeerFounded("Unknown User","/"+newPeerIp,8888);
                        dialog1.dismiss();//this close the dialog frame
                    }
                });

                try {
                    Log.d(TAG,"i want show a dialog");
                    dialog1.show();
                }catch(Exception e){
                    //Handle error
                    Log.e(TAG,"Dialog Error");
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



        //** Create an instance of the Login dialog fragment and show it **//
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

                //Fetch the userName inserted
                String usrName=new String(logName.getText().toString());

                if(usrName.equals("") || usrName.contains("(") || usrName.contains(")")){

                    Toast.makeText(MainActivity.this, "The name inserted is not valid!", Toast.LENGTH_LONG).show();
                    //Do nothing because the username is wrong

                }else {

                    Toast.makeText(MainActivity.this, "Logged as: " + logName.getText() + " ", Toast.LENGTH_LONG).show();
                   addChatLine(new String(String.valueOf(logName.getText())));
                    mUsername = new String(String.valueOf(logName.getText()));

                    //----------------------------------------------------
                    // Create the NsdServiceInfo object, and populate it.

                    try {
                        if(sk==null)
                            /*sk is a socket used for discovery and register the service*/
                            sk=new ServerSocket(0); // if sk != null a connection is established
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mNsdHelper = new NsdHelper(MainActivity.this);
                    mNsdHelper.mServiceName=mUsername;
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

                    dialog.dismiss();//this close the dialog frame
                }
            }
        });

        try {
            dialog.show();
            }catch(Exception e){
            //Handle error
        }


        /**
        *   Handle the WiFi part
        **/

        /**
        * N.B. Context class is an Interface to global information about an application environment. This is an abstract class whose implementation is provided by the Android system.
        * It allows access to application-specific resources and classes, as well as up-calls for application-level operations
        * such as launching activities, broadcasting and receiving intents, etc.
        *
        * */

        /**
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

        //textbox.append(Formatter.formatIpAddress(wifiInfo.getIpAddress()));

        mIp=Formatter.formatIpAddress(wifiInfo.getIpAddress());


        addChatLine("My Ip: "+mIp);

        listOfUsers.setOnItemClickListener(new AdapterView.OnItemClickListener(){


            @Override
            public void onItemClick(AdapterView<?> adattatore, final View componente, int pos, long id){

                // recupero il titolo memorizzato nella riga tramite l'ArrayAdapter
                final String titoloriga = (String) adattatore.getItemAtPosition(pos);

                ClientAsyncTask cl=new ClientAsyncTask(addr.get(pos).substring(1)); //I start from addr.get(pos)[1] in order to remove '/'
                Toast.makeText(MainActivity.this, "Chat request to "+ user.get(pos), Toast.LENGTH_LONG).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    //For build newest than HONEYCOMB i need to us this command to launch AsyncTask
                    cl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
                }
                else {
                    cl.execute((Void[])null);
                }



            }

        });



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
        if (id == R.id.expanded_menu) {
            System.exit(0);
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

                    if(!mListOfPeer.lookup(host) && !host.contains(mIp)) {

                        //If host.contains(mIp) means that this service run on my devices
                        Log.e(TAG, "New Peer trovato, lo aggiungo in hash table");

                        //Insert a new peer in the list if it's note present
                        mListOfPeer.insert(host, new Peer(host, "name"));
                        user.add(info);
                        addr.add(host);
                        adapter.notifyDataSetChanged(); //update adapter
                    }


                try {
                    sk.close();//I close the socket used for discovery
                } catch (IOException e) {
                    e.printStackTrace();
                }



            }
        });

    }

/*-----------------------*/




/*--------This thread is for sending connection request to the other peer-----------*/

    class ClientAsyncTask extends AsyncTask<Void,Void,Void> {


        private String address;

        public ClientAsyncTask( String address) {

            Log.d(TAG,"Init ClientAsyncTask Constructor");
            this.address=address;
        }

        @Override
        protected Void doInBackground(Void ... params) {

            Log.d(TAG,"Start communication with:"+this.address+": "+8888); //I use the 8888 port to start handshake

            //Client socket
            Socket socket = new Socket(); //Socket for transmitting the user-name on the server

            try {

                /**
                 * Create a client socket with the host,
                 * port, and timeout information.
                 */

                socket.bind(null);
                Log.d(TAG,"Start communication with:"+this.address+": "+8888);
                socket.connect((new InetSocketAddress(this.address, 8888)), 5000);
                Log.d(TAG,"Start communication with:"+this.address+": "+8888);


                /**
                 * Create an input stream for the response of the other peer
                 *
                 * - 'y' ==> the peer accept the connection and then i can start the hanshake
                 *
                 * - 'n' ==> the peer refuse the connection
                 *
                 * **/
                InputStream in=socket.getInputStream();
                final byte [] b=new byte[1];
                in.read(b);


                /*The other peer accept the connection request*/
                if((char) b[0]=='y') {

                    /*I'm A the initiator of session*/
                    /*Parameter for th key exchange protocol*/
                    byte A[]=mID;
                    byte B[]=new byte[20];
                    byte Na[]=new byte[20];
                    byte Na1[]=new byte[20];
                    byte Nb[];
                    byte Nb1[];

                    SecureRandom random = new SecureRandom();
                   //s random.nextBytes(A);
                    random.nextBytes(Na);
                    random.nextBytes(Na1);
                    PublicKey Kb;
                    byte KbByte[]=new byte[1024];

                    /*The other peer say "ok i accept the connection: give me your public key" , then i have to send my pubKey*/
                    DataOutputStream sendPublicKey= new DataOutputStream(socket.getOutputStream());


                    //create M1: A-->B:  A,Na
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                    outputStream.write( A );
                    outputStream.write( Na );
                    byte M1[]=outputStream.toByteArray();
                    sendPublicKey.write(M1);
                    Log.d(TAG,"Send M1: <"+ Arrays.copyOfRange(M1,0,19)+"-----"+Arrays.copyOfRange(M1,20,M1.length));


                    //wait for Message M2:  B-->A: B,Kb,{A,Na,Nb}Kb^-1
                    byte M2[]=new byte[310];

                    DataInputStream MessageIn = new DataInputStream(socket.getInputStream());

                    Log.d(TAG,"Received M2 size: "+MessageIn.read(M2));

                    //*Put in kb variable the Kb received from B*//
                    Cipher cipher1 = Cipher.getInstance("RSA/NONE/NoPadding", "BC");
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    Log.e(TAG,"kb received : "+Arrays.toString(Arrays.copyOfRange(M2,20,182)));
                    Kb = kf.generatePublic(new X509EncodedKeySpec(Arrays.copyOfRange(M2,20,182)));//convert from byte stream to key format

                    //I Decrypt the Mtemp block ==>  {A,Na,Nb}Kb^-1 with Kb in order to obtain A,Na,Nb
                    cipher1.init(Cipher.DECRYPT_MODE,Kb);
                    byte Mtemp[]=cipher1.doFinal(Arrays.copyOfRange(M2,182,M2.length));
                    Nb=Arrays.copyOfRange(Mtemp,60,Mtemp.length);
                    B=Arrays.copyOfRange(M2,0,20); //Obtain the ID of B


                    Log.e(TAG,"Mtempcripted: "+Arrays.toString(Arrays.copyOfRange(M2,182,M2.length)));
                    Log.e(TAG,"A received : "+Arrays.toString(Arrays.copyOfRange(Mtemp,0,20)));
                    Log.e(TAG,"My A : "+Arrays.toString(A));


                    /*I control A id received (if is equal to my A i)*/
                    if(Arrays.equals(Arrays.copyOfRange(Mtemp,0,20),A)){ //this function compare the 2 arrays

                        Log.e(TAG,"A is equal ok!!!!!!");

                    }else {

                        Log.e(TAG,"A is different error!!!");
                        Runnable r = new AlertWrongHandshakeProtocol();
                        Thread t1=new Thread(r);
                        t1.start();

                    }

                    /*I control Na received*/
                    if(Arrays.equals(Arrays.copyOfRange(Mtemp,20,40),Na)){ //this function compare the 2 arrays

                        Log.e(TAG,"Na is equal ok!!!!!!");

                    }else {

                        Log.e(TAG,"Na is different error!!!");
                        Runnable r = new AlertWrongHandshakeProtocol();
                        Thread t1=new Thread(r);
                        t1.start();

                    }
                    outputStream.reset();


                    //*I create M3: A-->B: A,{A,Ka,Nb}_Ka^(-1),{A,Ka,Na'}_Kb *//

                    /*I made {A,Ka,Na'}_Kb ==> M3_1 */
                    outputStream.write(A);
                    outputStream.write(pubKey.getEncoded());
                    outputStream.write(Na1);
                    cipher1.init(Cipher.ENCRYPT_MODE,Kb);
                    byte M3_1[]=outputStream.toByteArray();
                    /*I have to split the packet because is bigger than 128 (which is the key size that i use to encrypt) */
                    byte M3_1_1[]=cipher1.doFinal(Arrays.copyOfRange(M3_1,0,128));
                    byte M3_1_2[]=cipher1.doFinal(Arrays.copyOfRange(M3_1,128,M3_1.length));
                    Log.e(TAG,"Size of {A,Ka,Na'}_Kb : "+M3_1.length);
                    outputStream.reset();




                    /* I made {A,Ka,Nb}_Ka^(-1) ==> M3_2 */
                    outputStream.write(A);
                    outputStream.write(pubKey.getEncoded());
                    outputStream.write(Nb);
                    cipher1.init(Cipher.ENCRYPT_MODE,privKey);
                    byte M3_2[]=outputStream.toByteArray();
                    /*I have to split the packet because is bigger than 128 (which is the key size that i use to encrypt) */
                    byte M3_2_1[]=cipher1.doFinal(Arrays.copyOfRange(M3_2,0,128));
                    byte M3_2_2[]=cipher1.doFinal(Arrays.copyOfRange(M3_2,128,M3_2.length));

                    Log.e(TAG,"Size of {A,Ka,Nb}_Ka^-1 : "+M3_1.length);
                    outputStream.reset();

                    /*I made M3*/
                    outputStream.write(A);
                    outputStream.write(M3_2_1);
                    outputStream.write(M3_2_2);
                    outputStream.write(M3_1_1);
                    outputStream.write(M3_1_2);
                    byte M3[]=outputStream.toByteArray();
                    sendPublicKey.write(M3);
                    Log.e(TAG,"Message M3 size :"+M3.length);



                    /*Receive M4*/
                    outputStream.reset();
                    byte M4[]=new byte[1024];
                    int M4_size=MessageIn.read(M4);
                    Log.e(TAG,"M4 size : "+M4_size);

                    cipher1.init(Cipher.DECRYPT_MODE,privKey);
                    byte M4_1[]=cipher1.doFinal(Arrays.copyOfRange(M4,20,148));
                    outputStream.write(M4_1);


                     /*I control A id received (if is equal to my A i)*/
                    if(Arrays.equals(Arrays.copyOfRange(M4_1,0,20),A)){ //this function compare the 2 arrays

                        Log.e(TAG,"A is equal ok!!!!!!");

                    }else {

                        Log.e(TAG,"A is different error!!!");
                        Log.e(TAG,"A received : "+Arrays.toString(Arrays.copyOfRange(M4_1,0,20)));
                        Log.e(TAG,"My A : "+Arrays.toString(A));

                        Runnable r = new AlertWrongHandshakeProtocol();
                        Thread t1=new Thread(r);
                        t1.start();

                    }

                    SecretKeySpec Kab = new SecretKeySpec(Arrays.copyOfRange(M4_1,20,52), "AES");

                    /*I control Na1*/
                    if(Arrays.equals(Arrays.copyOfRange(M4_1,52,72),Na1)){ //this function compare the 2 arrays

                        Log.e(TAG,"Na' is equal ok!!!!!!");

                    }else {

                        Log.e(TAG,"Na' is different error!!!");
                        Log.e(TAG,"Na' received : "+Arrays.toString(Arrays.copyOfRange(M4_1,52,72)));
                        Log.e(TAG,"My Na' : "+Arrays.toString(Na1));

                        Runnable r = new AlertWrongHandshakeProtocol();
                        Thread t1=new Thread(r);
                        t1.start();
                        return null; //Error i close the protocol

                    }

                    Nb1=Arrays.copyOfRange(M4_1,72,92);

                    /*I make M5*/

                    /*Perform Nb'-1*/
                    Nb1[Nb1.length-1]-=1;
                    Cipher cipherAES = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                    cipherAES.init(Cipher.ENCRYPT_MODE, Kab);
                    byte M5[]=cipherAES.doFinal(Nb1);
                    sendPublicKey.write(M5);

                    //----------------------



                    //Update the peer in ther list ==>insert the session key used
                    final String addrPeer=socket.getInetAddress().getHostAddress();
                    if(!mListOfPeer.lookup(addrPeer)){

                        Log.e(TAG,"Peer not founded in the peer list ERROR!!");

                        mListOfPeer.insert(addrPeer,new Peer(addrPeer,""));
                        mListOfPeer.getPeer(addrPeer).setSessionKey(Kab);
                    }else{
                        mListOfPeer.getPeer(addrPeer).setSessionKey(Kab);
                    }

                    mListOfPeer.i=1;
                    Log.e(TAG,"Inserted "+addrPeer+" in hash table");


                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            new android.support.v7.app.AlertDialog.Builder(MainActivity.this)


                                    //I inflate the box of custom title
                                    .setTitle("OK chat request accepted!!")
                                    .setMessage("Key exchange protocol succeed!")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int which) {

                                                /* continue with CONNECTION ==> I have to launch a new dialog activity*/

                                                Intent myIntent = new Intent(MainActivity.this, ChatActivity.class); //Optional parameters
                                                myIntent.putExtra("com.example.gianni.myapplication", address); //Optional parameters
                                                MainActivity.this.startActivity(myIntent);

                                            }
                                    })

                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();

                        }

                    });


                }else{

                    /** int this case i've received 'n'
                     *  i do nothing: connection aborted
                     **/

                    //i launch an alert
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new android.support.v7.app.AlertDialog.Builder(MainActivity.this)


                                    //I inflate the box of custom title
                                    .setTitle("No!!")
                                    .setMessage("Connection refused by other peer")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            // return to the MainActivity


                                        }
                                    })

                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();

                        }

                    });
                }

            }catch (FileNotFoundException e) {
                Log.d(TAG,e.getMessage());
                //catch logic
            } catch (IOException e) {
                //catch logic
                Log.d(TAG,e.getMessage());
            }catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } finally {

                /**
                 * Clean up any open sockets when done
                 * transferring or if an exception occurred.
                 */

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
                    final ServerSocket serverSocket = new ServerSocket(8888);

                    for (; ; ) {

                        Log.d(TAG, "Wait for communication");

                        final Socket client = serverSocket.accept();

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
                                                try {

                                                    /*I'm B peer*/
                                                    /*Parameter for th key exchange protocol*/
                                                    byte A[]=mID;
                                                    byte B[]=new byte[20];
                                                    byte Na[];
                                                    byte Na1[];
                                                    byte Nb[]=new byte[20];
                                                    byte Nb1[]=new byte[20];

                                                    SecureRandom random = new SecureRandom();
                                                    //random.nextBytes(B);
                                                    random.nextBytes(Nb);
                                                    random.nextBytes(Nb1);
                                                    PublicKey Ka;

                                                    /*Permit  android.os.NetworkOnMainThreadException*/
                                                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                                    StrictMode.setThreadPolicy(policy);


                                                    /*Send that i want accept the communication*/
                                                    OutputStream os=client.getOutputStream();
                                                    os.write((byte) 'y');
                                                    os.flush();

                                                    /*I wait for a public Key of the peer*/
                                                    DataInputStream pubKeyPeerStream=new DataInputStream(client.getInputStream());

                                                    /*Wait for M1: A-->B: A,Na*/
                                                    byte M1[]=new byte[40];
                                                    pubKeyPeerStream.read(M1);
                                                    A=Arrays.copyOfRange(M1,0,20);
                                                    Na=Arrays.copyOfRange(M1,20,M1.length);
                                                    Log.d(TAG,"Message M1 received: <"+A+"------"+Na);

                                                    /* Create message M2: B-->A:  B,Kb,{A,Na,Nb}Kb^-1 */
                                                    byte Mtemp[];
                                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

                                                    outputStream.write( A );
                                                    outputStream.write( Na );
                                                    outputStream.write( Nb );
                                                    Mtemp=outputStream.toByteArray();
                                                    outputStream.close();
                                                    Cipher cipher1 = Cipher.getInstance("RSA/NONE/NoPadding", "BC"); //is important use this cipher to encrypt by means of private key
                                                    cipher1.init(Cipher.ENCRYPT_MODE, privKey);
                                                    byte cipherMtemp[] = cipher1.doFinal(Mtemp); //This is the sessionKey ecrypted
                                                    Log.e(TAG,"Mtemp :"+Arrays.toString(Mtemp));

                                                    ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream( );

                                                    Log.e(TAG,"Kb PUBLIC KEY"+Arrays.toString(pubKey.getEncoded()));
                                                    outputStream1.write( B );
                                                    outputStream1.write( pubKey.getEncoded() );
                                                    outputStream1.write(cipherMtemp);

                                                    byte M2[]=outputStream1.toByteArray();
                                                    outputStream1.close();
                                                    Log.e(TAG,"Message m2: "+Arrays.toString(M2));

                                                    //Send M2
                                                    Log.d(TAG,"Send M2 size:"+M2.length);
                                                    DataOutputStream MessageOut=new DataOutputStream(client.getOutputStream());
                                                    MessageOut.write(M2);
                                                    MessageOut.flush();
                                                    //MessageOut.close();

                                                    /*----Wait for M3------*/
                                                    byte M3[]=new byte[1024];
                                                    int M3Size=pubKeyPeerStream.read(M3);
                                                    Log.e(TAG,"M3 message received size: "+M3Size);

                                                    /*Recreate M3*/

                                                    //Recreate M3_1
                                                    outputStream.reset();
                                                    cipher1.init(Cipher.DECRYPT_MODE,privKey);
                                                    //M3_1 is splitted in 2 part oh 128 each
                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M3,276,276+128)));
                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M3,276+128,M3Size)));
                                                    byte M3_1[]=outputStream.toByteArray();

                                                    /*Obtain Ka*/
                                                    KeyFactory kf = KeyFactory.getInstance("RSA");
                                                    Ka = kf.generatePublic(new X509EncodedKeySpec(Arrays.copyOfRange(M3_1,20,182)));//convert from byte stream to key format


                                                    Log.e(TAG,"My A :"+Arrays.toString(A));

                                                    Log.e(TAG,"A received:"+Arrays.toString(Arrays.copyOfRange(M3_1,0,20)));
                                                    Na1=Arrays.copyOfRange(M3_1,182,M3_1.length);

                                                    //Recreate M3_2
                                                    outputStream.reset();
                                                    cipher1.init(Cipher.DECRYPT_MODE,Ka);outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M3,276,276+128)));
                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M3,20,128+20)));
                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M3,128+20,256+20)));
                                                    byte M3_2[]=outputStream.toByteArray();

                                                    //Control Nb
                                                    if(Arrays.equals(Arrays.copyOfRange(M3_2,182,202),Nb)){ //this function compare the 2 arrays

                                                        Log.e(TAG,"Nb is equal ok!!!!!!");

                                                    }else {

                                                        Log.e(TAG,"Nb is different error!!!");
                                                        Log.e(TAG,"My Nb: "+Arrays.toString(Nb));

                                                        Log.e(TAG,"Nb received: "+Arrays.toString(Arrays.copyOfRange(M3_2,182,202)));
                                                        Runnable r = new AlertWrongHandshakeProtocol();
                                                        Thread t1=new Thread(r);
                                                        t1.start();

                                                    }

                                                    /*Create Message M4*/

                                                    outputStream.reset();
                                                    /*Generating session Key with AES*/
                                                    KeyGenerator KabGen = KeyGenerator.getInstance("AES");
                                                    KabGen.init(256);
                                                    Key Kab = KabGen.generateKey();
                                                    outputStream.write(A);
                                                    outputStream.write(Kab.getEncoded());
                                                    outputStream.write(Na1);
                                                    outputStream.write(Nb1);
                                                    byte M4_1[]=outputStream.toByteArray();
                                                    Log.e(TAG,"M4_1 : "+Arrays.toString(M4_1));
                                                    outputStream.reset();
                                                    cipher1.init(Cipher.ENCRYPT_MODE,Ka);
                                                    outputStream.write(B);
                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M4_1,0,M4_1.length)));
//                                                    outputStream.write(cipher1.doFinal(Arrays.copyOfRange(M4_1,128,M4_1.length)));
                                                    byte M4[]=outputStream.toByteArray();
                                                    MessageOut.write(M4);

                                                    Log.e(TAG,"Size of Kab: "+Kab.getEncoded().length);

                                                    Log.e(TAG,"Size of M4: "+M4.length);


                                                    /*Receive and control M5*/
                                                    byte M5[]=new byte[1024];

                                                    int M5Size=pubKeyPeerStream.read(M5);
                                                    Nb1[Nb1.length-1]-=1;
                                                    Cipher cipherAES = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                                                    cipherAES.init(Cipher.DECRYPT_MODE, Kab);
                                                    byte M5_1[]=cipherAES.doFinal(Arrays.copyOfRange(M5,0,M5Size));

                                                    //Control Nb'-1
                                                    if(Arrays.equals(M5_1,Nb1)){ //this function compare the 2 arrays

                                                        Log.e(TAG,"Nb'-1 is equal ok!!!!!!");

                                                    }else {

                                                        Log.e(TAG,"Nb'-1 is different error!!!");
                                                        Log.e(TAG,"My Nb'-1: "+Arrays.toString(Nb1));

                                                        Log.e(TAG,"Nb'-1 received: "+M5_1);
                                                        Runnable r = new AlertWrongHandshakeProtocol();
                                                        Thread t1=new Thread(r);
                                                        t1.start();

                                                        return;  //I ignore this request becouse the protocol fails and jump to the next request
                                                    }

                                                    //--------------------------

                                                    //Update the peer in ther list ==>insert the session key used
                                                    String addrPeer=client.getInetAddress().getHostAddress();
                                                    if(!mListOfPeer.lookup(addrPeer)){

                                                        Log.e(TAG,"Peer not founded in the peer list ERROR!!");

                                                        mListOfPeer.insert(addrPeer,new Peer(addrPeer,""));
                                                        mListOfPeer.getPeer(addrPeer).setSessionKey(Kab);
                                                    }else{
                                                        mListOfPeer.getPeer(addrPeer).setSessionKey(Kab);
                                                    }


                                                    os.close();

                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } catch (InvalidKeySpecException e) {
                                                    e.printStackTrace();
                                                } catch (NoSuchAlgorithmException e) {
                                                    e.printStackTrace();
                                                } catch (BadPaddingException e) {
                                                    e.printStackTrace();
                                                } catch (IllegalBlockSizeException e) {
                                                    e.printStackTrace();
                                                } catch (NoSuchPaddingException e) {
                                                    e.printStackTrace();
                                                } catch (InvalidKeyException e) {
                                                    e.printStackTrace();
                                                } catch (NoSuchProviderException e) {
                                                    e.printStackTrace();
                                                }

                                                /*I launch a new activity for the chat with secure channel created*/

                                                Intent myIntent = new Intent(MainActivity.this, ChatActivity.class);
                                                MainActivity.this.startActivity(myIntent);
                                            }
                                        })

                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // send that i don't want connect with him
                                                try {

                                                    /*I just send to the peer that i don't wont init a chat with them*/
                                                    OutputStream os=client.getOutputStream();
                                                    os.write((byte) 'n');
                                                    os.flush();
                                                    os.close();

                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        })


                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }

                        });

                    }


                }catch(IOException e){

                            Log.e(TAG, e.getMessage());
                            return null;
                }

            }
        }

    //This Runnable show an alert when i find an error in the protocol
    class AlertWrongHandshakeProtocol implements Runnable{


        @Override
        public void run() {
            runOnUiThread(
                    new Runnable() {

                @Override
                public void run() {
                    new android.support.v7.app.AlertDialog.Builder(context)


                            //I inflate the box of custom title
                            .setTitle("Are you sure?")
                            .setMessage("Accept new chat connection?")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }
            );
        }
    });
    }
    }
}








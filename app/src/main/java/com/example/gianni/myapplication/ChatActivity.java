package com.example.gianni.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by gianni on 22/07/16.
 */

public class ChatActivity extends AppCompatActivity {

    TextView textbox;
    ImageButton sendBtn;
    String TAG="ChatActivity";
    EditText mess;
    OutputStream out;
    String address;
    Socket sendSk;
    RelativeLayout ll;
    Context mActivity=this;
    int marg=10;
    ListOfPeer mListOfPeer;
    Key sessionKey;
    Cipher cipher;
    byte[] msg;
    IvParameterSpec ivparams;

    //this is the hardcoded iv used for the CBC Mode, must be equal for the 2 devices
    public static final byte[] iv= { 65, 1, 2, 23, 4, 5, 6, 7, 32, 21, 10, 11, 12, 13, 84, 45 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mListOfPeer=new SingletonListOfPeer().getSingleton();


        try {

            cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

           /*Permit  android.os.NetworkOnMainThreadException*/
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ll = (RelativeLayout) findViewById(R.id.rel);

        Intent intent = getIntent();
        address = intent.getStringExtra("com.example.gianni.myapplication"); //if it's a string you stored




        if(address!=null) {
            //I have to initiate the new connection
            sendSk = new Socket();
            try {
                sendSk.bind(null);
                sendSk.connect((new InetSocketAddress(address, 5555)), 5000);
                Log.d(TAG, "Peer connected with" + sendSk.getInetAddress().getHostAddress());
                out = sendSk.getOutputStream();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        textbox=(TextView) findViewById(R.id.textView3);

        sendBtn=(ImageButton) findViewById(R.id.imageButton);

        mess= (EditText) findViewById(R.id.editText);

        textbox.append("\n"+address);

        RecvAsyncTask rec=new RecvAsyncTask(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //For build newest than HONEYCOMB i need to us this command to launch AsyncTask
            rec.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
        }
        else {
            rec.execute((Void[])null);
        }

        sendBtn.setOnClickListener(new View.OnClickListener() {

            //Assign a listener to click event
            @Override
            public void onClick(View v) {

                String s=mess.getText().toString();

                TextView tv = new TextView(mActivity);
                //tv.setText(s);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                //params.addRule(RelativeLayout., RelativeLayout.TRUE);
                tv.setLayoutParams(params);
                tv.setBackgroundResource(R.drawable.mymsg);
                tv.setWidth(s.length()*50);
                tv.setHeight(100);
                tv.setTextColor(Color.WHITE);
                tv.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Large);
                params.rightMargin = 70;
                params.topMargin=marg;
                marg+=150;


                byte [] plaintext=new byte[147];
                byte [] ciphero=new byte[160];

                try {
                    cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
/*
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                    outputStream.write((byte) s.length() );
                    outputStream.write( s.getBytes("utf-8") );
*/
                    byte []p=s.getBytes(); //The 1° byte is the number of char, from the 2° byte start the message

                    for(int i=0;i<s.getBytes().length;i++){
                        plaintext[i]=p[i];
                    }

                    Log.e(TAG,"Size of myMsg. "+plaintext.length);

                    ciphero=cipher.doFinal(plaintext);

                    Log.e(TAG,"Size of CryptmyMsg. "+plaintext.length);

                    textbox.append(new String(ciphero,"utf-8")+"\n");
                    out.write(ciphero,0,160);
                    out.flush();
                    Log.d(TAG,"Send message to other peer");
                } catch (IOException e) {
                    e.printStackTrace();
                }catch ( NullPointerException e){
                    Log.e(TAG," java.lang.NullPointerException");
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }

                //////
                try {
                    cipher.init(Cipher.DECRYPT_MODE, sessionKey);
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
                try {
                    tv.setText(new String(cipher.doFinal(ciphero),"UTF-8"));
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                ll.addView(tv, params);

            }
        });


    }

    class RecvAsyncTask extends AsyncTask

    {

        private Context context;

        public RecvAsyncTask(Context context) {
        this.context = context;
    }

        @Override
        protected Object doInBackground(Object[] params){

            Socket client;

            try {

                if (address == null) {
                    final ServerSocket serverSocket = new ServerSocket(5555);

                    client = serverSocket.accept();

                    Log.d(TAG, "Accepted connection from " + client.getInetAddress().getHostAddress());
                    out = client.getOutputStream();

                } else client = sendSk;


                Log.e(TAG,"Search "+client.getInetAddress().getHostAddress()+" in the hash table");
                //Take the session key from hash table
                sessionKey=mListOfPeer.getPeer(client.getInetAddress().getHostAddress()).getSessionKey();



                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textbox.append(new String(String.valueOf(sessionKey)));
                    }});




                cipher.init(Cipher.DECRYPT_MODE, sessionKey);

                 msg = new byte[160];



                InputStream inMsg = client.getInputStream();

                for (; ; ){

                    try {

                        inMsg.read(msg,0,160);
                        //Log.d(TAG, "Receive message " + new String(msg,"utf-8"));

                        Log.e(TAG, "Size of recvMsg: "+msg.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {


                            textbox.append(new String(msg)+"\n");

                            byte[] plainTextMsg= new byte[10];

                            try {
                                cipher.init(Cipher.DECRYPT_MODE, sessionKey);
                                plainTextMsg=cipher.doFinal(msg);

                            } catch (IllegalBlockSizeException e) {
                                e.printStackTrace();
                            } catch (BadPaddingException e) {
                                e.printStackTrace();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            }

                            // Add text
                            TextView tv = new TextView(mActivity);
                            try {
                                tv.setText(new String(plainTextMsg,"utf-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                            tv.setLayoutParams(params);
                            tv.setBackgroundResource(R.drawable.sendmsg);
                            tv.setWidth(new String(msg).length()*50);
                            tv.setHeight(100);
                            tv.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Large);
                            params.leftMargin = 107;
                            params.topMargin=marg;
                            marg+=150;

                            ll.addView(tv, params);


                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

            return  null;
        }
    }

}

package com.example.gianni.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

           /*Permit  android.os.NetworkOnMainThreadException*/
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ll = (RelativeLayout) findViewById(R.id.rel);

        Intent intent = getIntent();
        address = intent.getStringExtra("com.example.gianni.myapplication"); //if it's a string you stored

        // Add text
        TextView tv = new TextView(this);
        tv.setText("Mu textttttttttttt");
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.leftMargin = 107;

        ll.addView(tv, params);



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
                tv.setText(s);
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
                marg+=100;

                ll.addView(tv, params);


                try {

                    out.write(s.getBytes());
                    out.flush();
                    Log.d(TAG,"Send message to other peer");
                } catch (IOException e) {
                    e.printStackTrace();
                }catch ( NullPointerException e){
                    Log.e(TAG," java.lang.NullPointerException");
                }

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
                final byte[] msg = new byte[100];

                InputStream inMsg = client.getInputStream();

                for (; ; ){

                    try {

                        inMsg.read(msg);
                        Log.d(TAG, "Receive message " + new String(msg));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            // Add text
                            TextView tv = new TextView(mActivity);
                            tv.setText(new String(msg));
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                            //params.addRule(RelativeLayout., RelativeLayout.TRUE);
                            tv.setLayoutParams(params);
                            tv.setBackgroundResource(R.drawable.sendmsg);
                            tv.setWidth(new String(msg).length()*50);
                            tv.setHeight(100);
                            tv.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Large);
                            params.leftMargin = 107;
                            params.topMargin=marg;
                            marg+=100;

                            ll.addView(tv, params);

                            //textbox.append(new String(msg));
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return  null;
        }
    }

}

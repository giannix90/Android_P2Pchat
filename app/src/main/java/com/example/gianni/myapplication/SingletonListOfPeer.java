package com.example.gianni.myapplication;

/**
 * Created by gianni on 10/08/16.
 */

import android.util.Log;

/**
 * With this class we ensure that only one instance of the ListoOfPeer is created
 * **/
public class SingletonListOfPeer {

        private static volatile ListOfPeer l;

        public ListOfPeer getSingleton(){

            ListOfPeer tmp=l;

            if(tmp==null){

                synchronized(this){ //Synchronized on SingletonListOfPeer.class

                    if(tmp==null){

                        l=tmp=new ListOfPeer(); //Lazy initialization
                        Log.e("Singleton","New instance");
                    }
                }

            }
            return tmp;
        }

}

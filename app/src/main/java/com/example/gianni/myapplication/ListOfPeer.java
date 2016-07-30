package com.example.gianni.myapplication;

import java.net.InetAddress;
import java.util.Hashtable;

/**
 * Created by gianni on 24/07/16.
 */
public class ListOfPeer {

    Hashtable<String,Peer> peerList;

    public ListOfPeer(){
        peerList=new Hashtable<String, Peer>(); //Allocate a new hashTable
    }

    public boolean lookup(String addr){

       return peerList.containsKey(addr);//Tests if the specified object is a key in this hashtable.
    }

    public void insert(String addr,Peer p){
        peerList.put(addr,p);
    }
}

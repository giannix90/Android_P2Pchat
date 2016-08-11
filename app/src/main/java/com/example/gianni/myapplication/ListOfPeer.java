package com.example.gianni.myapplication;

import java.util.Hashtable;

/**
 * Created by gianni on 24/07/16.
 */
public class ListOfPeer {

    private static Hashtable<String,Peer> peerList;
    int i;

    public ListOfPeer(){
        peerList=new Hashtable<String, Peer>(); //Allocate a new hashTable
    }

    public boolean lookup(String addr){

       return peerList.containsKey(addr);//Tests if the specified object is a key in this hashtable.
    }

    public static void insert(String addr,Peer p){

        peerList.put(addr,p);
    }

    public static Peer getPeer(String addr){

        return peerList.get(addr);
    }
}

package com.example.gianni.myapplication;

import java.net.Socket;
import java.security.Key;

/**
 * Created by gianni on 22/07/16.
 */
public class Peer {
    String address;
    String Name;
    int id;
    Socket sk; //if active then indicates the socket used to communicate with this specific peer
    Key publicKey;
    Key sessionKey;

    public Peer (String address,String Name){
        this.address=address;
        this.Name=Name;

    }

    public void setAddres(String address){
        this.address=address;
    }

    public void setName(String name){
        this.Name=name;
    }

    public void setId(int id){
        this.id=id;
    }

    public  void  setSk(Socket sk){
        this.sk=sk;
    }

    public void setPublicKey(Key k){
        this.publicKey=k;
    }

    public void setSessionKey(Key k){
        this.sessionKey=k;
    }

    public Key getPublicKey(){
        return this.publicKey;
    }

    public Key getSessionKey(){
        return  this.sessionKey;
    }
}

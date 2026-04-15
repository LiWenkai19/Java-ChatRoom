package org.example;

// Java implementation for multithreaded chat client
// Save file as Client.java

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client{
    final static int ServerPort = 1234;

    public static void main(String args[]) throws UnknownHostException, IOException{
        Scanner scn = new Scanner(System.in);
        //Enter username
        System.out.println("Please enter your name:");
        String username=scn.nextLine();
        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        dos.writeUTF(username);
        // sendMessage thread
        Thread sendMessage = new Thread(new Runnable(){
            @Override
            public void run(){
                while (true){
                    try{
                        // read the message to deliver.
                        String msg = scn.nextLine();
                        //write on the output stream
                        dos.writeUTF(msg);
                        //check if the user wants to end the chat
                        if(msg.equals("logout")){
                            dos.close();
                            dis.close();
                            s.close();
                            scn.close();
                            break;
                        }
                    }catch (IOException e) {
                        break;
                    }
                }
            }
        });

        // readMessage thread
        Thread readMessage = new Thread(new Runnable(){
            @Override
            public void run(){
                while (true){
                    try {
                        // read the message sent to this client
                        String msg = dis.readUTF();
                        System.out.println(msg);
                    }catch (IOException e) {
                        break;
                    }
                }
            }
        });
        sendMessage.start();
        readMessage.start();

        try{
            sendMessage.join();
            readMessage.join();
        }catch(InterruptedException e){
            //e.printStackTrace();
        }

        dos.close();
        dis.close();
        s.close();
        scn.close();
    }
}

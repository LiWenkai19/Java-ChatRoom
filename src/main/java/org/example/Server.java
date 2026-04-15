package org.example;

// Java implementation of Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import org.w3c.dom.html.HTMLImageElement;

import java.io.*;
import java.util.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

// Server class
public class Server{

    // Vector to store active clients
    static Vector<ClientHandler> ar = new Vector<>();
    static Vector<recordMessage> record=new Vector<>();
    // counter for clients
    static int i = 0;

    public static void main(String[] args) throws IOException{
        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);

        Socket s;

        // running infinite loop for getting
        // client request
        while (true){
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            //generate a random ID of 5 digits
            String id=generateId();

            System.out.println("Creating a new handler for this client...");

            String username=dis.readUTF();
            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s,username, id, dis, dos);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.add(mtch);

            // start the thread.
            t.start();

            // increment i for new client.
            // i is used for naming only, and can be replaced
            // by any naming scheme
            i++;

            // Broadcast the welcome message to all clients
            broadcastMessage("Welcome "+id+"-"+mtch.getName()+"!");

            // Display the existing username to the new client
            sendExistingUsers(mtch);

            // Send all recorded message to the new client
            sendRecordMessages(mtch);
        }
    }

    // Broadcast message to all clients
    public static void broadcastMessage(String message){
        for(ClientHandler client:ar){
            if(client.isloggedin){
                try{
                    client.dos.writeUTF(message);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    // Send the existing username to the new client
    public static void sendExistingUsers(ClientHandler newClient){
        if(ar.size()>=2){
            try{
                newClient.dos.writeUTF("Currently connected users:");
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        for(ClientHandler client:ar){
            if(client.isloggedin&&!client.equals(newClient)){
                try{
                    newClient.dos.writeUTF("- "+client.getId()+"-"+client.getName());
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    // Send recorded messages to the new client
    public static void sendRecordMessages(ClientHandler newClient){
        for(recordMessage rec:record){
            try{
                newClient.dos.writeUTF(rec.id+"-"+rec.name+": "+rec.message+" ("+rec.timestamp+")");
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    // Generate a random ID number for the new client
    private static String generateId(){
        Random random=new Random();
        String id;
        do{
            id=String.format("%05d",random.nextInt(100000));
        }while(isIdInUse(id));
        return id;
    }

    // Determine whether the generated ID is repeated
    private static boolean isIdInUse(String id){
        for(ClientHandler client:ar){
            if(client.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // Store the recorded messages into a txt file
    public static void storageMessage() {
        try(BufferedWriter writer=new BufferedWriter(new FileWriter("record_messages.txt"))){
            for(recordMessage rec:record){
                writer.write(rec.id+"-"+rec.name+": "+rec.message+" ("+rec.timestamp+")");
                writer.newLine();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}

// RecordMessage class
class recordMessage {
    String id;
    String name;
    String message;
    String timestamp;
    Vector<String> receiveId = new Vector<>();
    Vector<String> receiveName = new Vector<>();
    public recordMessage(String id, String name, String message,String timestamp, Vector<String> receiveId, Vector<String> receiveName){
        this.id = id;
        this.name = name;
        this.message = message;
        this.timestamp= timestamp;
        this.receiveId = receiveId;
        this.receiveName = receiveName;
    }
}


// ClientHandler class
class ClientHandler implements Runnable{
    Scanner scn = new Scanner(System.in);
    private String name;
    private String id;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;

    public ClientHandler(Socket s, String name, String id, DataInputStream dis, DataOutputStream dos){
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.id=id;
        this.s = s;
        this.isloggedin=true;
    }

    // Get the username
    public String getName(){
        return name;
    }
    // Get the ID
    public String getId(){
        return id;
    }

    @Override
    public void run(){
        String received;
        while (true){
            try{
                // receive the string
                received = dis.readUTF();
                System.out.println(received);
                String[] parts=received.split(":");

                // Logout if the client send "logout"
                if(received.equals("logout")){
                    this.isloggedin=false;
                    this.s.close();
                    break;
                }
                // Search the chat logs if the client send "#search"
                if(received.startsWith("#search")){
                    String sample=received.substring("#search ".length()).trim();
                    searchChatLogs(sample);
                }else if(parts[0].equals("[print-receiver]")){ // Print the receiver of specific message
                    int partSize=parts[0].length();
                    String partMessage=received.substring(partSize+1);
                    for(recordMessage recordMessage:Server.record){
                        if(recordMessage.message.equals(partMessage)){
                            for (int i=0;i<recordMessage.receiveId.size();i++){
                                dos.writeUTF(recordMessage.receiveId.get(i)+"-"+recordMessage.receiveName.get(i));
                            }
                        }
                    }
                }else{  // Send ordinary message
                    String formattedMessage=addTime(id,name,received);
                    broadcastMessage(formattedMessage);
                    String timestamp=addTime("","","").split(": ")[1].replace(" (","").replace(")","");
                    Vector<String> receiveId=new Vector<>();
                    Vector<String> receiveName=new Vector<>();
                    for(ClientHandler client:Server.ar){
                        if(client.isloggedin&&!client.equals(this)){
                            receiveId.add(client.getId());
                            receiveName.add(client.getName());
                        }
                    }
                    recordMessage recordMessage=new recordMessage(id,name,received,timestamp,receiveId,receiveName);
                    Server.record.add(recordMessage);
                    Server.storageMessage();
                }
            }catch(IOException e){
                break;
            }
        }

        // Remove the logout client from the list
        Server.ar.remove(this);
        String logoutMessage=addTimeLogout(id,name," has left the chat room.");
        broadcastMessage("Server: User "+logoutMessage);
        try{
            // closing resources
            this.dis.close();
            this.dos.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    // Broadcast the ordinary message to all clients
    private void broadcastMessage(String message){
        for(ClientHandler client:Server.ar){
            if(client.isloggedin&&!client.equals(this)){
                try{
                    client.dos.writeUTF(message);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    // Add time to the message
    private String addTime(String id, String name, String message){
        LocalDateTime now=LocalDateTime.now();
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp=now.format(formatter);
        return id+"-"+name+": "+message+" ("+timestamp+")";
    }

    // Add time to the logout message
    private String addTimeLogout(String id,String name,String message){
        LocalDateTime now=LocalDateTime.now();
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp=now.format(formatter);
        return id+"-"+name+message+" ("+timestamp+")";
    }

    // Search the chat logs and output the chat logs that match the keyword or username
    private void searchChatLogs(String sample){
        boolean found=false;
        for(recordMessage recordMessage:Server.record){
            if(recordMessage.message.contains(sample)||recordMessage.name.contains(sample)){
                try{
                    dos.writeUTF(recordMessage.id+"-"+recordMessage.name+": "+recordMessage.message+" ("+recordMessage.timestamp+")");
                    found=true;
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        if(!found){
            try{
                dos.writeUTF("No messages found for chat log: "+sample);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}


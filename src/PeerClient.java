import javax.crypto.SecretKey;
import java.io.File;
import java.rmi.Naming;
import java.util.*;
import java.io.FileInputStream;


public class PeerClient {
    String masterPORT;
    String masterIP;
    String serverPORT;
    String serverIP;
    String myURI;
    Master masterServer;

    HashMap<String, String> usersCredentials = new HashMap<>();

    PeerClient(){
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream("../resources/config.properties"));
            //Fetching each property value
            this.masterPORT = prop.getProperty("MASTER_PORT");
            this.masterIP = prop.getProperty("MASTER_IP");
            this.serverPORT = prop.getProperty("SERVER_PORT");
            this.serverIP = prop.getProperty("SERVER_IP");
            this.myURI = "rmi://" + this.serverIP+":"+this.serverPORT + "/peer";
            this.masterServer =
                    (Master)Naming.lookup("rmi://"+this.masterIP+":"+this.masterPORT+"/master");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    PeerClient(int clientID){
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream("./resources/benchmark.properties"));
            //Fetching each property value
            this.masterPORT = prop.getProperty("MASTER_PORT");
            this.masterIP = prop.getProperty("MASTER_IP");
            this.serverPORT = prop.getProperty("SERVER_PORT_"+clientID);
            this.serverIP = prop.getProperty("SERVER_IP_"+clientID);
            this.myURI = "rmi://" + this.serverIP+":"+this.serverPORT + "/peer";
            this.masterServer =
                    (Master)Naming.lookup("rmi://"+this.masterIP+":"+this.masterPORT+"/master");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void createFile(String fileName, String fileData){
        try{
            //fetch IP and PORT of random peer
            Map.Entry<Set<String>, SecretKey> response = masterServer.create(fileName, myURI);
            Set<String> peersURI = response.getKey();
            SecretKey key = response.getValue();
            if(peersURI == null) {
                System.out.println(fileName + " already exists");
                return;
            }
            // lookup method to find reference of remote object
            for(String peerURI : peersURI){
                FDS peerServer =
                        (FDS)Naming.lookup(peerURI);
                System.out.println(AES.encrypt(fileName, key));
                peerServer.create(AES.encrypt(fileName, key),
                                  AES.encrypt(fileData, key));
            }
            System.out.println("Successfully created " + fileName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public void readFile(String fileName){
        try{
            Map.Entry<String, SecretKey> response = masterServer.read(fileName, myURI);
            String message = response.getKey();
            SecretKey key = response.getValue();
            if(message.equals(fileName + " doesn't exist")){
                System.out.println(message);
                return;
            } else if(message.equals("The peer doesn't have permission to read")){
                System.out.println("You don't have permission to read");
                return;
            }

            // connect with server
            FDS peerServer =
                    (FDS)Naming.lookup(message);
            System.out.println("Encrypted file name");
            String fileData = peerServer.read(AES.encrypt(fileName, key));
            if(fileData==null){
                System.out.println("Failed to read " + fileName);
                return;
            }
            System.out.println("File Data : \n"+ AES.decrypt(fileData, key));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateFile(String fileName, String newData){
        try {
            Map.Entry<Map.Entry<String, SecretKey>, Set<String>> response = masterServer.update(fileName, myURI);
            String message = response.getKey().getKey();
            SecretKey key = response.getKey().getValue();
            Set<String> peersPath = response.getValue();
            if(message.equals(fileName + " doesn't exist")){
                System.out.println(response);
                return;
            } else if(message.equals("The peer doesn't have permission to write")){
                System.out.println("You don't have permission to write");
                return;
            }
            // connect with server
            for(String peer : peersPath){
                FDS peerServer =
                        (FDS)Naming.lookup(peer);
                String oldData = AES.decrypt(peerServer.read(AES.encrypt(fileName, key)), key);

                peerServer.write(AES.encrypt(fileName, key),
                        AES.encrypt(oldData+newData, key));
            }
            System.out.println("Successfully updated the " + fileName + " data");
        }
            catch(Exception e){
            System.out.println(e);
        }
    }
    public void createDirectory(String directoryName){
        try {
            Map.Entry<Set<String>, SecretKey> response = masterServer.create(directoryName, myURI);
            Set<String> peersURI = response.getKey();
            SecretKey key = response.getValue();
            if(peersURI == null) {
                System.out.println(directoryName + " already exists");
                return;
            }
            // lookup method to find reference of remote object
            for(String peerURI : peersURI){
                FDS peerServer =
                        (FDS)Naming.lookup(peerURI);
                peerServer.createDirectory(AES.encrypt(directoryName, key));
            }
            System.out.println("Successfully created " + directoryName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public void writeFile(String fileName, String data){
        try {
            Map.Entry<Map.Entry<String, SecretKey>, Set<String>> response = masterServer.update(fileName, myURI);
            String message = response.getKey().getKey();
            SecretKey key = response.getKey().getValue();
            Set<String> peers = response.getValue();
            if(message.equals(fileName + " doesn't exit")){
                System.out.println(response);
                return;
            } else if(message.equals("The peer doesn't have permission to write")){
                System.out.println("You don't have permission to write");
                return;
            }
            // connect with server
            for(String peer : peers){
                FDS peerServer =
                        (FDS)Naming.lookup(peer);
                peerServer.write(AES.encrypt(fileName, key),
                        AES.encrypt(data, key));
            }
            System.out.println("Successfully wrote to the " + fileName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public void delete(String fileName){
        try{
            String response = masterServer.delete(fileName, myURI);
            if(response.equals(fileName + " doesn't exit")){
                System.out.println(response);
                return;
            } else if(response.equals("The peer doesn't have permission to delete/restore")){
                System.out.println("You don't have permission to delete");
                return;
            }

//            // connect with server
//            FDS peerServer =
//                    (FDS)Naming.lookup(response);
//            peerServer.delete(fileName);
            System.out.println("successfully deleted "+ fileName);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void restore(String fileName){
        try{
            String response = masterServer.restore(fileName, myURI);
            System.out.println(response);
            if(response.equals(fileName + " already exist")){
                System.out.println(response);
                return;
            } else if(response.equals("The peer doesn't have permission to delete/restore")){
                System.out.println("You don't have permission to delete/restore");
                return;
            }
//            FDS peerServer =
//                    (FDS)Naming.lookup(response);
//            peerServer.restore(fileName);
            System.out.println(fileName + " Successfully Restored ");
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void delegatePermission(String fileName, String otherURI, String permission) {
        try{
            String response = masterServer.delegatePermission(fileName, myURI, otherURI, permission);
            System.out.println(response);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void validateUser(String username, String password){
        if(usersCredentials.containsKey(username)){
            if(!password.equals(usersCredentials.get(username))){
                System.out.println("invalid credentials: /n");
                getUserDetails();
            }
        }else{
            System.out.println("invalid credentials: /n");
            getUserDetails();
        }
    }

    public void getUserDetails(){
        try{
            Scanner sc = new Scanner(System.in);
            System.out.println("Please login: /n");
            System.out.println("username : ");
            String username = sc.nextLine();
            System.out.println("Password : ");
            String password = sc.nextLine();
            validateUser(username, password);
            System.out.println("/n");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadUsers(){
        try{
            File fileObj = new File("../resources/users.txt");
            Scanner reader = new Scanner(fileObj);
            String users[] = new String[2];
            int i=0;
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                users[i++] = data;
            }
            String usernames[] = users[0].split("=")[1].split(",");
            String passwords[] = users[1].split("=")[1].split(",");
            for(i=0;i<usernames.length;i++){
                usersCredentials.put(usernames[i], passwords[i]);
            }
            reader.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        String response;
        int userInput;
        Scanner sc = new Scanner(System.in);
        loadUsers();
        getUserDetails();
        try
        {
            System.out.println("Welcome to the Peer to Peer Distributed File System");
            loadUsers();
            while(true){
                System.out.println( "Please select one of the below options" + "\n" +
                        "1. Create file" + " " +
                        "2. Create Directory"+" "+
                        "3. Read file" + " " +
                        "4. Write file" + "\n" +
                        "5. Update file" + " " +
                        "6. Delete file" + " " +
                        "7. Restore file" + "\n" +
                        "8. Delegate permissions " +
                        "9. Help" + " " +
                        "10. Exit"
                );
                if(sc.hasNextInt())
                    userInput = Integer.parseInt(sc.nextLine());
                else {
                    sc.nextLine();
                    userInput = 0;
                }
                if(userInput == 1) {
                    System.out.println("Enter File name to be created - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter File data - ");
                    String fileData = sc.nextLine();
                    this.createFile(fileName, fileData);
                }
                else if (userInput == 2) {
                    System.out.println("Enter Directory name to be created - ");
                    String directoryName = sc.nextLine();
                    this.createDirectory(directoryName);
                } else if (userInput == 3){
                    System.out.println("Enter File name to read - ");
                    String fileName = sc.nextLine();
                    this.readFile(fileName);
                } else if (userInput == 4){
                    System.out.println("Enter File name to be updated - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter the file data - ");
                    String fileData = sc.nextLine();
                    this.writeFile(fileName, fileData);
                } else if (userInput == 5){
                    System.out.println("Enter File name to be updated - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter new data to be appended - ");
                    String fileData = sc.nextLine();
                    this.updateFile(fileName, fileData);
                } else if (userInput == 6){
                    System.out.println("Enter File name to delete - ");
                    String fileName = sc.nextLine();
                    this.delete(fileName);
                } else if (userInput == 7){
                    System.out.println("Enter File name to restore - ");
                    String fileName = sc.nextLine();
                    this.restore(fileName);
                } else if(userInput == 8){
                    System.out.println("Enter file name to delegate the permission");
                    String fileName = sc.nextLine();
                    System.out.println("Enter IP address and port ex:10.0.4.245:1099");
                    String uri = sc.nextLine();
                    uri = "rmi://" + uri +"/peer";
                    System.out.println("Enter permission i.e, read, write, delete");
                    String permission = sc.nextLine();
                    this.delegatePermission(fileName, uri, permission);
                } else if (userInput == 9){
                    this.displayHelp();
                } else if (userInput == 10){
                    System.out.println("Exiting out of file distributed system");
                    break;
                }
                else{
                    System.out.println("Invalid Choice, please enter correct choice");
                }
            };
        }
        catch(Exception ae)
        {
            System.out.println(ae);
        }
    }

    public static void main(String args[]) {
        new PeerClient().run();
    }

    public void displayHelp() {
        System.out.println(
                "1. Create - create a file with the given name and given data" + "\n" +
                "2. Read - read a file with the given name" + "\n" +
                "3. Write - Replace the contents of the file with new given data" + "\n" +
                "4. Update - Append new content to the existing contents of the file" + "\n" +
                "5. Delete - Delete the file" + "\n" +
                "6. Restore - Restore the file"
        );
    }
}

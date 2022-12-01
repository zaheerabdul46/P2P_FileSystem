import java.io.IOException;
import java.rmi.*;
import java.util.*;

public interface Master extends Remote{
    // Declaring the method prototype
    public boolean hasFile(String filename) throws RemoteException;
    public List<String> getPaths(String filename) throws RemoteException;
    public String getPath() throws IOException;
    public boolean registerPeer(String peerData) throws IOException;
    public String updateCache(String path, String filename) throws RemoteException;
}
/*
 * This class is used to for device management
 */
import java.util.*;

public class DeviceManager  {

   public Hashtable<String, Device> deviceList;
 
   public DeviceManager () {
      deviceList = new Hashtable<String, Device>();
   }

   void addDevice(Device device) {
      deviceList.put(device.name,device);
   }

   Device getDevice(String name) {
      Device device = null;
      device = deviceList.get(name);
      return device;
   }
}; 

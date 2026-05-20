package com.gluonapplication;


public class DataElement implements DataTypes {
 
   public String name;
   public Type type;
   public String svrName = "";
   public String svrValue = "";
   public String svrUnits = "";
   public int bit;
   public double max, min;

   public DataElement(String _name, Type _type, String _svrName) {
      name = _name;
      type = _type;
      svrName = _svrName;
   }
   public DataElement(String _name, Type _type, String _svrName, String _svrUnits) {
      this(_name, _type, _svrName);
      svrUnits = _svrUnits;
   }

   public DataElement(String _name, Type _type, String _svrName, String _svrUnits, double _min, double _max) {
      this(_name, _type, _svrName, _svrUnits);
      min = _min;
      max = _max;
   }
   public DataElement(String _name, Type _type, String _svrName, int _bit) {
      this(_name, _type, _svrName);
      bit = _bit;
   }
   public DataElement(String _name, Type _type, String _svrName, double _min, double _max) {
      this(_name, _type, _svrName);
      min = _min;
      max = _max;
   }
}




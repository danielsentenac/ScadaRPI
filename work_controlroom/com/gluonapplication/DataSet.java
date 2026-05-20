package com.gluonapplication;

import java.util.Vector;



public class DataSet implements DataTypes, Cloneable {

   final Vector<DataElement> list;
   final Vector<String> svrNameList;
   final Vector<String> svrUnitsList;
   Vector<String> svrValueList;

   public DataSet() {
      list = new Vector<>();
      svrNameList = new Vector<>();
      svrUnitsList = new Vector<>();
      svrValueList = new Vector<>();
   }

   public void Init() {
      for (DataElement v : list ) {
         if (v.svrName != null) {
            svrNameList.addElement(v.svrName);
            svrUnitsList.addElement(v.svrUnits);
         }
         else {
            System.out.println("!!!!!!!!!!!!!!!!!!!WARNING NULL CHANNEL DETECTED for " + v.name);
            svrNameList.addElement("---"); // IMPORTANT TO KEEP THE SAME SIZE WITH svrValueList and data vectors
            svrUnitsList.addElement("---"); // IMPORTANT TO KEEP THE SAME SIZE WITH svrValueList and data vectors
         }
      }
   }

   @Override
   public DataSet clone() throws CloneNotSupportedException {
                return (DataSet) super.clone();
   }
  
   public DataElement getDataElement(String name) {
      DataElement dataElement = null;
      for (DataElement v : list ) {
         if (v.name.equals(name)) {
            dataElement = v;
            break;
         }
      }
      return dataElement;
   }
}



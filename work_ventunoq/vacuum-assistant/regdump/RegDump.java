import com.pi4j.io.serial.*;
import java.util.*;

/** Builds the devices exactly like Main.java (without starting them) and prints the Modbus register map as CSV. */
public class RegDump {
    public static void main(String[] a) throws Exception {
        java.util.logging.Logger.getLogger("Main").setLevel(java.util.logging.Level.OFF);
        List<Device> l = new ArrayList<Device>();
        Device mg = new MaxiGauge("MG", 0, "x", Baud._9600, DataBits._8, Parity.NONE, StopBits._1, FlowControl.NONE); l.add(mg);
        Device xgs = new XGSGauge("XGS", mg.mbRegisterEnd, "x", Baud._9600, DataBits._8, Parity.NONE, StopBits._1, FlowControl.NONE); l.add(xgs);
        Device sicem = new Sicem("SICEM", xgs.mbRegisterEnd, "x", Baud._9600, DataBits._8, Parity.NONE, StopBits._1, FlowControl.NONE); l.add(sicem);
        Device dcu = new TurboPfeifferDCU("DCU", sicem.mbRegisterEnd, "x", Baud._9600, DataBits._8, Parity.NONE, StopBits._1, FlowControl.NONE); l.add(dcu);
        Device m1 = new Controllino_1("M1", dcu.mbRegisterEnd, Integer.parseInt(a[0])); l.add(m1);
        Device m2 = new Controllino_2("M2", m1.mbRegisterEnd, Integer.parseInt(a[1])); l.add(m2);
        Device tc = new TCMuxShield("TCMUX", m2.mbRegisterEnd); l.add(tc);
        Device rot = new MksRotor("ROT", tc.mbRegisterEnd, "x", Baud._9600, DataBits._8, Parity.NONE, StopBits._1, FlowControl.NONE); l.add(rot);
        Device ley = new RgaLeyspec("LEYSPEC", rot.mbRegisterEnd, "192.168.250.11", 1024, 1, 199, 3, "EE50"); l.add(ley);
        System.out.println("channel,device,element,access,regtype,offset,length");
        for (Device d : l)
            for (Map.Entry<String, DataElement> e : d.dataList.entrySet()) {
                DataElement x = e.getValue();
                int len = (x.mbRegisterType == DataTypes.RegisterType.INT32 || x.mbRegisterType == DataTypes.RegisterType.FLOAT32) ? 2 : 1;
                System.out.println(d.name + "_" + x.name + "," + d.name + "," + x.name + "," + x.type + "," + x.mbRegisterType + "," + x.mbRegisterOffset + "," + len);
            }
        System.exit(0);
    }
}

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.*;
import com.intelligt.modbus.jlibmodbus.slave.*;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

/**
 * Stand-in for the work_tube1500w panel: a Modbus TCP server (same library and settings as
 * ModbusSlaveThread.java) filled with plausible values according to registers.csv.
 * Usage: java -cp .:jlibmodbus-1.2.9.7.jar PanelSim registers.csv [port]
 */
public class PanelSim {
    public static void main(String[] a) throws Exception {
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_RELEASE);
        int port = a.length > 1 ? Integer.parseInt(a[1]) : 1288;
        List<String[]> rows = new ArrayList<String[]>();
        BufferedReader r = new BufferedReader(new FileReader(a[0]));
        String line = r.readLine();
        while ((line = r.readLine()) != null) rows.add(line.split(","));
        r.close();

        TcpParameters tp = new TcpParameters();
        tp.setHost(InetAddress.getByName("0.0.0.0")); tp.setPort(port); tp.setKeepAlive(true);
        ModbusSlaveTCP slave = (ModbusSlaveTCP) ModbusSlaveFactory.createModbusSlaveTCP(tp);
        slave.setServerAddress(Modbus.TCP_DEFAULT_ID);
        slave.setBroadcastEnabled(true);
        slave.setReadTimeout(5000);
        DataHolder dh = new DataHolder();
        ModbusHoldingRegisters hr = new ModbusHoldingRegisters(1000);
        dh.setHoldingRegisters(hr);
        slave.setDataHolder(dh);
        slave.listen();
        System.out.println("PanelSim listening on port " + port + " with " + rows.size() + " channels");

        Map<String, Double> base = new HashMap<String, Double>();
        base.put("MG_PR1", 2.1e-2); base.put("MG_PR2", 4.5e-7); base.put("MG_PR3", 3.1e-7); base.put("MG_PR5", 1.2e-3); base.put("MG_PR6", 2.3e+1);
        base.put("XGS_PR7", 2.8e-7); base.put("ROT_PR1", 3.0e-7); base.put("LEYSPEC_TP", 3.0e-7);
        base.put("DCU_P21SPEED", 1000.0); base.put("DCU_P21FSPEED", 1000.0); base.put("DCU_P21HR", 12345.0); base.put("DCU_P21PWR", 18.0);
        base.put("LEYSPEC_002", 1.0e-8); base.put("LEYSPEC_017", 5.0e-8); base.put("LEYSPEC_018", 2.0e-7);
        base.put("LEYSPEC_028", 3.0e-8); base.put("LEYSPEC_032", 2.0e-9); base.put("LEYSPEC_044", 8.0e-9);
        base.put("LEYSPEC_ERROR", 0.0); base.put("LEYSPEC_ERRORCODE", 0.0); base.put("LEYSPEC_SEMHV", 0.0); base.put("LEYSPEC_MODE", 0.0);
        base.put("LEYSPEC_RUNNING", 1.0); base.put("LEYSPEC_EMISSION", 1.0); base.put("LEYSPEC_RF", 1.0); base.put("LEYSPEC_FILAMENT", 1.0);
        // States of a normal pumped-down situation (codes as in channel_notes.csv)
        for (String g : new String[]{"MG_PR1", "MG_PR2", "MG_PR3", "MG_PR5", "MG_PR6"}) { base.put(g + "ST", 0.0); base.put(g + "SST", 2.0); }
        for (String g : new String[]{"MG_PR4"}) { base.put(g + "ST", 5.0); base.put(g + "SST", 0.0); }
        base.put("XGS_PR7ST", 1.0); base.put("XGS_PR7DST", 0.0); base.put("XGS_PR7FST", 1.0); base.put("ROT_PR1ST", 1.0);
        base.put("DCU_P21ST", 1.0); base.put("DCU_P21STYST", 0.0); base.put("DCU_P21TPST", 0.0); base.put("DCU_P21BTPST", 0.0); base.put("DCU_P21BERR", 0.0);
        base.put("M1_V21ST", 1.0); base.put("M1_V22ST", 1.0); base.put("M1_V23ST", 2.0); base.put("M1_V1ST", 1.0);
        base.put("M1_BYPASSST", 2.0); base.put("M1_COMPRESSAIRST", 0.0); base.put("M1_P22ST", 1.0);
        base.put("M2_CHANNEL_1_ONOFF_ST", 2.0); base.put("M2_CHANNEL_2_ONOFF_ST", 2.0);
        base.put("M2_SICEM_ALRM_ST", 0.0); base.put("M2_SICEM_COMERR_ST", 0.0); base.put("M2_MUXSHIELD_COMERR_ST", 0.0);
        Random rnd = new Random(1);
        while (true) {
            for (String[] c : rows) {
                String ch = c[0], dev = c[1], el = c[2], access = c[3], type = c[4];
                int off = Integer.parseInt(c[5]);
                if (access.equals("TRIGGER")) continue;
                double noise = 1.0 + 0.02 * (rnd.nextDouble() - 0.5);
                double v;
                if (base.containsKey(ch)) v = base.get(ch) * (type.equals("FLOAT32") ? noise : 1.0);
                else if (access.equals("COM_STATUS")) v = 0;                       // 0 = communication OK
                else if (dev.equals("LEYSPEC") && el.matches("\\d+")) v = 1.0e-12 * noise;
                else if (ch.startsWith("SICEM_SETP")) v = 20.0;
                else if (ch.startsWith("SICEM_T") || ch.startsWith("TCMUX_TEMP")) v = 22.0 * noise;
                else if (access.endsWith("STATUS")) v = 1;
                else v = 0;
                if (type.equals("FLOAT32")) hr.setFloat32At(off, (float) v);
                else if (type.equals("INT32")) hr.setInt32At(off, (int) v);
                else hr.setInt16At(off, (int) v);
            }
            Thread.sleep(1000);
        }
    }
}

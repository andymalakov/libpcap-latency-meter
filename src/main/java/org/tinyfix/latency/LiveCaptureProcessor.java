package org.tinyfix.latency;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.tinyfix.latency.common.CaptureSettings;

import java.util.ArrayList;
import java.util.List;

public class LiveCaptureProcessor<T> extends AbstractCaptureProcessor<T> {

    protected int interfaceId = -1;
    protected String captureFilter;

    @Override
    protected boolean parseCommandLineArgument (String arg) {
        if (arg.startsWith("-interface:")) {
            interfaceId = Integer.parseInt(value(arg));
            return true;
        }
        if (arg.startsWith("-filter:")) {
            captureFilter = value(arg);
            return true;
        }
        return super.parseCommandLineArgument(arg);
    }

    @Override
    protected void printHelp () {
        super.printHelp();
        System.out.println("\t-interface:N\t- Specifies index of interface to listen on. Default is 0.");
        System.out.println("\t-filter:<capture filter>\t- Specifies LIBPCAP capture filter, for example: \"(tcp src port 2509) or (tcp dst port 2508)\"");
    }

    @Override
    protected void run(String ... args) throws Exception {
        super.run(args);

        if (interfaceId == -1) {
            printNetworkInterfaces();
            interfaceId = 0;
        }
        PcapIf device = selectPcapIf(interfaceId);
        System.out.println("Recording from: " + device.getName() + " (" + device.getDescription() + ')');


        StringBuilder err = new StringBuilder();

        final Pcap pcap = Pcap.openLive(device.getName(), CaptureSettings.PACKET_SNAP_LENGTH, Pcap.MODE_NON_PROMISCUOUS, CaptureSettings.OPEN_LIVE_TIMEOUT_MILLIS, err);
        if (pcap == null)
            throw new IllegalArgumentException(err.toString());

        if (captureFilter != null)
            setupFilter(pcap, captureFilter);

        runCaptureLoop(pcap, err);
    }


    private static void setupFilter(Pcap pcap, String expression) throws Exception {
        PcapBpfProgram program = new PcapBpfProgram();
        final int netmask = (int)Long.parseLong(CaptureSettings.FILTER_NETWORK_MASK_HEX.toLowerCase(),  16);
        if (pcap.compile(program, expression, CaptureSettings.OPTIMIZE_FILTER ? 1 : 0, netmask) != Pcap.OK)
            throw new Exception("Error compiling LIBPCAP filter: " + pcap.getErr());

        if (pcap.setFilter(program) != Pcap.OK)
            throw new Exception("Error seeting LIBPCAP filter: " + pcap.getErr());
    }

    private static PcapIf selectPcapIf(int interfaceId) throws Exception {
        List<PcapIf> interfaces = listNetworkInterfaces();
        if (interfaceId < 0 || interfaceId >= interfaces.size())
            throw new Exception("Invalid interface ID specified: " + interfaceId);
        return interfaces.get(interfaceId);
    }

    private static List<PcapIf> listNetworkInterfaces() throws Exception {
        final StringBuilder errbuf = new StringBuilder();
        List<PcapIf> result = new ArrayList<>(); // Will be filled with NICs
        int r = Pcap.findAllDevs(result, errbuf);
        if (r == Pcap.ERROR || result.isEmpty())
            throw new Exception ("Can't read list of devices, error is :" + errbuf.toString());

        return result;
    }


    private static void printNetworkInterfaces() throws Exception {
        List<PcapIf> interfaces = listNetworkInterfaces();
        System.out.println("Network devices found:");
        for (int i=0; i < interfaces.size(); i++) {
            PcapIf device = interfaces.get(i);
            String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
            System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
        }
    }


    public static void main (String [] args) throws Exception {
        new LiveCaptureProcessor().run(args);
    }

}
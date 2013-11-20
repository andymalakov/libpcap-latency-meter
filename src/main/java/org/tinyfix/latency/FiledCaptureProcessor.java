package org.tinyfix.latency;

import org.jnetpcap.Pcap;

public class FiledCaptureProcessor extends AbstractCaptureProcessor {
    private String captureFileName = "capture.pcap";

    @Override
    protected boolean parseCommandLineArgument (String arg) {
        if (arg.startsWith("-pcap:")) {
            captureFileName = value(arg);
            return true;
        }
        return super.parseCommandLineArgument(arg);
    }

    @Override
    protected void printHelp () {
        super.printHelp();
        System.out.println("\t-pcap:filename\t- Specifies filename of input capture file (in PCAP format).");
    }

    @Override
    protected void run(String ... args) throws Exception {
        super.run(args);

        StringBuilder err = new StringBuilder();
        Pcap pcap = Pcap.openOffline(captureFileName, err);
        if (pcap == null)
            throw new IllegalArgumentException(err.toString());

        runCaptureLoop(pcap, err);
    }

    public static void main (String [] args) throws Exception {
        new FiledCaptureProcessor().run(args);
    }
}

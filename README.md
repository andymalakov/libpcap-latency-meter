# libpcap-latency-meter


December 2025 Update: [I made a much better tool](https://github.com/epam/ebpf-fix-latency-tool) for the same purpose!


### What is it?
**LIBPCAP Latency Meter** is a small Java based tool that measures time difference between inbound and outbound TCP packets messages.

### How does it work?
The tool intercepts inbound and outbound TCP packets containing your network messages. All captured packets carry time stamps. This tool matches outbound packets with corresponding inbound packets using some kind of correlation key (For example, value of specific FIX tags). 

![Signal latency](https://raw.github.com/andymalakov/libpcap-latency-meter/master/doc/signal-latency.png)

For example, this tool can be used measure latency of trading signals. FX market data feed usually contain a field [QuoteEntryID(299)](http://btobits.com/fixopaedia/fixdic44/tag_299_QuoteEntryID_.html), while FX orders may contain a field [QuoteID(117)](http://btobits.com/fixopaedia/fixdic42/tag_117_QuoteID_.html). Tool can correlate inbound and outbound messages based on the value provided in these tags.

![Tick-to-Quote packets correlation](https://raw.github.com/andymalakov/libpcap-latency-meter/master/doc/quote-to-order.png)

Simple statistics (min/max/avg) are printed out during run time and latency log of each signal is recorded into CSV file.

See github [Project Wiki](https://github.com/andymalakov/libpcap-latency-meter/wiki/Overview) for more information.


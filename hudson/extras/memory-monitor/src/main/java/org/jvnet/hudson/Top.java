package org.jvnet.hudson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * {@link MemoryMonitor} that parses the output from the <tt>top</tt> command.
 * @author Kohsuke Kawaguchi
 */
final class Top extends MemoryMonitor {
    public MemoryUsage monitor() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("top","-b");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.getOutputStream().close();

        // obtain first 8 lines, then kill 'top'
        List<String> lines = new ArrayList<String>();
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=in.readLine())!=null && lines.size()<8)
                lines.add(line);
            proc.destroy();
            in.close();
        }

        long[] values = new long[4];
        Arrays.fill(values,-1);

        OUTER:
        for( int i=0; i<4; i++ ) {
            for( Pattern p : PATTERNS[i] ) {
                for (String line : lines) {
                    try {
                        Matcher m = p.matcher(line);
                        if(m.find()) {
                            values[i] = parse(m.group(1));
                            continue OUTER;
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Failed to parse "+line);
                    }
                }
            }
        }

        return new MemoryUsage(values);
    }

    private static long parse(String token) {
        token = token.trim().toUpperCase();
        long multiplier = 1;
        if(token.endsWith("B"))
            token = cutTail(token);
        if(token.endsWith("K")) {
            multiplier = 1024;
            token = cutTail(token);
        }
        if(token.endsWith("M")) {
            multiplier = 1024*1024;
            token = cutTail(token);
        }
        if(token.endsWith("G")) {
            multiplier = 1024*1024*1024;
            token = cutTail(token);
        }

        return Long.parseLong(token)*multiplier;
    }

    private static String cutTail(String token) {
        return token.substring(0,token.length()-1);
    }

/*
On Solaris 10 + top from blastwave
==================================
$ uname -a && which top && top -b | head
SunOS wsinterop 5.10 Generic_118844-19 i86pc i386 i86pc
/opt/csw/bin/top
last pid: 27683;  load avg:  0.04,  0.04,  0.04;       up 39+01:47:58  18:16:35
99 processes: 97 sleeping, 1 zombie, 1 on cpu

Memory: 3647M phys mem, 1621M free mem, 2047M swap, 2047M free swap



On Ubuntu 8.4
==================================
% uname -a && which top && top -b | head
Linux unicorn 2.6.24-19-generic #1 SMP Wed Jun 18 14:15:37 UTC 2008 x86_64 GNU/Linux
/usr/bin/top
top - 18:28:09 up 2 days, 22:39, 10 users,  load average: 1.26, 1.41, 1.35
Tasks: 181 total,   1 running, 179 sleeping,   0 stopped,   1 zombie
Cpu(s):  4.9%us, 21.4%sy,  0.0%ni, 70.7%id,  2.9%wa,  0.0%hi,  0.1%si,  0.0%st
Mem:   4057400k total,  3369188k used,   688212k free,    82488k buffers
Swap:  4192956k total,   655028k used,  3537928k free,  1171404k cached

  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
 7041 kohsuke   20   0  823m 411m  15m S   97 10.4 675:46.69 VirtualBox
 6606 root      20   0  241m 107m  19m S   12  2.7  16:30.86 Xorg
 6907 kohsuke   20   0  134m  14m 9184 S    2  0.4   0:51.56 metacity
*/

    private static final Pattern[][] PATTERNS = new Pattern[][] {
        // total phys. memory
        new Pattern[] {
            Pattern.compile("^Memory:.* ([0-9]+[kmbKMG]) phys mem"), // Sol10+blastwave
            Pattern.compile("^Mem:.* ([0-9]+[kmbKMG]) total") // Linux
        },

        // available phys. memory
        new Pattern[] {
            Pattern.compile("^Memory:.* ([0-9]+[kmbKMG]) free mem"), // Sol10+blastwave
            Pattern.compile("^Mem:.* ([0-9]+[kmbKMG]) free") // Linux
        },

        // total swap memory
        new Pattern[] {
            Pattern.compile("^Memory:.* ([0-9]+[kmbKMG]) swap"), // Sol10+blastwave
            Pattern.compile("^Swap:.* ([0-9]+[kmbKMG]) total") // Linux
        },

        // available swap memory
        new Pattern[] {
            Pattern.compile("^Memory:.* ([0-9]+[kmbKMG]) free swap"), // Sol10+blastwave
            Pattern.compile("^Swap:.* ([0-9]+[kmbKMG]) free") // Linux
        }
    };
}

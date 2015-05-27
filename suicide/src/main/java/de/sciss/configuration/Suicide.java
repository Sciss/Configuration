package de.sciss.configuration;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class Suicide {
    public static void main(String[] args) {
        final Scanner sc = new Scanner(System.in);
        System.out.println("::: scsynth on suicide watch v2 :::");
        final DateFormat logFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS 'suicide!'", Locale.US);
        final long[] detected = new long[10];   // ten XRUNs in ten seconds; ring buffer
        int detectIdx = 0;
        while (sc.hasNextLine()) {
            final String line = sc.nextLine();
            System.err.println(line);
            // System.out.println("+++ " + line);
            if (line.contains("JackAudioDriver::ProcessGraphAsyncMaster: Process error")) {
                final long now = System.currentTimeMillis();
                detected[detectIdx] = now;
                detectIdx = (detectIdx + 1) % detected.length;
                final long oldest = detected[detectIdx];
                final boolean kill = (now - oldest) < 10000;

                if (kill) {
                    Arrays.fill(detected, 0L);  // do not trigger multiple times
                    // System.err.println("NOW " + now + " OLDEST" + oldest);
                    // final boolean equals = line.equals("JackAudioDriver::ProcessGraphAsyncMaster: Process error");
                    // System.err.println("::: suicide! " + equals);
                    System.err.println(logFormat.format(new java.util.Date()));
                    final ProcessBuilder pb = new ProcessBuilder("killall", "scsynth");
                    try {
                        final Process p = pb.start();
                        try {
                            p.waitFor(); // exitValue();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

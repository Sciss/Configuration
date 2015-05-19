package de.sciss.configuration;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Scanner;

public class Suicide {
    public static void main(String[] args) {
        final Scanner sc = new Scanner(System.in);
        System.out.println("::: scsynth on suicide watch :::");
        final DateFormat logFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS 'suicide!'", Locale.US);
        while (sc.hasNextLine()) {
            final String line = sc.nextLine();
            System.err.println(line);
            // System.out.println("+++ " + line);
            if (line.contains("JackAudioDriver::ProcessGraphAsyncMaster: Process error")) {
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

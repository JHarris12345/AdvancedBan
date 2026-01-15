package me.leoko.advancedban.hytale.utils;

import me.leoko.advancedban.Universal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarnWordsLog {
    private static File file;

    public static void logToFile(String logMessage) {
        SimpleDateFormat time = new SimpleDateFormat("[dd MMM yyyy @ HH:mm:ss] ");

        try {
            file = new File(Universal.get().getMethods().getDataFolder(), "WarnWordsLog.yml");

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(time.format(new Date()) + logMessage);
            pw.flush();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package me.leoko.advancedban.manager;

import me.leoko.advancedban.Universal;
import me.leoko.advancedban.utils.Punishment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HistoryLog {
    private static File file;

    public static void logToFile(Punishment punishment) {
        try {
            file = new File(Universal.get().getMethods().getDataFolder(), "HistoryLog.yml");

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(fw);

            // Separate with ";;;" instead of just ";" because some punishment reasons have a ";" in it
            // Always ends with "null" because this is the punishment.getCalculation() bit which is always null (I've not worked out what it is)
            String logMessage = punishment.getName() + ";;;" + punishment.getUuid() + ";;;" + punishment.getReason() + ";;;" + punishment.getOperator() + ";;;" + punishment.getType().name() + ";;;" + punishment.getStart() + ";;;" + punishment.getEnd() + ";;;" + "null";

            pw.println(logMessage);
            pw.flush();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

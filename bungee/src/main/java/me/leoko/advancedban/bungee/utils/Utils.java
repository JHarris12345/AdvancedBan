package me.leoko.advancedban.bungee.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String colour(String string) {
        Pattern pattern = Pattern.compile("&?#[A-Fa-f0-9]{6}");
        Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            String hexCode = string.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
            String replaceAmp = replaceSharp.replace("&", "");

            char[] ch = replaceAmp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            string = string.replace(hexCode, builder.toString());
            matcher = pattern.matcher(string);
        }

        return ChatColor.translateAlternateColorCodes('&', string);
    }
}

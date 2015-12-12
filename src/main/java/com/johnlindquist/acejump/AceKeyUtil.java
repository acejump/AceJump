package com.johnlindquist.acejump;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/23/12
 * Time: 3:39 PM
 */
public class AceKeyUtil {

    /*todo: I hate this, but I keep debating whether I wanted to allow numbers and special chars in the "AllowedCharacters" set. Strict mapping to my USA keyboard :(*/
    public static String getLowerCaseStringFromChar(char keyChar) {

        String s = String.valueOf(keyChar);
        if (s.equals("!")) {
            return "1";

        } else if (s.equals("@")) {
            return "2";

        } else if (s.equals("#")) {
            return "3";

        } else if (s.equals("$")) {
            return "4";

        } else if (s.equals("%")) {
            return "5";

        } else if (s.equals("^")) {
            return "6";

        } else if (s.equals("&")) {
            return "7";

        } else if (s.equals("*")) {
            return "8";

        } else if (s.equals("(")) {
            return "9";

        } else if (s.equals(")")) {
            return "0";
        } else if (s.equals("_")) {
            return "-";
        } else if (s.equals("+")) {
            return "=";
        } else if (s.equals("{")) {
            return "[";
        } else if (s.equals("}")) {
            return "]";
        } else if (s.equals("|")) {
            return "\\";
        } else if (s.equals(":")) {
            return ";";
        } else if (s.equals("<")) {
            return ",";
        } else if (s.equals(">")) {
            return ".";
        } else if (s.equals("?")) {
            return "/";
        }
        return s.toLowerCase();
    }

}

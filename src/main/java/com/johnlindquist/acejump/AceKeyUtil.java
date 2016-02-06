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
        switch (s) {
            case "!":
                return "1";

            case "@":
                return "2";

            case "#":
                return "3";

            case "$":
                return "4";

            case "%":
                return "5";

            case "^":
                return "6";

            case "&":
                return "7";

            case "*":
                return "8";

            case "(":
                return "9";

            case ")":
                return "0";
            case "_":
                return "-";
            case "+":
                return "=";
            case "{":
                return "[";
            case "}":
                return "]";
            case "|":
                return "\\";
            case ":":
                return ";";
            case "<":
                return ",";
            case ">":
                return ".";
            case "?":
                return "/";
        }
        return s.toLowerCase();
    }

}

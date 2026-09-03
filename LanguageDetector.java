package com.blindgyi.tts;

public class LanguageDetector {
    public static boolean isBurmese(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u1000' && c <= '\u109F') {
                return true;
            }
        }
        return false;
    }
}

package com.blindgyi.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartTokenizer {
    // Burmese syllable and token matching pattern
    private static final Pattern MYANMAR_TOKEN_PATTERN = Pattern.compile(
        "([က-အ](?:်[က-အ])?(?:[ျြွှ])*(?:[ါ-ေံးုိုီ]*)(?:်)?|[၊။]|\\s+|[^\\s\u1000-\u109F])"
    );

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        String cleaned = TextNormalizer.normalize(text);
        Matcher matcher = MYANMAR_TOKEN_PATTERN.matcher(cleaned);
        
        while (matcher.find()) {
            String token = matcher.group();
            if (token != null && !token.trim().isEmpty()) {
                tokens.add(token);
            }
        }
        
        return tokens;
    }
}

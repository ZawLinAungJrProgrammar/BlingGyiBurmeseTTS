package com.blindgyi.tts;

public class TextNormalizer {
    public static String normalize(String text) {
        if (text == null) return "";
        // လိုအပ်သော စာသားပြင်ဆင်မှုများ (ဥပမာ - ကွက်လပ်များ ဖယ်ရှားခြင်း စသည်)
        return text.trim();
    }
}

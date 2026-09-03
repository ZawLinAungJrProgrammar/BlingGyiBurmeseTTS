package com.blindgyi.tts.g2p;

import java.util.ArrayList;
import java.util.List;

public class BurmeseG2PEngine {

    public static class Phoneme {
        public String symbol;
        public double duration; 
        public double f1, f2, f3;
        public double pitch;

        public Phoneme(String symbol, double duration, double f1, double f2, double f3, double pitch) {
            this.symbol = symbol;
            this.duration = duration;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.pitch = pitch;
        }
    }

    public static List<Phoneme> textToPhonemes(String text) {
        List<Phoneme> phonemeList = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return phonemeList;
        }

        // မြန်မာယူနစ်ကုဒ် ဘလောက်တစ်ခုလုံးကို ခြုံငုံမိစေရန် စစ်ဆေးခြင်း
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // မြန်မာစာလုံးများနှင့် အသံသင်္ကေတများ (U+1000 မှ U+109F အတွင်း)
            if (c >= '\u1000' && c <= '\u109F') {
                phonemeList.add(new Phoneme(String.valueOf(c), 80.0, 500.0, 1500.0, 2500.0, 120.0));
            } 
            // အင်္ဂလိပ်စာ သို့မဟုတ် ကိန်းဂဏန်းများပါလာပါက
            else if (Character.isLetterOrDigit(c)) {
                phonemeList.add(new Phoneme(String.valueOf(c), 70.0, 400.0, 1400.0, 2400.0, 120.0));
            }
        }

        // စာသားပါလျက်နဲ့ Phoneme စာရင်း အလွတ်ဖြစ်နေပါက အသံထွက်ထွက်စေရန် Default Fallback ထည့်ပေးခြင်း
        if (phonemeList.isEmpty() && !text.trim().isEmpty()) {
            phonemeList.add(new Phoneme("default", 100.0, 500.0, 1500.0, 2500.0, 120.0));
        }

        return phonemeList;
    }
}

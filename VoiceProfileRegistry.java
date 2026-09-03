package com.blindgyi.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceProfileRegistry {
    public static final List<VoiceProfile> profiles = new ArrayList<>();

    static {
        profiles.add(new VoiceProfile("voice_profile_1", "ပုံမှန်အသံ (Standard)", new Locale("my", "MM"), 120.0, 1.0));
        profiles.add(new VoiceProfile("voice_profile_2", "အသံထူ/လေးနက် (Deep)", new Locale("my", "MM"), 95.0, 0.95));
        profiles.add(new VoiceProfile("voice_profile_3", "အသံသွက်/မြန်ဆန် (Fast)", new Locale("my", "MM"), 140.0, 1.2));
    }

    public static VoiceProfile getProfileById(String id) {
        for (VoiceProfile profile : profiles) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }
        return profiles.get(0);
    }
}

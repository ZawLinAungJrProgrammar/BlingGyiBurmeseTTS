package com.blindgyi.tts;

import java.util.Locale;

public class VoiceProfile {
    public String id;
    public String name;
    public Locale locale;
    public double pitch;
    public double speed;

    public VoiceProfile(String id, String name, Locale locale, double pitch, double speed) {
        this.id = id;
        this.name = name;
        this.locale = locale;
        this.pitch = pitch;
        this.speed = speed;
    }
}

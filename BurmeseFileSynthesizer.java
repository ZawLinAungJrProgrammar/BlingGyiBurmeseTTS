Package com.blindgyi.tts;

import android.content.Context;

public class BurmeseFileSynthesizer {
    private Context context;
    private VoiceProfile currentProfile;

    static {
        System.loadLibrary("tts-engine");
    }

    private native byte[] nativeSynthesizeToPcm(String text, double pitch, double speed);

    public BurmeseFileSynthesizer(Context context) {
        this.context = context;
    }

    public void applyVoiceProfile(VoiceProfile profile) {
        this.currentProfile = profile;
    }

    public byte[] synthesizeToPcm(String text) {
        if (text == null || text.isEmpty()) {
            return new byte[0];
        }

        double pitch = (currentProfile != null) ? currentProfile.pitch : 120.0;
        double speed = (currentProfile != null) ? currentProfile.speed : 1.0;

        return nativeSynthesizeToPcm(text, pitch, speed);
    }
}

Package com.blindgyi.tts;

import android.accessibilityservice.AccessibilityService;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;

public class BlindGyiAccessibilityService extends AccessibilityService {
    private BurmeseFileSynthesizer synthesizer;
    private AudioTrack audioTrack;

    @Override
    public void onCreate() {
        super.onCreate();
        synthesizer = new BurmeseFileSynthesizer(this);
        
        int sampleRate = 16000;
        int bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(bufferSize, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            List<CharSequence> textList = event.getText();
            if (textList != null && !textList.isEmpty()) {
                CharSequence text = textList.get(0);
                if (text != null && !text.toString().isEmpty()) {
                    String typedText = text.toString();
                    if (LanguageDetector.isBurmese(typedText)) {
                        playTypedText(typedText);
                    }
                }
            }
        }
    }

    private void playTypedText(String text) {
        new Thread(() -> {
            String normalizedText = TextNormalizer.normalize(text);
            byte[] pcmData = synthesizer.synthesizeToPcm(normalizedText);
            if (pcmData != null && pcmData.length > 0) {
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.play();
                    }
                    audioTrack.write(pcmData, 0, pcmData.length);
                }
            }
        }).start();
    }

    @Override
    public void onInterrupt() {
        if (audioTrack != null) {
            audioTrack.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioTrack != null) {
            audioTrack.release();
        }
    }
}

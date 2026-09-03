package com.blindgyi.tts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlindGyiTTSService extends TextToSpeechService implements TextToSpeech.OnInitListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private BurmeseFileSynthesizer fileSynthesizer;
    private VoiceProfile currentProfile = VoiceProfileRegistry.getProfileById("voice_profile_1");
    private TextToSpeech delegatedTts;
    private String selectedEngine = "com.google.android.tts";
    private SharedPreferences preferences;
    private boolean isDelegatedReady = false;
    
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean isAudioFocusGranted = false;

    private ExecutorService executorService;
    private Handler mainHandler;

    private static final String CHANNEL_ID = "BlindGyiTTS_ServiceChannel";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        fileSynthesizer = new BurmeseFileSynthesizer(this);
        preferences = getSharedPreferences("BlindGyiTTS_Prefs", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createForegroundNotification("BlindGyi TTS Active"));

        loadPreferences();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BlindGyi TTS Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createForegroundNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BlindGyi TTS")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }

    private boolean requestAudioDuckingFocus() {
        if (audioManager == null) return false;

        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(focusChange -> {})
                    .build();
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        } else {
            int result = audioManager.requestAudioFocus(
                    focusChange -> {},
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );
            isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return isAudioFocusGranted;
    }

    private void abandonAudioDuckingFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
        isAudioFocusGranted = false;
    }

    private void loadPreferences() {
        selectedEngine = preferences.getString("SELECTED_ENGLISH_ENGINE", "com.google.android.tts");
        String voiceId = preferences.getString("SELECTED_VOICE_ID", "voice_profile_1");
        currentProfile = VoiceProfileRegistry.getProfileById(voiceId);
        fileSynthesizer.applyVoiceProfile(currentProfile);

        if (delegatedTts != null) {
            delegatedTts.shutdown();
        }
        isDelegatedReady = false;
        delegatedTts = new TextToSpeech(this, this, selectedEngine);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("SELECTED_VOICE_ID".equals(key)) {
            String voiceId = sharedPreferences.getString(key, "voice_profile_1");
            currentProfile = VoiceProfileRegistry.getProfileById(voiceId);
            fileSynthesizer.applyVoiceProfile(currentProfile);
        } else if ("SELECTED_ENGLISH_ENGINE".equals(key)) {
            selectedEngine = sharedPreferences.getString(key, "com.google.android.tts");
            if (delegatedTts != null) {
                delegatedTts.shutdown();
            }
            isDelegatedReady = false;
            delegatedTts = new TextToSpeech(this, this, selectedEngine);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && delegatedTts != null) {
            delegatedTts.setLanguage(Locale.ENGLISH);
            isDelegatedReady = true;
        }
    }

    @Override
    public int onIsLanguageAvailable(String lang, String country, String variant) {
        if ("my".equals(lang) || "en".equals(lang)) {
            return TextToSpeech.LANG_AVAILABLE;
        }
        return TextToSpeech.LANG_AVAILABLE;
    }

    @Override
    public String[] onGetLanguage() {
        return new String[]{"my", "MMR", "en", "USA"};
    }

    @Override
    public int onLoadLanguage(String lang, String country, String variant) {
        return onIsLanguageAvailable(lang, country, variant);
    }

    @Override
    public void onStop() {
        abandonAudioDuckingFocus();
        if (delegatedTts != null) {
            delegatedTts.stop();
        }
    }

    @Override
    public List<Voice> onGetVoices() {
        List<Voice> voices = new ArrayList<>();
        for (VoiceProfile profile : VoiceProfileRegistry.profiles) {
            Set<String> langs = new HashSet<>();
            langs.add(profile.locale.getLanguage());
            voices.add(new Voice(profile.id, profile.locale, Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, langs));
        }
        return voices;
    }

    @Override
    public int onLoadVoice(String voiceName) {
        currentProfile = VoiceProfileRegistry.getProfileById(voiceName);
        fileSynthesizer.applyVoiceProfile(currentProfile);
        return TextToSpeech.SUCCESS;
    }

    @Override
    public void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        CharSequence charSeq = request.getCharSequenceText();
        if (charSeq == null || charSeq.toString().trim().isEmpty()) {
            callback.error();
            return;
        }

        String rawText = charSeq.toString();

        executorService.execute(() -> {
            requestAudioDuckingFocus();
            try {
                if (isBurmeseText(rawText)) {
                    String textToSpeak = TextNormalizer.normalize(rawText);
                    if (textToSpeak.isEmpty()) {
                        callback.error();
                        return;
                    }

                    int sampleRate = 16000;
                    // Native C++ (.so) ဘက်မှ PCM Data များကို ယူဆောင်ခြင်း
                    byte[] pcmData = fileSynthesizer.synthesizeToPcm(textToSpeak);

                    if (pcmData == null || pcmData.length == 0) {
                        callback.error();
                        return;
                    }

                    callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1);
                    callback.audioAvailable(pcmData, 0, pcmData.length);
                    callback.done();

                } else {
                    if (isDelegatedReady && delegatedTts != null) {
                        synthesizeViaDelegatedEngine(rawText, callback);
                    } else {
                        callback.error();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.error();
            } finally {
                abandonAudioDuckingFocus();
            }
        });
    }

    private boolean isBurmeseText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u1000' && c <= '\u109F') {
                return true;
            }
        }
        return false;
    }

    private void synthesizeViaDelegatedEngine(String text, SynthesisCallback callback) {
        File tempFile = new File(getCacheDir(), "tts_delegated_temp.wav");
        Bundle params = new Bundle();
        
        int result = delegatedTts.synthesizeToFile(text, params, tempFile, "DelegatedTTSId");
        if (result == TextToSpeech.SUCCESS && tempFile.exists() && tempFile.length() > 0) {
            try {
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] audioBytes = new byte[(int) tempFile.length()];
                int bytesRead = fis.read(audioBytes);
                fis.close();

                if (bytesRead > 44) {
                    callback.start(22050, AudioFormat.ENCODING_PCM_16BIT, 1);
                    callback.audioAvailable(audioBytes, 44, audioBytes.length - 44);
                    callback.done();
                } else {
                    callback.error();
                }
                
                tempFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
                callback.error();
            }
        } else {
            callback.error();
        }
    }

    @Override
    public void onDestroy() {
        abandonAudioDuckingFocus();
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (delegatedTts != null) {
            delegatedTts.stop();
            delegatedTts.shutdown();
        }
        super.onDestroy();
    }
}

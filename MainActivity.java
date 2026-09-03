package com.blindgyi.tts;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private Button btnVoiceProfile, btnSpeak;
    private EditText inputText;
    private Spinner spinnerEnglishEngine;
    private SeekBar seekRate, seekPitch;
    private TextToSpeech tts;
    private VoiceProfile selectedProfile = VoiceProfileRegistry.getProfileById("voice_profile_1");
    private List<TextToSpeech.EngineInfo> installedEngines = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnVoiceProfile = findViewById(R.id.btnVoiceProfile);
        btnSpeak = findViewById(R.id.btnSpeak);
        inputText = findViewById(R.id.inputText);
        spinnerEnglishEngine = findViewById(R.id.spinnerEnglishEngine);
        seekRate = findViewById(R.id.seekRate);
        seekPitch = findViewById(R.id.seekPitch);

        checkAndRequestPermissions();

        tts = new TextToSpeech(this, this);

        btnVoiceProfile.setOnClickListener(v -> showVoiceProfileSelectionDialog());

        btnSpeak.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            saveCurrentPreferences();
            if (!text.isEmpty()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_SPEAK_ID");
            } else {
                Toast.makeText(this, "ကျေးဇူးပြု၍ စာသားထည့်ပါ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } else {
                savePermissionStatus(true);
            }
        } else {
            savePermissionStatus(true);
        }
    }

    private void savePermissionStatus(boolean isGranted) {
        SharedPreferences prefs = getSharedPreferences("BlindGyiTTS_Prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("IS_PERMISSION_GRANTED", isGranted).apply();
    }

    private void loadSystemTtsEngines() {
        installedEngines = tts.getEngines();
        List<String> engineNames = new ArrayList<>();
        for (TextToSpeech.EngineInfo engine : installedEngines) {
            engineNames.add(engine.label + " (" + engine.name + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, engineNames);
        spinnerEnglishEngine.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("BlindGyiTTS_Prefs", MODE_PRIVATE);
        String savedEngine = prefs.getString("SELECTED_ENGLISH_ENGINE", "com.google.android.tts");
        for (int i = 0; i < installedEngines.size(); i++) {
            if (installedEngines.get(i).name.equals(savedEngine)) {
                spinnerEnglishEngine.setSelection(i);
                break;
            }
        }
    }

    private void saveCurrentPreferences() {
        SharedPreferences prefs = getSharedPreferences("BlindGyiTTS_Prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        int selectedPosition = spinnerEnglishEngine.getSelectedItemPosition();
        if (selectedPosition >= 0 && selectedPosition < installedEngines.size()) {
            String enginePkg = installedEngines.get(selectedPosition).name;
            editor.putString("SELECTED_ENGLISH_ENGINE", enginePkg);
        }

        editor.putFloat("SPEECH_RATE", (seekRate.getProgress() + 10) / 50.0f);
        editor.putFloat("SPEECH_PITCH", (seekPitch.getProgress() + 10) / 50.0f);
        editor.apply();
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences("BlindGyiTTS_Prefs", MODE_PRIVATE);
        float rate = prefs.getFloat("SPEECH_RATE", 1.0f);
        float pitch = prefs.getFloat("SPEECH_PITCH", 1.0f);
        
        seekRate.setProgress((int) (rate * 50 - 10));
        seekPitch.setProgress((int) (pitch * 50 - 10));
    }

    private void showVoiceProfileSelectionDialog() {
        List<VoiceProfile> profilesList = VoiceProfileRegistry.profiles;
        String[] profileNames = new String[profilesList.size()];
        for (int i = 0; i < profilesList.size(); i++) {
            profileNames[i] = profilesList.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("အသံပုံစံ ရွေးချယ်ရန်");
        builder.setItems(profileNames, (dialog, which) -> {
            VoiceProfile chosenProfile = profilesList.get(which);
            selectedProfile = chosenProfile;
            btnVoiceProfile.setText("Voice Profile: " + chosenProfile.name);
            
            SharedPreferences sharedPref = getSharedPreferences("BlindGyiTTS_Prefs", Context.MODE_PRIVATE);
            sharedPref.edit().putString("SELECTED_VOICE_ID", chosenProfile.id).apply();
            
            Toast.makeText(this, chosenProfile.name + " ကို ရွေးချယ်လိုက်ပါပြီ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("မလုပ်တော့ပါ", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("my", "MM"));
            loadSystemTtsEngines();
            loadSavedPreferences();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

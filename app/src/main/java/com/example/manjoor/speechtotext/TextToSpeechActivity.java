package com.example.manjoor.speechtotext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class TextToSpeechActivity extends Activity implements TextToSpeech.OnInitListener {
    TextToSpeech mTTS = null;
    private final int ACT_CHECK_TTS_DATA = 1000;
    private final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2000;
    private int permissionCount = 0;
    private String mAudioFilename = "";
    private final String mUtteranceID = "totts";

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final EditText ettext = (EditText)findViewById(R.id.ettext);
        final Button bsay = (Button)findViewById(R.id.bsay);
        bsay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saySomething(ettext.getText().toString().trim(), 1);
            }
        });

        final Button bsave = (Button)findViewById(R.id.bsave);
        bsave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveToAudioFile(ettext.getText().toString().trim());
            }
        });

        // perform the dynamic permission request
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);

        // Create audio file location
        File sddir = new File(Environment.getExternalStorageDirectory() + "/TextToSpeechActivity/");
        sddir.mkdirs();
        mAudioFilename = sddir.getAbsolutePath() + "/" + mUtteranceID + ".wav";

        // Check to see if we have TTS voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    permissionCount++;
            default:
                break;
        }
    }

    private void saveToAudioFile(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTTS.synthesizeToFile(text, null, new File(mAudioFilename), mUtteranceID);
            Toast.makeText(this, "Save file", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, String> hm = new HashMap();
            hm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, mUtteranceID);
            mTTS.synthesizeToFile(text, hm, mAudioFilename);
            Toast.makeText(this, "Save file 1", Toast.LENGTH_SHORT).show();
        }
        
        mTTS.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            public void onUtteranceCompleted(String uid) {
                if (uid.equals(mUtteranceID)) {
                    Toast.makeText(TextToSpeechActivity.this, "Saved to " + mAudioFilename, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saySomething(String text, int qmode) {
        if (qmode == 1)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACT_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // data exists, so we instantiate the TTS engine
                mTTS = new TextToSpeech(this, this);
            } else {
                // data is missing, so we start the TTS installation process
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                int result = mTTS.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                } else {
                    saySomething("TTS is ready", 0);
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }
}

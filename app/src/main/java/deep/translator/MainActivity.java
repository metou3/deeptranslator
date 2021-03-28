package deep.translator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;


import android.speech.tts.TextToSpeech;

public class MainActivity extends AppCompatActivity {
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    String fromLang;
    String toLang;
    ArrayAdapter<CharSequence> adapterFromLang;
    ArrayAdapter<CharSequence> adapterToLang;
    PyObject obj;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        EditText inputText = findViewById(R.id.input_text);


        Button micButton = findViewById(R.id.btnRecord);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                inputText.setText("");
                inputText.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                inputText.setText(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        micButton.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                speechRecognizer.stopListening();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });

//        setupTTS("en");

        if (!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }


        Python python1 = Python.getInstance();

        PyObject myPython =python1.getModule("script");

        PyObject obj1 = myPython.callAttr("get_language","fr");

        Log.d(MainActivity.class.getSimpleName(),obj1.toString());

        PyObject translateMe = myPython.callAttr("translate_me","hello how are you doing today","en","fr");
        Log.d(MainActivity.class.getSimpleName(),translateMe.toString());


        EditText outputText = findViewById(R.id.output_text);

        Button btnTranslate = findViewById(R.id.btnTranslate);

        ImageButton btnCopyInputText = findViewById(R.id.copyInputText);
        ImageButton btnCopyOutputText = findViewById(R.id.copyOutputText);

        btnCopyInputText.setOnClickListener(v -> {
            copyToClipBoard(inputText.getText().toString());
            Toast.makeText(this, R.string.text_copied,Toast.LENGTH_SHORT).show();
        });

        btnCopyOutputText.setOnClickListener(v -> {
            copyToClipBoard(outputText.getText().toString());
            Toast.makeText(this, R.string.text_copied,Toast.LENGTH_SHORT).show();
        });

        ImageButton btnSpeakInputText = findViewById(R.id.btnSpeakInputText);
        btnSpeakInputText.setOnClickListener(v -> {
            textToSpeech(inputText.getText().toString());
        });

        ImageButton btnSpeakOutputText = findViewById(R.id.btnSpeakOutputText);
        btnSpeakOutputText.setOnClickListener(v -> {
            textToSpeech(outputText.getText().toString());
        });


        Spinner spinner = (Spinner) findViewById(R.id.from_lang);
// Create an ArrayAdapter using the string array and a default spinner layout
        adapterFromLang = ArrayAdapter.createFromResource(this,
                R.array.supported_languages, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapterFromLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapterFromLang);



        Spinner spinnerToLang = (Spinner) findViewById(R.id.to_lang);
// Create an ArrayAdapter using the string array and a default spinner layout
        adapterToLang = ArrayAdapter.createFromResource(this,
                R.array.supported_languages, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapterToLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinnerToLang.setAdapter(adapterToLang);


        String[] allLanguages = getResources().getStringArray(R.array.supported_languages);
        final int[] position = {0};
        for (String language:allLanguages) {
            if (language.equalsIgnoreCase(Locale.getDefault().getDisplayLanguage())){
                spinner.setSelection(position[0]);
               break;
           }else {
                position[0]++;
            }
        }


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected_language = adapterFromLang.getItem(position).toString();
                PyObject hl_tag = myPython.callAttr("get_language_tag",selected_language);
                fromLang = hl_tag.toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerToLang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected_language = adapterToLang.getItem(position).toString();
                PyObject hl_tag = myPython.callAttr("get_language_tag",selected_language);
                toLang = hl_tag.toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        AtomicReference<Python> python = new AtomicReference<>(Python.getInstance());

        AtomicReference<PyObject> pyObject = new AtomicReference<>(python.get().getModule("script"));

        btnTranslate.setOnClickListener(v -> {
            python.set(Python.getInstance());

            pyObject.set(python.get().getModule("script"));

            PyObject obj = pyObject.get().callAttr("translate_me",inputText.getText().toString(),fromLang,toLang);

            outputText.setText(obj.toString());
        });


    }

    public void copyToClipBoard(String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("translation", text);
        clipboard.setPrimaryClip(clip);
    }

    public void textToSpeech(String data){

        Log.i("TTS", "button clicked: " + data);
        int speechStatus = textToSpeech.speak(data, TextToSpeech.QUEUE_FLUSH, null);

        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }
}
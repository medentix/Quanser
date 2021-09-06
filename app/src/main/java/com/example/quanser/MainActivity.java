package com.example.quanser;
//import android.annotation.SuppressLint;
//import android.app.ActionBar;
//import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanser.Modals.QuestionModal;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quanser.algorithms.Cosine_Similarity;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech mTTS;
    private Button mRead;
    private Button btnSubmit;

    Cosine_Similarity cs1 = new Cosine_Similarity();
    double sim_score;

    private AppBarConfiguration mAppBarConfiguration;

    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;

    //questions
    ArrayList<QuestionModal> questionsList;

    TextView tvQuestion;
    EditText etAnswer;
    Button btnShowAnswer ,btnNext, btnPrev;

    int index;

//    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRead = findViewById(R.id.btn_read);
        //Text To Speech Codes below
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                    if(i == TextToSpeech.SUCCESS) {
                        int result = mTTS.setLanguage(Locale.ENGLISH);

                        if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                            Log.e("TTS","Language not supported");
                        }
                        else
                        {
                            mRead.setEnabled(true);
                        }
                    }
                    else{
                        Log.e("TTS","Initialization failed");
                    }
            }
        });

        mRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speak();
            }
        });

//        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//        getSupportActionBar().setCustomView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder addQuestionDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                View addQuestionView = getLayoutInflater().inflate(R.layout.add_question, null);
                final EditText etQuestion = addQuestionView.findViewById(R.id.editText_question_dialog);
                final EditText etAnswer =  addQuestionView.findViewById(R.id.editText_answer_dialog);

                addQuestionDialogBuilder.setView(addQuestionView);
                addQuestionDialogBuilder.setTitle("Add Question...");
                addQuestionDialogBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String que = etQuestion.getText().toString();
                        String ans = etAnswer.getText().toString();
                        if (que.isEmpty() || ans.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Question or Answer should not be empty", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            QuestionModal question = new QuestionModal(que, ans);
                            myRef.child("que00" + (questionsList.size() + 1)).setValue(question).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(MainActivity.this, "Question Added Successfully!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                });
                addQuestionDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                AlertDialog addQuestionDialog = addQuestionDialogBuilder.create();
                addQuestionDialog.setCancelable(false);
                addQuestionDialog.show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        myRef = database.getReference("questions");

        questionsList = new ArrayList<>();
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    QuestionModal queObj = dataSnapshot.getValue(QuestionModal.class);
                    questionsList.add(queObj);
                }
                index = 0;
                setTvQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Something bad happened", Toast.LENGTH_SHORT).show();
            }
        });
        btnSubmit = findViewById(R.id.btn_submit);
        tvQuestion = findViewById(R.id.txtQuestion);
        etAnswer = findViewById(R.id.editText_answer_dialog);
        btnShowAnswer = findViewById(R.id.btn_showAnswer);
        btnNext =findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);
        String tmp;


        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double dbTmp = cs1.Cosine_Similarity_Score(etAnswer.getText().toString(),questionsList.get(index).getAns())*100;
                int iTmp = (int)dbTmp;
                etAnswer.setText("Similarity ~ "+Integer.toString(iTmp)+"%");
            }
        });

        btnShowAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etAnswer.setText(questionsList.get(index).getAns());
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index++;
                if (index  == questionsList.size()) {
                    index = 0;
                }
                setTvQuestion();
            }
        });


        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index--;
                if(index < 0) {
                    index = questionsList.size() - 1;
                }
                setTvQuestion();
            }
        });
    }// OnCreate method closing braces


    //SpeechToText codes below
    public void getSpeechInput(View view){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent,10);
        }
        else{
            Toast.makeText(this, "doesn't support speech input", Toast.LENGTH_SHORT).show();
        }

    }// end of getSpeechInput()

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case 10:
                if(resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    etAnswer.setText(result.get(0));
                }
                break;

        }

    }// created because of SpeechToText

    private void speak(){
        String text = tvQuestion.getText().toString();

        mTTS.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }//created because of TexttoSpeech.

    @Override
    protected void onDestroy() {
        if(mTTS != null){
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }//this is also created because of Text to Speech

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    void setTvQuestion() {
        tvQuestion.setText(questionsList.get(index).getQue());
    }
}
package com.jarnie.cappybara;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.jarnie.subtitleFile.Caption;
import com.jarnie.subtitleFile.FormatSRT;
import com.jarnie.subtitleFile.TimedTextObject;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EditorActivity extends AppCompatActivity {


    public static final String VIDEO_URI = "com.example.hellojava.VIDEO_URI";
    public static final String VIDEO_NAME = "com.example.hellojava.VIDEO_NAME";
    public static final String SUB_URI = "com.example.hellojava.SUB_URI";
    public static final String SUB_NAME = "com.example.hellojava.SUB_NAME";
    public static final String AUDIO_URI = "com.example.hellojava.AUDIO_URI";
    public static final String AUDIO_NAME = "com.example.hellojava.AUDIO_NAME";


    private Uri videoUri;
    private String videoName;

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter capAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private ProgressBar progressBar;
    private List<Button> buttons;

    private Uri subUri;
    private String subName;
    private TimedTextObject captions;
    private CaptionAdapterData capData;

    private Uri audioUri;
    private String audioName = "audio.wav"; // hardcoded MIME



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        progressBar = findViewById(R.id.asrProgress);
        progressBar.setVisibility(View.GONE);

        initButtons();


        playerView = findViewById(R.id.playerView);

        // Get Intent that started this activity and extract video uri
        Intent intent = getIntent();
        videoName = intent.getStringExtra(ImportActivity.VIDEO_NAME);
        String vuriString = intent.getStringExtra(ImportActivity.VIDEO_URI);
        videoUri = Uri.parse(vuriString);

        // Initialize subtitle file uri
        subName = "sub.srt";
//        initSubFile(subName, placeholderSubs()); // Pass blank string or ASR-generated-parsed string instead of placeholder
        initSubFile(subName, " ");
        initTTO();

        initRecyclerView();


    }

    private void initButtons() {
        buttons = new ArrayList<>();
        buttons.add((Button)findViewById(R.id.previewButton));
        buttons.add((Button)findViewById(R.id.addCaption));
        buttons.add((Button)findViewById(R.id.genCaps));
    }

    public void toPreviewActivity(View view) {

        updateSubFile();
        releasePlayer();

        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(VIDEO_URI, videoUri.toString());
        intent.putExtra(VIDEO_NAME, videoName);
        intent.putExtra(SUB_URI, subUri.toString());
        intent.putExtra(SUB_NAME, subName);
        startActivity(intent);
    }

    public void addCaption(View view) {
        int t_start = (int)player.getCurrentPosition();
        capData.addNewCaption(t_start);
        capAdapter.notifyDataSetChanged();
    }

    public void genCaptions(View view) {

        createAudioFile();

        Log.d("asrdebug", "starting mobileffmpeg");
        FFmpegConverter.videoToAudio(videoUri, audioUri);




        Log.d("asrdebug","creating ASRGoogle");
        ASRGoogle asr = new ASRGoogle(this, audioUri, audioName);
        new asrTask(asr).execute();

    }


    /*------------------------  BEGIN PLAYER FUNCTIONS ------------------------*/


    private void initializePlayer() {

        /* Create new ExoPlayer and bind to PlayerView */
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        /* Add media source */
        MediaSource videoSource = buildMediaSource(videoUri);

        /* Initialize player with state information */
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare(videoSource, false, false);

    }

    private void releasePlayer() {
        if(player != null) {
            /* Store state to resume playeback after destroying player */
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "hello-java");
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        hideSystemUi();
        if(Util.SDK_INT < 24 || player == null) {
            initializePlayer();
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }




    /*------------------------  BEGIN EDITOR FUNCTIONS ------------------------*/


    private void initRecyclerView() {
        // Bind view
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true); // performance improvement

        // Set layout
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Bind adapter with data
//        capAdapter = new CaptionAdapter(ttoToData(captions));
        capData = new CaptionAdapterData(captions);
        capAdapter = new CaptionAdapter(capData);
        recyclerView.setAdapter(capAdapter);
    }

    // Create new file named filename containing initText and store its uri
    private void initSubFile(String filename, String initText) {
        File file = new File(getApplicationContext().getFilesDir(), filename);

        // TEMPORARY. initialize file w placeholder text sdfdsfgsdfhfhg
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext()
                    .openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(initText);
            outputStreamWriter.close();
        }
        catch (Exception e) {
            Log.d("uh", "initSubFile: "+ e.toString());
        }

        subUri = Uri.fromFile(file);
    }

    // Initialize TimedTextObject. subUri must be initialized first
    private void initTTO() {
        FormatSRT formatter = new FormatSRT();
        InputStream is = uriToIS(subUri);
        TimedTextObject tto;
        try {
            tto = formatter.parseFile(subName, is);
            captions = tto;
        }
        catch (Exception e) {
            Log.d("uh", e.toString());
        }
    }

    // InputStream from Uri. Caller must call InputStream.close()
    private InputStream uriToIS(Uri uriSrc) {
        ContentResolver cr = getContentResolver();
        try {
            return cr.openInputStream(uriSrc);
        }
        catch(Exception e) {
            Log.d("uh",e.toString());
        }
        return null;
    }


    // Update .srt file pointed to by subUri with content from capData/tto
    private void updateSubFile() {
        FormatSRT formatter = new FormatSRT();
        String[] lines = formatter.toFile(captions);

        try {
            // OutputStreamWriter automatically clears file before writing
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext()
                    .openFileOutput(subName, Context.MODE_PRIVATE));
            for(String line : lines) {
                outputStreamWriter.write(line);
                outputStreamWriter.write('\n');
            }
            outputStreamWriter.close();
        }
        catch (Exception e) {
            Log.d("uh", "updateSubFile: "+ e.toString());
        }
    }


    private void createAudioFile() {
        File audioFile = new File(getApplicationContext().getFilesDir(), audioName);
        audioUri = Uri.fromFile(audioFile);

    }






























    /*  ------------------------ ASYNCTASK STUFF ------------------------ */

    private class asrTask extends AsyncTask<Void, Integer, List<Caption>> {

        private ASRtoCaption asr;

        public asrTask(ASRtoCaption asr) {
            this.asr = asr;
        }

        protected void onPreExecute() {
            disableUI();
        }

        protected List<Caption> doInBackground(Void... params) {

            return asr.getCaptions();
        }

        protected void onProgressUpdate(Integer... progress) {
//            outputText.append("Progress: "+progress[0]);


        }

        protected void onPostExecute(List<Caption> result) {
//            printResults(result);
            addCaptions(result);
            enableUI();
        }

        /* ----------- */

        private void addCaptions(List<Caption> captions) {
            for(Caption caption : captions) {
                capData.addCaption(caption);
            }
            capAdapter.notifyDataSetChanged();
        }

        private void disableUI() {
            progressBar.setVisibility(View.VISIBLE);
            for(Button button : buttons) {
                button.setEnabled(false);
            }

        }
        private void enableUI() {
            progressBar.setVisibility(View.GONE);
            for(Button button : buttons) {
                button.setEnabled(true);
            }
        }
    }






























    // Returns a string representing contents of a placeholder srt file
    private String placeholderSubs() {
        return "1\n" +
                "00:00:00,220 --> 00:00:01,215\n" +
                "First Text Example!!\n" +
                "\n" +
                "2\n" +
                "00:00:03,148 --> 00:00:05,053\n" +
                "Second Text Example!!\n" +
                "\n" +
                "3\n" +
                "00:00:08,004 --> 00:00:09,884\n" +
                "Third Text Example!!\n" +
                "\n" +
                "4\n" +
                "00:00:11,300 --> 00:00:12,900\n" +
                "Fourth Text Example!!\n" +
                "\n" +
                "5\n" +
                "00:00:15,500 --> 00:00:16,700\n" +
                "Fifth Text Example!!\n" +
                "\n" +
                "6\n" +
                "00:00:18,434 --> 00:00:20,434\n" +
                "Sixth Text Example!!\n" +
                "\n" +
                "7\n" +
                "00:00:22,600 --> 00:00:23,700\n" +
                "Last Text Example";
    }





}

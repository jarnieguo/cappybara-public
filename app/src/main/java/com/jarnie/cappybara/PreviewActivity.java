package com.jarnie.cappybara;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

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

public class PreviewActivity extends AppCompatActivity {


    public static final String VIDEO_URI = "com.example.hellojava.VIDEO_URI";
    public static final String VIDEO_NAME = "com.example.hellojava.VIDEO_NAME";
    public static final String SUB_URI = "com.example.hellojava.SUB_URI";
    public static final String SUB_NAME = "com.example.hellojava.SUB_NAME";



    private Uri videoUri;
    private String videoName;

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    private Uri subUri;
    private String subName;
    private TimedTextObject captions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        playerView = findViewById(R.id.previewPlayer);

        // Get Intent that started this activity and extract uri
        Intent intent = getIntent();
        String vuriString = intent.getStringExtra(EditorActivity.VIDEO_URI);
        videoName = intent.getStringExtra(EditorActivity.VIDEO_NAME);
        String suriString = intent.getStringExtra(EditorActivity.SUB_URI);
        subName = intent.getStringExtra(EditorActivity.SUB_NAME);



        videoUri = Uri.parse(vuriString);
        subUri = Uri.parse(suriString);
    }


    public void toEditorActivity(View view) {
        // TODO
        // need to preserve subtitle state there and keep SRT file on this activity
        // need to pass same values as importactivity?
        releasePlayer();
        onBackPressed();
    }


    public void toExportActivity(View view) {

        releasePlayer();


        Intent intent = new Intent(this, ExportActivity.class);
        intent.putExtra(VIDEO_URI, videoUri.toString());
        intent.putExtra(VIDEO_NAME, videoName);
        intent.putExtra(SUB_URI, subUri.toString());
        intent.putExtra(SUB_NAME, subName);
        startActivity(intent);
    }


    /*------------------------  BEGIN PLAYER FUNCTIONS ------------------------*/


    private void initializePlayer() {

        /* Create new ExoPlayer and bind to PlayerView */
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        /* Add media source */

        // TODO : import/create subtitle file instead of whatever im doing
        MediaSource videoSource = buildMergedSource(videoUri, subUri);


        /* Initialize player with state information */
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare(videoSource, false, false);
//        player.prepare(mergedSource, false, false);

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

    private MediaSource buildMergedSource(Uri videoUri, Uri subtitleUri) {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "hello-java");

        // Create video source
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).
                createMediaSource(videoUri);

        // Create subtitle source
        Format subtitleFormat = Format.createTextSampleFormat(
                null, MimeTypes.APPLICATION_SUBRIP, C.SELECTION_FLAG_DEFAULT, null);
        MediaSource subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory).
                createMediaSource(subtitleUri, subtitleFormat, C.TIME_UNSET);

        return new MergingMediaSource(videoSource, subtitleSource);
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
        hideSystemUi();
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




}

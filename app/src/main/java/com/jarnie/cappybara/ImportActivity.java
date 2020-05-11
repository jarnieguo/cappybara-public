package com.jarnie.cappybara;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ImportActivity extends AppCompatActivity {

    public static final String VIDEO_URI = "com.example.hellojava.VIDEO_URI";
    public static final String VIDEO_NAME = "com.example.hellojava.VIDEO_NAME";
//    public static final String VIDEO_NAME = "video";

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int REQUEST_FILE_SELECT = 1;

    private String videoName = "video";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        showFilesDir();

    }

    private void showFilesDir() {
//        TextView fileList = findViewById(R.id.fileList);

        String[] files = getApplicationContext().fileList();
        for(String f : files) {
//            fileList.append("\n"+f);
            Log.d("uh", "filesDir: "+f);
        }
    }


    public void captureVideo(View view) {
        dispatchTakeVideoIntent();
    }

    public void chooseFile(View view) {
        dispatchChooseFileIntent();
    }

    private void dispatchChooseFileIntent() {
        Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFileIntent.setType("video/*");
        if(chooseFileIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooseFileIntent, REQUEST_FILE_SELECT);
        }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if(takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();

//            TextView textView = findViewById(R.id.uriTextView);
//            textView.setText(videoUri.toString());
            Log.d("uh", "ImportActivity file Uri: "+videoUri.toString());

            startEditorActivity(
                    saveFile(videoUri, videoName)
            );
//            startEditorActivity(videoUri);
        }
    }

    private void startEditorActivity(Uri videoUri) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(VIDEO_URI, videoUri.toString());
        intent.putExtra(VIDEO_NAME, videoName);
        startActivity(intent);
    }

    // TODO
    private String mimeToExtension(String mime) {
        if(mime.equals("video/mp4"))
            return ".mp4";
        return "";
    }

    /* Copy file from uriSource to app external storage(?) with filename. Adds extension
     *  automatically. Returns a new URI to the copied file. */
    private Uri saveFile(Uri uriSource, String filename) {
        try {
            ContentResolver cr = getContentResolver();

            String mime = cr.getType(uriSource); // Only works for content uris
            videoName = filename + mimeToExtension(mime);
            File file = new File(getApplicationContext().getFilesDir(), videoName);

            InputStream in = cr.openInputStream(uriSource);
            OutputStream out = new FileOutputStream(file);

            // Copy bytes
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) > 0) {
                out.write(buf,0,len);
            }
            out.close();
            in.close();


            return Uri.fromFile(file);

        }
        catch (Exception e) {
            // handle
            Log.d("uh", e.toString());
        }
        Log.d("uh", "hgjuygkuygkuyhjKKLMbkjh");
        return uriSource;
    }
}

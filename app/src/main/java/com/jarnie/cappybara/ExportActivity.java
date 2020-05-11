package com.jarnie.cappybara;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ExportActivity extends AppCompatActivity {

    static final int REQUEST_CREATE_VIDEO = 1;
    static final int REQUEST_CREATE_SUBFILE = 2;


    // App storage uris
    private Uri videoUri;
    private Uri subUri;

    private String videoName;
    private String subName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);


        // Get Intent that started this activity and extract uri
        Intent intent = getIntent();
        String vuriString = intent.getStringExtra(EditorActivity.VIDEO_URI);
        videoName = intent.getStringExtra(EditorActivity.VIDEO_NAME);
        String suriString = intent.getStringExtra(EditorActivity.SUB_URI);
        subName = intent.getStringExtra(EditorActivity.SUB_NAME);

        videoUri = Uri.parse(vuriString);
        subUri = Uri.parse(suriString);

    }

    public void dispatchCreateVidIntent(View view) {
        Intent createDocIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT); // api 19
        createDocIntent.setType("video/*");

        if(createDocIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(createDocIntent, REQUEST_CREATE_VIDEO);
        }
    }

    public void dispatchCreateSubIntent(View view) {
        Intent createDocIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT); // api 19
        createDocIntent.setType("text/srt");

        if(createDocIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(createDocIntent, REQUEST_CREATE_SUBFILE);
        }
    }

    public void dispatchFinishIntent(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CREATE_VIDEO && resultCode == RESULT_OK) {

            Uri newUri = data.getData();

            Uri tempUri = FFmpegConverter.embedSRT(getApplicationContext(), videoUri, subUri);
            copyFile(tempUri, newUri);

        }
        else if(requestCode == REQUEST_CREATE_SUBFILE && resultCode == RESULT_OK) {

            Uri newUri = data.getData();
            Uri temp = copyFile(subUri, newUri);
            Log.d("uh", "returned from copyFile: "+temp.toString());
        }
    }



    /* Copy file from uriSource to app external storage(?) with filename. Adds extension
     *  automatically. Returns a new URI to the copied file. */
    private Uri copyFile(Uri src, Uri dst) {
        try {
            ContentResolver cr = getContentResolver();

            ParcelFileDescriptor fd = cr.openFileDescriptor(dst, "w");

            InputStream in = cr.openInputStream(src);
            OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd);

            // Copy bytes
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) > 0) {
                out.write(buf,0,len);
            }
            out.close();
            in.close();

            Log.d("uh", "reached end of copyFile");

            return dst;
//            return Uri.fromFile(file);

        }
        catch (Exception e) {
            // handle
            Log.d("uh", e.toString());
        }
        return src;
    }




    private void setOldUri(String uri) {
        TextView oldText = findViewById(R.id.oldUri);
        oldText.setText(uri);
    }

    private void setNewUri(String uri) {
        TextView newText = findViewById(R.id.newUri);
        newText.setText(uri);
    }
}

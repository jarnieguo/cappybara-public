package com.jarnie.cappybara;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;

public class FFmpegConverter {

    /*  ------------------------ FFMPEG STUFF ------------------------ */


    public static void videoToAudio(Uri vuri, Uri auri) {

        String rawCommand = "-y -i "+vuri.toString()+" -f wav -ac 1 "+auri.toString();
        exec(rawCommand);

    }

    public static Uri embedSRT(Context context, Uri vuri, Uri suri) {

        String tempFileName = "temp.mp4";
        File file = new File(context.getFilesDir(), tempFileName);
        if(file.delete()) {
            Log.d("uh", "Existing temp video file deleted");
        }
        try {
            file.createNewFile();
        } catch(Exception e) {
            Log.d("uh", "failed create new file in embedSRT: "+e);
        }
        Uri tempUri = Uri.fromFile(file);


        String rawCommand =
                "-y -i " + vuri.toString() +" -i " + suri.toString() + " -c copy -c:s mov_text "+tempUri.toString();

        exec(rawCommand);
        return tempUri;
    }


    private static void exec(String cmd) {
        int rc = FFmpeg.execute(cmd);
        if(rc == Config.RETURN_CODE_SUCCESS) {
            Log.d("uh", "mobileffmpeg success: "+cmd);
        }
        else if(rc == Config.RETURN_CODE_CANCEL) {
            Log.d("uh", "mobileffmpeg command cancelled");
        }
        else {
            Log.d("uh", "mobileffmpeg execution failed on command "+cmd);
            Config.printLastCommandOutput(Log.INFO);
        }
    }

}

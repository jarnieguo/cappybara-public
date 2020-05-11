package com.jarnie.cappybara;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.WordInfo;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.Lists;
import com.google.protobuf.Duration;
import com.jarnie.subtitleFile.Caption;
import com.jarnie.subtitleFile.Time;
import com.jarnie.subtitleFile.TimedTextObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ASRGoogle implements ASRtoCaption<SpeechRecognitionResult>{


    /*
    * TO USE GOOGLE STT WITH YOUR OWN CREDENTIALS:
    * Create project and bucket on cloud storage. Fill in their
    * names in the projectId and bucketName variables.
    * Download project service account key as JSON file and
    * place into the res/raw/ folder as credential.json
    */
    private final String projectId = "Cappybara";
    private final String bucketName = "cappybara-bucket";



    private Context context;
    private Uri audioUri;
    private String audioName;
    private GoogleCredentials credentials;
    private List<SpeechRecognitionResult> asrResults;

    private final int MAX_CHARS = 32;
    private final int MAX_LINES = 2; // DCMP: 2 lines at a time max
    private final int MIN_DURATION = 1000; // ms
    private final int MAX_DURATION = 6000; // ms; DCMP: 6 seconds max



    ASRGoogle(Context ctx, Uri auri, String audioName) {
        this.context = ctx;
        this.audioUri = auri;
        this.audioName = audioName;
    }

    public List<Caption> getCaptions() {
        return generateCaptions(executeASR());
    }



    public List<Caption> generateCaptions(List<SpeechRecognitionResult> results) {
        Log.d("asrdebug", "converting asr results to caption list...");


        List<Caption> captions = new ArrayList<>();

        if(results == null) {
            Log.d("asrdebug", "asr results were null???!!!!");
            return captions;
        }


        // TODO



        Caption caption = new Caption();
        StringBuilder string = new StringBuilder(MAX_CHARS);


        for (SpeechRecognitionResult result : results) {
            // There can be several alternative transcripts for a given chunk of speech. Just use the
            // first (most likely) one here.
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);

            List<WordInfo> words = alternative.getWordsList();
            int n = words.size();
            for(int i = 0; i < n; i++) {

                WordInfo wordInfo = words.get(i);
                int wordLen = wordInfo.getWord().length();

                if(string.length() + wordLen > MAX_CHARS && i < n-1) {
                    // Push current caption
                    caption.content = string.toString();
                    captions.add(caption);
                    Log.d("asrdebug", caption.readableString());

                    // Start new caption
                    caption = new Caption();
                    string = new StringBuilder(MAX_CHARS);
                }

                if(caption.start == null) {
                    int start = durationToMilli(wordInfo.getStartTime());
                    caption.start = new Time(start);
                }

                int end = durationToMilli(wordInfo.getEndTime());
                caption.end = new Time(end);

                string.append(wordInfo.getWord());
                string.append(" ");

                if(i == n-1) { // Push caption and start new one
                    // Push current caption
                    caption.content = string.toString();
                    captions.add(caption);
                    Log.d("asrdebug", caption.readableString());

                    // Start new caption
                    caption = new Caption();
                    string = new StringBuilder(MAX_CHARS);
                }

            }



        }
        return captions;
    }


    private int secToMilli(int seconds) {
        return seconds*1000;
    }
    private int nanoToMilli(int nano) {
        return nano/1000000;
    }
    private int durationToMilli(Duration t) {
        return secToMilli((int)t.getSeconds()) + nanoToMilli(t.getNanos());
    }




    /**
     * Execute: upload to cloud, perform speech recognition
     * **/
    public List<SpeechRecognitionResult> executeASR() {

        setCredentials();

        try {
            uploadObject(projectId, bucketName, audioName, audioUri);
        } catch(Exception e) {
            Log.d("asrdebug","Failed upload audio: "+e);
        }

        String gcsUri = "gs://"+bucketName+"/"+audioName;
//        outputText.append("Attempting to recognize wav\n");
        try {
            asyncRecognizeGcs(gcsUri);
        } catch (Exception e) {
            Log.d("asrdebug", "Failed asyncRecognize in startActivity: "+e);
        }
        return asrResults;
    }







    /*  ------------------------ GOOGLE CLOUD STUFF ------------------------ */

    private void setCredentials() {
        try {

            final InputStream stream = context.getResources().openRawResource(R.raw.credential);
            GoogleCredentials gcredentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

            credentials = gcredentials;
//            outputText.append("Made google credentials\n");
        } catch(Exception e) {
            Log.d("asrdebug", "Failed to make credentials: "+e);
        }

    }

    private void uploadObject(
                              String projectId, String bucketName, String objectName, Uri filePath) throws IOException {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        // The path to your file to upload
        // String filePath = "path/to/your/file"

        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try {
            storage.create(blobInfo, Files.readAllBytes(Paths.get(new URI(filePath.toString()))));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.out.println(
                "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }


    /**
     * Performs non-blocking speech recognition on remote FLAC file and prints the transcription.
     *
     * @param gcsUri the path to the remote LINEAR16 audio file to transcribe.
     */
    private void asyncRecognizeGcs(String gcsUri) throws Exception {

        SpeechSettings speechSettings =
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

        // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
        try (SpeechClient speech = SpeechClient.create(speechSettings)) {

            Log.d("asrdebug", "Created SpeechClient");

            // Configure remote file request for FLAC
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
//                            .setEncoding(AudioEncoding.FLAC)
                            .setLanguageCode("en-US")
//                            .setSampleRateHertz(16000)
                            .setModel("video") // UNCOMMENT this line to use video model ($)
                            .setEnableWordTimeOffsets(true)
                            .setEnableAutomaticPunctuation(true)
                            .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            Log.d("asrdebug", "Created RecognitionConfig and Audio");

            // Use non-blocking call for getting file transcription
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                    speech.longRunningRecognizeAsync(config, audio);
            while (!response.isDone()) {
//                outputText.append("Waiting for response...\n");
                Thread.sleep(10000);
            }

            List<SpeechRecognitionResult> results = response.get().getResultsList();
            asrResults = results;

            Log.d("asrdebug", "Got results list");

        }
    }


    private void printResults(List<SpeechRecognitionResult> results) {
        for (SpeechRecognitionResult result : results) {
            // There can be several alternative transcripts for a given chunk of speech. Just use the
            // first (most likely) one here.
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//            outputText.append("\nTranscription: \n"+ alternative.getTranscript()+"\n");
            for(WordInfo wordInfo : alternative.getWordsList()) {
                long ti = wordInfo.getStartTime().getSeconds();
                long tf = wordInfo.getStartTime().getSeconds();
//                outputText.append(ti+" - "+tf+": "+ wordInfo.getWord()+"\n");
            }
        }
    }





}

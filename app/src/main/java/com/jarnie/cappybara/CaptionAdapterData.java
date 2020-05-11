package com.jarnie.cappybara;

import android.util.Log;

import com.jarnie.subtitleFile.Caption;
import com.jarnie.subtitleFile.Time;
import com.jarnie.subtitleFile.TimedTextObject;

import java.util.ArrayList;
import java.util.Collections;


// Wrapper class for TTO to use with CaptionAdapter
// Allows indexing and add/delete/modify captions
public class CaptionAdapterData {

    private ArrayList<Integer> keys;
    private TimedTextObject tto;




    // Constructor takes a TTO
    public CaptionAdapterData(TimedTextObject tto) {
        this.tto = tto;
        this.keys = new ArrayList<>(tto.captions.keySet());
    }

    // Return the caption at index position
    public Caption captionAt(int index) {
        return tto.captions.get(keys.get(index));
    }

    public int size() {
        // to fix?
        if(keys.size() != tto.captions.size()) {
            Log.d("uh", "arraylist and tto sizes don't match in CaptionAdapterData");
        }

        return keys.size();
    }

    public void addCaption(Caption caption) {

        if(!validateCaption(caption)) {
            // TODO: Handle
            Log.d("uh", "Attempted to add invalid caption");
            return; // ??
        }

        // Add caption to tto
        tto.addCaption(caption);

        // Find position and add caption key to arraylist
        int startTime = caption.start.getMseconds();
        int position = Collections.binarySearch(keys, startTime);
        if(position < 0)
            position = -position - 1;
        keys.add(position, startTime);

    }


    // Generates a new empty caption and adds it to data structure
    public void addNewCaption() {
        addCaption(generateCaption());
    }
    public void addNewCaption(int startMs) {
        addCaption(generateCaption(startMs));
    }

    // Generates a new empty caption that goes at the end of current caption list
    // TODO: this wont work if i start validating caption timestamps (if existing caption lasts til end of video)
    public Caption generateCaption() {

        final int DEFAULT_LENGTH = 1000;
        final String DEFAULT_TEXT = "Caption text";

        // Get last caption info
        Caption lastCap = (tto.captions.size() == 0) ? null : tto.captions.lastEntry().getValue();
        int lastEnd = (lastCap == null) ? 0 : lastCap.end.getMseconds();

        Time start = new Time(lastEnd+1);
        Time end = new Time(lastEnd + DEFAULT_LENGTH);

        Caption cap = new Caption();
        cap.start = start;
        cap.end = end;
        cap.content = DEFAULT_TEXT;
        return cap;
    }

    // Generate new caption of default length, starting at startMs
    public Caption generateCaption(int startMs) {
        final int DEFAULT_LENGTH = 1000;
        final String DEFAULT_TEXT = "Caption text";

        Time start = new Time(startMs);
        Time end = new Time(startMs + DEFAULT_LENGTH);

        Caption cap = new Caption();
        cap.start = start;
        cap.end = end;
        cap.content = DEFAULT_TEXT;
        return cap;
    }


    // Removes and returns the caption at index
    public Caption removeCaption(int index) { // pass index? key?
        // TODO

        int key = keys.get(index);
        Caption caption = tto.captions.get(key);

        // Remove from arraylist and from tto
        keys.remove(index);
        tto.captions.remove(key);

        return caption;
    }

    public void updateCaption(Caption caption) { // pass index? key?
        // TODO
    }

    public void updateCaptionStart(int index, String start) {
        // Get caption pointer and remove caption from data
        Caption cap = removeCaption(index);

        // Update caption
        // TODO: time string validation, probably factor out too
        int ms = Time.stringToMs(start);
        cap.start.setMseconds(ms);

        // Put caption back in
        addCaption(cap);
    }

    public void updateCaptionEnd(int index, String end) {
        // TODO: value check for format validity and timestamp/vidlength validity

        int ms = Time.stringToMs(end);
        captionAt(index).end.setMseconds(ms);
    }

    public void updateCaptionContent(int index, String content) {
        captionAt(index).content = content;
    }



    // Return true if caption is valid (existing or to be added)
    private boolean validateCaption(Caption caption) {
        // TODO
        // Valid fields
        // Valid timestamps
        // No overlap with existing captions in tto
        // No times past end of video (prob need another constructor parameter)

        return true;
    }



}

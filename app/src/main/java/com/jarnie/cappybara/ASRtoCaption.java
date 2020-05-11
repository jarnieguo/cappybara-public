package com.jarnie.cappybara;

import com.jarnie.subtitleFile.Caption;

import java.util.List;

public interface ASRtoCaption<Result> {

    List<Caption> getCaptions();

    List<Caption> generateCaptions(List<Result> results);

    List<Result> executeASR();

}

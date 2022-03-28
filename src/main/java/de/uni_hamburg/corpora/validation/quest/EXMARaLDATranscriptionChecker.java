package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import org.jdom.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class EXMARaLDATranscriptionChecker extends TranscriptionChecker {
    public EXMARaLDATranscriptionChecker(Properties properties) {
        super(properties);
    }

    @Override
    public String getDescription() {
        return "Checker for the transcription in an EXMARaLDA transcription file";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    @Override
    List<Element> getTranscriptionTiers(CorpusData cd) {
        return null;
    }

    @Override
    String getTranscriptionText(Element tier) {
        return null;
    }
}

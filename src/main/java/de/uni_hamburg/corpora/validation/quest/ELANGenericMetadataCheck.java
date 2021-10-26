package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import java.util.Collection;
import java.util.Collections;

public class ELANGenericMetadataCheck extends GenericMetadataChecker implements CorpusFunction {

    public ELANGenericMetadataCheck() {
        super(false);
    }

    public ELANGenericMetadataCheck(boolean hasfixingoption) {
        super(hasfixingoption);
    }


    @Override
    public String getDescription() {
        return "Checks for the generic meta data in an ELAN corpus";
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Valid for ELAN format
        return Collections.singleton(ELANData.class);
    }

    @Override
    protected Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values) {
        return null;
    }
}

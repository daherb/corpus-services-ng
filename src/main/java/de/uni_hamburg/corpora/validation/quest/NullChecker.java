package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

public class NullChecker extends Checker implements CorpusFunction {

    public NullChecker(Properties properties) {
        super(false,properties);
    }

    @Override
    public String getDescription() {
        return "Does nothing and never fails";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) {
        Report stats = new Report();
        // report success
        stats.addCorrect(getFunction(),cd,"Success");
        return stats;
    }

    @Override
    public Report function(Corpus c, Boolean fix) {
        Report stats = new Report();
        for (CorpusData cdata : c.getCorpusData()) {
            stats.merge(execute(cdata));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Use reflections to get all corpus data classes
        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
        // Get all classes derived from CorpusData
        return reflections.getSubTypesOf(CorpusData.class).stream().filter((c) -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
    }


}

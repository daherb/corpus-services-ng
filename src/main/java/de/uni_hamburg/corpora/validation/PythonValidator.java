package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.reflections.Reflections;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PythonValidator extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getFunction());

    String pythonScript;

    public PythonValidator(Properties properties) {
        this(false, properties);
    }

    public PythonValidator(boolean hasfixingoption, Properties properties) {
        super(hasfixingoption, properties);
        if (properties.containsKey("python-script"))
            pythonScript = properties.getProperty("python-script");
    }

    @Override
    public String getDescription() {
        return "Runs a Python script as a checker";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();

        if (pythonScript != null && !pythonScript.isEmpty()) {
            Runtime rt = Runtime.getRuntime();
            File logFile = File.createTempFile("python",".log");
            String command = String.format("python %s %s %s %s %s",pythonScript, cd.getClass().getSimpleName(),
                    Paths.get(cd.getURL().toURI()), null, logFile);
            Process pr = rt.exec(command);
            //logger.info(command);
            try {
                logger.info(command + " " + pr.waitFor());
                // Load Python log and add to report
                report.merge(new Report(getFunction(), Report.load(logFile.toString())));
            }
            catch (InterruptedException e) {
                report.addCritical(getFunction(),e,"Exception when waiting for process");
            }
            logFile.delete();
        }
        else
            report.addCritical(getFunction(),"No Python script given");
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData()) {
            // Check if we can actually run function on file
            if (getIsUsableFor().contains(cd.getClass()))
                report.merge(function(cd,fix));
        }
        report.dump("/tmp/report-dump.json");
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Use reflections to get all corpus data classes
        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
        // Get all classes derived from CorpusData
        return reflections.getSubTypesOf(CorpusData.class).stream().filter((c) -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
    }
}

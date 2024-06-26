package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import mpi.eudico.server.corpora.util.ProcessReport;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

import mpi.eudico.server.corpora.clomimpl.util.EAFValidator ;


/**
 * Using the built-in ELAN file checker in corpus services
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ELANValidatorChecker extends Checker implements CorpusFunction {

    // The local logger that can be used for debugging
    Logger logger = Logger.getLogger(this.getClass().toString());

    public ELANValidatorChecker(Properties properties) {
        super(false, properties) ;
    }

    @Override
    public String getDescription() {
        return "Checker that runs the ELAN validator on an EAF file";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        String fileUri = URLDecoder.decode(String.valueOf(Paths.get(cd.getURL().toURI()).toAbsolutePath().toUri().toURL()), "utf-8");
        // Validate the elan file using the ELAN EAFValidator
        EAFValidator eafValidator = new EAFValidator(fileUri);
        eafValidator.validate();
        ProcessReport eafReport = eafValidator.getReport();
        // TODO: Analyse report to give better feedback
        // Extract the errors and warnings from the validator report
//        Boolean matched = false;
//        for (String line : eafReport.getReportAsString().split("\n+")) {
//            Matcher m = Pattern.compile("Received (\\d+) warnings and (\\d+) errors").matcher(line);
//            if (m.matches()) {
//                matched = true;
//                int warningCount = Integer.parseInt(m.group(1));
//                int errorCount = Integer.parseInt(m.group(2));
//                if (warningCount == 0 && errorCount == 0)
//                    report.addNote(getFunction(), "No errors and warnings");
//                else if (warningCount > 0 && errorCount == 0)
//                    report.addWarning(getFunction(), "Encountered " + warningCount + " warnings and no errors");
//                else if (warningCount == 0 && errorCount > 0)
//                    report.addCritical(getFunction(), "Encountered no warnings and " + errorCount + " errors");
//                else
//                    report.addCritical(getFunction(), "Encountered " + warningCount + " warnings and " + errorCount + " errors");
//            }
//        if (!matched)
//            report.addCritical(getFunction(),
//                    ReportItem.newParamMap(
//                            new String[]{"function", "filename", "description"},
//                            new Object[]{getFunction(), cd.getFilename(), "Error extracting warning and error counts"}));
//        }
        report.addNote(getFunction(),cd,"EAF Validator report:\n" + eafReport.getReportAsString());
        return report;
    }

    /**
     * Checker function for a corpus
     * @param c the corpus
     * @param fix the fixing parameter
     * @return detailed report of the checker
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Apply function for each supported file
        Collection usable = this.getIsUsableFor();
        for (CorpusData cdata : c.getCorpusData()) {
            if (usable.contains(cdata.getClass())) {
                report.merge(function(cdata, fix));
            }
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Only works for ELAN data
        return Collections.singleton(ELANData.class);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Missing file or path to be checked and/or output file");
            System.err.println(args.length);
        }
        else {
            CorpusIO cio = new CorpusIO();
            try {
                Report report = new Report();
                Corpus corpus = new Corpus(cio.read(Paths.get(args[0]).toAbsolutePath().normalize().toUri().toURL(),
                        report));
                ELANValidatorChecker eav = new ELANValidatorChecker(new Properties());
                report.merge(eav.function(corpus,false));
                BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
                bw.write(ReportItem.generateHTML(report.getRawStatistics()));
                bw.close();
            } catch (URISyntaxException | IOException | SAXException | JexmaraldaException | ClassNotFoundException | XPathExpressionException | NoSuchAlgorithmException | ParserConfigurationException | JDOMException | FSMException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }
}

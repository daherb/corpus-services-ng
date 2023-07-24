/**
 * @file ExbErrorChecker.java
 *
 * A command-line tool / non-graphical interface
 * for checking errors in exmaralda's EXB files.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 * A command-line tool for checking EXB files.
 */
public class ExbPatternChecker {

    BasicTranscription bt;
    ValidatorSettings settings;
    List<String> conventions = new ArrayList<String>();
    List<String> problems = new ArrayList<String>();

    final String function = "exb-patterns";

    private void tryLoadBasicTranscription(String filename)
        throws SAXException, JexmaraldaException {
        if (bt == null) {
            bt = new BasicTranscription(filename);
        }
    }

    public Report check(File f) {
        Report stats = new Report();
        try {
            stats = exceptionalCheck(f);
        } catch (ParserConfigurationException pce) {
            stats.addException(pce, "Unknown parser error");
        } catch (SAXException saxe) {
            stats.addException(saxe, "Unknown parser error");
        } catch (IOException ioe) {
            stats.addException(ioe, "Unknown read error");
        }
        return stats;
    }

    public Report exceptionalCheck(File f)
            throws SAXException, IOException, ParserConfigurationException {
        // XXX: get conventions from settings somehow
        List<Pattern> correctPatterns = new ArrayList<Pattern>();
        for (String convention : conventions) {
            correctPatterns.add(Pattern.compile(convention));
        }
        List<Pattern> errorPatterns = new ArrayList<Pattern>();
        for (String problem : problems) {
            errorPatterns.add(Pattern.compile(problem));
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);
        NodeList events = doc.getElementsByTagName("event");
        Report stats = new Report();
        for (int i = 0; i < events.getLength(); i++) {
            Element event = (Element)events.item(i);
            NodeList eventTexts = event.getChildNodes();
            for (int j = 0; j < eventTexts.getLength(); j++) {
                Node maybeText = eventTexts.item(j);
                if (maybeText.getNodeType() != Node.TEXT_NODE) {
                    if (maybeText.getNodeType() == Node.ELEMENT_NODE &&
                            maybeText.getNodeName().equals("ud-information")) {
                        // XXX: ud-information is weird I'll just skip it...
                        continue;
                    }
                    System.err.println("This is not a text node: " +
                            maybeText);
                    continue;
                }
                Text eventText = (Text) maybeText;
                String text = eventText.getWholeText();
                int k = 0;
                for (Pattern pattern : correctPatterns) {
                    Matcher matcher = pattern.matcher(text);
                    if (!matcher.matches()) {
                        stats.addCritical(function,
                                    "Text: " + text + " does not fit to the " +
                                    "conventions given.", "Expression was: " +
                                    conventions.get(k));
                    }
                }
                k = 0;
                for (Pattern pattern : errorPatterns) {
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.matches()) {
                        stats.addCritical(function,
                                    "Text: " + text + " does not fit to the " +
                                    "conventions given.", "Expression was: " +
                                    errorPatterns.get(k));
                    }
                }
            }
        }
        return stats;
    }

    public Report doMain(String[] args) {
        settings = new ValidatorSettings("ExbPatternChecker",
                "Checks Exmaralda .exb file annotations for conventions using " +
                "patterns", "If input is a directory, performs recursive check "
                + "from that directory, otherwise checks input file\n" +
                "Patterns are given as regular expressions to match against " +
                "(regular expression is compiled with java.util.regex)");
        // XXX: the option version is quite useless unless for quick checks
        List<Option> patternOptions = new ArrayList<Option>();
        patternOptions.add(new Option("a", "accept", true, "add an acceptable "
                    + "pattern"));
        patternOptions.add(new Option("d", "disallow", true, "add an illegal "
                    + "pattern"));
        CommandLine cmd = settings.handleCommandLine(args, patternOptions);
        if (cmd == null) {
            System.exit(0);
        }
        if (!cmd.hasOption("accept") && !cmd.hasOption("disallow")) {
            System.err.println("Nothing to accept or disallow, " +
                    "skipping checks.");
            System.exit(0);
        }
        if (cmd.hasOption("accept")) {
            conventions.add(cmd.getOptionValue("accept"));
        }
        if (cmd.hasOption("disallow")) {
            problems.add(cmd.getOptionValue("disallow"));
    }
        if (settings.isVerbose()) {
            System.out.println("Checking exb files for unconventional " +
                    "annotations...");
        }
        Report stats = new Report();
        for (File f : settings.getInputFiles()) {
            if (settings.isVerbose()) {
                System.out.println(" * " + f.getName());
            }
            stats = check(f);
        }
        return stats;
    }

    public static void main(String[] args) {
        ExbPatternChecker checker = new ExbPatternChecker();
        Report stats = checker.doMain(args);
        System.out.println(stats.getSummaryLines());
        System.out.println(stats.getErrorReports());
    }
}

package de.uni_hamburg.corpora.validation.quest;

//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;

import com.opencsv.bean.CsvToBeanBuilder;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20211018
 *
 * Abstract Checker for the generic metadata
 */
abstract class GenericMetadataChecker extends Checker implements CorpusFunction {

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public GenericMetadataChecker(Properties properties) {
        super(false, properties);
    }

    // The local logger that can be used for debugging
    final Logger logger = Logger.getLogger(this.getClass().toString());

    // Flag to see if the checker is properly set up
    boolean setUp = false;

    // Data structure representing all metadata criteria
    List<GenericMetadataCriterion> criteria;

    // Data structure to keep track of all the values in a corpus
    HashMap<String,Set<String>> allValues = new HashMap<>();

    /**
     * Checker function for a single document in a corpus
     * @param cd the corpus file
     * @param fix the fixing option
     * @return a detailed report of the checker
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
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Only work if properly set up
        if (setUp && shouldBeChecked(cd.getURL())) {
            for (GenericMetadataCriterion c : criteria) {
                // Collect all values of properties that match one of the locators
                 ArrayList<String> values = new ArrayList<>();
                for (String locator : c.locator) {
                    // Ignore any property without locator
                    if (!locator.equals("N/A")) {
                        // get the values based on the locator
                        report.merge(getValuesForLocator(cd, locator, values));
                    }
                }
                // Check if we have at least lower bound elements (if lower bounds are defined, i.e. not N/A)
                if (c.bounds.lower != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                        GenericMetadataCriterion.compareToBounds(values.size(), c.bounds.lower) < 0)
                    report.addCritical(getFunction(), cd, "Less than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.lower) + " occurrences of " + c.name + " found: " + values.size());
                // Check if we have at most upper bound elements (if upper bounds are defined, i.e. not N/A)
                if (c.bounds.upper != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                        GenericMetadataCriterion.compareToBounds(values.size(), c.bounds.upper) > 0)
                    report.addCritical(getFunction(), cd, "More than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.upper) + " occurrences of " + c.name + " found: " + values.size());
                // Store all values that have a reasonable type for potential statistics
                if (!c.type.contains(Optional.empty())) {
                    if (allValues.containsKey(c.name)) {
                        allValues.get(c.name).addAll(values);
                    }
                    else {
                        allValues.put(c.name, new HashSet<>(values));
                    }
                }
                // Now check all the results
                for (String value : values) {
                    // Get the value of the node, either from an element or an attribute
                    // DEBUG check the path for a property
//                    if (c.name.equalsIgnoreCase("PublicationYear")) {
//                        String path = getPathForElement((Element) o);
//                        logger.log(Level.INFO, "PublicationYear: " + path);
//                    }
                    // Check if we can parse the value as one of the valid types
                    boolean parsable = false;
                    for (Optional<String> t : c.type) {
                        // No type, we don't have to parse it
                        if (!t.isPresent())
                            parsable = true;
                        else {
                            // String is always valid
                            if (t.get().equalsIgnoreCase("string")) {
                                parsable = true;
                            }
                            // Testing an URI/URL
                            else if (t.get().equalsIgnoreCase("uri")) {
                                try {
                                    // Just check if it is a uri
                                    new URI(value);
                                    parsable = true;
                                } catch (URISyntaxException e) {
                                    // We only want to create a log item if there is no fallback to string
                                    if (!c.type.stream().map((o) -> o.orElse("").toLowerCase(Locale.ROOT))
                                            .collect(Collectors.toSet()).contains("string"))
                                        report.addCritical(getFunction(), cd, "Invalid URI syntax " + e);
                                    // But we skip the rest of this iteration
                                    continue ;
                                }
                                // Also try it as a URL
                                URL url = null;
                                try {
                                    // Handle URIs can start with hdl: and can be resolved using a proxy
                                    if (value.startsWith("hdl:")) {
                                        url = new URL(value.replace("hdl:", "https://hdl.handle.net/"));

                                    }
                                    // DOI URIs can start with hdl: and can be resolved using a proxy
                                    else if (value.startsWith("doi:")) {
                                        url = new URL(value.replace("doi:", "https://doi.org/"));
                                    }
                                    // HTTP URIs are URLs
                                    else if (value.startsWith("http")) {
                                        url = new URL(value);
                                    }
                                } catch (MalformedURLException e) {
                                    // We only want to create a log item if there is no fallback to string
                                    if (!c.type.stream().map((o) -> o.orElse("").toLowerCase(Locale.ROOT))
                                            .collect(Collectors.toSet()).contains("string"))
                                        report.addWarning(getFunction(), cd, "Malformed URL " + e);
                                    // But we skip the rest of this iteration
                                    continue ;
                                }
                                // If we succeed in creating a URL object we can try to connect
                                /*if (url != null) {
                                    try {
                                        TimeUnit.MILLISECONDS.sleep(500);
                                        HttpURLConnection con = ((HttpURLConnection) url.openConnection());
                                        con.setRequestMethod("GET");
                                        int code = con.getResponseCode();
                                        if (code != 200)
                                            report.addWarning(getFunction(), cd, "Error connecting to url " + url + ": " + code);
                                    } catch (IOException | InterruptedException e) {
                                        report.addWarning(getFunction(), cd, "Exception when connecting to url " + e);
                                    }

                                }*/

                            }
                            // Check date
                            else if (t.get().equalsIgnoreCase("date")) {
                                // Try various date formats
                                try {
                                    new SimpleDateFormat("yyyy-MM-dd").parse(value);
                                    parsable = true;
                                } catch (ParseException e) {
                                    parsable = false;
                                }
                                if (!parsable)
                                    try {
                                        new SimpleDateFormat("yyyy-MM").parse(value);
                                        parsable = true;
                                    } catch (ParseException e) {
                                        parsable = false;
                                    }
                                if (!parsable)
                                    try {
                                        new SimpleDateFormat("yyyy").parse(value);
                                        parsable = true;
                                    } catch (ParseException e) {
                                        parsable = false;
                                    }
                            }
                        }
                    }
                    // Error if everything failed
                    if (!parsable) {
                        // Get all the types we attempted, if they don't exist replace them by n/a and join them
                        // using commas
                        String attemptedTypes =
                                String.join(",",
                                        c.type.stream().map((s) -> s.orElse("n/a")).collect(Collectors.toSet()));
                        // Create error message for problematic value. Only warn if it is unknown but create error if unspecified
                        if (value.equals("Unknown"))
                            report.addWarning(getFunction(), cd, "Unexpected value " + value +
                                    " for " + c.name + ". Expected type: " + attemptedTypes);
                        else
                            report.addCritical(getFunction(), cd, "Unexpected value " + value +
                                    " for " + c.name + ". Expected type: " + attemptedTypes);
                    }
                }
            }
        }
        else if (!setUp)
            report.addCritical(getFunction(), cd, "No criteria file loaded");
        return report;
    }

    /**
     * Determines if a file should be checked depending on the filename
     * @param filename the filename
     * @return if the file should be checked
     */
    protected boolean shouldBeChecked(URL filename) {
        return true;
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
        if (setUp) {
            // Apply function for each supported file
            Collection usable = this.getIsUsableFor();
            for (CorpusData cdata : c.getCorpusData()) {
                if (usable.contains(cdata.getClass())) {
                    report.merge(function(cdata, fix));
                }
            }
            // Add statistocs of the parameter is set
            if (props.containsKey("metadata-stats") && props.getProperty("metadata-stats").equalsIgnoreCase("true")) {
                StringBuilder stats = new StringBuilder();
                for (String cat : allValues.keySet()) {
                    Set<String> vals = allValues.get(cat).stream().filter((v) -> !v.isEmpty() && !v.matches("[\\s\\n]*"))
                            .collect(Collectors.toSet());
                    logger.info(cat + " - " + vals.size());
                    if (!vals.isEmpty()) {
                        stats.append("\n");
                        stats.append(cat);
                        stats.append(":\n - ");
                        stats.append(String.join("\n - ", vals));
//                        vals.stream().filter((v) -> !v.isEmpty() || !v.matches("[\\s\\n]+"))
//                                .collect(Collectors.joining("\n - "))));
                    }
                }
                report.addNote(getFunction(), getFunction() + " summary\n" + stats);
            }
        } else
            report.addCritical(getFunction(), "No criteria file loaded");
        return report;
    }

    /**
     * Function to get a collection of values based on a locator
     *
     * @param locator the locator for the values
     * @return the values determiend by the locator
     */
    protected abstract Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values);

    /**
     * Function to load a criteria file and set up the checker
     * @param filename the criteria as a csv file to be loaded
     */
    public void setCriteriaFile(String filename) {
        try {
            // Read CSV file
            criteria = new CsvToBeanBuilder<GenericMetadataCriterion>(new FileReader(filename))
                    .withType(GenericMetadataCriterion.class)
                    .withSkipLines(1) // skip header
                    .build()
                    .parse();
            setUp = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Encountered exception when loading criteria ", e);
        }
    }
}

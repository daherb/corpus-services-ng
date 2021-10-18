package de.uni_hamburg.corpora.validation;

//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;

import com.opencsv.bean.CsvToBeanBuilder;
import de.uni_hamburg.corpora.*;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210728
 *
 * Abstract Checker for the generic metadata
 */
abstract class GenericMetadataChecker extends Checker implements CorpusFunction {

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public GenericMetadataChecker() {
        this(false);
    }

    /**
     * Default constructor with optional fixing option
     * @param hasfixingoption the fixing option
     */
    public GenericMetadataChecker(boolean hasfixingoption) {
        super(hasfixingoption);
    }

    // The local logger that can be used for debugging
    final Logger logger = Logger.getLogger(this.getClass().toString());

    // Flag to see if the checker is properly set up
    boolean setUp = false;

    // Data structure representing all metadata criteria
    List<GenericMetadataCriterion> criteria;

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
        if (setUp) {
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
                    report.addCritical(function, cd, "Less than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.lower) + " occurrences of " + c.name + " found: " + values.size());
                // Check if we have at most upper bound elements (if upper bounds are defined, i.e. not N/A)
                if (c.bounds.upper != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                        GenericMetadataCriterion.compareToBounds(values.size(), c.bounds.upper) > 0)
                    report.addCritical(function, cd, "More than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.upper) + " occurrences of " + c.name + " found: " + values.size());
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
                                        report.addCritical(function, cd, "Invalid URI syntax " + e);
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
                                        report.addWarning(function, cd, "Malformed URL " + e);
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
                                            report.addWarning(function, cd, "Error connecting to url " + url + ": " + code);
                                    } catch (IOException | InterruptedException e) {
                                        report.addWarning(function, cd, "Exception when connecting to url " + e);
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
                        // Function to get either an optional string value or "n/a"
                        Function<Optional<String>,String> getPresent = ((t) -> t.isPresent() ? t.get() : "n/a");
                        // Get all the types we attempted
                        String attemptedTypes = getPresent.apply(c.type.stream().map(getPresent).reduce((s1,s2) -> s1 + "," + s2));
                        // Create error message for problematic value. Only warn if it is unknown but create error if unspecified
                        if (value.equals("Unknown"))
                            report.addWarning(function, cd, "Unexpected value " + value +
                                    " for " + c.name + ". Expected type: " + attemptedTypes);
                        else
                            report.addCritical(function, cd, "Unexpected value " + value +
                                    " for " + c.name + ". Expected type: " + attemptedTypes);
                    }
                }
            }
        } else
            report.addCritical(function, cd, "No criteria file loaded");
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
        if (setUp) {
            // Apply function for each supported file
            Collection usable = this.getIsUsableFor();
            for (CorpusData cdata : c.getCorpusData()) {
                if (usable.contains(cdata.getClass())) {
                    report.merge(function(cdata, fix));
                }
            }
        } else
            report.addCritical(function, "No criteria file loaded");
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
            // DEBUG
            //logger.log(Level.INFO, "Filename: " + filename);
            //logger.log(Level.INFO, "Reading CSV " + filename);
            // Read CSV file
            criteria = new CsvToBeanBuilder<GenericMetadataCriterion>(new FileReader(filename))
                    .withType(GenericMetadataCriterion.class)
                    .withSkipLines(1) // skip header
                    .build()
                    .parse();
            // DEBUG write criteria to log
            /*for (GenericMetadataCriterion c : criteria) {
                logger.log(Level.INFO,"Name: " + c.name + ", Bounds: " + c.bounds.lower + " - " + c.bounds.upper +
                        ", Type: " + c.type + ", XPath: " + c.locator);
            }*/
            // DEBUG Write criteria to json file
            /*ObjectMapper mapper = new ObjectMapper();
            // Allows serialization even when getters are missing
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            try {
                FileWriter fw = new FileWriter("/tmp/imdi.json");
                fw.write(mapper.writeValueAsString(criteria));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            // Checker is properly set up
            setUp = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Encountered exception when loading criteria ", e);
        }
    }
}

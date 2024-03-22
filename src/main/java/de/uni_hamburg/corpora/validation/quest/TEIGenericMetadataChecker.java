package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.*;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20211007
 *
 * Checker for the generic metadata using TEI headers
 */
public class TEIGenericMetadataChecker extends GenericMetadataChecker implements CorpusFunction {

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public TEIGenericMetadataChecker(Properties properties) throws FileNotFoundException {
        super(properties);
        if (properties != null && !properties.isEmpty() && properties.containsKey("tei-criteria-file"))
            setCriteriaFile(properties.getProperty("tei-criteria-file"));
        else {
            loadCriteriaResource("tei-generic.csv");
        }
    }

    /**
     * Function providing a description of a checker
     * @return the checker description
     */
    @Override
    public String getDescription() {
        return "Checks the generic metadata in an TEI corpus";
    }

    /**
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Valid for TEI format (and XML)
        return Arrays.asList(new Class[]{TEIData.class,UnspecifiedXMLData.class});
    }

//    @Override
//    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
//        Report report = new Report();
//        report.addNote(function,"Checking " + cd.getFilename());
//        report.merge(super.function(cd,fix));
//        return report;
//    }
    /**
     * Function to get a collection of values based on a locator
     *
     * @param locator the locator for the values
     * @return the values determined by the locator
     */
    @Override
    protected Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values) {
        Report report = new Report();
        // Workaround for default namespace "" kind of following
        // http://www.edankert.com/defaultnamespaces.html
        XPathBuilder<Object> builder = new XPathBuilder<Object>(locator,Filters.fpassthrough());
        builder.setNamespace("tei", "http://www.tei-c.org/ns/1.0");
        XPathExpression<Object> xpath = builder.compileWith(new JaxenXPathFactory());
        List<Object> nodes = xpath.evaluate(((TEIData) cd).getJdom());
        xpath = null;
        // Convert nodes to string values
        for (Object o : nodes) {
            // Get the value of the node, either from an element or an attribute
            if (o instanceof Element)
                values.add(((Element) o).getValue());
            else if (o instanceof Attribute)
                values.add(((Attribute) o).getValue());
                // Result of a XPath predicate -> only keep if the result is true
                // This allows e.g. predicates where the counts of elements can be compared
            else if (o instanceof Boolean) {
                if ((Boolean) o)
                    values.add(((Boolean) o).toString());
            }
            else {
                // Error if it is neither element nor attribute
                report.addCritical(getFunction(), cd, "Unexpected object type: " + o.getClass().getName());
                break;
            }
        }
        return report ;

    }

    /**
     * Function to get the path for an element within an XML document
     * @param e the element
     * @return the path as a string
     */
    public static String getPathForElement(Element e) {
        LinkedList<String> path = new LinkedList<>();
        Element cur = e;
        while (!cur.equals(cur.getDocument().getRootElement())) {
            path.addFirst(cur.getName());
            cur = cur.getParentElement();
        }
        Optional<String> optPath = path.stream().reduce((s1, s2) -> s1 + "/" + s2);
        return optPath.orElse("") ;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> p = super.getParameters();
        p.put("tei-criteria-file", "The file for TEI generic metadata criteria");
        return p;
    }
}

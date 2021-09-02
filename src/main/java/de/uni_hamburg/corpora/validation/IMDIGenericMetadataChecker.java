package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import java.util.*;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210728
 *
 * Checker for the generic metadata within an IMDI corpus
 */
public class IMDIGenericMetadataChecker extends GenericMetadataChecker implements CorpusFunction {

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public IMDIGenericMetadataChecker() {
        this(false);
    }

    /**
     * Default constructor with optional fixing option
     * @param hasfixingoption the fixing option
     */
    public IMDIGenericMetadataChecker(boolean hasfixingoption) {
        super(hasfixingoption);
    }

    /**
     * Function providing a description of a checker
     * @return the checker description
     */
    @Override
    public String getDescription() {
        return "Checks the generic metadata in an IMDI corpus";
    }

    /**
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     * @throws ClassNotFoundException inherited
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        // Valid for IMDI format
        return Collections.singleton(IMDIData.class);
    }

    /**
     * Function to get a collection of values based on a locator
     *
     * @param locator the locator for the values
     * @return the values determiend by the locator
     */
    @Override
    protected Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values) {
        Report report = new Report();
        // Workaround for default namespace "" kind of following
        // http://www.edankert.com/defaultnamespaces.html
        try {
            XPath xpath = XPath.newInstance(locator);
            xpath.addNamespace(Namespace.getNamespace("imdi", "http://www.mpi.nl/IMDI/Schema/IMDI"));
            List<Object> nodes = xpath.selectNodes(((IMDIData) cd).getJdom());
            // Convert nodes to string values
            for (Object o : nodes) {
                // Get the value of the node, either from an element or an attribute
                if (o instanceof Element)
                    values.add(((Element) o).getValue());
                else if (o instanceof Attribute)
                    values.add(((Attribute) o).getValue());
                else {
                    // Error if it is neither element nor attribute
                    report.addCritical(function, cd, "Unexpected object type: " + o.getClass().getName());
                    break;
                }
            }
        }
        catch (JDOMException e) {
            e.printStackTrace();
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
}

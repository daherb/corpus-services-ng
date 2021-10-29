package de.uni_hamburg.corpora.validation.quest;

import com.helger.collection.pair.Pair;
import de.uni_hamburg.corpora.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210728
 *
 * Checker for the generic metadata within a CHILDES/TalkBank corpus
 */
public class ChildesGenericMetadataChecker extends GenericMetadataChecker implements CorpusFunction {

    public ChildesGenericMetadataChecker(Properties properties) {
        super(properties);
    }


    @Override
    public String getDescription() {
        return "Checks the generic metadata in a Childes corpus";
    }

//    @Override
//    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
//        HashSet<String> metadataKeys = ((ChildesMetadata) cd).getMetadataKeys();
//        Report stats = new Report();
//        // The requirements are defined in an ambiguous way, either as a conjunction or disjunction of alternatives
//        // We assume it is a disjunction at the moment
//        String[][] requiredKeys = {
//                // Class generic basic
//                {"CMDI_PID", "Identifier"},           // Property: identifier - REQUIRED - 1..
//                {"Title"},                            // Property: title - REQUIRED - 1..
//                {"Description"},                      // Property: description - REQUIRED - 1..
//                // Not applicable                     // Property: version - OPTIONAL - 0..1
//                // Not applicable                     // Property: keywords - RECOMMENDED - 0..1
//                {"Rights"},                           // Property: license - REQUIRED - 1
//                // Not applicable                     // Property: rightsHolder - RECOMMENDED - 0..1
//                {"IMDI_AccessAvailability"},          // Property: accessRights - OPTIONAL - 0..1
//                {"Date"},                             // Property: publicationYear - REQUIRED - 1
//                {"Publisher"},                        // Property: publisher - REQUIRED - 1
//                {"Creator"},                          // Property: creator - REQUIRED - 1..
//                // Class person
//                // Not applicable                     // Property: name - OPTIONAL - 0..1
//                // Not applicable                     // Property: familyName - REQUIRED - 1..1
//                // Not applicable                     // Property: givenName - RECOMMENDED - 0..1
//                // Not applicable                     // Property: identifier - REQUIRED - 1..
//                // Not applicable                     // Property: affiliation - REQUIRED/RECOMMENDED/OPTIONAL - 0..1/1..
//                // Not applicable                     // Property: email - RECOMMENDED (REQUIRED for rightsholder) - 0..
//                // Class organization
//                // Not applicable                     // Property: name - REQUIRED - 1
//                // Not applicable                     // Property: identifier - RECOMMENDED - 0..
//                // Not applicable                     // Property: url - RECOMMENDED - 0..1
//                // Not applicable                     // Property: email - RECOMMENDED (REQUIRED for rightsholder) - 0../1..
//                // Class generic extended
//                {"Relation"},                         // Property: sameAs - OPTIONAL - 0..
//                {"Relation"},                         // Property: isPartOf - OPTIONAL - 0..
//                {"Relation"},                         // Property: hasPart - OPTIONAL - 0..
//                {"Relation"},                         // Property: isBasedOn - OPTIONAL - 0..
//                // Class language data
//                {"Subject.olac:language", "Language"},// Property: objectLanguage - REQUIRED - 1..
//                {"Type.olac:linguistic-type"},        // Property: linguisticDataType - RECOMMENDED - 0..
//                {"IMDI_Modalities"},                  // Property: modality - RECOMMENDED - 0..
//                // Class language
//                {"Language"},                         // Property: name - REQUIRED - 1
//                // Not applicable                     // Property: preferredLabel - RECOMMENDED - 0..1
//                {"Subject.olac:language"}             // Property: identifier - REQUIRED 1..
//                // Class AV data
//                // Not yet specified
//
//        } ;
//        // For all the properties get the keys
//        for (String[] keys : requiredKeys) {
//            // for each of the properties check if one of the keys matches
//            boolean matched = Arrays.stream(keys).reduce(false,(v,k) -> v || metadataKeys.contains(k), (a,b) -> a || b);
//            if (!matched)
//                stats.addWarning(this.getClass().getName(), "One of these metadata elements is missing: " + Arrays.stream(keys).reduce("",(x,y) -> x + ", " + y));
//        }
//        return stats;
//        }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Only valid for CHILDES metadata format
        return Collections.singleton(ChildesMetadata.class);
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
        ChildesMetadata md = (ChildesMetadata) cd;
        // Find all metadata fields matching the locator
        for (Pair<String,String> kv : md.getMetadata()) {
            if (kv != null && kv.hasFirst() && kv.getFirst() != null &&
                    kv.getFirst().equalsIgnoreCase(locator)) {
                    values.add(kv.getSecond());
            }
        }
        return report ;
    }
}

package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import de.uni_hamburg.corpora.utilities.TypeConverter;

import java.util.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.Tier;
import org.xml.sax.SAXException;

/**
 * This class creates a sort- and filterable html overview in table form of all
 * tiers existing in the exbs linked in the coma file to make error " checking
 * and harmonizing easier.
 */
public class ComaTierOverviewCreator extends Checker implements CorpusFunction {

    String comaLoc = "";

    public ComaTierOverviewCreator(Properties properties) {
        //no fixing available
        super(false,properties);
    }

    /**
     * Main functionality of the feature; checks the coma file whether or not
     * there are more than one segmentation algorithms used in the corpus.
     * Issues warnings and returns report which is composed of errors.
     */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException, JexmaraldaException, ClassNotFoundException {
        Report stats = new Report();
        ComaData ccd = (ComaData) cd;
        CorpusIO cio = new CorpusIO();
        Collection<URL> resulturls;
        ArrayList<Tier> tiers = new ArrayList<>();
        ArrayList<EXMARaLDATranscriptionData> btds = new ArrayList<>();
        String htmltemplate = TypeConverter.InputStream2String(getClass().getResourceAsStream("/xsl/tier_overview_datatable_template.html"));
        String overviewTable = "";
        String communicationsTable = "";
        resulturls = ccd.getAllBasicTranscriptionURLs();
        for (URL resulturl : resulturls) {
            CorpusData cdexb = cio.readFileURL(resulturl);
            if (cdexb!=null) {
            EXMARaLDATranscriptionData btexb = (EXMARaLDATranscriptionData) cdexb;

            btds.add(btexb);
            Tier t;
            for (int i = 0; i < btexb.getEXMARaLDAbt().getBody().getNumberOfTiers(); i++) {
                t = btexb.getEXMARaLDAbt().getBody().getTierAt(i);
                tiers.add(t);
            }
            } else {
               stats.addCritical(function, cd, "The linked basic transcription " +  resulturl + " cannot be opened."); 
            }
        }
        List<String> stringtiers = new ArrayList<String>();
        for (Tier tier : tiers) {
            //stringtiers.add(tier.getCategory() + "-" + tier.getType() + "-" + tier.getDisplayName());
            stringtiers.add(tier.getCategory() + " (type: " + tier.getType() + ")");
        }
        Set<String> hash_Set = new TreeSet<String>(stringtiers);
        //System.out.println(tiers);
        //now we have all the existing tiers from the exbs, we need to make a table out of it
        //use the html template and add the content into id
        if (!tiers.isEmpty()) {
            // get the HTML stylesheet

            String h1 = "<h1> Tier Overview over Whole Corpus (" + resulturls.size() + " exbs) </h1>";
            String header = "<table id=\"\" class=\"compact\">\n"
                    + "   <thead>\n"
                    + "      <tr>\n"
                    + "         <th class=\"compact\">Category-Type-DisplayName</th>\n"
                    + "         <th class=\"compact\">Number of Tiers</th>\n"
                    + "      </tr>\n"
                    + "   </thead>\n"
                    + "   <tbody>\n";
            /* for (Tier tier : tiers) {
                //stringtiers.add(tier.getCategory() + "-" + tier.getType() + "-" + tier.getDisplayName());
                stringtiers.add(tier.getCategory() + "-" + tier.getType());
            } */
            // add the tables to the html
            //first table: one column with categories, one with count
            // add the overviewTable to the html
            //first table: one column with categories, one with count
            String content = "";

            for (String s : hash_Set) {
                content = content + "<tr><td class=\"compact\">" + s + "</td><td class=\"compact\">" + Collections.frequency(stringtiers, s) + "</td></tr>";
            }
            String footer = "   </tbody>\n"
                    + "</table>";

            overviewTable = h1 + header + content + footer;

        } else {
            stats.addWarning(function, cd, "No tiers found in the linked exbs. ");
        }
        //now each exb linked in the coma file
        //TODO
        if (!btds.isEmpty()) {
            String h1 = "<h1> Tiers in each exb </h1>";
            communicationsTable = h1;
            //first is the column for filename, then all the tier category/type combinations
            String header = "<table id=\"\" class=\"compact\">\n"
                    + "   <thead>\n"
                    + "<th class=\"compact\"> Exb Filename </th>";
            for (String s : hash_Set) {
                header = header + "<th class=\"compact\">" + s + "</th>";
            }
            header = header + "</tr>"
                    + "   </thead>\n"
                    + "   <tbody>\n";
            String content = "";
            for (EXMARaLDATranscriptionData btd : btds) {
                //first is the column for filename, then all the tier category/type combinations
                content = content + "<tr><td class=\"compact\">" + btd.getFilename() + "</td>";
                for (String s : hash_Set) {
                    //TO DO
                    String[] catType = s.split("type: ");
                    String category = catType[0].substring(0, catType[0].length() - 2);
                    String type = catType[1].substring(0, catType[1].length() - 1);
                    String[] ids = btd.getEXMARaLDAbt().getBody().getTiersOfType(type);
                    int noOfEvents = 0;
                    boolean existence = false;
                    if (ids.length > 0) {
                        for (String id : ids) {
                            if (category.equals(btd.getEXMARaLDAbt().getBody().getTierWithID(id).getCategory())) {
                                noOfEvents += btd.getEXMARaLDAbt().getBody().getTierWithID(id).getNumberOfEvents();
                                existence = true;
                            }
                        }
                        if (existence) {
                            if (noOfEvents > 0) {
                                content = content + "<td class=\"compact\">" + noOfEvents + "</td>";
                            } else {
                                content = content + "<td class=\"compact\">0</td>";
                            }
                        } else {
                            content = content + "<td class=\"compact\"></td>";
                        }
                    } else {
                        content = content + "<td class=\"compact\"></td>";
                    }
                }
                content = content + "</tr>";
            }

            String footer = " </tr>\n"
                    + "   </tbody>\n"
                    + "</table>";
            communicationsTable = h1 + header + content + footer;
        } else {
            stats.addWarning(function, cd, "No linked exbs found in the coma file. ");
        }
        String htmlend = "   </body>\n</html>";
        //add timestamp
        String timestamp = "";       
        timestamp += "   <div id='timestamp'>Generated: ";        
        Timestamp time = new Timestamp(System.currentTimeMillis());
        timestamp += time + "</div>\n";
        String result = htmltemplate + timestamp + overviewTable + communicationsTable + htmlend;
        //String result = htmltemplate + overviewTable;

        URL overviewurl = new URL(cd.getParentURL(), "curation/tier_overview.html");
        cio.write(result, overviewurl);

        stats.addCorrect(function, cd, "created tier overview at " + overviewurl);

        return stats; // return the report with warnings
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ComaData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class creates a sort- and filterable html overview in table form "
                + " of all tiers existing in the exbs linked in the coma file to make error "
                + "checking and harmonizing easier. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, XPathExpressionException, JexmaraldaException, ClassNotFoundException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}

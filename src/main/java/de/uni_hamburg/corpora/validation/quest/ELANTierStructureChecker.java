package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Lists;
import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Tier structure checker for ELAN corpora
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ELANTierStructureChecker extends TierStructureChecker {

    // The list of speakers in the copus
    private final Set<String> speakers = new HashSet<>();
	private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public ELANTierStructureChecker(Properties properties) {
        super(properties);
        // Add speakers given as properties
        if (properties.containsKey("elan-speakers")) {
            speakers.addAll(Arrays.asList(properties.getProperty("elan-speakers").split(",")));
        }
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        if (props.containsKey("refco-file")) {
            RefcoChecker rc = new RefcoChecker(props);
            rc.setRefcoFile(Paths.get(Paths.get(c.getBaseDirectory().toURI()).toString(),props.getProperty("refco-file")).toString());
            speakers.addAll(rc.getDocumentedSpeakers());
        }
        return super.function(c, fix);
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ELANData.class);
    }

    @Override
    Set<Map<String, String>> getTierStructure(Report report, CorpusData cd) {
        Set<Map<String,String>> tiers = new HashSet<>();
        if (cd instanceof ELANData) {
            List<Element> tierElements =
                    Lists.newArrayList(new XPathBuilder<Element>("//TIER",Filters.element()).compileWith(xpathFactory )
                            .evaluate(((ELANData) cd).getJdom()));
            for (Element e : tierElements) {
                Map<String,String> tierAttribs = new HashMap<>();
                {
                    Attribute id = e.getAttribute("TIER_ID");
                    if (id != null) {
                        // if the id contains a speaker name it is replaced by the generic placeholder "speaker"
                        if (speakers.isEmpty())
                            tierAttribs.put("id", id.getValue());
                        else
                            tierAttribs.put("id", id.getValue().replaceAll(
                                    String.join("|", speakers),"speaker"));
                        // Also get constraints
                        Element linguisticTypeElement =
                                new XPathBuilder<Element>("//LINGUISTIC_TYPE[@LINGUISTIC_TYPE_ID=\"" + id.getValue() + "\"]",Filters.element())
                                        .compileWith(xpathFactory).evaluateFirst(((ELANData) cd).getJdom());
                        if (linguisticTypeElement != null) {
                            Attribute constraints = linguisticTypeElement.getAttribute("CONSTRAINTS");
                            if (constraints != null)
                                tierAttribs.put("constraints", constraints.getValue());
                        }
                    }

                }
                {
                    Attribute type = e.getAttribute("LINGUISTIC_TYPE_REF");
                    if (type != null)
                        tierAttribs.put("type", type.getValue());
                }
//                    {
//                    Attribute speaker = e.getAttribute("PARTICIPANT");
//                    if (speaker != null)
//                        tierAttribs.put("speaker", speaker.getValue());
//                    }
                {
                    Attribute parent = e.getAttribute("PARENT_REF");
                    if (parent != null)
                        if (speakers.isEmpty())
                            tierAttribs.put("parent", parent.getValue());
                        else
                            tierAttribs.put("parent", parent.getValue().replaceAll(
                                    String.join("|", speakers),"speaker"));
                }
                tiers.add(tierAttribs);
            }
        }
        else
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                    new Object[]{getFunction(),"Not an ELAN file: " + cd.getFilename()}));
        return tiers;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("elan-speakers","List of speakers used in the ELAN corpus, separated by comma");
        return params;
    }
}

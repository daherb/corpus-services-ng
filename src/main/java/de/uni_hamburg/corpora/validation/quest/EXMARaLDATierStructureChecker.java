package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Lists;
import de.uni_hamburg.corpora.*;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

import java.util.*;

/**
 * Tier structure checker for EXMARaLDA corpora
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class EXMARaLDATierStructureChecker extends TierStructureChecker {

    public EXMARaLDATierStructureChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    @Override
    Set<Map<String, String>> getTierStructure(Report report, CorpusData cd) {
        Set<Map<String,String>> tiers = new HashSet<>();
        if (cd instanceof EXMARaLDATranscriptionData) {
            List<Element> tierElements =
                    Lists.newArrayList(
                            new XPathBuilder<Element>("//tier", Filters.element()).compileWith(new JaxenXPathFactory()).evaluate(((EXMARaLDATranscriptionData) cd).getJdom())
                    );
            for (Element e : tierElements) {
                Map<String,String> tierAttribs = new HashMap<>();
                // Including the idea leads to too many problems
//                    {
//                        Attribute id = e.getAttribute("id");
//                        if (id != null)
//                            tierAttribs.put("id", id.getValue());
//                    }
                {
                    Attribute name = e.getAttribute("name");
                    if (name != null)
                        tierAttribs.put("name", name.getValue());
                }
                {
                    Attribute category = e.getAttribute("category");
                    if (category != null)
                        tierAttribs.put("category", category.getValue());
                }
                {
                    Attribute type = e.getAttribute("type");
                    if (type != null)
                        tierAttribs.put("type", type.getValue());
                }
//                    {
//                    Attribute speaker = e.getAttribute("speaker");
//                    if (speaker != null)
//                        tierAttribs.put("speaker", speaker.getValue());
//                    }
//                    {
//                        Attribute parent = e.getAttribute("PARENT_REF");
//                        if (parent != null)
//                            tierAttribs.put("parent", parent.getValue());
//                    }
                tiers.add(tierAttribs);
            }
        }
        else
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                    new Object[]{getFunction(),"Not an EXMARaLDA EXB file: " + cd.getFilename()}));
        return tiers;
    }
}

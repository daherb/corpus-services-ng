/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.validation.Checker;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class CheckBag extends Checker implements CorpusFunction{

    public CheckBag(boolean hasfixingoption, Properties properties) {
        super(hasfixingoption, properties);
    }

    public CheckBag(Properties properties) {
        super(false, properties);
    }
    public CheckBag() {
        super(false,new Properties());
    }
    
    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Path baseDir = Path.of(c.getBaseDirectory().toURI());
        BagVerifier verifier = new BagVerifier();
        Bag bag;
        
        try {
            bag = new BagReader().read(baseDir);
            if (props.containsKey("fetch-files") && props.getProperty("fetch-files").equalsIgnoreCase("true")) {
                report.merge(fetchFiles(bag));
            }
            report.merge(checkCompleteness(verifier, bag));
            report.merge(checkValidity(verifier, bag));
        } catch (UnparsableVersionException | MaliciousPathException | UnsupportedAlgorithmException | InvalidBagitFileFormatException | InterruptedException   ex) {
            report.addCritical(getFunction(), ex, "Exception while validating bag");
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return new HashSet<>();
    }

    @Override
    public String getDescription() {
        return "Checks a corpus folder if it is a valid BagIt";
    }

    @Override
    public Map<String, String> getParameters() {
        HashMap params = new HashMap();
        params.put("fetch-files", "Optional flag if files from fetch.txt should be fetched before validation");
        return params;
    }

    
    private Report checkValidity(BagVerifier verifier, Bag bag) throws IOException, InterruptedException {
        Report report = new Report();
        try {
            verifier.isValid(bag, false);
            report.addCorrect(getFunction(), "Success: bag is valid");
        }
        catch (CorruptChecksumException e) {
            // - when
            report.addCritical(getFunction(), e, "The computed hash doesn't match given hash");
        }
        catch (MissingPayloadManifestException e) {
            report.addCritical(getFunction(), e, "There is not at least one payload manifest");
        }
        catch (MissingBagitFileException e) {
            report.addCritical(getFunction(), e, "There is no bagit.txt file");
        }
        catch (MissingPayloadDirectoryException e) {
            report.addCritical(getFunction(), e, "There is no /data directory");
        }
        catch (FileNotInPayloadDirectoryException e) {
            report.addCritical(getFunction(), e, "A manifest lists a file but it is not in the payload directory");
        }
        catch (MaliciousPathException e) {
            report.addCritical(getFunction(), e, "There is path that is referenced in the manifest that is outside the bag root directory");
        }
        catch (VerificationException e) {
            report.addCritical(getFunction(), e, "Some other exception happened during processing so capture it here");
        }
        catch (UnsupportedAlgorithmException e) {
            report.addCritical(getFunction(), e, "The manifest uses a algorithm that isn't supported");
        }
        catch (InvalidBagitFileFormatException e) {
            report.addCritical(getFunction(), e, "The manifest is not formatted properly");
        }
        return report;
    }

    private Report checkCompleteness(BagVerifier verifier, Bag bag) throws IOException, InterruptedException {
        Report report = new Report();
        try {
            verifier.isComplete(bag, false);
            report.addCorrect(getFunction(), "Success: bag is complete");
        }
        catch(MissingPayloadManifestException e) {
            report.addCritical(getFunction(), e, "There is not at least one payload manifest");
        }
        catch(MissingBagitFileException e) {
            report.addCritical(getFunction(), e, "There is no bagit.txt file");
        }
        catch(MissingPayloadDirectoryException e) {
            report.addCritical(getFunction(), e, "There is no /data directory");
        }
        catch(FileNotInPayloadDirectoryException e) {
            report.addCritical(getFunction(), e, "A manifest lists a file but it is not in the payload directory");
                }
        catch(MaliciousPathException e) {
            report.addCritical(getFunction(), e, "There is path that is referenced in the manifest that is outside the bag root directory");
        }
        catch(UnsupportedAlgorithmException e) {
            report.addCritical(getFunction(), e, "The manifest uses a algorithm that isn't supported");
        }
        catch(InvalidBagitFileFormatException e) {
            report.addCritical(getFunction(), e, "The manifest is not formatted properly");
        }
        return report;
    }

    private Report fetchFiles(Bag bag) {
        Report report = new Report();
        for (FetchItem item : bag.getItemsToFetch()) {
            try {
                // Create parent directories if missing`
                if (!item.getPath().getParent().toFile().exists()) {
                    item.getPath().getParent().toFile().mkdirs();
                }
                // Download file
                
                item.getUrl().openStream().transferTo(new FileOutputStream(item.getPath().toFile()));
                // Create blank file
                // item.getPath().toFile().createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(CheckBag.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return report;
    }
}

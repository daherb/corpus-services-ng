/**
 * @file CmdiXsdErrorReportGenerator.java
 *
 * Create a user-friendly error report from validation errors in a coma file.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora.validation;


import de.uni_hamburg.corpora.Report;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

/**
 * XML schema validation error handler for coma file errors. This is specific
 * to HZSK use of coma file data, the class tries to turn XML Schema errors
 * into readable descriptions of what effect a validation error may have to
 * the deposited corpus and how to fix it.
 */
public class CmdiXsdErrorReportGenerator implements ErrorHandler {

    // store latest file name for laughs
    private String currentFileName;
    private Report stats;
    final String CMDI_XSD = "cmdi-validate-xsd";

    /**
     * Create an error report generator.
     */
    public CmdiXsdErrorReportGenerator() {
        super();
        stats = new Report();
    }

    public Report getErrors() {
        return stats;
    }

    /**
     * Classify XML schema validation errors into HZSK infos.
     */
    private void storeException(SAXParseException saxpe) {
        String msg = saxpe.getMessage();
        if (msg.contains("cvc-enumeration-valid") &&
                msg.contains("xml;format-variant")) {
            stats.addNote(CMDI_XSD, "Acceptable validation error in mimetypes: "
                    + "xml format variants");
        } else if (msg.contains("cvc-complex-type") &&
                msg.contains("MimeType")) {
            stats.addNote(CMDI_XSD, "Acceptable validation error in mimetypes: "
                    + "xml format variants");
        } else {
            stats.addException(CMDI_XSD, saxpe,
                    "It's a validation error! (unrecognised):" +
                    saxpe.getMessage());
        }
   }

   /**
    * Handle fatal XML schema validation error.
    * This handler ignores the severity of validation errors.
    */
    public void fatalError(SAXParseException saxpe) throws SAXException {
        storeException(saxpe);
    }

    /**
     * Handle XML schema validation error.
     * This handler ignores the severity of validation errors.
     */
    public void error(SAXParseException saxpe) throws SAXException {
        storeException(saxpe);
    }

    /**
     * Handle XML schema valdition warning
     * This handler ignores the severity of validation errors.
     */
    public void warning(SAXParseException saxpe) throws SAXException {
        storeException(saxpe);

    }
}


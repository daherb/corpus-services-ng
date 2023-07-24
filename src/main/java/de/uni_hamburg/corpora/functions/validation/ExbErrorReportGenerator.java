/**
 * @file ExbErrorReportGenerator.java
 *
 * Create a user-friendly error report from validation errors in exb files.
 *
 * @author Ozan Ozdemir <ozan.oezdemir@studium.uni-hamburg.de>
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
public class ExbErrorReportGenerator implements ErrorHandler {

    // store latest file name for laughs
    private String currentFileName;
    private Report stats;
    final String EXB_SCHEMA = "exb-validate-schema";

    /**
     * Create an error report generator.
     */
    public ExbErrorReportGenerator(String fileName) {
        super();
        stats = new Report();
        currentFileName = fileName;
    }

    public Report getErrors() {
        return stats;
    }

    /**
     * Adds the exceptions to the report
     */
    private void storeException(SAXParseException saxpe) {
        // yeah this hack relies on parsing the localised(?) messages...
        String msg = saxpe.getMessage();
        String embeddedExceptions = saxpe.toString();
        System.out.println(embeddedExceptions);
        stats.addException(EXB_SCHEMA, saxpe, "In " + currentFileName + ", " + embeddedExceptions);
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
     * Handle XML schema validation warning
     * This handler ignores the severity of validation errors.
     */
    public void warning(SAXParseException saxpe) throws SAXException {
        storeException(saxpe);

    }
}


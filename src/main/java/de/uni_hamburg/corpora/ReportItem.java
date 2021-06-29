/**
 * @file StatisticsStuff.java
 *
 * Auxiliary data structure for user friendly errors.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import org.xml.sax.SAXParseException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * Error message class is meant to facilitate creating user friendly error
 * messages in HZSK validators. It kind of forces the programmer to at least
 * rephrase an exception to two messages describing the problem and suggested
 * solution. Can be used without exception as well.
 */
public class ReportItem {

    /**
     * Severity of error tells whether or how urgently it needs fixing:
     * <ol>
     * <li>critical errors <b>must</b> to be fixed before data can be added to
     * repo</li>
     * <li>warnings <b>should</b> fixed before data can be used</li>
     * <li>notes are informative and can usually be ignored</li>
     * <li>missing is a special case of error somehow</li>
     * <li>correct is a case that <b>must not</b> be acted upon</li>
     * <li>if validator outputs fixed version it should note errors that are
     * automatically corrected as fixed</li>
     * <li>unknown is used when developer doesn't actually expect or understand
     * the error enough to describe it</li>
     * </ol>
     * If the validator reports all occurrences of things it goes through, the
     * success % or readiness % can be reported.
     */
    public enum Severity {
        CRITICAL,
        WARNING,
        NOTE,
        MISSING,
        CORRECT,
        IFIXEDITFORYOU,
        UNKNOWN
    }

    /** Description of error */
    private String what;
    /** How to fix the error */
    private String howto;
    /** Exception that may be related, for developer debugging mainly */
    private Throwable e;
    /** Severity of error */
    private Severity severity = Severity.CRITICAL;
    /** Errors when parsing a file should include the name of the file */
    private String filename;
    /**
     * Errors when parsing file should if possible point to lines where error
     * is.
     */
    private String lines;
    /** Errors with file parsing can also include the columns when known.*/
    private String columns;
    //name of the function that caused the error
    private String function;

    /**
     * Default constructor should only be used when nothing at all is known
     * of the error.
     */
    public ReportItem() {
        // Default severity is critical (see above)
        what = "Totally unknown error";
        howto = "No known fixes";
        e = null;
        function = "Unknown function";
    }

    public ReportItem(Severity s, String what) {
        // Call constructor above
        this();
        this.severity = s;
        this.what = what;
    }

    public ReportItem(Severity s, Throwable e, String what) {
        // Call constructor above
        this(s,what);
        this.e = e;
    }

    public ReportItem(Severity s, Throwable e, String filename, String what) {
        // Call constructor above
        this(s,e,what);
        this.filename = filename;
    }
    
    public ReportItem(Severity s, String filename, String what, String function) {
        // Call constructor above
        this(s,what);
        this.filename = filename;
        this.function = function;
    }
    
    /**
     * Errors found by XML validation errors should always include a
     * SAXParseException. This can be used to extract file location informations
     * in most situations.
     */
    public ReportItem(Severity s, SAXParseException saxpe, String what) {
        this(s,what);
        if (saxpe != null) {
            this.e = (Throwable) saxpe ;
            this.filename = saxpe.getSystemId();
            this.lines = "" + saxpe.getLineNumber();
            this.columns = "" + saxpe.getColumnNumber();
        }
//        this.howto = howto; // This does not make sense but is null anyway
        this.function = "Unknown function";
    }


    /**
     * Generic file parsing error that can not be pointed to a line location
     * can be constructed from filename and descriptions.
     */
    public ReportItem(Severity s, String filename,
            String what, String function, String howto) {
        // Call constructor above
        this(s,filename,what,function);
        this.howto = howto;
    }

    /**
     * Severity of the error
     */
    public Severity getSeverity() {
        return this.severity;
    }

    /**
     * whether the stuff should be counted towards good statistic.
     */
    public boolean isGood() {
        switch (this.severity) {
            case CORRECT:
            case NOTE:
            case IFIXEDITFORYOU:
                return true ;
            case WARNING:
            case CRITICAL:
            case MISSING:
            case UNKNOWN: // ???
                return false ;
            default:
                throw new IllegalArgumentException("Missed a severity case in isGood :-( "+ this.severity.toString());
        }
    }

    /**
     * whether the stuff should be counted towards bad statistic.
     */
    public boolean isBad() {
        switch (this.severity) {
            case CORRECT:
            case NOTE:
            case IFIXEDITFORYOU:
                return false;
            case WARNING:
            case CRITICAL:
            case MISSING:
            case UNKNOWN: // ???
                return true;
            default:
                throw new IllegalArgumentException("Missed a severity case in isBad :-( "+ this.severity.toString());
        }
    }
    /**
     * whether the stuff should be presented as severe problem.
     */
    public boolean isSevere() {
        switch (this.severity) {
            case CORRECT:
            case WARNING:
            case NOTE:
            case IFIXEDITFORYOU:
                return false ;
            case CRITICAL:
            case MISSING:
            case UNKNOWN: // ???
                return true ;
            default:
                throw new IllegalArgumentException("Missed a severity case in isSevere :-( " + this.severity.toString());
        }
    }
    
        /**
     * whether the stuff should be counted towards bad statistic.
     */
    public boolean isFix() {
        switch (this.severity) {
            case IFIXEDITFORYOU:
                return true;
            case CORRECT:
            case NOTE:
            case CRITICAL:
            case MISSING:
            case WARNING:
            case UNKNOWN:
                return false;
            default:
                throw new IllegalArgumentException("Missed a severity case in isFix :-( " + this.severity.toString());
        }
    }

    /**
     * Location of error in filename:lines.columns format if any.
     */
    public String getLocation() {
        String loc = new String();
        if (filename != null) {
            loc = filename;
        } else {
            loc = "";
        }
        if (lines != null) {
            if (columns != null) {
                loc += ":" + lines + "." + columns;
            } else {
                loc += ":" + lines;
            }
        }
        return loc;
    }

    /**
     * Description of the error.
     */
    public String getWhat() {
        return this.what;
    }

    /**
     * A suggested fix to the error.
     */
    public String getHowto() {
        if (this.howto != null) {
            return this.howto;
        } else {
            return "";
        }
    }
    
    /**
     * A suggested fix to the error.
     */
    public String getFunction() {
        if (this.function != null) {
            return this.function;
        } else {
            return "";
        }
    }

    /**
     * a localised message from the excpetion if any.
     */
    public String getLocalisedMessage() {
        if (e != null) {
            return e.getLocalizedMessage();
        } else {
            return "";
        }
    }

    /**
     * A short string about the stuff.
     */
    public String getSummary() {
        String s = "    ";
        if (!getLocation().equals("")) {
            s += getLocation() + ": ";
        }
        s += getWhat();
        return s;
    }

    /**
     * A pretty printed string with most informations about the error. Can be
     * super long.
     */
    public String toString() {
        return getLocation() + ": " + getWhat() + ". " + getHowto() + ". " +
            getLocalisedMessage() + "\n" + getStackTrace();
    }

    /**
     * The stack trace of the exception if any.
     */
    public String getStackTrace() {
        if (e != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        } else {
            return "";
        }
    }

    /**
     * Generate a plain text report from validation errors for end user. This
     * can be presented on command-line.
     *
     * @param  verbose if true, generates detailed report of all errors,
     *                 otherwise returns a summary and critical errors.
     */
    public static String
        generatePlainText(Collection<ReportItem> errors,
                boolean verbose) {
        String report = new String();
        int criticals = 0;
        int warnings = 0;
        int notes = 0;
        int unknowns = 0;
        for (ReportItem error : errors) {
            switch (error.getSeverity()) {
                case CRITICAL:
                    criticals++;
                    break;
                case WARNING:
                    warnings++;
                    break;
                case NOTE:
                    notes++;
                    break;
                case UNKNOWN:
                    unknowns++;
                    break;
                default:
                    break;
            }
        }
        report += "Messages (" + errors.size() + "), of which: ";
        if (verbose) {
            report += criticals + " critical, " + warnings + " warnings, " +
                notes + " notes and " + unknowns + " not classified\n";
        } else {
            report += criticals + " critical and " + warnings +
                " warnings (and " + (notes + unknowns) +
                " hidden as non-problems or unknown)\n";
        }
        for (ReportItem error : errors) {
            if (verbose) {
                report += "  " + error + "\n";
            } else if (error.getSeverity() == ReportItem.Severity.WARNING ||
                    error.getSeverity() == ReportItem.Severity.CRITICAL) {
                report += "  " + error.getLocation() + ": " +
                    error.getWhat() + "\n" +
                    "    " + error.getHowto() + "\n";
            }
        }
        return report;
    }

    /**
     * Generate a very short summary of validation errors.
     * TODO: Make more fine-grained to make e.g. correct items explicit
     */
    public static String generateSummary(Collection<ReportItem>
            errors) {
        String report = new String();
        int criticals = 0;
        int warnings = 0;
        int notes = 0;
        int unknowns = 0;
        for (ReportItem error : errors) {
            switch (error.getSeverity()) {
                case CRITICAL:
                case MISSING: // made missing critical as well to match isSevere
                    criticals++;
                    break;
                case WARNING:
                    warnings++;
                    break;
                case NOTE:
                    notes++;
                    break;
                default: //case UNKNOWN: // Changed the unknown case to default to cover e.g. even the okay or fixed
                    unknowns++;
                    break;
                //default:
                //    break;
            }
        }
        report = "Total of " +  (criticals + warnings + notes + unknowns) +
            " messages: " + criticals + " critical errors, " +
            warnings + " warnings, " + notes + " notes and " + unknowns +
            " others.";
        return report;
    }

    /**
     * Generate a simple HTML snippet version of validation errors.
     * Includes quite ugly table of all the reports with a java script to hide
     * errors based on severity.
     */
    public static String generateHTML(Collection<ReportItem>
            errors) {
        int criticals = 0;
        int warnings = 0;
        int notes = 0;
        int unknowns = 0;
        for (ReportItem error : errors) {
            switch (error.getSeverity()) {
                case CRITICAL:
                    criticals++;
                    break;
                case WARNING:
                    warnings++;
                    break;
                case NOTE:
                    notes++;
                    break;
                case UNKNOWN:
                    unknowns++;
                    break;
                default:
                    break;
            }
        }
        String report = new String();
        //report = "<p>The following errors are from XML Schema validation only.</p>\n";
        report += "<script type='text/javascript'>\n" +
            "function showClick(clicksource, stuff) {\n\t" +
            "  var elems = document.getElementsByClassName(stuff);\n" +
            "  for (i = 0; i < elems.length; i++) {\n" +
            "    if (clicksource.checked) {\n" +
            "      elems[i].style.display = 'table-row';\n" +
            "    } else {\n" +
            "      elems[i].style.display = 'none';\n" +
            "    }\n" +
            "  }\n" +
            "}\n</script>";
        report += "<form>\n" +
            "  <input type='checkbox' id='critical' name='critical' value='show' checked='checked' onclick='showClick(this, &apos;critical&apos;)'>"
            + "Show criticals (" + criticals + ")</input>" +
            "  <input type='checkbox' name='warning' value='show' checked='checked' onclick='showClick(this, &apos;warning&apos;)'>"
            + "Show warnings (" + warnings + ")</input>" +
            "  <input type='checkbox' name='note' value='show' onclick='showClick(this, &apos;note&apos;)'>"
            + "Show notes (" + notes + ")</input>" +
            "  <input type='checkbox' name='unknown' value='show' onclick='showClick(this, &apos;unknown&apos;)'>"
            + "Show unknowns (" + unknowns + ")</input>" +
            "</form>";
        report += "<table>\n  <thead><tr>" +
            "<th>File:line.column</th><th>Error</th>" +
            "<th>Fix</th><th>Original</th>" +
            "</tr></thead>\n";
        report += "  <tbody>\n";
        for (ReportItem error : errors) {
            switch (error.getSeverity()) {
                case CRITICAL:
                    report += "<tr class='critical'><td style='border-left: red solid 3px'>";
                    break;
                case WARNING:
                    report += "<tr class='warning'><td style='border-left: yellow solid 3px'>";
                    break;
                case NOTE:
                    report += "<tr class='note' style='display: none;'><td style='border-left: green solid 3px'>";
                    break;
                case UNKNOWN:
                    report += "<tr class='unknown' style='display: none;'><td style='border-left: orange solid 3px'>";
                    break;
                default:
                    report += "<tr class='other' style='display: none;'><td style='border-left: black solid 3px'>";
                    break;
            }
            report += error.getLocation() + "</td>";
            report += "<td style='border: red solid 1px; white-space: pre'>" +
                error.getWhat() +
                "</td>";
            report += "<td style='border: green solid 1px; white-space: pre'>" +
                error.getHowto() +
                "</td>";
            report += "<td style='font-face: monospace; color: gray; border: gray solid 1px; white-space: pre'>(" +
                error.getLocalisedMessage() +
                ")</td>\n";
            report += "<!-- " + error.getStackTrace() + " -->\n";
            report += "</tr>";
        }
        report += "  </tbody>\n  </table>\n";
        return report;
    }

    
    
    /**
     * Generate a simple HTML snippet version of validation errors.
     * Includes quite ugly table of all the reports with a java script to hide
     * errors based on severity.
     */
    public static String generateDataTableHTML(Collection<ReportItem> errors, String summarylines) {
        
        String report = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        
        report += "<html>\n   <head>\n";
        
        report += "<title>Corpus Check Report</title>\n";
        report += "<meta charset=\"utf-8\"></meta>\n";
        
        //add JS libraries
        report += "<script>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/js/jquery/jquery-3.1.1.min.js")) + "</script>\n";
        report += "<script>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/js/DataTables/jquery.dataTables-1.10.12.min.js")) + "</script>\n";
        report += "<script>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/js/DataTables/dataTables-bootstrap.min.js")) + "</script>\n";
        report += "<script>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/js/bootstrap/bootstrap-3.3.7.min.js")) + "</script>\n";
        
        //add CSS
        report += "<style>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/css/DataTables/dataTables.bootstrap.min.css")) + "</style>\n";
        report += "<style>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/css/DataTables/buttons.dataTables.min.css")) + "</style>\n";
        report += "<style>" + TypeConverter.InputStream2String(ReportItem.class.getResourceAsStream("/css/bootstrap/bootstrap-3.3.7.min.css")) + "</style>\n";
        
        //add custom CSS
        report += "<style>"+
                "body{padding:15px;}"+
                "#timestamp{margin-bottom:30px;}"+
                ".critical{ background:#ffdddd; } "+
                ".other{ background:#ffd39e; } "+
                ".warning{ background:#fafcc2; } "+
                ".char_Cyrillic{ color:#b51d0d; } "+
                ".char_Greek{ background:#022299; } "+
                ".char_Armenian{ background:#ad7600; } "+
                ".char_Georgian{ background:#9c026d; } "+
                "</style>\n";
        report += "   </head>\n   <body>\n";
        
        //add timestamp
        report += "   <div id='timestamp'>Generated: ";        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        report += timestamp + "</div>\n";
        
        report += "<table>\n  <thead><tr>" +
            "<th>Type</th>"+
            "<th>Function</th>"+
            "<th>Filename:line.column</th>"+
            "<th>Error</th>" +
            "<th>Fix</th>"+
            "<th>Original</th>" +
            "</tr></thead>\n";
        report += "  <tbody>\n";
        for (ReportItem error : errors) {
            switch (error.getSeverity()) {
                case CRITICAL:
                    report += "<tr class='critical'><td style='border-left: red solid 3px'>Critical</td><td>";
                    break;
                case WARNING:
                    report += "<tr class='warning'><td style='border-left: yellow solid 3px'>Warning</td><td>";
                    break;
                case NOTE:
                    report += "<tr class='note'><td style='border-left: green solid 3px'>Note</td><td>";
                    break;
                case UNKNOWN:
                    report += "<tr class='unknown'><td style='border-left: orange solid 3px'>Unknown</td><td>";
                    break;
                default:
                    report += "<tr class='other'><td style='border-left: black solid 3px'>Other</td><td>";
                    break;
            }
            report += error.getFunction() + "</td><td>";
            report += error.getLocation() + "</td>";
            report += "<td style='white-space: pre'>" +
                error.getWhat() +
                "</td>";
            report += "<td style='white-space: pre'>" +
                error.getHowto() +
                "</td>";
            report += "<td style='font-face: monospace; color: gray; border: gray solid 1px; white-space: pre;'>(" +
                error.getLocalisedMessage() +
                ")</td>\n";
            report += "<!-- " + error.getStackTrace() + " -->\n";
            report += "</tr>";
        }
        report += "  </tbody>\n  </table>\n";
        
        //initiate DataTable on <table>
        report += "<script>$(document).ready( function () {\n" +
                  "    $('table').DataTable({ 'iDisplayLength': 50 });\n" +
                  "} );</script>";
        
        report += "   <footer style='white-space: pre'>" + summarylines + "</footer>";
        report += "   </body>\n</html>";
        
        return report;
    }

}

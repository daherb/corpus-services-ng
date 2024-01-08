/**
 * A servlet frontend for validating coma files.
 *
 * @file HzskValidationServlet.java
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.validation.ComaErrorReportGenerator;
import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Collection;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.XMLConstants;
import org.xml.sax.SAXException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 * A Servlet that validates a coma file.
 */
@MultipartConfig
public class HzskValidationServlet extends HttpServlet {


    /**
     * Initiate servlet context.
     */
    @Override
    public void init() throws ServletException {
        super.init();
        final ServletConfig cfg  = getServletConfig();
        final ServletContext ctx = getServletContext();
    }


    /**
     * Destroy the servlet.
     */
    @Override
    public void destroy() {
        super.destroy();
    }


    /**
     * Handles GET requests by showing an upload form. The file to validate is
     * uploaded in POST requests.
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter output = response.getWriter();
        output.print("<!DOCTYPE html!>\n<html>\n  <head>\n");
        output.print("    <title>HZSK corpus validation</title>\n");
        output.print("  </head>\n  <body>\n");
        output.print("    <h1>HZSK validations</h1>\n");
        output.print("    <p>A Service as a service that validates quality of "
                + "your corpora files.</p>\n");
        output.print("    <form action='validate.coma' method='post' enctype='multipart/form-data'>\n");
        output.print("    <h2>Coma</h2>\n");
        output.print("      <input type='file' name='coma'/>\n");
        output.print("      <input type='submit' name='upload'/>\n");
        output.print("    </form>\n");
        output.print("    <form action='validate.exb' method='post' enctype='multipart/form-data'>\n");
        output.print("    <h2>Exb</h2>\n");
        output.print("      <input type='file' name='exb'/>\n");
        output.print("      <input type='submit' name='upload'/>\n");
        output.print("    </form>\n  </body>\n</html>");
        output.flush();
    }

    /**
     * Servlet c/p version of xml Validation script in ComaErrorChecker.
     */
    private void generateComaReport(InputStream coma, PrintWriter output)
            throws SAXException, IOException {
        // XXX: some validation depends on language of exception message
        Locale.setDefault(Locale.ENGLISH);
        URL COMA_XSD = new URL("http://www.exmaralda.org/xml/comacorpus.xsd");
        Source xmlStream = new StreamSource(coma);
        SchemaFactory schemaFactory =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(COMA_XSD);
        Validator validator = schema.newValidator();
        ComaErrorReportGenerator eh = new ComaErrorReportGenerator();
        validator.setErrorHandler(eh);
        validator.validate(xmlStream);
        output.print("    <h3>coma validations</h3>\n");
        output.print("    <p>These errors are found in XML validation</p>\n");
        //output.print(ErrorMessage.generateHTML(eh.getErrors()));
        output.print("  </body>\n</html>");
    }

    private void generateExbReport(String filename, PrintWriter output)
            throws SAXException, JexmaraldaException {
        //ExbErrorChecker checker = new ExbErrorChecker(filename);
        output.print("    <h3>exb validations</h3>\n");
        ///output.print(ErrorMessage.generateHTML(
        //             checker.getStructureErrors()));
        output.print("  </body>\n</html>");
    }

    /**
     * Handles post request by validating the uploaded file. If no coma file
     * can be found, shows very basic error plain text errors.
     */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        PrintWriter output = response.getWriter();
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            output.print("<!DOCTYPE html!>\n<html>\n  <head>\n");
            output.print("    <title>HZSK validation</title>\n");
            output.print("  </head>\n  <body>\n");
            output.print("  <h1>HZSK corpus validations</h1>\n");
            output.print("  <p>These are the results of validating the uploaded"
                    + " files</p>\n");
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                String fileName = getSubmittedFileName(part);
                if (fileName == null) {
                    continue;
                }
                output.print("<h2><code>" + fileName + "</code></h2>");
                InputStream fileInputStream = part.getInputStream();
                File localFile = File.createTempFile("hzsk-validate", ".xml");
                localFile.deleteOnExit();
                String localFileName = localFile.getAbsolutePath();
                part.write(localFileName);
                if (part.getName().equals("coma")) {
                    generateComaReport(fileInputStream, output);
                }
                else if (part.getName().equals("exb")) {
                    generateExbReport(localFileName, output);
                } else {
                    output.print("<h3>Unknown file type–No validations</h3>\n" +
                            "<p>There are no validation checks for this file " +
                            "type.</p>\n");
                }
            }
        } catch (ServletException se) {
            output.print("<!DOCTYPE html>\n" +
                    "<html><head><title>Error</title></head>\n" +
                    "  <body><h1>FAILED!</h1>\n" +
                    "    <p>No uploaded founds filed.</p>\n" +
                    "<pre>");
            se.printStackTrace(output);
            output.print("</pre>\n</body>\n</html>");
        } catch (SAXException saxe) {
            output.print("<h1>FAILED!</h1>\n" +
                    "    <p>not an XML file</p>\n" +
                    "<pre>");
            saxe.printStackTrace(output);
            output.print("</pre>\n</body>\n</html>");
        } catch (JexmaraldaException je) {
            output.print("<h1>FAILED!</h1>\n" +
                    "    <p>not an exb file</p>\n" +
                    "<pre>");
            je.printStackTrace(output);
            output.print("</pre>\n</body>\n</html>");
        }
        output.flush();
    }

    /**
     * c/p from stackoverflow for post file name handling.
     * @see https://stackoverflow.com/questions/2422468/how-to-upload-files-to-server-using-jsp-servlet
     */
    private static String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
            }
        }
        return null;
    }
}

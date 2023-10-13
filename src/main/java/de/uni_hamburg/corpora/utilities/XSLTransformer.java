/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.utilities;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.serialize.MessageEmitter;
import net.sf.saxon.trans.XPathException;

/**
 *
 * @author Daniel Jettka
 *
 * Class for performing XSLT transformation with
 * net.sf.saxon.TransformerFactoryImpl or other custom implementation.
 */
public class XSLTransformer {

    private TransformerFactory tranformerFactory;
    private Transformer transformer;
    private String transformerFactoryImpl = "net.sf.saxon.TransformerFactoryImpl";
    private Map<String, Object> parameters = new HashMap<>();
    private Map<String, String> outputProperties = new HashMap<>();

    /**
     * Class constructor.
     */
    public XSLTransformer() throws TransformerConfigurationException {
        tranformerFactory = TransformerFactory.newInstance(transformerFactoryImpl, null);
    }

    /**
     * Class constructor specifying the TransformerFactoryImpl used for XSLT
     * transformation.
     */
    public XSLTransformer(String impl) throws TransformerConfigurationException {
        transformerFactoryImpl = impl;
        tranformerFactory = TransformerFactory.newInstance(transformerFactoryImpl, null);
    }

    /**
     * Returns a String object that represents the result of an XSLT
     * transformation.
     *
     * @param xml XML as String object that is used as the basis for the XSLT
     * transformation
     * @param xsl XSLT stylesheet as String object
     * @return the result of the XSLT transformation as String object
     */
    public String transform(String xml, String xsl) throws TransformerException {
        StreamSource xslSource = TypeConverter.String2StreamSource(xsl);
        StreamSource xmlSource = TypeConverter.String2StreamSource(xml);
        return transform(xmlSource, xslSource);
    }

    /**
     * Returns a String object that represents the result of an XSLT
     * transformation.
     *
     * @param xml XML as StreamSource object that is used as the basis for the
     * XSLT transformation
     * @param xsl XSLT stylesheet as StreamSource object
     * @return the result of the XSLT transformation as String object
     */
    public String transform(StreamSource xmlSource, StreamSource xslSource) throws TransformerException {
        final StringWriter messageOut = new StringWriter();
        String result = null;
        try {
            if(xslSource != null){
                transformer = tranformerFactory.newTransformer(xslSource);
                
                //trying to get xsl:message into error reports            
                // 20230831: Setting recovery policy does not seem possible in recent version of Saxon
                // ((net.sf.saxon.jaxp.TransformerImpl) transformer).getUnderlyingController().setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
                ((net.sf.saxon.jaxp.TransformerImpl) transformer).getUnderlyingController().setMessageEmitter(new MessageEmitter() {
                    @Override
                    public void open() throws XPathException {
                        setWriter(messageOut);
                        super.open();
                    }
                });
            } else{                
                transformer = tranformerFactory.newTransformer();
            }
            
            // set the output properties for XSLT transformation
            for (Map.Entry<String, String> param : outputProperties.entrySet()) {
                transformer.setOutputProperty(param.getKey(), param.getValue());
            }
            // set the parameters for XSLT transformation
            for (Map.Entry<String, Object> param : parameters.entrySet()) {
                transformer.setParameter(param.getKey(), param.getValue());
            }
            
            //transform and fetch result
            StringWriter resultWriter = new StringWriter();
            transformer.transform(xmlSource, new StreamResult(resultWriter));
            result = resultWriter.toString();

        } catch (TransformerException e) {
            //System.out.println("Message: " + e.getLocalizedMessage());
            String message = messageOut.toString(); // this is the "exception message\n" that you want
            System.out.println("MESSAGE: " + message);
            throw new TransformerException(message, e); // rethrow using the captured message, if you really want that "exception message" available to a caller in e.getMessage()
        }

        return result;
    }
    
    
    /**
     * Returns a String object that represents the result of an XSLT
     * transformation.
     *
     * @param xmlSource XML as StreamSource object that is used as the basis for the
     * XSLT transformation
     * @return the result of the XSLT transformation as String object
     */
    public String transform(StreamSource xmlSource) throws TransformerException {
        return transform(xmlSource, null);        
    }
    
    
    /**
     * Returns a String object that represents the result of an XSLT
     * transformation.
     *
     * @param xml XML as String object that is used as the basis for the XSLT
     * transformation
     * @return the result of the XSLT transformation as String object
     */
    public String transform(String xml) throws TransformerException {
        StreamSource xmlSource = TypeConverter.String2StreamSource(xml);
        return transform(xmlSource);
    }
    

    /**
     * Set a single parameter for the XSLT transformation.
     *
     * @param parameterName Name of the parameter
     * @param parameterValue Value of the parameter
     * @return
     */
    public void setParameter(String parameterName, Object parameterValue) {
        parameters.put(parameterName, parameterValue);
    }

    /**
     * Set a bunch of parameters for the XSLT transformation.
     *
     * @param params Map object representing parameter names and values
     * @return
     */
    public void setParameters(Map<String, Object> params) {
        parameters = params;
    }

    /**
     * Get a single parameter that was set for the XSLT transformation.
     *
     * @param parameterName Name of the parameter that shall be returned
     * @return Value of the supplied parameter
     */
    public Object getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    /**
     * Get all parameters as Map object that were set for the XSLT
     * transformation.
     *
     * @param
     * @return Map representing all parameters
     */
    public Map getParameters() {
        return parameters;
    }

    /**
     * Set a single property for the XSLT transformation.
     *
     * @param propertyName Name of the output property
     * @param propertyValue Value of the output property
     * @return
     */
    public void setOutputProperty(String propertyName, String propertyValue) {
        outputProperties.put(propertyName, propertyValue);
    }

    /**
     * Set a bunch of properties for the XSLT transformation.
     *
     * @param outputProps Map object representing property names and values
     * @return
     */
    public void setOutputProperties(Map<String, String> outputProps) {
        outputProperties = outputProps;
    }

    /**
     * Get a single property that was set for the XSLT transformation.
     *
     * @param propertyName Name of the property that shall be returned
     * @return Value of the supplied property
     */
    public Object getOutputProperty(String propertyName) {
        return outputProperties.get(propertyName);
    }

    /**
     * Get all properties as Map object that were set for the XSLT
     * transformation.
     *
     * @param
     * @return Map representing all properties
     */
    public Map getOutputProperties() {
        return outputProperties;
    }
    
    /**
     * Set TransformerFactoryImpl (represented as class name in String) for the
     * XSLT transformation.
     *
     * @param impl TransformerFactoryImpl represented as class name in String
     * @return
     */
    public void setTransformerFactoryImpl(String impl) {
        transformerFactoryImpl = impl;
        tranformerFactory = TransformerFactory.newInstance(transformerFactoryImpl, null);
    }

    /**
     * Get TransformerFactoryImpl (represented as class name in String) for the
     * XSLT transformation.
     *
     * @param
     * @return TransformerFactoryImpl represented as class name in String
     */
    public String getTransformerFactoryImpl() {
        return transformerFactoryImpl;
    }

}

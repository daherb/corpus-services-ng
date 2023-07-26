/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.uni_hamburg.corpora.utilities.publication.ids;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Methods for accessing the datacite API for minting and updating PIDs
 * 
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class DataciteAPITools {

//    private final String repositoryId;
//    private final String password;
    private final URI baseUri;
    private final String basicAuthString;
    // private final ObjectMapper om;
    
    public DataciteAPITools(URI baseUri, String repositoryId, String password) {
        this.baseUri = baseUri;
//        this.repositoryId = repositoryId;
//        this.password = password;
        String combined = repositoryId + ":" + password;
        this.basicAuthString = Base64.getEncoder()
                .encodeToString(combined.getBytes());
//        om = new ObjectMapper();
//        om.findAndRegisterModules();
    }
    
        /**
     * Create a suitable HttpClient
     * 
     * @return The created Client
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException 
     */
    private HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return client;
    }
    
    /**
     * Create a suitable HttRequest Builder including header
     * 
     * @param uri The URI to be requested
     * @return 
     */
    private HttpRequest.Builder getHttpRequestBuilder(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Basic " + basicAuthString)
                .header("Accept", "application/json")
                .header("Application-Type", "application/json");
    }
    
    /**
     * Gets the body of a HttpResponse if it was successful. Throws an IOException otherwise
     * @param <T> The body type of the response
     * @param response the response itself
     * @return the body
     * @throws IOException if a HTTP error happens, i.e. a status code >= 400
     */
    private<T> T getBody(HttpResponse<T> response) throws IOException {
        if (response.statusCode() < 400) {
            return response.body();
        }
        else {
            throw new IOException("HTTP error encountered for request `" + response.request().toString() + "` Status: " + response.statusCode() + " Body: `" + response.body()+ "`");
        }
    }
    
    /***
     * List all DOIs registered with the current prefix
     * @param prefix The prefix the DOIs are registered with
     * @return The resulting list of DOIs
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public JsonElement ListAllDOIs(String prefix) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        URI uri = new URI(baseUri + "/dois" + "?prefix=" +prefix);
        HttpRequest request = getHttpRequestBuilder(uri.normalize()).GET().build();
        // return om.readValue(getBody(getHttpClient().send(request, BodyHandlers.ofString())), JsonObject.class);
        return JsonParser.parseString(getBody(getHttpClient().send(request, BodyHandlers.ofString())));
    }
    
    /***
     * Gets the data stored in the DOI record
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return The resulting metadata object
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public JsonElement GetDOIData(String prefix, String id) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        URI uri = new URI(baseUri + "/dois" + "/" +prefix + "/" + id);
        HttpRequest request = getHttpRequestBuilder(uri.normalize()).GET().build();
        // return om.readValue(getBody(getHttpClient().send(request, BodyHandlers.ofString())), JsonObject.class);
        return JsonParser.parseString(getBody(getHttpClient().send(request, BodyHandlers.ofString())));
    }
    
    /***
     * Create a new draft DOI within the prefix.If no id is given the id is 
 generated randomly
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return 
     * @throws java.net.URISyntaxException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws java.security.KeyManagementException 
     * @throws java.io.IOException 
     * @throws java.lang.InterruptedException 
     */
    public JsonElement createDraftDOI(String prefix, Optional<String> id) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        URI uri = new URI(baseUri + "/dois");
        String idString = "\"prefix\": \"" +prefix;
        if (id.isPresent()) {
            idString = "\"doi\": \"" + prefix + "/" + id.get();
        }
        String bodyString = "{\n" +
                "  \"data\": {\n" +
                "    \"type\": \"dois\",\n" +
                "    \"attributes\": {\n" +
                "       "+ idString + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonElement body = JsonParser.parseString(bodyString);
        HttpRequest request = getHttpRequestBuilder(uri.normalize())
                .header("Content-Type","application/vnd.api+json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyString)).build();
        LOG.info(request.headers().toString());
        return JsonParser.parseString(getBody(getHttpClient().send(request, BodyHandlers.ofString())));
    }
    private static final Logger LOG = Logger.getLogger(DataciteAPITools.class.getName());
    
    /***
     * Updates the data stored in the DOI
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @param metadata The metadata
     * @return 
     */
    public JsonObject updateDOI(String prefix, String id, JsonObject metadata) {
        return new JsonObject();
    }
    
    /***
     * Changes a DOI state from draft to registered
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return 
     */
    public JsonObject registerDraftDOI(String prefix, String id) {
        return new JsonObject();
    }
    
    /***
     * Changes a DOI state from draft or registered to findable
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return 
     */
    public JsonObject publishDOI(String prefix, String id) {
        return new JsonObject();
    }
    
    /***
     * Changes the DOI state from findable to registered
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return 
     */
    public JsonObject hideDOI(String prefix, String id) {
        return new JsonObject();
    }
    
    /***
     * Deletes a draft DOI
     * @param prefix The prefix the DOI is registered with
     * @param id The DOI id itself
     * @return The HTTP status code of the request, should be 204 for success
     * @throws java.net.URISyntaxException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws java.security.KeyManagementException 
     * @throws java.io.IOException 
     * @throws java.lang.InterruptedException 
     */
    public int deleteDraftDOI(String prefix, String id) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        URI uri = new URI(baseUri + "/dois/" + prefix + "/" + id);
        HttpRequest request = getHttpRequestBuilder(uri.normalize())
                .header("Content-Type","application/vnd.api+json")
                .DELETE().build();
        return getHttpClient().send(request, BodyHandlers.ofString()).statusCode();
    }
}

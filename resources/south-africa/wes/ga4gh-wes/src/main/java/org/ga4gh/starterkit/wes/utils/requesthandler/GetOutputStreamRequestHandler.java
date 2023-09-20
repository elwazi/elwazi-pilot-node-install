package org.ga4gh.starterkit.wes.utils.requesthandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ga4gh.starterkit.common.exception.BadRequestException;
import org.ga4gh.starterkit.common.exception.ResourceNotFoundException;
import org.ga4gh.starterkit.common.requesthandler.RequestHandler;
import org.ga4gh.starterkit.wes.model.AccessMethod;
import org.ga4gh.starterkit.wes.model.AccessType;
// import org.ga4gh.starterkit.wes.model.FileAccessObject;
// import org.ga4gh.starterkit.wes.utils.cache.AccessCacheItem;
import org.ga4gh.starterkit.wes.model.RunLog;
import org.ga4gh.starterkit.wes.model.RunOutputStream;
import org.ga4gh.starterkit.wes.model.State;
import org.ga4gh.starterkit.wes.model.WesRun;
import org.ga4gh.starterkit.wes.utils.hibernate.WesHibernateUtil;
import org.ga4gh.starterkit.wes.utils.runmanager.RunManager;
import org.ga4gh.starterkit.wes.utils.runmanager.RunManagerFactory;
import org.ga4gh.starterkit.wes.utils.runmanager.language.LanguageHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Request handling logic for getting full log information (according to the WES spec)
 * from a requested workflow run
 * 
 * @see org.ga4gh.starterkit.wes.controller.Runs#getRunLog getRunLog
 */
public class GetOutputStreamRequestHandler implements RequestHandler<RunOutputStream> {

    @Autowired
    private RunManagerFactory runManagerFactory;

    @Autowired
    private WesHibernateUtil hibernateUtil;

    private String runId;
    private String fileId;


    /**
     * Instantiates a new GetRunLogRequestHandler
     */
    public GetOutputStreamRequestHandler() {

    }

    /**
     * Prepares the request handler with input params from the controller function
     * @param runId run identifier     
     * @param fileId run identifier

     * @return the prepared request handler
     */
    public GetOutputStreamRequestHandler prepare(String runId, String fileId) {
        this.runId = runId;
        this.fileId = fileId;

        return this;
    }

    /**
     * Obtains full log information for the requested workflow run
     */
    public RunOutputStream handleRequest() {
        // load the persisten WesRun by its id to obtain workflow language,
        // engine associated with the run
        WesRun wesRun = hibernateUtil.readEntityObject(WesRun.class, runId, true);

        if (wesRun == null) {
            throw new ResourceNotFoundException("No WES Run by the id: " + runId);
        }

        
        // if the run log JSON has been previously generated, load from db
        // and return
        // Check if the run log JSON has been previously generated
        if (wesRun.getFinalRunLogJson() != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                RunOutputStream runOutputStream = mapper.readValue(wesRun.getFinalRunLogJson(), RunOutputStream.class);
                

                String matchingFileId = findMatchingKey(runOutputStream.getOutputs(), fileId);                           


                String outputFileUrl_in = runOutputStream.getOutputs().get(matchingFileId); 

                String currentDirectory = System.getProperty("user.dir"); //get current dir

                String outputFileUrl = outputFileUrl_in.replace("file://" + currentDirectory + "/", ""); //clean path                
                
                File file = new File(outputFileUrl);
                if (file.exists()) {
                    // InputStream inputStream = FileInputStream(file);
                    InputStream inputStream = new FileInputStream(file);

                    StringBuilder contentBuilder = new StringBuilder();

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }
                    }

                    String inputStreamContent = contentBuilder.toString();

                    runOutputStream.setOutputStream(inputStreamContent);

                    return runOutputStream; 
                } else {
                    throw new ResourceNotFoundException("Output file not found: " + outputFileUrl);
                }
            } catch (JsonProcessingException ex) {
                System.out.println("Could not load RunLog from pre-existing JSON, attempting to load directly" + ex);
            } catch (IOException e) {
                System.out.println("Error while accessing the output file: " + e);
            }
        }

        // If the log information is not available or the file can't be retrieved, return null or throw an exception.
        return null; // You can modify this to return an appropriate response or throw an exception as needed.
    }

                    private static String findMatchingKey(Map<String, String> map, String fileId) {
                    for (String key : map.keySet()) {
                        if (key.endsWith(fileId)) {
                            return key;
                        }
                    }
                    return null; // If no matching key is found
                }
}

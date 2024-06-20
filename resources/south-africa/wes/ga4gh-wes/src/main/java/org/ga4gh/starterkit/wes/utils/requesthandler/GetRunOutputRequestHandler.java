package org.ga4gh.starterkit.wes.utils.requesthandler;

import org.ga4gh.starterkit.common.config.ServerProps;
import static org.ga4gh.starterkit.wes.constant.WesApiConstants.WES_API_V1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter; 
import org.ga4gh.starterkit.common.exception.BadRequestException;
import org.ga4gh.starterkit.common.exception.ResourceNotFoundException;
import org.ga4gh.starterkit.common.requesthandler.RequestHandler;
import org.ga4gh.starterkit.common.util.logging.LoggingUtil;
import org.ga4gh.starterkit.wes.model.AccessMethod;
import org.ga4gh.starterkit.wes.model.AccessType;
import org.ga4gh.starterkit.wes.model.AccessURL;
// import org.ga4gh.starterkit.wes.model.FileAccessObject;
// import org.ga4gh.starterkit.wes.utils.cache.AccessCacheItem;
import org.ga4gh.starterkit.wes.model.RunLog;
import org.ga4gh.starterkit.wes.model.RunOutput;
import org.ga4gh.starterkit.wes.model.RunOutputStream;
import org.ga4gh.starterkit.wes.model.State;
import org.ga4gh.starterkit.wes.model.WesRun;
import org.ga4gh.starterkit.wes.utils.hibernate.WesHibernateUtil;
import org.ga4gh.starterkit.wes.utils.runmanager.RunManager;
import org.ga4gh.starterkit.wes.utils.runmanager.RunManagerFactory;
import org.ga4gh.starterkit.wes.utils.runmanager.language.LanguageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.ga4gh.starterkit.wes.utils.requesthandler.AccessRequestHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;



import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Request handling logic for getting full log information (according to the WES spec)
 * from a requested workflow run
 * 
 * @see org.ga4gh.starterkit.wes.controller.Runs#getRunLog getRunLog
 */
public class GetRunOutputRequestHandler implements RequestHandler<RunOutput> {

    @Autowired
    private RunManagerFactory runManagerFactory;

    @Autowired
    private WesHibernateUtil hibernateUtil;

    private String runId;

    @Resource(name = "accessRequestHandler")
    private AccessRequestHandler accessRequestHandler;     

    @Autowired
    private ServerProps serverProps;


    @Autowired
    private LoggingUtil loggingUtil;       

    /**
     * Instantiates a new GetRunLogRequestHandler
     */
    public GetRunOutputRequestHandler() {

    }

    /**
     * Prepares the request handler with input params from the controller function
     * @param runId run identifier
     * @return the prepared request handler
     */
    public GetRunOutputRequestHandler prepare(String runId) {
        this.runId = runId;
        return this;
    }

    /**
     * Obtains full log information for the requested workflow run
     */
    public RunOutput handleRequest() {
        // load the persisten WesRun by its id to obtain workflow language,
        // engine associated with the run
        WesRun wesRun = hibernateUtil.readEntityObject(WesRun.class, runId, true);



        if (wesRun == null) {
            throw new ResourceNotFoundException("No WES Run by the id: " + runId);
        }

        // if the run log JSON has been previously generated, load from db
        // and return
        //we want to add the files to cache from here....
        if (wesRun.getFinalRunLogJson() != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {

                JsonNode jsonNode = mapper.readTree(wesRun.getFinalRunLogJson()); 

                JsonNode outputsNode = jsonNode.get("outputs");  //outputs as json


                Map<String, String> outputMap = new HashMap<>();

                if (outputsNode != null && outputsNode.isObject()) {

                outputsNode.fields().forEachRemaining(entry -> {
                    String fileName = entry.getKey();

                    AccessURL streamURL = getAccessURLById(runId, fileName); 
                    String outputFleURL = streamURL.getUrl().toString();

                    outputMap.put(fileName, outputFleURL);
                });
            }

            RunOutput runOut = new RunOutput();

            runOut.setRunId(runId);
            runOut.setState(State.UNKNOWN);

            runOut.setOutputs(outputMap);
                
            System.out.println("RunOutput object in GetOutoputRH...");

            return runOut; //mapper.readValue(wesRun.getFinalRunLogJson(), RunOutput.class);

            } catch (JsonProcessingException ex) {
                System.out.println("Could not load RunLog from pre-existing JSON, attempting to load directly" + ex);
            }
        }

        // load the RunOutput directly by custom API methods for Nextflow, WDL, etc.
        RunOutput runOutput = new RunOutput();
        runOutput.setRunId(runId);
        runOutput.setState(State.UNKNOWN);

        return runOutput;
    }

private AccessURL getAccessURLById(String runId, String fileId) {

    if (fileId.contains("/")) {
        int indexOfSlash = fileId.indexOf("/");
        if (indexOfSlash != -1) {
            fileId = fileId.substring(indexOfSlash + 1);
        }
    } 
    
    loggingUtil.debug("Public API request: AccessURL for RUN id '" + runId + "', file id '" + fileId + "'");
    return accessRequestHandler.prepare(runId, fileId).handleRequest();
}

}

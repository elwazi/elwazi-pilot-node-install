package org.ga4gh.starterkit.wes.utils.requesthandler;

import java.net.URI;
import org.ga4gh.starterkit.common.config.ServerProps;
import static org.ga4gh.starterkit.wes.constant.WesApiConstants.WES_API_V1;
import org.ga4gh.starterkit.common.exception.ResourceNotFoundException;
import org.ga4gh.starterkit.common.requesthandler.RequestHandler;
import org.ga4gh.starterkit.common.util.logging.LoggingUtil;
import org.ga4gh.starterkit.wes.model.AccessURL;
import org.ga4gh.starterkit.wes.model.AccessMethod;
import org.ga4gh.starterkit.wes.model.AccessType;
import org.ga4gh.starterkit.wes.model.RunOutputStream;
import org.ga4gh.starterkit.wes.model.AccessURL;
import org.ga4gh.starterkit.wes.utils.cache.AccessCache;
import org.ga4gh.starterkit.wes.utils.cache.AccessCacheItem;
import org.springframework.beans.factory.annotation.Autowired;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Request handling logic for providing an AccessURL from a provided WesRun id
 * and file ID/Name
 */
public class AccessRequestHandler implements RequestHandler<AccessURL> {

    @Autowired
    private ServerProps serverProps;

    @Autowired
    AccessCache accessCache;


    @Autowired
    private LoggingUtil loggingUtil;

    @Autowired
    private WesHibernateUtil hibernateUtil;    

    private String runId;
    private String fileId;

    /**
     * Instantiates a new AccessRequestHandler
     */
    public AccessRequestHandler() { //attempting streaming from here
    }

    /**
     * Prepares the request handler with input params from the controller function
     * @param runId WEsRun identifier
     * @param fileId file id/name
     * @return the prepared request handler
     */
    public AccessRequestHandler prepare(String runId, String fileId) {
        this.runId = runId;
        this.fileId = fileId;

        return this;
    }

    /**
     * Create an access cache item with the supplied parameters, from ObjectRequestHandler
     * @param runId WEsRun identifier
     * @param fileId file id/name
     * @param objectPath file path/URL to byte source 
     * @param accessType path/URL type
     * @param mimeType media type
     * @return access cache item populated with the supplied parameters
     */
    private AccessCacheItem generateAccessCacheItem(String runId, String fileId, String objectPath, AccessType accessType) {
        AccessCacheItem item = new AccessCacheItem();
        item.setObjectId(runId);
        item.setAccessId(fileId);
        item.setObjectPath(objectPath);
        item.setAccessType(accessType);
        // item.setMimeType(mimeType)??;
        return item;
    }

    /**
     * Provides an AccessURL for the given WesRun id and file name/id
     */
    public AccessURL handleRequest() {


        String outputFileUrl = null;
        //to get file path from here...
        WesRun wesRun = hibernateUtil.readEntityObject(WesRun.class, runId, true);
        if (wesRun.getFinalRunLogJson() != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                RunOutputStream runOutputStream = mapper.readValue(wesRun.getFinalRunLogJson(), RunOutputStream.class);
                

                //getting actual path for fileId within a dir
                String matchingFileId = findMatchingKey(runOutputStream.getOutputs(), fileId); 

                String outputFileUrl_in = runOutputStream.getOutputs().get(matchingFileId); 
                // System.out.println(outputFileUrl_in);

                String currentDirectory = System.getProperty("user.dir"); //get current dir

                outputFileUrl = outputFileUrl_in.replace("file://" + currentDirectory + "/", ""); //clean path                

            } catch (JsonProcessingException ex) {
                System.out.println("Could not load RunLog from pre-existing JSON, attempting to load directly" + ex);
             } 
        }

        AccessMethod accessMethod = new AccessMethod();
        accessMethod.setType(AccessType.https);


        AccessCacheItem accessCacheItem = generateAccessCacheItem(
            runId,
            fileId,
            outputFileUrl,
            accessMethod.getType()
            // mimetype...?;
        );
        accessCache.put(runId, fileId, accessCacheItem);
       

        AccessCacheItem cacheItem = accessCache.get(runId, fileId);
        //ignoring checking if file is in cache, file shold be added to cache
        if (cacheItem == null) {
            String exceptionMessage = "Taku's trace AccessRequestHandler.AccessURL.handlerequest: invalid access_id/object_id " + fileId + '/' + runId;
            loggingUtil.error("Exception occurred: " + exceptionMessage);
            throw new ResourceNotFoundException(exceptionMessage);
        }

        AccessURL accessURL = generateAccessURLForFile();
        return accessURL;
    }


    private static String findMatchingKey(Map<String, String> map, String fileId) {
        for (String key : map.keySet()) {
            if (key.endsWith(fileId)) {
                return key;
            }
        }
        return null; // If no matching key is found
    }


    /**
     * Constructs the streaming endpoint URL for the given ids
     * @return AccessURL pointing to this service's streaming endpoint
     */
    private AccessURL generateAccessURLForFile() {

        String path = WES_API_V1 + "/stream/" + runId + "/" + fileId;

        StringBuffer uriBuffer = new StringBuffer(serverProps.getScheme() + "://");
        uriBuffer.append(serverProps.getHostname());
        if (!serverProps.getPublicApiPort().equals("80")) {
            uriBuffer.append(":" + serverProps.getPublicApiPort());
        }
        // System.out.println("AccessRH.AccessURL.generateAccessURLForFile");
        uriBuffer.append(path);
        // System.out.println("taku's trace, generated access URL for file");
        return new AccessURL(URI.create(uriBuffer.toString()));
    }
}

package org.ga4gh.starterkit.wes.utils.requesthandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.ga4gh.starterkit.common.exception.ResourceNotFoundException;
import org.ga4gh.starterkit.common.requesthandler.RequestHandler;
import org.ga4gh.starterkit.wes.utils.cache.AccessCache;
import org.ga4gh.starterkit.wes.utils.cache.AccessCacheItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enables handling logic for the streaming endpoint, allows files stored on the 
 * service's file system to be streamed over the API
 */
public class FileStreamRequestHandler implements RequestHandler<Void> {

    @Autowired
    private AccessCache accessCache;

    private String runId;
    private String fileId;
    private HttpServletResponse response;

    /**
     * Prepares the request handler with input params from the controller function
     * @param runId WES Run identifier
     * @param fileId file name/identifier
     * @param response low-level Spring response object handling file streaming
     * @return the prepared request handler
     */
    public FileStreamRequestHandler prepare(String runId, String fileId, HttpServletResponse response) {
        this.runId = runId;
        this.fileId = fileId;
        this.response = response;
        return this;
    }

    /**
     * Streams the file contents referenced by the provided object id and access id
     * to client
     */
    public Void handleRequest() {
        // look up the access cache to see if a valid set of object id and 
        // access id was provided
        System.out.println("taku's trace, file stream handler");
        AccessCacheItem cacheItem = accessCache.get(runId, fileId);
        if (cacheItem == null) {
            throw new ResourceNotFoundException("invalid file_id/run_id");
        }

        try {
            // Open file input stream
            
            InputStream inputStream = new FileInputStream(new File(cacheItem.getObjectPath()));

            // Set Response headers
            // response.addHeader("Content-Disposition", "attachment");
            response.addHeader("Content-Disposition", "attachment; filename="+fileId);
            if (cacheItem.getMimeType() != null) {
                response.setContentType(cacheItem.getMimeType());
            }

            // copy file input stream to response's output stream
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            // TODO THROW REST CONTROLLER EXCEPTION
            return null;
        }

        return null;
    }
}

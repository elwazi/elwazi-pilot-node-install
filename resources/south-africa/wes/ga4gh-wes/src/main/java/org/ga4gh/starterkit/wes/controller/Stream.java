package org.ga4gh.starterkit.wes.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import static org.ga4gh.starterkit.wes.constant.WesApiConstants.WES_API_V1;
import org.ga4gh.starterkit.common.util.logging.LoggingUtil;
import org.ga4gh.starterkit.wes.model.AccessURL;
import org.ga4gh.starterkit.wes.utils.requesthandler.AccessRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.FileStreamRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stream handler, enables client to access bytes for files on the server machine
 * that the client doesn't have direct access to 
 */
@RestController
@RequestMapping(WES_API_V1 + "/stream")
public class Stream {

    @Resource(name = "fileStreamRequestHandler")
    private FileStreamRequestHandler fileStreamRequestHandler;

    @Autowired
    private LoggingUtil loggingUtil;

    @Resource(name = "accessRequestHandler")
    private AccessRequestHandler accessRequestHandler;    

    /**
     * Stream file bytes for the requested WES Run's output file name/id
     * @param runId WES Run id
     * @param fileId file name/id
     * @param response Spring HttpServletResponse
     */
    @GetMapping(path = "/{run_id:.+}/{file_id:.+}")
    public void streamFile(
        @PathVariable(name = "run_id") String runId,
        @PathVariable(name = "file_id") String fileId,
        HttpServletResponse response
    ) {
        loggingUtil.debug("Public API request: local file streaming. run id='" + runId + "', file id='" + fileId + "'");
        fileStreamRequestHandler.prepare(runId, fileId, response).handleRequest();
    }
}

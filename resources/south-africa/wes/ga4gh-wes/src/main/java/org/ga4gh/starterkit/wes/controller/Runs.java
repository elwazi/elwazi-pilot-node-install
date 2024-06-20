package org.ga4gh.starterkit.wes.controller;

import static org.ga4gh.starterkit.wes.constant.WesApiConstants.WES_API_V1;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.ga4gh.starterkit.common.util.logging.LoggingUtil;
import org.ga4gh.starterkit.wes.model.AccessURL;
import org.ga4gh.starterkit.wes.utils.requesthandler.AccessRequestHandler;
import org.ga4gh.starterkit.wes.model.RunId;
import org.ga4gh.starterkit.wes.model.RunLog;
import org.ga4gh.starterkit.wes.model.RunOutput;
import org.ga4gh.starterkit.wes.model.RunOutputStream;
import org.ga4gh.starterkit.wes.model.RunStatus;
import org.ga4gh.starterkit.wes.model.RunsListResponse;
import org.ga4gh.starterkit.wes.model.State;
import org.ga4gh.starterkit.wes.model.WorkflowType;
import org.ga4gh.starterkit.wes.utils.hibernate.WesHibernateUtil;
import org.ga4gh.starterkit.wes.utils.requesthandler.FileStreamRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.GetRunLogRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.GetRunOutputRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.GetOutputStreamRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.GetRunStatusRequestHandler;
import org.ga4gh.starterkit.wes.utils.requesthandler.SubmitRunRequestHandler;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller functions for launching, monitoring, and canceling workflow runs
 */
    @RestController
    @RequestMapping(WES_API_V1 + "/runs")
    public class Runs {

        @Resource
        private SubmitRunRequestHandler submitRunRequest;

        @Resource
        private GetRunLogRequestHandler getRunLog;

        @Resource
        private GetRunOutputRequestHandler getRunOutput;

        @Resource
        private GetOutputStreamRequestHandler getRunOutputStream;

        @Resource
        private GetRunStatusRequestHandler getRunStatus;

        @Autowired
        private LoggingUtil loggingUtil;      
        
        @Resource(name = "accessRequestHandler")
        private AccessRequestHandler accessRequestHandler;        

    @Autowired
    private WesHibernateUtil hibernateUtil;

    /**
     * Display run list
     * @return run list
     */
    @GetMapping
    public RunsListResponse getRuns() throws JsonProcessingException {

        // get run logs from wes_run table
        Session session = hibernateUtil.newTransaction();
        String listRunsQuery = "select final_run_log_json from wes_run;";
        NativeQuery<String> query = session.createSQLQuery(listRunsQuery);
        List<String> rawRecords = query.getResultList();

        // System.out.println("rawRecords-Trace: " + rawRecords); //taku's trace

        hibernateUtil.endTransaction(session);

        // get run_id and state for each run log
        List<RunStatus> runStatusArrayList = new ArrayList<>();
        for (String rawRecord : rawRecords){
            if(rawRecord!=null) {
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> map = mapper.readValue(rawRecord,Map.class);
                runStatusArrayList.add(
                        new RunStatus(
                                (String) map.get("run_id"),
                                State.valueOf((String) map.get("state"))));
            }
        }
        RunsListResponse runsListResponse = new RunsListResponse(runStatusArrayList);
        System.out.println("runsListResponse-Trace: " + runsListResponse);  //taku's trace
        return runsListResponse;
    }

    /**
     * Launch a new workflow run
     * @param workflowType workflow language specification
     * @param workflowTypeVersion workflow language specification version
     * @param workflowUrl URL to workflow source
     * @param workflowParams raw JSON string of workflow run input parameters
     * @param tags raw JSON string indicating key:value tags
     * @param workflowEngineParameters raw JSON string indicating key:value engine parameters
     * @return run identifier for the newly submitted run
     */
    @PostMapping
    public RunId createRun(
        @RequestParam("workflow_type") WorkflowType workflowType,
        @RequestParam("workflow_type_version") String workflowTypeVersion,
        @RequestParam("workflow_url") String workflowUrl,
        @RequestParam("workflow_params") String workflowParams,
        @RequestParam(name = "tags", required = false) String tags,
        @RequestParam(name = "workflow_engine_parameters", required = false) String workflowEngineParameters
        // @RequestParam("workflow_attachment") List<String> workflowAttachment
    ) {
        return submitRunRequest.prepare(workflowType, workflowTypeVersion, workflowUrl, workflowParams, tags, null).handleRequest();
    }

    /**
     * Get log information for a requested run 
     * @param runId run identifier
     * @return run log information
     */
    @GetMapping(path = "/{run_id:.+}")
    public RunLog getRunLog(
        @PathVariable(name = "run_id") String runId
    ) {
        return getRunLog.prepare(runId).handleRequest();
    }

    /**
     * Cancel a run
     * @param runId run identifier to cancel
     * @return run identifier
     */
    @PostMapping(path = "/{run_id:.+}/cancel")
    public RunId cancelRun(
        @PathVariable(name = "run_id") String runId
    ) {
        return null;
    }

    /**
     * Get run state/status for a requested run
     * @param runId run identifier
     * @return run status
     */
    @GetMapping(path = "/{run_id:.+}/status")
    public RunStatus runStatus(
        @PathVariable(name = "run_id") String runId
    ) {
        return getRunStatus.prepare(runId).handleRequest();
    }

    @GetMapping(path = "/{run_id:.+}/outputs")
    public RunOutput runOutput(
        @PathVariable(name = "run_id") String runId
    ) {
        return getRunOutput.prepare(runId).handleRequest();
    }

// /**
//  * Stream file bytes for the requested WesRun ID and file name/id, streaming in console
//  * @param runId Run identifier     
//  * @param fileId File identifier
//  * @return RunOutputStream object
//  */
// @GetMapping(path = "/{run_id:.+}/stream/{file_id:.+}")
// public RunOutputStream runOutputStream(
//     @PathVariable(name = "run_id") String runId,
//     @PathVariable(name = "file_id") String fileId
// ) {
//     System.out.println("Taku's trace: FileOutput..."); // You can keep this for debugging
//     // Assuming that getRunOutputStream.prepare() returns a RunOutputStream object
//     return getRunOutputStream.prepare(runId, fileId).handleRequest();
// }


/**
 * Get an access URL for fetching the file Object's  file bytes
 * @param runId WesRun id
 * @param fileId output file identifier/name
 * @return a WES output file AccessURL indicating file bytes location
 */ 
@GetMapping(path = "/{run_id:.+}/access/{file_id:.+}")
public AccessURL getAccessURLById(
    @PathVariable(name = "run_id") String runId,
    @PathVariable(name = "file_id") String fileId
) {
    loggingUtil.debug("Public API request: AccessURL for RUN id '" + runId + "', file id '" + fileId + "'");
    System.out.println("Taku's trace, streaming");
    return accessRequestHandler.prepare(runId, fileId).handleRequest();
}


    /**
     * @return SubmitRunRequestHandler return the submitRunRequest
     */
    public SubmitRunRequestHandler getSubmitRunRequest() {
        return submitRunRequest;
    }

    /**
     * @param submitRunRequest the submitRunRequest to set
     */
    public void setSubmitRunRequest(SubmitRunRequestHandler submitRunRequest) {
        this.submitRunRequest = submitRunRequest;
    }

    /**
     * @return GetRunLogRequestHandler return the getRunLog
     */
    public GetRunLogRequestHandler getGetRunLog() {
        return getRunLog;
    }

    /**
     * @param getRunLog the getRunLog to set
     */
    public void setGetRunLog(GetRunLogRequestHandler getRunLog) {
        this.getRunLog = getRunLog;
    }

    /**
     * @return GetRunOutputRequestHandler return the getRunOutput
     */
    public GetRunOutputRequestHandler getGetRunOutput() {
        return getRunOutput;
    }

    /**
     * @param getRunOutput the getRunOutput to set
     */
    public void setGetRunOutput(GetRunOutputRequestHandler getRunOutput) {
        this.getRunOutput = getRunOutput;
    }

    /**
     * @return GetOutputStreamRequestHandler return the getRunOutputStream
     */
    public GetOutputStreamRequestHandler getGetRunOutputStream() {
        return getRunOutputStream;
    }

    /**
     * @param getRunOutputStream the getRunOutputStream to set
     */
    public void setGetRunOutputStream(GetOutputStreamRequestHandler getRunOutputStream) {
        this.getRunOutputStream = getRunOutputStream;
    }

    /**
     * @return GetRunStatusRequestHandler return the getRunStatus
     */
    public GetRunStatusRequestHandler getGetRunStatus() {
        return getRunStatus;
    }

    /**
     * @param getRunStatus the getRunStatus to set
     */
    public void setGetRunStatus(GetRunStatusRequestHandler getRunStatus) {
        this.getRunStatus = getRunStatus;
    }

    /**
     * @return LoggingUtil return the loggingUtil
     */
    public LoggingUtil getLoggingUtil() {
        return loggingUtil;
    }

    /**
     * @param loggingUtil the loggingUtil to set
     */
    public void setLoggingUtil(LoggingUtil loggingUtil) {
        this.loggingUtil = loggingUtil;
    }

    /**
     * @return AccessRequestHandler return the accessRequestHandler
     */
    public AccessRequestHandler getAccessRequestHandler() {
        return accessRequestHandler;
    }

    /**
     * @param accessRequestHandler the accessRequestHandler to set
     */
    public void setAccessRequestHandler(AccessRequestHandler accessRequestHandler) {
        this.accessRequestHandler = accessRequestHandler;
    }

    /**
     * @return WesHibernateUtil return the hibernateUtil
     */
    public WesHibernateUtil getHibernateUtil() {
        return hibernateUtil;
    }

    /**
     * @param hibernateUtil the hibernateUtil to set
     */
    public void setHibernateUtil(WesHibernateUtil hibernateUtil) {
        this.hibernateUtil = hibernateUtil;
    }

}
package org.ga4gh.starterkit.wes.model;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Directly from WES specification, contains information associated with
 * a workflow run, status, and outputs and streaming URLs
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunOutputStream {

    // Run identifier
    private String runId;    
    
    //file name to get single file and path
    private String fileId;


    // Workflow run state
    private State state;

    // References to output file locations produced during the workflow run
    private Map<String, String> outputs;

    //for input stream, used a different approach
    private String outputStream;

}

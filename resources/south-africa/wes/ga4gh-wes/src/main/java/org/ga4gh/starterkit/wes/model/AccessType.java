package org.ga4gh.starterkit.wes.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Enumeration of different Access Types for
 * Access Types correspond closely with URL
 * schemes for fetching file bytes (e.g. file://, https://, s3://, etc.)
 */
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public enum AccessType {
    // TODO: incorporate other data source schemes, for now only file, s3, https
    s3,
    // GS,
    // FTP,
    // GSIFTP,
    // GLOBUS,
    // HTSGET,
    https,
    file,
}

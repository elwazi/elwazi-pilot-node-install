package org.ga4gh.starterkit.wes.utils.cache;

import org.ga4gh.starterkit.wes.model.AccessType;

/**
 * A single item within the access cache, stores information on how to access
 * the file bytes for a composite WES Run id + file name/id
 */
public class AccessCacheItem {

    private String runId;
    private String fileId;
    private String objectPath;
    private AccessType accessType;
    private String mimeType;

    /**
     * Instantiates a new AccessCacheItem
     */
    public AccessCacheItem() {

    }

    /**
     * Assign runId
     * @param runId WES Run id
     */
    public void setObjectId(String runId) {
        this.runId = runId;
    }

    /**
     * Retrieve runId
     * @return WES RUN id
     */
    public String getObjectId() {
        return runId;
    }

    /**
     * Assign fileId
     * @param fileId file name/id
     */
    public void setAccessId(String fileId) {
        this.fileId = fileId;
    }

    /**
     * Retrieve fileId
     * @return file nameid
     */
    public String getAccessId() {
        return fileId;
    }

    /**
     * Assign output file Path
     * @param objectPath path to the file bytes
     */
    public void setObjectPath(String objectPath) {
        this.objectPath = objectPath;
    }

    /**
     * Retrieve objectPath
     * @return path to the file bytes
     */
    public String getObjectPath() {
        return objectPath;
    }

    /**
     * Assign accessType
     * @param accessType access type for file byte source (ie URL scheme)
     */
    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    /**
     * Retrieve accessType
     * @return access type for file byte source (ie URL scheme)
     */
    public AccessType getAccessType() {
        return accessType;
    }

    /**
     * Assign mimeType
     * @param mimeType valid media type
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Retrieve mimeType
     * @return valid media type
     */
    public String getMimeType() {
        return mimeType;
    }
}

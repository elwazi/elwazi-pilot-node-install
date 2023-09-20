package org.ga4gh.starterkit.wes.utils.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Cache singleton storing information mapping WES run ids and file name/id
 * to the byte source for a requested WES Run's output file
 */
public class AccessCache {

    /**
     * a cache mapping WES run ID + output file name/id to richer AccessCacheItem info
     */
    private LoadingCache<String, AccessCacheItem> cache;

    /**
     * Instantiates a new AccessCache
     */
    public AccessCache() {
        buildCache();
    }

    /**
     * Builds the cache
     */
    private void buildCache() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(
                new CacheLoader<String, AccessCacheItem>(){
                    public AccessCacheItem load(String key) {
                        return new AccessCacheItem();
                    }
                }
            );
    }

    /**
     * Add a new item to the cache
     * @param runId WES Run id
     * @param fileId file name/id
     * @param value the access cache item providing info on how to access the bytes
     */
    public void put(String runId, String fileId, AccessCacheItem value) {
        cache.put(getCompositeKey(runId, fileId), value);
    }

    /**
     * Retrieve an item from the cache
     * @param runId WES Run id
     * @param fileId file name/id
     * @return the access cache item for the given ids, provides info on how to access the bytes
     */
    public AccessCacheItem get(String runId, String fileId) {
        return cache.getIfPresent(getCompositeKey(runId, fileId));
    }

    /**
     * Construct a key for the cache based on WES Run id and file name/id
     * @param runId WES Run id
     * @param fileId file name/id
     * @return a composite id constructed from both ids
     */
    private String getCompositeKey(String runId, String fileId) {
        // System.out.println("AccessCache, compositekey: "+runId.toString() + ":" + fileId);
        return runId.toString() + ":" + fileId;
    }
}

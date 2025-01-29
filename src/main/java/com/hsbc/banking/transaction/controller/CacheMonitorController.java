package com.hsbc.banking.transaction.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheMonitorController {
    private final CacheManager cacheManager;

    public CacheMonitorController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (caffeineCache != null) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats cacheStats = nativeCache.stats();
                
                Map<String, Object> cacheMetrics = new HashMap<>();
                cacheMetrics.put("hitCount", cacheStats.hitCount());
                cacheMetrics.put("missCount", cacheStats.missCount());
                cacheMetrics.put("hitRate", cacheStats.hitRate());
                cacheMetrics.put("missRate", cacheStats.missRate());
                cacheMetrics.put("evictionCount", cacheStats.evictionCount());
                cacheMetrics.put("estimatedSize", nativeCache.estimatedSize());
                
                stats.put(cacheName, cacheMetrics);
            }
        });
        
        return stats;
    }
} 
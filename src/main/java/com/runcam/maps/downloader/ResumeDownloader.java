package com.runcam.maps.downloader;

import com.runcam.maps.utils.TileCoordinateUtils;
import com.runcam.maps.utils.ChinaBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResumeDownloader {
    private static final Logger logger = LoggerFactory.getLogger(ResumeDownloader.class);
    
    private final TileDownloader downloader;
    private final ProgressTracker progressTracker;
    
    public ResumeDownloader(String outputDirectory, int maxConcurrentDownloads, int timeoutSeconds) {
        this.downloader = new TileDownloader(outputDirectory, maxConcurrentDownloads, timeoutSeconds);
        this.progressTracker = new ProgressTracker(outputDirectory);
    }
    
    public void downloadTilesWithResume(int minZoom, int maxZoom) {
        try {
            long alreadyDownloaded = progressTracker.getDownloadedCount();
            if (alreadyDownloaded > 0) {
                logger.info("发现已下载 {} 个瓦片，将继续下载未完成的部分", alreadyDownloaded);
            }
            
            downloadZoomLevelWithResume(minZoom, maxZoom);
            
        } finally {
            downloader.shutdown();
        }
    }
    
    private void downloadZoomLevelWithResume(int minZoom, int maxZoom) {
        ChinaBounds bounds = new ChinaBounds();
        
        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            int minX = TileCoordinateUtils.lon2tileX(bounds.getWest(), zoom);
            int maxX = TileCoordinateUtils.lon2tileX(bounds.getEast(), zoom);
            int minY = TileCoordinateUtils.lat2tileY(bounds.getNorth(), zoom);
            int maxY = TileCoordinateUtils.lat2tileY(bounds.getSouth(), zoom);
            
            logger.info("检查缩放等级 {} 的下载进度...", zoom);
            
            int skipped = 0;
            int toDownload = 0;
            
            // 先统计需要下载的数量
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (progressTracker.isDownloaded(x, y, zoom)) {
                        skipped++;
                    } else {
                        toDownload++;
                    }
                }
            }
            
            if (toDownload == 0) {
                logger.info("缩放等级 {} 已全部下载完成 (共 {} 个瓦片)", zoom, skipped);
                continue;
            }
            
            logger.info("缩放等级 {}: 跳过已下载 {} 个，需要下载 {} 个", 
                zoom, skipped, toDownload);
            
            // 使用原始的下载器，但会跳过已下载的
            downloader.downloadTiles(zoom, zoom);
        }
    }
    
    public void resetProgress() {
        progressTracker.reset();
        logger.info("已重置下载进度");
    }
    
    public long getDownloadedCount() {
        return progressTracker.getDownloadedCount();
    }
}
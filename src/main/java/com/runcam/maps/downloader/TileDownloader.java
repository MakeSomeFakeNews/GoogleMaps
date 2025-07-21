package com.runcam.maps.downloader;

import com.runcam.maps.utils.TileCoordinateUtils;
import com.runcam.maps.utils.ChinaBounds;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class TileDownloader {
    private static final Logger logger = LoggerFactory.getLogger(TileDownloader.class);

    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    private final String outputDirectory;
    private final int maxConcurrentDownloads;
    private final int timeoutSeconds;

    private final AtomicLong totalTiles = new AtomicLong(0);
    private final AtomicLong downloadedTiles = new AtomicLong(0);
    private final AtomicLong failedTiles = new AtomicLong(0);

    public TileDownloader(String outputDirectory, int maxConcurrentDownloads, int timeoutSeconds) {
        this.outputDirectory = outputDirectory;
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.timeoutSeconds = timeoutSeconds;

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
                        .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                        .build())
                .build();

        this.executorService = new ThreadPoolExecutor(
                maxConcurrentDownloads,
                maxConcurrentDownloads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxConcurrentDownloads * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void downloadTiles(int minZoom, int maxZoom) {
        try {
            Files.createDirectories(Paths.get(outputDirectory));

            ChinaBounds bounds = new ChinaBounds();

            for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                downloadZoomLevel(zoom, bounds);
            }

            logger.info("Download completed. Total: {}, Downloaded: {}, Failed: {}",
                    totalTiles.get(), downloadedTiles.get(), failedTiles.get());

        } catch (IOException e) {
            logger.error("Error creating output directory", e);
        }
    }

    private void downloadZoomLevel(int zoom, ChinaBounds bounds) {
        int minX = TileCoordinateUtils.lon2tileX(bounds.getWest(), zoom);
        int maxX = TileCoordinateUtils.lon2tileX(bounds.getEast(), zoom);
        int minY = TileCoordinateUtils.lat2tileY(bounds.getNorth(), zoom);
        int maxY = TileCoordinateUtils.lat2tileY(bounds.getSouth(), zoom);

        // 检查已下载的文件数量
        ProgressTracker tracker = new ProgressTracker(outputDirectory);
        int alreadyDownloaded = 0;
        int totalInZoom = (maxX - minX + 1) * (maxY - minY + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                String filePath = String.format("%s/%d/%d/%d.jpg", outputDirectory, zoom, x, y);
                if (Files.exists(Paths.get(filePath))) {
                    alreadyDownloaded++;
                }
            }
        }

        logger.info("缩放等级 {}: 共 {} 个瓦片, 已下载 {} 个, 需下载 {} 个",
                zoom, totalInZoom, alreadyDownloaded, totalInZoom - alreadyDownloaded);

        if (alreadyDownloaded == totalInZoom) {
            logger.info("缩放等级 {} 已全部完成，跳过", zoom);
            totalTiles.addAndGet(totalInZoom);
            downloadedTiles.addAndGet(totalInZoom);
            return;
        }

        long tilesInZoom = totalInZoom;
        totalTiles.addAndGet(tilesInZoom);
        downloadedTiles.addAndGet(alreadyDownloaded);

        CountDownLatch latch = new CountDownLatch(totalInZoom - alreadyDownloaded);
        AtomicInteger currentInZoom = new AtomicInteger(alreadyDownloaded);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                final int tileX = x;
                final int tileY = y;
                final int tileZ = zoom;

                String filePath = String.format("%s/%d/%d/%d.jpg", outputDirectory, zoom, x, y);
                if (Files.exists(Paths.get(filePath))) {
                    continue; // 跳过已下载的
                }

                int finalAlreadyDownloaded = alreadyDownloaded;
                executorService.submit(() -> {
                    try {
                        downloadTile(tileX, tileY, tileZ);
                        int current = currentInZoom.incrementAndGet();
                        long totalDownloaded = downloadedTiles.incrementAndGet();

                        // 显示进度（只显示当前需要下载的文件进度）
                        int needDownload = totalInZoom - finalAlreadyDownloaded;
                        if (needDownload > 0) {
                            double percent = (double) current / needDownload * 100;
                            if (current % 100 == 0 || current == needDownload) {
                                System.out.printf("缩放等级 %d: %d/%d (%.1f%%)%n",
                                        tileZ, current, needDownload, percent);
                                
                                if (current == needDownload) {
                                    System.out.printf("缩放等级 %d 完成!%n", tileZ);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("下载失败 {}/{}/{}: {}", tileX, tileY, tileZ, e.getMessage());
                        failedTiles.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        try {
            latch.await();
            logger.info("缩放等级 {} 完成! 成功: {}, 失败: {}",
                    zoom, currentInZoom.get(), failedTiles.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("下载中断", e);
        }
    }

    private void downloadTile(int x, int y, int z) throws IOException {
        String url = buildGoogleMapsUrl(x, y, z);
        String filePath = String.format("%s/%d/%d/%d.jpg", outputDirectory, z, x, y);

        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return;
        }

        Files.createDirectories(path.getParent());

        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        try (var response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                try (InputStream inputStream = response.getEntity().getContent();
                     OutputStream outputStream = Files.newOutputStream(path)) {
                    inputStream.transferTo(outputStream);
                }
            } else {
                throw new IOException("HTTP " + response.getCode());
            }
        }
    }

    private String buildGoogleMapsUrl(int x, int y, int z) {
        // Rotate between different Google Maps servers to avoid rate limiting
        int serverNum = (x + y + z) % 4;
        String[] servers = {"mt0", "mt1", "mt2", "mt3"};
        return String.format("https://%s.google.com/vt/lyrs=s&x=%d&y=%d&z=%d", servers[serverNum], x, y, z);
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            httpClient.close();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error shutting down", e);
        }
    }

    public double getProgress() {
        return totalTiles.get() > 0 ? (double) downloadedTiles.get() / totalTiles.get() : 0;
    }

    public long getTotalTiles() {
        return totalTiles.get();
    }

    public long getDownloadedTiles() {
        return downloadedTiles.get();
    }

    public long getFailedTiles() {
        return failedTiles.get();
    }
}
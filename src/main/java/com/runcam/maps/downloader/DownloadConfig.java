package com.runcam.maps.downloader;

public class DownloadConfig {
    private String outputDirectory = "./tiles";
    private int minZoom = 1;
    private int maxZoom = 12;
    private int maxConcurrentDownloads = 10;
    private final int timeoutSeconds = 30;
    
    public DownloadConfig() {}
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public int getMinZoom() {
        return minZoom;
    }
    
    public void setMinZoom(int minZoom) {
        this.minZoom = minZoom;
    }
    
    public int getMaxZoom() {
        return maxZoom;
    }
    
    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }
    
    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }
    
    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public String toString() {
        return "DownloadConfig{" +
                "outputDirectory='" + outputDirectory + '\'' +
                ", minZoom=" + minZoom +
                ", maxZoom=" + maxZoom +
                ", maxConcurrentDownloads=" + maxConcurrentDownloads +
                ", timeoutSeconds=" + timeoutSeconds +
                '}';
    }
}
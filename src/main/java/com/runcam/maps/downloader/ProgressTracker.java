package com.runcam.maps.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ProgressTracker {
    private final String progressFile;
    private final Set<String> downloadedTiles;
    
    public ProgressTracker(String outputDirectory) {
        this.progressFile = outputDirectory + "/progress.txt";
        this.downloadedTiles = new HashSet<>();
        loadProgress();
    }
    
    private void loadProgress() {
        Path path = Paths.get(progressFile);
        if (Files.exists(path)) {
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(downloadedTiles::add);
            } catch (IOException e) {
                System.err.println("Warning: Could not load progress file: " + e.getMessage());
            }
        }
    }
    
    public boolean isDownloaded(int x, int y, int z) {
        return downloadedTiles.contains(getTileKey(x, y, z));
    }
    
    public void markDownloaded(int x, int y, int z) {
        String key = getTileKey(x, y, z);
        downloadedTiles.add(key);
        
        try {
            Files.write(Paths.get(progressFile), (key + "\n").getBytes(), 
                       java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Warning: Could not save progress: " + e.getMessage());
        }
    }
    
    private String getTileKey(int x, int y, int z) {
        return z + "/" + x + "/" + y;
    }
    
    public int getDownloadedCount() {
        return downloadedTiles.size();
    }
    
    public void reset() {
        downloadedTiles.clear();
        try {
            Files.deleteIfExists(Paths.get(progressFile));
        } catch (IOException e) {
            System.err.println("Warning: Could not reset progress file: " + e.getMessage());
        }
    }
}
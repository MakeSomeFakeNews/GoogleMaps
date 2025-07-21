package com.runcam.maps;

import com.runcam.maps.downloader.DownloadConfig;
import com.runcam.maps.downloader.TileDownloader;
import com.runcam.maps.utils.TileCoordinateUtils;

import java.util.Scanner;

public class GoogleMapApplication {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Google Maps China Tile Downloader ===");
        
        DownloadConfig config = new DownloadConfig();
        
        // 配置参数
        System.out.print("输出目录 (默认: ./tiles): ");
        String outputDir = scanner.nextLine().trim();
        if (!outputDir.isEmpty()) {
            config.setOutputDirectory(outputDir);
        }
        
        System.out.print("最小缩放等级 (1-18, 默认: 1): ");
        String minZoomStr = scanner.nextLine().trim();
        if (!minZoomStr.isEmpty()) {
            config.setMinZoom(Integer.parseInt(minZoomStr));
        }
        
        System.out.print("最大缩放等级 (1-18, 默认: 12): ");
        String maxZoomStr = scanner.nextLine().trim();
        if (!maxZoomStr.isEmpty()) {
            config.setMaxZoom(Integer.parseInt(maxZoomStr));
        }
        
        System.out.print("并发下载数 (默认: 10): ");
        String concurrentStr = scanner.nextLine().trim();
        if (!concurrentStr.isEmpty()) {
            config.setMaxConcurrentDownloads(Integer.parseInt(concurrentStr));
        }
        
        // 计算预计下载的瓦片数量
        long totalTiles = 0;
        for (int zoom = config.getMinZoom(); zoom <= config.getMaxZoom(); zoom++) {
            totalTiles += TileCoordinateUtils.getTileCountInChina(zoom);
        }
        
        System.out.println("\n=== 下载配置 ===");
        System.out.println("输出目录: " + config.getOutputDirectory());
        System.out.println("缩放等级: " + config.getMinZoom() + " - " + config.getMaxZoom());
        System.out.println("并发数: " + config.getMaxConcurrentDownloads());
        System.out.println("预计瓦片数量: " + String.format("%,d", totalTiles));
        System.out.println("预计存储空间: " + String.format("%.2f GB", totalTiles * 0.05));
        
        System.out.print("\n确认开始下载? (y/N): ");
        String confirm = scanner.nextLine().trim();
        
        if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
            // 检查是否有已下载的文件并计算续传进度
            long existingFiles = 0;
            for (int zoom = config.getMinZoom(); zoom <= config.getMaxZoom(); zoom++) {
                try {
                    java.nio.file.Path zoomDir = java.nio.file.Paths.get(config.getOutputDirectory(), String.valueOf(zoom));
                    if (java.nio.file.Files.exists(zoomDir)) {
                        existingFiles += java.nio.file.Files.walk(zoomDir)
                            .filter(p -> p.toString().endsWith(".jpg"))
                            .count();
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
            
            if (existingFiles > 0) {
                System.out.println("检测到已下载 " + existingFiles + " 个文件，将自动续传...");
            }
            
            TileDownloader downloader = new TileDownloader(
                config.getOutputDirectory(),
                config.getMaxConcurrentDownloads(),
                config.getTimeoutSeconds()
            );
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n正在安全关闭下载器...");
                downloader.shutdown();
                System.out.println("下载进度已保存，下次启动将自动续传");
            }));
            
            try {
                System.out.println("开始下载，按 Ctrl+C 可随时中断...");
                System.out.println("================================");
                
                downloader.downloadTiles(config.getMinZoom(), config.getMaxZoom());
                
                System.out.println("================================");
                System.out.println("下载完成!");
                System.out.printf("总计: %d 个瓦片\n", downloader.getDownloadedTiles());
                if (downloader.getFailedTiles() > 0) {
                    System.out.printf("失败: %d 个瓦片\n", downloader.getFailedTiles());
                }
            } catch (Exception e) {
                System.err.println("下载出错: " + e.getMessage());
            } finally {
                downloader.shutdown();
            }
        } else {
            System.out.println("下载取消");
        }
        
        scanner.close();
    }
}

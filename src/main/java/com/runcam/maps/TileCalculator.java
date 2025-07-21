package com.runcam.maps;

public class TileCalculator {

    public static final double NORTH = 53.55;
    public static final double SOUTH = 18.16;
    public static final double EAST = 134.77;
    public static final double WEST = 73.50;

    public static int lon2tileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * Math.pow(2.0, zoom));
    }

    public static int lat2tileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        double n = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        return (int) Math.floor((1.0 - n / Math.PI) / 2.0 * Math.pow(2.0, zoom));
    }

    public static void main(String[] args) {
        long totalTiles = 0;

        for (int zoom = 3; zoom <= 13; zoom++) {
            int x1 = lon2tileX(EAST, zoom);
            int y1 = lat2tileY(NORTH, zoom);
            int x2 = lon2tileX(WEST, zoom);
            int y2 = lat2tileY(SOUTH, zoom);

            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);

            int tilesThisZoom = (endX - startX + 1) * (endY - startY + 1);
            totalTiles += tilesThisZoom;

            System.out.printf("Zoom %2d: %d tiles\n", zoom, tilesThisZoom);
        }

        System.out.println("Total tiles from zoom 3 to 20: " + totalTiles);
        double totalSizeKB = totalTiles * 20.0;
        double totalSizeGB = totalSizeKB / 1024 / 1024;

        System.out.printf("Total size: %.2f GB\n", totalSizeGB);
    }
}

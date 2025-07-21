package com.runcam.maps.utils;

public class TileCoordinateUtils {
    
    public static int lon2tileX(double lon, int zoom) {
        return (int) Math.floor(((lon + 180) / 360) * Math.pow(2, zoom));
    }
    
    public static int lat2tileY(double lat, int zoom) {
        return (int) Math.floor(
            ((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2) 
            * Math.pow(2, zoom)
        );
    }

    public static int getTileCountInChina(int zoom) {
        ChinaBounds bounds = new ChinaBounds();
        int minX = lon2tileX(bounds.getWest(), zoom);
        int maxX = lon2tileX(bounds.getEast(), zoom);
        int minY = lat2tileY(bounds.getNorth(), zoom);
        int maxY = lat2tileY(bounds.getSouth(), zoom);
        
        return (maxX - minX + 1) * (maxY - minY + 1);
    }
}
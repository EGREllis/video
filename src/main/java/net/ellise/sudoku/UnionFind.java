package net.ellise.sudoku;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UnionFind {
    private Map<Long,Integer> map = new HashMap<>();
    private int nRegions;
    private int nextKey;
    private int maxX;
    private int maxY;
    private boolean isX;

    public static UnionFind regionFor(BufferedImage image) {
        Raster data = image.getData();
        return new UnionFind(data.getWidth() - data.getMinX(), data.getHeight() - data.getMinY());
    }

    public UnionFind(int maxX, int maxY) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.isX = maxX > maxY;
    }

    public void connect(int x1, int y1, int x2, int y2) {
        long key = encode(x1, y1);
        long other = encode(x2, y2);
        if (map.containsKey(key) && map.containsKey(other) && map.get(key).equals(map.get(other))) {
            // Already connected
        } else if (map.containsKey(key) && map.containsKey(other) && !map.get(key).equals(map.get(other))) {
            // Two neighbouring regions have just collided
            // We need to merge one with the other
            int newGroup = map.get(key);
            int oldGroup = map.get(other);
            Set<Long> oldKeys = new HashSet<>();
            for (Map.Entry<Long,Integer> entry : map.entrySet()) {
                if (entry.getValue() == oldGroup) {
                    oldKeys.add(entry.getKey());
                }
            }
            for (Long oldKey : oldKeys) {
                map.put(oldKey, newGroup);
            }
            nRegions--;
        } else if (map.containsKey(key)) {
            map.put(other, map.get(key));
        } else if (map.containsKey(other)) {
            map.put(key, map.get(other));
        } else {
            map.put(key, nextKey);
            map.put(other, nextKey);
            nextKey++;
            nRegions++;
        }
    }

    public void normalise() {
        Set<Integer> groups = new HashSet<>(map.values());
        Map<Long,Integer> normalised = new HashMap<>();
        int nGroupId = 0;
        for (int groupId : groups) {
            for (Map.Entry<Long,Integer> entry : map.entrySet()) {
                if (entry.getValue().equals(groupId)) {
                    normalised.put(entry.getKey(), nGroupId);
                }
            }
            nGroupId++;
        }
        nRegions = nGroupId;
        nextKey = nGroupId;
        map = normalised;
    }

    public int getBiggestGroup() {
        Map<Integer,Integer> sizes = getSizeOfRegions();
        int key = 0;
        int max = sizes.get(0);
        for (Map.Entry<Integer,Integer> entry : sizes.entrySet()) {
            if (entry.getValue() > max) {
                key = entry.getKey();
                max = entry.getValue();
            }
        }
        return key;
    }

    public Map<Integer, Integer> getSizeOfRegions() {
        Map<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            int region = entry.getValue();
            if (result.containsKey(region)) {
                int count = result.get(region);
                result.put(region, count+1);
            } else {
                result.put(region, 1);
            }
        }
        return result;
    }

    private long encode(int x, int y) {
        if (isX) {
            return ((long)x)*maxX + y;
        } else {
            return ((long)y)*maxY + x;
        }
    }

    private Point decode(long hash) {
        if (isX) {
            int y = (int)(hash % maxX);
            int x = (int)((hash - y)/maxX);
            return new Point(x, y);
        } else {
            int x = (int)(hash % maxY);
            int y = (int)((hash-x)/maxY);
            return new Point(x, y);
        }
    }

    public int getNumberOfRegions() {
        return nRegions;
    }

    public int getMaxRegion() {
        return nextKey;
    }

    public int getRegion(int x, int y) {
        long key = encode(x, y);
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            return -1;
        }
    }
}

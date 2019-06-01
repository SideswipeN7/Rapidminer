package com.rapidminer.protoRM;

import java.util.*;


public class PointContainer {
    private LinkedList<PointData> pointsFriends = new LinkedList<>();
    private LinkedList<PointData> pointsEnemies = new LinkedList<>();

    public void sort() {
        Collections.sort(pointsFriends);
        Collections.sort(pointsEnemies);
    }

    public void add(PointType type, PointData pointData) {
        if (type == PointType.MyClass) {
            pointsFriends.add(pointData);
        } else if (type == PointType.OtherClass) {
            pointsEnemies.add(pointData);
        }
    }

    public PointPair get() {
        return new PointPair(pointsFriends.getFirst(), pointsEnemies.getFirst());
    }

    public boolean hasNext() {
        return pointsEnemies.size() > 1 || pointsFriends.size() > 1;
    }

    public PointPair next() {
        generateNewPair();
        return new PointPair(pointsFriends.getFirst(), pointsEnemies.getFirst());
    }

    private void generateNewPair() {
        if (pointsFriends.size() > 1 && pointsEnemies.size() > 1) {
            PointData mySecond = pointsFriends.get(1);
            PointData otherSecond = pointsEnemies.get(1);
            if (mySecond.compareTo(otherSecond) >= 0) {
                pointsFriends.pollFirst();
            } else {
                pointsEnemies.pollFirst();
            }
        } else if (pointsFriends.size() == 1 && pointsEnemies.size() > 1) {
            pointsEnemies.pollFirst();
        } else if (pointsFriends.size() > 1 && pointsEnemies.size() == 1) {
            pointsFriends.pollFirst();
        }
    }
}
package com.rapidminer.protoRM;

import com.rapidminer.tools.container.Tupel;

import java.util.*;

;


public class PointContainer {
    private double id_;
    private HashMap<PointType, LinkedList<PointData>> pointListMap_ = new HashMap<>();

    public PointContainer(double id_) {
        this.id_ = id_;
    }

    public void addPoint(PointType type, PointData point) {
        LinkedList<PointData> list = pointListMap_.get(type);
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(point);
        pointListMap_.put(type, list);
    }

    public void sort() {
        for (PointType type : PointType.values()) {
            LinkedList<PointData> list = pointListMap_.get(type);
            if (list != null) {
                Collections.sort(list);
                pointListMap_.put(type, list);
            }
        }
    }

    public Tupel<PointData, PointData> getPair() {
        LinkedList<PointData> myPoints = pointListMap_.get(PointType.MyClass);
        LinkedList<PointData> otherPoints = pointListMap_.get(PointType.OtherClass);
        if (myPoints.getFirst().compareTo(otherPoints.getFirst()) <= 0) {
            return new Tupel<>(myPoints.getFirst(), otherPoints.getFirst());
        } else {
            return new Tupel<>(otherPoints.getFirst(), myPoints.getFirst());
        }
    }

    public void generateNewPair() throws NullPointerException {
        LinkedList<PointData> myPoints = pointListMap_.get(PointType.MyClass);
        LinkedList<PointData> otherPoints = pointListMap_.get(PointType.OtherClass);
        if (myPoints.size() > 1 && otherPoints.size() > 1) {
            PointData myNextData = myPoints.get(1);
            PointData otherNextData = myPoints.get(1);
            if (myNextData.compareTo(otherNextData) >= 0) {
                myPoints.pollFirst();
            } else {
                otherPoints.pollFirst();
            }
        } else if (myPoints.size() == 1 && otherPoints.size() > 1) {
                otherPoints.pollFirst();
        } else if (myPoints.size() > 1 && otherPoints.size() == 1) {
                myPoints.pollFirst();
        } else {
            throw new NullPointerException();
        }
    }

    public long getPairId() {
        Tupel<PointData, PointData> tuple = getPair();
        return Cantor.pair((long) tuple.getFirst().getPointId(), (long) tuple.getSecond().getPointId());
    }
}

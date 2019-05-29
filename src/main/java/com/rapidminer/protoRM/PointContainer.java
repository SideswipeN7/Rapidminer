package com.rapidminer.protoRM;

import com.rapidminer.tools.container.Tupel;

import java.util.*;

;


public class PointContainer {
    private HashMap<PointType, LinkedList<PointData>> pointListMap_ = new HashMap<>();
    private int iterator_;
    private LinkedList<PointData> iterFirst;
    private LinkedList<PointData> iterSecond;

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
        setFirstPair();
    }

    public Tupel<PointData, PointData> getPair() {
        PointData point1 = iterFirst.get(iterator_);
        PointData point2 = iterSecond.getFirst();
        if (point1.compareTo(point2) <= 0) {
            return new Tupel<>(point1, point2);
        } else {
            return new Tupel<>(point2, point1);
        }
    }

    public void generateNewPair() throws NullPointerException {
            iterator_++;
        if (iterSecond.size() == 1 && iterator_ >= iterFirst.size()) {
            throw new NullPointerException();
        } else if (iterSecond.size() > 1 && iterator_  == iterFirst.size()) {
            iterSecond.pollFirst();
            iterator_ = 0;
        }
    }

    private void setFirstPair() {
        LinkedList<PointData> myPoints = pointListMap_.get(PointType.MyClass);
        LinkedList<PointData> otherPoints = pointListMap_.get(PointType.OtherClass);
        PointData my = myPoints.getFirst();
        PointData other = otherPoints.getFirst();
        if (my.compareTo(other) <= 0) {
            iterSecond = myPoints;
            iterFirst = otherPoints;
        } else {
            iterSecond = otherPoints;
            iterFirst = myPoints;
        }
        iterator_ = 0;
    }

    public long getPairId() {
        Tupel<PointData, PointData> tuple = getPair();
        return Cantor.pair((long) tuple.getFirst().getPointId(), (long) tuple.getSecond().getPointId());
    }
}

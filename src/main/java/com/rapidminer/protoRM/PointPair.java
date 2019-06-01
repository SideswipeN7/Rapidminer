package com.rapidminer.protoRM;


public class PointPair {
    private PointData first_;
    private PointData second_;


    PointPair(PointData point1, PointData point2) {
        if (point1.getId() > point2.getId()) {
            first_ = point2;
            second_ = point1;
        } else {
            first_ = point1;
            second_ = point2;
        }
    }

    public PointData getFirstPoint() {
        return first_;
    }

    public PointData getSecondPoint() {
        return second_;
    }

    public long getPairId() {
        return Cantor.pair((long) first_.getId(), (long) second_.getId());
    }
}

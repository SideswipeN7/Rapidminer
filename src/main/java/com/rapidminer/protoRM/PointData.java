package com.rapidminer.protoRM;

public class PointData implements Comparable<PointData> {
    private double pointId_;
    private double distance_;

    public PointData(double pointId, double distance) {
        this.pointId_ = pointId;
        this.distance_ = distance;
    }

    public double getId() {
        return pointId_;
    }

    @Override
    public int compareTo(PointData otherPoint) {
        Double my = distance_;
        return my.compareTo(otherPoint.distance_);
    }
}

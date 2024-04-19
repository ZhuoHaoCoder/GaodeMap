package com.example.gaodemap.util;

import com.amap.api.services.route.BusStep;

public class SchemeBusStep extends BusStep {
    private boolean isWalk = false;
    private boolean isBus = false;
    private boolean isRailway = false;
    private boolean isTaxi = false;
    private boolean isStart = false;
    private boolean isEnd = false;
    private boolean arrowExpend = false;

    public SchemeBusStep(BusStep busStep) {
        if (busStep != null) {
            this.setBusLine(busStep.getBusLine());
            this.setWalk(busStep.getWalk());
            this.setRailway(busStep.getRailway());
            this.setTaxi(busStep.getTaxi());
        }
    }

    public boolean isWalk() {
        return isWalk;
    }

    public void setWalk(boolean walk) {
        isWalk = walk;
    }

    public boolean isBus() {
        return isBus;
    }

    public void setBus(boolean bus) {
        isBus = bus;
    }

    public boolean isRailway() {
        return isRailway;
    }

    public void setRailway(boolean railway) {
        isRailway = railway;
    }

    public boolean isTaxi() {
        return isTaxi;
    }

    public void setTaxi(boolean taxi) {
        isTaxi = taxi;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }

    public boolean isArrowExpend() {
        return arrowExpend;
    }

    public void setArrowExpend(boolean arrowExpend) {
        this.arrowExpend = arrowExpend;
    }
}

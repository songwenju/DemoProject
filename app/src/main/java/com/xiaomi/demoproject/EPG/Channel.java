package com.xiaomi.demoproject.EPG;

import java.util.List;

public class Channel {
    long id;
    String name;
    List<Program> mProgramList;
    boolean isLocked;


    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Channel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Program> getProgramList() {
        return mProgramList;
    }

    public void setProgramList(List<Program> programList) {
        mProgramList = programList;
    }




    @Override
    public String toString() {
        return "Channel{" +
                "name='" + name + '\'' +
                ", mProgramList=" + mProgramList +
                '}';
    }
}

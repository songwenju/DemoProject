package com.xiaomi.demoproject.EPG;

import java.util.List;

public class Channel {
    String name;
    List<Program> mProgramList;
    public Channel( String name) {
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

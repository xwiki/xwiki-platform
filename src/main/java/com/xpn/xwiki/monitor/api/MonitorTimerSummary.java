/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 3 déc. 2004
 * Time: 23:15:23
 */
package com.xpn.xwiki.monitor.api;

public class MonitorTimerSummary {
    private String name;
    private long duration = 0;
    private long nbcalls = 0;
    private long nbrequests = 0;

    public MonitorTimerSummary(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addTimer(long duration) {
        this.duration += duration;
        this.nbcalls++;
        this.nbrequests = 1;
    }

    public long getDuration() {
        return duration;
    }

    public long getNbCalls() {
        return nbcalls;
    }

    public void add(MonitorTimerSummary stimer) {
        duration += stimer.getDuration();
        nbcalls += stimer.getNbCalls();
        nbrequests++;
    }

    public long getRequests() {
        return nbrequests;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append(name);
        str.append(": duration=");
        str.append(getDuration());
        str.append(" nbcalls=");
        str.append(getNbCalls());
        str.append(" nbrequests=");
        str.append(getRequests());
        return str.toString();
    }
}

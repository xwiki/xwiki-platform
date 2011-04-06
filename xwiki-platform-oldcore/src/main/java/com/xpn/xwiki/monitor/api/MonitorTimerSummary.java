/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
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

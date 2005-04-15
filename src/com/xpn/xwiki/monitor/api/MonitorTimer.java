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
 * Time: 23:01:58
 */
package com.xpn.xwiki.monitor.api;

import java.util.Date;

public class MonitorTimer extends Object {
    private String name;
    private String details;
    private Date startDate;
    private Date endDate;

    public MonitorTimer(String name, String details) {
        this.setName(name);
        this.setDetails(details);
    }

    public void setStartDate() {
        startDate = new Date();
    }

    public void setEndDate() {
        endDate = new Date();
    }

    public long getDuration() {
        return endDate.getTime()-startDate.getTime();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append(" Name: ");
        str.append(name);
        str.append(" Details: ");
        str.append(" Start Date: ");
        str.append(startDate);
        str.append(" End Date: ");
        str.append(endDate);
        str.append(" Duration: ");
        try {
        str.append(endDate.getTime()-startDate.getTime());
        } catch (Exception e) {}
        return str.toString();
    }
}

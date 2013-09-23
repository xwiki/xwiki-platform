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
 */
package com.xpn.xwiki.monitor.api;

import java.util.Date;

public class MonitorTimer
{
    private String name;

    private String details;

    private Date startDate;

    private Date endDate;

    public MonitorTimer(String name, String details)
    {
        this.setName(name);
        this.setDetails(details);
    }

    public void setStartDate()
    {
        startDate = new Date();
    }

    public void setEndDate()
    {
        endDate = new Date();
    }

    public long getDuration()
    {
        return endDate.getTime() - startDate.getTime();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    @Override
    public String toString()
    {
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
            str.append(endDate.getTime() - startDate.getTime());
        } catch (Exception e) {
        }
        return str.toString();
    }
}

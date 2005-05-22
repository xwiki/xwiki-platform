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
 * Date: 31 juil. 2004
 * Time: 12:42:37
 */
package com.xpn.xwiki.stats.impl;

import com.xpn.xwiki.objects.BaseObject;

import java.util.Date;
import java.util.Calendar;

public class DocumentStats extends XWikiStats {

    public DocumentStats() {
    }

    public DocumentStats(String docName, String action, Date period, int periodtype) {
        super(period, periodtype);
        setName(docName);
        setClassName("internal");
        String nb = action + getPeriod();
        setNumber(nb.hashCode());
        setAction(action);
    }

    public String getAction() {
        return getStringValue("action");
    }

    public void setAction(String action) {
        setStringValue("action", action);
    }

    public int getUniqueVisitors() {
        return getIntValue("uniqueVisitors");
    }

    public void  setUniqueVisitors(int uniqueVisitors) {
        setIntValue("uniqueVisitors", uniqueVisitors);
    }

    public void  incUniqueVisitors() {
        setIntValue("uniqueVisitors", getUniqueVisitors() + 1);
    }

    public int getVisits() {
        return getIntValue("visits");
    }

    public void  setVisits(int visits) {
        setIntValue("visits", visits);
    }

    public void  incVisits() {
        setIntValue("visits", getVisits() + 1);
    }


}

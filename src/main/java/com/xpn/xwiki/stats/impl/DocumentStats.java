/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author sdumitriu
 */

package com.xpn.xwiki.stats.impl;

import java.util.Date;

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

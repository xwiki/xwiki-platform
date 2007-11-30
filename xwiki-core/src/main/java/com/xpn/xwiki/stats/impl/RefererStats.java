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

package com.xpn.xwiki.stats.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class RefererStats extends XWikiStats {

    public RefererStats() {
        super();
    }

    public RefererStats(String docName, String referer, Date period, int periodtype) {
        super(period, periodtype);
        setName(docName);
        setClassName("internal");
        String nb = referer + getPeriod();
        setNumber(nb.hashCode());
        setReferer(referer);
    }

    public String getReferer() {
        return getStringValue("referer");
    }

    public void setReferer(String referer) {
        setStringValue("referer", referer);
    }
    
    public URL getURL()
    {
        try {
            return new URL(getReferer());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}

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
 * Date: 3 août 2004
 * Time: 10:40:19
 */
package com.xpn.xwiki.stats.impl;

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
}

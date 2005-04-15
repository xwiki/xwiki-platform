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
 * Time: 11:46:18
 */
package com.xpn.xwiki.stats.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;

import java.util.Date;
import java.util.List;
import java.util.Collection;

public interface XWikiStatsService extends XWikiActionNotificationInterface {
    public void init(XWikiContext context);
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context);
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context);
    public DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context);
    public List getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException;
    public Collection getRecentActions(String action, int size, XWikiContext context);
}

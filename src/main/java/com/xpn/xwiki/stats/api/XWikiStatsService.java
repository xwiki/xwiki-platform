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

package com.xpn.xwiki.stats.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.stats.impl.DocumentStats;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface XWikiStatsService extends XWikiActionNotificationInterface {
    public void init(XWikiContext context);
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context);
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context);
    public DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context);
    public List getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException;
    public Collection getRecentActions(String action, int size, XWikiContext context);
}

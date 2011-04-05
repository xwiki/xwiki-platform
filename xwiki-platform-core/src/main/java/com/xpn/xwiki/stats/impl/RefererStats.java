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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

/**
 * The referer statistics database object.
 * 
 * @version $Id$
 */
public class RefererStats extends XWikiStats
{
    /**
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(RefererStats.class);

    /**
     * The properties of document statistics object.
     * 
     * @version $Id$
     */
    public enum Property
    {
        /**
         * The referer.
         */
        referer
    }

    /**
     * Default {@link RefererStats} constructor.
     */
    public RefererStats()
    {
    }

    /**
     * @param docName the name of the wiki/space/document.
     * @param referer the referer.
     * @param periodDate the date of the period.
     * @param periodType the type of the period.
     */
    public RefererStats(String docName, String referer, Date periodDate, PeriodType periodType)
    {
        super(periodDate, periodType);

        setName(docName);
        String nb = referer + getPeriod();
        setNumber(nb.hashCode());
        setReferer(referer);
    }

    /**
     * @return the referer.
     */
    public String getReferer()
    {
        return getStringValue(Property.referer.toString());
    }

    /**
     * @param referer the referer.
     */
    public void setReferer(String referer)
    {
        setStringValue(Property.referer.toString(), referer);
    }

    /**
     * @return the referer URL.
     */
    public URL getURL()
    {
        URL url = null;

        try {
            url = new URL(getReferer());
        } catch (MalformedURLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to construct URL from referer", e);
            }
        }

        return url;
    }
}

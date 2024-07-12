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
package com.xpn.xwiki.criteria.impl;

import java.util.Date;

import org.xwiki.stability.Unstable;

/**
 * information about document versions used to retreive a set of document versions.
 *
 * @version $Id$
 * @see com.xpn.xwiki.doc.XWikiDocument#getRevisions(RevisionCriteria, com.xpn.xwiki.XWikiContext)
 * @since 1.4M1
 */
public class RevisionCriteria
{
    /**
     * regexp matching the author of version set.
     */
    private String author = "";

    /**
     * date range of the version set
     */
    private Period period = PeriodFactory.createMaximumPeriod();

    /**
     * range allowing to limit the size of the set by getting only N first items or N last items
     */
    private Range range = RangeFactory.createAllRange();

    /**
     * include minor edit versions in the set.
     */
    private boolean includeMinorVersions = false;

    /**
     * Default constructor, the default query match the versions created by any author, from January 1, 1970, 00:00:00
     * GMT (epoch) to the maximum possible date (Long.MAX_VALUE), minor versions aren't included
     */
    public RevisionCriteria()
    {
        // Nothing to do here.
    }

    /**
     * Fully featured constructor, allow to set all the query parameters, null arguments are ignored
     */
    public RevisionCriteria(String author, Period period, Range range,
        boolean includeMinorVersions)
    {
        if (author != null) {
            setAuthor(author);
        }
        if (period != null) {
            setPeriod(period);
        }
        if (range != null) {
            setRange(range);
        }
        setIncludeMinorVersions(includeMinorVersions);
    }

    /**
     * @return author the author of version set.
     */
    public String getAuthor()
    {
        return this.author;
    }

    /**
     * @param author the author of version set.
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @return period the Period (time limits) desired for the results
     */
    public Period getPeriod()
    {
        return this.period;
    }

    /**
     * Set the Period (time limits) desired for the results
     *
     * @param period
     */
    public void setPeriod(Period period)
    {
        this.period = period;
    }

    /**
     * @return range the Range (size limits) desired for the results
     */
    public Range getRange()
    {
        return this.range;
    }

    /**
     * Set the Range (size limits) desired for the results
     *
     * @param range desired range @see Range
     */
    public void setRange(Range range)
    {
        this.range = range;
    }

    /**
     * @return minimum date of version set.
     */
    public Date getMinDate()
    {
        return new Date(this.period.getStart());
    }

    /**
     * @return maximum date of version set.
     */
    public Date getMaxDate()
    {
        return new Date(this.period.getEnd());
    }

    /**
     * include minor versions in the set.
     */
    public boolean getIncludeMinorVersions()
    {
        return this.includeMinorVersions;
    }

    /**
     * @param includeMinorVersions true to include minor versions in the set, false to ignore them.
     */
    public void setIncludeMinorVersions(boolean includeMinorVersions)
    {
        this.includeMinorVersions = includeMinorVersions;
    }

    /**
     * Checks that the criteria are all-inclusive.
     *
     * @return false if some revisions might be filtered out by these criteria, true otherwise.
     * @since 15.10.8
     * @since 16.2.0RC1
     */
    @Unstable
    public boolean isAllInclusive()
    {
        return this.includeMinorVersions
            && this.getAuthor().isEmpty()
            && this.getRange().getSize() == 0
            && this.getPeriod().getStart() == Long.MIN_VALUE
            && this.getPeriod().getEnd() == Long.MAX_VALUE;
    }
}

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

import java.util.Date;

import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

/**
 * The document statistics database object.
 * 
 * @version $Id$
 */
public class DocumentStats extends XWikiStats
{
    /**
     * The properties of document statistics object.
     * 
     * @version $Id$
     */
    public enum Property
    {
        /**
         * The action made on the document ("view", "save", ...).
         */
        action,

        /**
         * The number of unique visitors.
         */
        uniqueVisitors,

        /**
         * The number of visits.
         */
        visits
    }

    /**
     * Default {@link DocumentStats} constructor.
     */
    public DocumentStats()
    {
    }

    /**
     * @param docName the name of the wiki/space/document.
     * @param action the action made on the document ("view", "save", ...).
     * @param periodDate the date of the period.
     * @param periodType the type of the period.
     */
    public DocumentStats(String docName, String action, Date periodDate, PeriodType periodType)
    {
        super(periodDate, periodType);
        
        setName(docName);
        String nb = action + getPeriod();
        setNumber(nb.hashCode());
        setAction(action);
    }

    /**
     * @return the action made on the document ("view", "save", ...).
     */
    public String getAction()
    {
        return getStringValue(Property.action.toString());
    }

    /**
     * @param action the action made on the document ("view", "save", ...).
     */
    public void setAction(String action)
    {
        setStringValue(Property.action.toString(), action);
    }

    /**
     * @return the number of unique visitors.
     */
    public int getUniqueVisitors()
    {
        return getIntValue(Property.uniqueVisitors.toString());
    }

    /**
     * @param uniqueVisitors the number of unique visitors.
     */
    public void setUniqueVisitors(int uniqueVisitors)
    {
        setIntValue(Property.uniqueVisitors.toString(), uniqueVisitors);
    }

    /**
     * Add 1 to the number of unique visitors.
     */
    public void incUniqueVisitors()
    {
        setIntValue(Property.uniqueVisitors.toString(), getUniqueVisitors() + 1);
    }

    /**
     * @return the number of visits.
     */
    public int getVisits()
    {
        return getIntValue(Property.visits.toString());
    }

    /**
     * @param visits the number of visits.
     */
    public void setVisits(int visits)
    {
        setIntValue(Property.visits.toString(), visits);
    }

    /**
     * Add 1 to the number of visits.
     */
    public void incVisits()
    {
        setIntValue(Property.visits.toString(), getVisits() + 1);
    }
}

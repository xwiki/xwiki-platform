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
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.criteria.impl.DurationFactory;
import com.xpn.xwiki.criteria.impl.PeriodFactory;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.RevisionCriteriaFactory;
import com.xpn.xwiki.criteria.impl.ScopeFactory;

/**
 * Criteria service api.
 */
public class CriteriaService extends Api
{
    public CriteriaService(XWikiContext context)
    {
        super(context);
    }

    /**
     * @return A helper factory for creating {@link com.xpn.xwiki.criteria.impl.Scope} objects in
     *         velocity.
     */
    public ScopeFactory getScopeFactory()
    {
        return getXWikiContext().getWiki().getCriteriaService(getXWikiContext())
            .getScopeFactory();
    }

    /**
     * @return A helper factory for creating {@link com.xpn.xwiki.criteria.impl.Period} objects in
     *         velocity.
     */
    public PeriodFactory getPeriodFactory()
    {
        return getXWikiContext().getWiki().getCriteriaService(getXWikiContext())
            .getPeriodFactory();
    }

    /**
     * @return A helper factory for creating {@link com.xpn.xwiki.criteria.impl.Duration} objects in
     *         velocity.
     */
    public DurationFactory getDurationFactory()
    {
        return getXWikiContext().getWiki().getCriteriaService(getXWikiContext())
            .getDurationFactory();
    }

    /**
     * @return A helper factory for creating {@link com.xpn.xwiki.criteria.impl.Range} objects in
     *         velocity.
     */
    public RangeFactory getRangeFactory()
    {
        return getXWikiContext().getWiki().getCriteriaService(getXWikiContext())
            .getRangeFactory();
    }

    /**
     * @return A helper factory for creating {@link com.xpn.xwiki.criteria.impl.RevisionCriteria}
     *         objects in velocity.
     */
    public RevisionCriteriaFactory getRevisionCriteriaFactory()
    {
        return getXWikiContext().getWiki().getCriteriaService(getXWikiContext())
            .getRevisionCriteriaFactory();
    }
}

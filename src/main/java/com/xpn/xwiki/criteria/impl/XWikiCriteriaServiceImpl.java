/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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
package com.xpn.xwiki.criteria.impl;

import com.xpn.xwiki.criteria.api.XWikiCriteriaService;
import com.xpn.xwiki.XWikiContext;

public class XWikiCriteriaServiceImpl implements XWikiCriteriaService
{
    public XWikiCriteriaServiceImpl(XWikiContext context) {}
    
    public DurationFactory getDurationFactory() {
        return new DurationFactory();
    }

    public PeriodFactory getPeriodFactory() {
        return new PeriodFactory();
    }

    public RangeFactory getRangeFactory() {
        return new RangeFactory();
    }

    public RevisionCriteriaFactory getRevisionCriteriaFactory() {
        return new RevisionCriteriaFactory();
    }

    public ScopeFactory getScopeFactory() {
        return new ScopeFactory();
    }
}

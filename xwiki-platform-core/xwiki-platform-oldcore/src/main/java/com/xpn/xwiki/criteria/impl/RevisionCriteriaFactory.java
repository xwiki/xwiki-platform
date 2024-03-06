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

import org.xwiki.stability.Unstable;

public class RevisionCriteriaFactory
{
    public RevisionCriteriaFactory()
    {
    }

    /**
     * Creates a default RevisionCriteria, the default criteria matches the versions created by any author, from January
     * 1, 1970, 00:00:00 GMT (epoch) to the current date, minor versions aren't included.
     */
    public RevisionCriteria createRevisionCriteria()
    {
        return new RevisionCriteria();
    }

    /**
     * Creates a revision criteria matching all the revisions created by the given author.
     *
     * @param author the author of the result set
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(String author)
    {
        return new RevisionCriteria(author, PeriodFactory.createMaximumPeriod(),
            RangeFactory.createAllRange(), false);
    }

    /**
     * Creates a revision criteria matching all the revisions created by the given author.
     *
     * @param author the author of the result set
     * @param includeMinorVersions include minor versions in the set
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(String author, boolean includeMinorVersions)
    {
        return new RevisionCriteria(author, PeriodFactory.createMaximumPeriod(),
            RangeFactory.createAllRange(), includeMinorVersions);
    }

    /**
     * Creates a revision criteria matching the revisions created by any author, during the given period.
     *
     * @param period the time period during which the revisions has been created
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(Period period)
    {
        return new RevisionCriteria("", period, RangeFactory.createAllRange(), false);
    }

    /**
     * Creates a revision criteria matching the revisions created by any author, during the given period.
     *
     * @param period the time period during which the revisions has been created
     * @param includeMinorVersions include minor versions in the set
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(Period period, boolean includeMinorVersions)
    {
        return new RevisionCriteria("", period, RangeFactory.createAllRange(), includeMinorVersions);
    }

    /**
     * Creates a revision criteria matching the revisions created by the given author, during the given period.
     *
     * @param author the author of the result set
     * @param period the time period during which the revisions has been created
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(String author, Period period)
    {
        return new RevisionCriteria(author, period, RangeFactory.createAllRange(), false);
    }

    /**
     * Creates a revision criteria matching the revisions created by the given author, during the given period.
     *
     * @param author the author of the result set
     * @param period the time period during which the revisions has been created
     * @param includeMinorVersions include minor versions in the set
     * @return a new revision criteria
     */
    public RevisionCriteria createRevisionCriteria(String author, Period period,
        boolean includeMinorVersions)
    {
        return new RevisionCriteria(author, period, RangeFactory.createAllRange(), includeMinorVersions);
    }

    /**
     * Creates a revision criteria matching every revision, filtering only minor versions.
     *
     * @param includeMinorVersions include minor versions in the set
     * @return a new revision criteria
     * @since 15.10.8
     * @since 16.2.0RC1
     */
    @Unstable
    public RevisionCriteria createRevisionCriteria(boolean includeMinorVersions)
    {
        return new RevisionCriteria("", PeriodFactory.createMaximumPeriod(), RangeFactory.createAllRange(),
            includeMinorVersions);
    }
}

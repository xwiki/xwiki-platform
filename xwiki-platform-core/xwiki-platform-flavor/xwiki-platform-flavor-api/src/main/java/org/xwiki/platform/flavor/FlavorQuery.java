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
package org.xwiki.platform.flavor;

import org.xwiki.extension.Extension;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.stability.Unstable;

/**
 * A query to an extension repository returning only flavors.
 *  
 * @version $Id$
 * @since 7.1M2 
 */
@Unstable
public class FlavorQuery extends ExtensionQuery
{
    /**
     * No filtering. Usually return all flavors.
     */
    public FlavorQuery()
    {
        super();
        init();
    }

    /**
     * @param query the query to execute
     */
    public FlavorQuery(String query)
    {
        super(query);
        init();
    }

    /**
     * @param query the query to duplicate
     */
    public FlavorQuery(ExtensionQuery query)
    {
        super(query);
        init();
    }

    private void init()
    {
        addFilter(Extension.FIELD_CATEGORY, "flavor", ExtensionQuery.COMPARISON.EQUAL);
    }

    /**
     * Add a filter on the flavors' name. 
     * @param name name of the flavor to find
     * @return this query.
     */
    public FlavorQuery filterByName(String name)
    {
        addFilter(Extension.FIELD_NAME, name, COMPARISON.MATCH);
        return this;
    }

    /**
     * Add a filter on the flavors' summary.
     * @param summary a summary to find on the extension repository
     * @return this query.
     */
    public FlavorQuery filterBySummary(String summary)
    {
        addFilter(Extension.FIELD_SUMMARY, summary, COMPARISON.MATCH);
        return this;
    }

    /**
     * Order the results by name. 
     * @param order asc or desc
     * @return this query.
     */
    public FlavorQuery orderByName(ORDER order)
    {
        addSort(Extension.FIELD_NAME, order);
        return this;
    }

    /**
     * Order the results by rating.
     * @param order asc or desc
     * @return this query.
     */
    public FlavorQuery orderByRating(ORDER order)
    {
        addSort(RatingExtension.FIELD_AVERAGE_VOTE, order);
        return this;
    }
    
}

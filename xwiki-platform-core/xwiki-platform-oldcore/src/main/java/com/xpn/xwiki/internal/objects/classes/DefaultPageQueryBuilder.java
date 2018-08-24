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
package com.xpn.xwiki.internal.objects.classes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PageClass;

/**
 * Builds a query that returns the values allowed for a Page property.
 *
 * @version $Id$
 * @since 10.6
 */
@Component
@Singleton
public class DefaultPageQueryBuilder implements QueryBuilder<PageClass>
{
    @Inject
    @Named("explicitlyAllowedValues")
    private QueryBuilder<DBListClass> explicitlyAllowedValuesQueryBuilder;

    @Inject
    @Named("implicitlyAllowedValues")
    private QueryBuilder<PageClass> implicitlyAllowedValuesQueryBuilder;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    /**
     * The filter used to show/hide hidden (technical) documents. We use a provider because we need a new instance for
     * each query execution.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-8160">XWIKI-8160: HiddenDocument query filter can put corrupted
     *      user documents in the cache (virtual mode)</a>
     */
    @Inject
    @Named("hidden/document")
    private Provider<QueryFilter> hiddenFilterProvider;

    @Override
    public Query build(PageClass pageClass) throws QueryException
    {
        Query query;
        if (StringUtils.isEmpty(pageClass.getSql())) {
            query = this.implicitlyAllowedValuesQueryBuilder.build(pageClass);
            // We can filter hidden documents here because we control how the query is build ('doc' alias is present).
            query.addFilter(hiddenFilterProvider.get());
            // We don't need the viewable filter here as the query builder already adds its own viewable filter.
            query.addFilter(this.documentFilter);
        } else {
            query = this.explicitlyAllowedValuesQueryBuilder.build(pageClass);
            // NOTE: If the user provides an explicit query then he's responsible for filtering hidden documents if he
            // needs it. We can't use the hidden document filter here because we don't control the query.
            query.addFilter(this.documentFilter);
            query.addFilter(this.viewableFilter);
        }

        return query;
    }
}

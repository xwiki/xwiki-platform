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

    @Override
    public Query build(PageClass pageClass) throws QueryException
    {
        Query query;
        if (StringUtils.isEmpty(pageClass.getSql())) {
            query = this.implicitlyAllowedValuesQueryBuilder.build(pageClass);
            // We don't need the viewable filter here as the query builder already adds its own viewable filter.
            query.addFilter(this.documentFilter);
        } else {
            query = this.explicitlyAllowedValuesQueryBuilder.build(pageClass);
            query.addFilter(this.documentFilter);
            query.addFilter(this.viewableFilter);
        }

        return query;
    }
}

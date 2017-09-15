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
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.classes.DBListClass;

/**
 * Builds a query that returns the values allowed for a Database List property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultDBListQueryBuilder implements QueryBuilder<DBListClass>
{
    @Inject
    @Named("explicitlyAllowedValues")
    private QueryBuilder<DBListClass> explicitlyAllowedValuesQueryBuilder;

    @Inject
    @Named("implicitlyAllowedValues")
    private QueryBuilder<DBListClass> implicitlyAllowedValuesQueryBuilder;

    @Override
    public Query build(DBListClass dbListClass) throws QueryException
    {
        if (StringUtils.isEmpty(dbListClass.getSql())) {
            return this.implicitlyAllowedValuesQueryBuilder.build(dbListClass);
        } else {
            return this.explicitlyAllowedValuesQueryBuilder.build(dbListClass);
        }
    }
}

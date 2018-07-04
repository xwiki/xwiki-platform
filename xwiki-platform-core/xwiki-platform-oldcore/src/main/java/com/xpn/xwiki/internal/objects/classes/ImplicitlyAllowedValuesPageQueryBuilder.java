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
import com.xpn.xwiki.objects.classes.PageClass;

/**
 * Builds a query from the meta data of a Page property.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@Component
@Named("implicitlyAllowedValues")
@Singleton
public class ImplicitlyAllowedValuesPageQueryBuilder implements QueryBuilder<PageClass>
{
    @Inject
    @Named("implicitlyAllowedValues")
    private QueryBuilder<DBListClass> implicitlyAllowedValuesQueryBuilder;

    @Override
    public Query build(PageClass pageClass) throws QueryException
    {
        DBListClass dbListClass = (DBListClass) pageClass.clone();
        if (StringUtils.isEmpty(dbListClass.getIdField())) {
            dbListClass.setIdField("doc.fullName");
        }

        return implicitlyAllowedValuesQueryBuilder.build(dbListClass);
    }
}

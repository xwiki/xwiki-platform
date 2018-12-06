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
package org.xwiki.rest.internal.resources.classes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryBuilder;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.objects.classes.DBListClass;

/**
 * Provides values for Database List properties.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("DBList")
@Singleton
public class DBListClassPropertyValuesProvider extends AbstractListClassPropertyValuesProvider<DBListClass>
{
    @Inject
    private QueryBuilder<DBListClass> allowedValuesQueryBuilder;

    @Inject
    private AuthorExecutor authorExecutor;

    @Override
    protected Class<DBListClass> getPropertyType()
    {
        return DBListClass.class;
    }

    @Override
    protected PropertyValues getAllowedValues(DBListClass dbListClass, int limit, String filter) throws Exception
    {
        // Execute the query with the rights of the class last author because the query may not be safe.
        return this.authorExecutor.call(
            () -> getValues(this.allowedValuesQueryBuilder.build(dbListClass), limit, filter, dbListClass),
            dbListClass.getOwnerDocument().getAuthorReference(), dbListClass.getDocumentReference());
    }
}

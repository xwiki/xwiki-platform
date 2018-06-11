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

import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;

import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Base class for {@link ClassPropertyValuesProvider} implementations that work with list properties.
 * 
 * @param <T> the concrete list property type
 * @version $Id$
 * @since 9.8
 */
public abstract class AbstractListClassPropertyValuesProvider<T extends ListClass>
    extends AbstractClassPropertyValuesProvider<T>
{
    @Inject
    @Named("usedValues")
    protected QueryBuilder<ListClass> usedValuesQueryBuilder;

    @Override
    protected PropertyValues getUsedValues(T propertyDefinition, int limit, String filter) throws QueryException
    {
        Query query = this.usedValuesQueryBuilder.build(propertyDefinition);
        if (propertyDefinition.isMultiSelect() && !propertyDefinition.isRelationalStorage()) {
            query.addFilter(new SplitValueQueryFilter(propertyDefinition.getSeparators(), limit, filter));
        }
        return getValues(query, limit, filter, propertyDefinition);
    }
}

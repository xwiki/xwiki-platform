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
package org.xwiki.store.hibernate.internal;

import java.util.Optional;

import javax.annotation.Priority;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.store.hibernate.DatabaseProductNameResolver;

import com.xpn.xwiki.store.DatabaseProduct;

/**
 * Implementation of {@link DatabaseProductNameResolver} based on the old {@link DatabaseProduct} resolution, except for
 * MariaDB (since it's identified as MySQL) which have a dedicated {@link DatabaseProductNameResolver} with a higher
 * priority.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component
@Singleton
// Make sure it's executed before other resolvers
@Priority(ComponentDescriptor.DEFAULT_PRIORITY + 1000)
// TODO: deprecated and move DatabaseProduct to legacy and use dedicated DatabaseProductNameResolver components to
// resolve all database identifiers
@Named("legacy")
public class LegacyDatabaseProductNameResolver implements DatabaseProductNameResolver
{
    @Override
    public Optional<String> resolve(String databaseProductName)
    {
        DatabaseProduct product = DatabaseProduct.toProduct(databaseProductName);

        if (product != DatabaseProduct.UNKNOWN) {
            return Optional.of(product.getJDBCScheme());
        }

        return Optional.empty();
    }
}

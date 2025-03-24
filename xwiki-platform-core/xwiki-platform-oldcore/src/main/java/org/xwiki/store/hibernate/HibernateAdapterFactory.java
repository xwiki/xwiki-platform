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
package org.xwiki.store.hibernate;

import java.sql.DatabaseMetaData;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * A factory in charge of providing a specific {@link HibernateAdapter} instance, generally based on information found
 * in the {@link DatabaseMetaData} (database server related metadata) and {@link Configuration} (Hibernate configuration
 * properties).
 * <p>
 * If no specific adapter is returned by the various factories, a default one is used.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Unstable
@Role
public interface HibernateAdapterFactory
{
    /**
     * Create a new {@link HibernateAdapter} instance corresponding to the passed {@link DatabaseMetaData}.
     * 
     * @param metaData the metadata gathered from the databases
     * @param configuration the Hibernate configuration
     * @return an {@link HibernateAdapter} instance if the this factory matches the passed metadata
     * @throws HibernateException when failing to resolve the adapter
     */
    Optional<HibernateAdapter> createHibernateAdapter(DatabaseMetaData metaData, Configuration configuration)
        throws HibernateException;
}

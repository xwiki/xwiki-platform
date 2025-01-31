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

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.hibernate.AbstractHibernateAdapter;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;

/**
 * The {@link HibernateAdapter} for Oracle.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component
@Named(OracleHibernateAdapter.HINT)
@Singleton
public class OracleHibernateAdapter extends AbstractHibernateAdapter
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "oracle";

    @Override
    public void updateDatabaseAfter(Metadata metadata, Session session) throws HibernateStoreException
    {
        if (isCompressionAllowed()) {
            // Make sure all the configured tables have the right compression configuration
            updateTableCompression(metadata, session);
        }
    }

    private void updateTableCompression(Metadata metadata, Session session)
    {
        // Gather compressed tables
        Set<String> compressedTables = getCompressedTables(session);

        // Make sure each table compression status matches the configuration
        for (PersistentClass entity : metadata.getEntityBindings()) {
            // Get the exact table name for the entity
            String tableName = getTableName(entity);

            // Check if the table is configured to be compressed
            boolean compressed = isCompressed(entity);

            // Compute the query statement
            if (compressedTables.contains(tableName) != compressed) {
                // Create the query
                NativeQuery<?> query = session
                    .createNativeQuery("ALTER TABLE " + tableName + " " + (compressed ? "COMPRESS" : "NOCOMPRESS"));

                // Execute the query
                session.getTransaction().begin();
                query.executeUpdate();
                session.getTransaction().commit();
            }
        }
    }

    /**
     * @param session the session in which to execute the query
     * @return the tables which are configured to be compressed
     */
    public Set<String> getCompressedTables(Session session)
    {
        NativeQuery<String> query =
            session.createNativeQuery("SELECT DISTINCT table_name FROM user_tables WHERE compression = 'ENABLED'");

        return query.list().stream().map(String::toUpperCase).collect(Collectors.toSet());
    }

    @Override
    public String getTableName(String tableName)
    {
        // Oracle generally needs the table name to be upper case
        return tableName != null ? tableName.toUpperCase() : null;
    }

    @Override
    protected String cleanDatabaseName(String name)
    {
        // Oracle generally needs the schema name to be upper case
        return super.cleanDatabaseName(name).toUpperCase();
    }

    @Override
    public String escapeDatabaseName(String databaseName)
    {
        // - Oracle converts user names in uppercase when no quotes is used.
        // For example: "create user xwiki identified by xwiki;" creates a user named XWIKI (uppercase)
        // - In Hibernate.cfg.xml we just specify: <property name="hibernate.connection.username">xwiki</property> and
        // Hibernate
        // seems to be passing this username as is to Oracle which converts it to uppercase.
        //
        // Thus for Oracle we don't escape the schema.
        return databaseName;
    }

    @Override
    public boolean isCatalog()
    {
        return false;
    }
}

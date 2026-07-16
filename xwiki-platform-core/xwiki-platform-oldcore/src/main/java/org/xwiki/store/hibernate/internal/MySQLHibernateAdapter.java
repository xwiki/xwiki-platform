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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.hibernate.AbstractHibernateAdapter;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;

/**
 * The default {@link HibernateAdapter} for MySQL. Based on MySQL 5.7 specifications.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component
@Singleton
@Named(MySQLHibernateAdapter.HINT)
public class MySQLHibernateAdapter extends AbstractHibernateAdapter
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "mysql";

    @Override
    public void updateDatabaseAfter(Metadata metadata, Session session) throws HibernateStoreException
    {
        updateRowFormats(metadata, session);
    }

    protected void updateRowFormats(Metadata metadata, Session session) throws HibernateStoreException
    {
        // Gather tables row formats
        Map<String, String> rowFormats = getRowFormats(session);

        boolean compressionAllowed = isCompressionAllowed();

        // Make sure each table row format matches the configuration
        for (PersistentClass entity : metadata.getEntityBindings()) {
            // Get the exact table name for the entity
            String tableName = getTableName(entity);

            // Check if the table is configured to be compressed
            boolean compressed = compressionAllowed && entity.getMetaAttribute(META_ATTRIBUTE_COMPRESSED) != null;

            // Compute the query statement
            String statement = getAlterRowFormatString(tableName, compressed, rowFormats, session);
            if (statement != null) {
                // Create the query
                NativeQuery<?> query = session.createNativeQuery(statement);

                // Execute the query
                this.logger.info("Changing the table row type (currently [{}]) with [{}]", rowFormats.get(tableName),
                    statement);
                session.getTransaction().begin();
                query.executeUpdate();
                session.getTransaction().commit();
            }
        }
    }

    /**
     * @param tableName the name of the table to modify
     * @param compressed true if the table should be compressed
     * @param rowFormats the current row formats
     * @param session the session in which to execute the query
     * @return the SQL statement to execute if the table is not already compressed
     * @throws HibernateStoreException when failing to check if the table is already compressed
     */
    public String getAlterRowFormatString(String tableName, boolean compressed, Map<String, String> rowFormats,
        Session session) throws HibernateStoreException
    {
        String rowFormat = rowFormats.get(tableName);

        this.logger.debug("Row format for table [{}]: {}", tableName, rowFormat);

        String expectedRowFormat = compressed ? getCompressedRowFormat() : getDefaultRowFormat();

        if (!StringUtils.equalsIgnoreCase(rowFormat, expectedRowFormat)) {
            return "ALTER TABLE " + tableName + " ROW_FORMAT=" + expectedRowFormat;
        }

        return null;
    }

    /**
     * @param session the session in which to execute the query
     * @return the row format of each table in the current database
     */
    public Map<String, String> getRowFormats(Session session)
    {
        String currentDatabase = getDatabaseFromWikiName();
        NativeQuery<Object[]> query = session.createNativeQuery("SELECT DISTINCT NAME, ROW_FORMAT FROM "
            + getInformationShemaInnoDBTables() + " WHERE LOWER(NAME) LIKE '" + currentDatabase.toLowerCase() + "/%'");

        List<Object[]> results = query.list();

        Map<String, String> rowFormats = new HashMap<>(results.size());
        for (Object[] entry : results) {
            String completeName = (String) entry[0];
            String tableName = completeName.substring(completeName.indexOf('/') + 1);
            rowFormats.put(tableName, (String) entry[1]);
        }

        return rowFormats;
    }

    /**
     * @return the table where to find the InnoDB tables metadata
     */
    public String getInformationShemaInnoDBTables()
    {
        return "INFORMATION_SCHEMA.INNODB_SYS_TABLES";
    }

    /**
     * @return the row format to use by default
     */
    public String getDefaultRowFormat()
    {
        return "Dynamic";
    }

    /**
     * @return the row format to use for compressed tables
     */
    public String getCompressedRowFormat()
    {
        return "Compressed";
    }

    @Override
    public boolean isCompressionAllowed()
    {
        return getCompressionAllowedConfiguration().orElse(true);
    }
}

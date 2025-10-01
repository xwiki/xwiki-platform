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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

/**
 * A base implementation of {@link HibernateAdapter} with a default implementation for each API.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Unstable
public abstract class AbstractHibernateAdapter implements HibernateAdapter
{
    private static final String VIRTUAL_MODE_SCHEMA = "schema";

    @Inject
    protected WikiDescriptorManager wikis;

    @Inject
    protected Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Inject
    private HibernateConfiguration hibernateConfiguration;

    // Configuration

    @Override
    public boolean isConfiguredInSchemaMode()
    {
        String virtualModePropertyValue = this.hibernateStore.getConfiguration().getProperty("xwiki.virtual_mode");
        if (virtualModePropertyValue == null) {
            virtualModePropertyValue = VIRTUAL_MODE_SCHEMA;
        }
        return StringUtils.equals(virtualModePropertyValue, VIRTUAL_MODE_SCHEMA);
    }

    @Override
    public String getDatabaseFromWikiName()
    {
        return getDatabaseFromWikiName(this.wikis.getCurrentWikiId());
    }

    @Override
    public String getDatabaseFromWikiName(String wikiId)
    {
        if (wikiId == null) {
            return null;
        }

        String database = wikiId;

        // Apply the main wiki database configuration
        String mainWikiId = this.wikis.getMainWikiId();
        if (StringUtils.equalsIgnoreCase(wikiId, mainWikiId)) {
            database = this.hibernateConfiguration.getDB();
            if (database == null) {
                // Some databases have special database for main wiki
                database = getDefaultMainWikiDatabase(mainWikiId);
            }
        }

        // Apply prefix
        String prefix = this.hibernateConfiguration.getDBPrefix();
        database = prefix + database;

        // Make sure the database/schema format is correct
        database = cleanDatabaseName(database);

        return database;
    }

    protected String getDefaultMainWikiDatabase(String wikiId)
    {
        return wikiId;
    }

    protected String cleanDatabaseName(String name)
    {
        // Minus (-) is not supported by many databases
        // TODO: move it to adapters which really needs it ?
        String cleanName = name.replace('-', '_');

        return cleanName;
    }

    @Override
    public String getTableName(PersistentClass persistentClass)
    {
        return getTableName(persistentClass.getTable());
    }

    @Override
    public String getTableName(Table table)
    {
        return getTableName(table.getName());
    }

    @Override
    public String getTableName(String tableName)
    {
        return tableName;
    }

    @Override
    public boolean isCompressed(PersistentClass entity)
    {
        return entity.getMetaAttribute(META_ATTRIBUTE_COMPRESSED) != null;
    }

    @Override
    public String escapeDatabaseName(String databaseName)
    {
        String closeQuote = String.valueOf(getDialect().closeQuote());
        return getDialect().openQuote() + databaseName.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
    }

    /**
     * @return the value of the compression configuration, or empty if not configured
     */
    protected Optional<Boolean> getCompressionAllowedConfiguration()
    {
        String configuration = this.hibernateStore.getConfiguration().getProperty("xwiki.compressionAllowed");
        if (configuration == null) {
            return Optional.empty();
        }

        return Optional.of(Boolean.valueOf(configuration));
    }

    @Override
    public boolean isCompressionAllowed()
    {
        return getCompressionAllowedConfiguration().orElse(false);
    }

    // Global

    @Override
    public SessionFactory getSessionFactory()
    {
        return this.hibernateStore.getSessionFactory();
    }

    @Override
    public Dialect getDialect()
    {
        return this.hibernateStore.getDialect();
    }

    // Database operations

    @Override
    public void updateDatabase(Metadata metadata) throws HibernateStoreException
    {
        try {
            SessionFactory sessionFactory = getSessionFactory();

            // Custom update before the Hibernate standard DDL
            try (Session session = sessionFactory.openSession()) {
                updateDatabaseBegin(metadata, session);
            }

            // Hibernate standard DDL
            updateDatabaseStandard(metadata);

            // Custom update after the Hibernate standard DDL
            try (Session session = sessionFactory.openSession()) {
                updateDatabaseAfter(metadata, session);
            }
        } catch (Exception e) {
            throw new HibernateException("Failed to update the database", e);
        }
    }

    private void updateDatabaseStandard(Metadata metadata) throws HibernateStoreException
    {
        SchemaUpdate updater = new SchemaUpdate();
        updater.execute(EnumSet.of(TargetType.DATABASE), metadata);
        List<Exception> exceptions = updater.getExceptions();

        if (!exceptions.isEmpty()) {
            // Print the errors
            for (Exception exception : exceptions) {
                this.logger.error(exception.getMessage(), exception);
            }

            throw new HibernateStoreException("Failed to update the database. See the previous log for all errors",
                exceptions.get(0));
        }
    }

    /**
     * Execute everything not handled by the Hibernate DDL which needs to be done before.
     * 
     * @param metadata the configuration of the database to apply
     * @param session the session in which to execute the update
     * @throws HibernateStoreException when failing to update the database
     */
    protected void updateDatabaseBegin(Metadata metadata, Session session) throws HibernateStoreException
    {

    }

    /**
     * Execute everything not handled by the Hibernate DDL which needs to be done after.
     * 
     * @param metadata the configuration of the database to apply
     * @param session the session in which to execute the update
     * @throws HibernateStoreException when failing to update the database
     */
    protected void updateDatabaseAfter(Metadata metadata, Session session) throws HibernateStoreException
    {

    }

    /**
     * @return true if the current database product is catalog based, false for a schema based databases
     */
    public boolean isCatalog()
    {
        return getDialect().canCreateCatalog();
    }
}

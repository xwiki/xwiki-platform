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
package com.xpn.xwiki.internal.store.hibernate;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;

/**
 * Expose various Hibernate related configurations.
 * 
 * @version $Id$
 * @since 11.2RC1
 */
@Component(roles = HibernateConfiguration.class)
@Singleton
public class HibernateConfiguration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource xwikiConfiguration;

    private String path;

    /**
     * @return true if custom mapping is enabled
     */
    public boolean hasCustomMappings()
    {
        return "1".equals(this.xwikiConfiguration.getProperty("xwiki.store.hibernate.custommapping", "1"));
    }

    /**
     * @return true if dynamic custom mapping is enabled
     */
    public boolean hasDynamicCustomMappings()
    {
        return "1".equals(this.xwikiConfiguration.getProperty("xwiki.store.hibernate.custommapping.dynamic", "0"));
    }

    /**
     * @return the path to the hibernate configuration file
     */
    public String getPath()
    {
        return this.path != null ? this.path
            : this.xwikiConfiguration.getProperty("xwiki.store.hibernate.path", getPath());
    }

    /**
     * Allows to set the current Hibernate config file path.
     *
     * @param path the path to the hibernate configuration file
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * @return the databse/schema name for the main wiki
     */
    public String getDB()
    {
        return this.xwikiConfiguration.getProperty("xwiki.db");
    }

    /**
     * @return a prefix to apply to the database/shema name of each wiki
     */
    public String getDBPrefix()
    {
        return this.xwikiConfiguration.getProperty("xwiki.db.prefix", "");
    }

    /**
     * @return true if the database schema should be automatically updated at startup
     * @deprecated since 3.3M1
     */
    @Deprecated
    public boolean isUpdateSchema()
    {
        return this.xwikiConfiguration.getProperty("xwiki.store.hibernate.updateschema", 1) != 0;
    }

    /**
     * @return true if schema updates and migrations are enabled
     */
    public boolean isMigrationEnabled()
    {
        return this.xwikiConfiguration.getProperty("xwiki.store.migration", 0) == 1 && isUpdateSchema();
    }

    /**
     * @return true if schema updates and migrations are enabled
     */
    public boolean isExitAfterMigration()
    {
        return this.xwikiConfiguration.getProperty("xwiki.store.migration.exitAfterEnd", 0) == 1;
    }

    private List<String> getList(String key)
    {
        return Arrays.asList(StringUtils.split(this.xwikiConfiguration.getProperty(key, ""), " ,"));
    }

    /**
     * @return the databases to migrate.
     */
    public List<String> getMigrationDatabases()
    {
        return getList("xwiki.store.migration.databases");
    }

    /**
     * @return the migration version
     */
    public String getMigrationVersion()
    {
        return this.xwikiConfiguration.getProperty("xwiki.store.migration.version");
    }

    /**
     * @return the forced migrations
     */
    public List<String> getForcedMigrations()
    {
        return getList("xwiki.store.migration.force");
    }

    /**
     * @return the ignored migrations
     */
    public List<String> getIgnoredMigrations()
    {
        return getList("xwiki.store.migration.ignored");
    }
}

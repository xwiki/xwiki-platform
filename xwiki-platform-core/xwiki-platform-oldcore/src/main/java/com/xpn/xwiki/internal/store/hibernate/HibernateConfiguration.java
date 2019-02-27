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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;

/**
 * Expose various Hibernate related configurations.
 * 
 * @version $Id$
 * @since 11.1RC
 */
@Component(roles = HibernateConfiguration.class)
public class HibernateConfiguration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource xwikiConfiguration;

    private String path;

    public boolean hasCustomMappings()
    {
        return "1".equals(this.xwikiConfiguration.getProperty("xwiki.store.hibernate.custommapping", "1"));
    }

    public boolean hasDynamicCustomMappings()
    {
        return "1".equals(this.xwikiConfiguration.getProperty("xwiki.store.hibernate.custommapping.dynamic", "0"));
    }

    public String getPath()
    {
        return this.path != null ? this.path
            : this.xwikiConfiguration.getProperty("xwiki.store.hibernate.path", getPath());
    }

    /**
     * Allows to set the current Hibernate config file path.
     *
     * @param path
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    public String getDB()
    {
        return this.xwikiConfiguration.getProperty("xwiki.db");
    }

    public String getDBPrefix()
    {
        return this.xwikiConfiguration.getProperty("xwiki.db.prefix", "");
    }

    public boolean isUpdateSchema()
    {
        return this.xwikiConfiguration.getProperty("xwiki.store.hibernate.updateschema", 1) != 0;
    }
}

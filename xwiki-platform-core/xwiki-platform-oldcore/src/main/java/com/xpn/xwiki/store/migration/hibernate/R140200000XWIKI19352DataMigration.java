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
package com.xpn.xwiki.store.migration.hibernate;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Remove the xwikilinks table to allow it to be recreated from scratch. This is needed because we migrate from a 
 * composite id to a unique id key, and the composite id index is not removed automatically. While we could selectively
 * remove the deprecated index, it is easier to remove the table and let it be recreated automatically, since 
 * {@code R140200001XWIKI19352DataMigration} would drop all the table content anyway. 
 *
 * @version $Id$
 * @since 14.2
 */
@Component
@Singleton
@Named(R140200000XWIKI19352DataMigration.HINT)
public class R140200000XWIKI19352DataMigration extends AbstractHibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "140200000XWIKI19352";

    @Override
    public String getDescription()
    {
        return "Drop the xwikilinks table if it exists.";
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, all the work is done as Liquibase changes
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140200000);
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        return "<changeSet author=\"xwikiorg\" id=\"" + HINT + "\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <tableExists tableName=\"xwikilinks\"/>\n"
            + "  </preConditions>"
            + "  <dropTable tableName=\"xwikilinks\"/>\n"
            + "</changeSet>\n"
            + "\n";
    }
}

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
 * Migration for XWIKI-6691: Reduce the size of the ASE_REQUESTID column to 48-chars.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("R43000XWIKI6691")
@Singleton
public class R43000XWIKI6691DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Reduce the size of the ASE_REQUESTID column to 48-chars";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(43000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, all the work is done as Liquibase changes
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        StringBuilder result = new StringBuilder();
        result.append("  <changeSet id=\"R").append(this.getVersion().getVersion()).append("\" author=\"xwikiorg\">\n")
            .append("        <preConditions onFail=\"CONTINUE\">\n")
            .append("            <tableExists tableName=\"activitystream_events\"/>\n")
            .append("            <columnExists tableName=\"activitystream_events\" columnName=\"ase_requestid\"/>\n")
            .append("        </preConditions>")
            .append("    <comment>Reduce the size of the ASE_REQUESTID column to 48-chars</comment>\n")
            .append("    <modifyDataType tableName=\"activitystream_events\"")
            .append(" columnName=\"ase_requestid\" newDataType=\"varchar(48)\"/>\n")
            .append("  </changeSet>\n");
        return result.toString();
    }
}

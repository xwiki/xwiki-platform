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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-13474 Allow storing size of attachment bigger than 2GB.
 *
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@Named("R90000XWIKI13474")
@Singleton
public class R90000XWIKI13474DataMigration extends AbstractHibernateDataMigration
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Convert attachment size type to BIGINT to allow attachments bigger than 2GB.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(90000);
    }

    @Override
    public void hibernateMigrate()
    {
        // All the work is done by Liquibase
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        final XWikiHibernateBaseStore store = getStore();
        Configuration configuration = store.getConfiguration();

        PersistentClass attachmentClass = configuration.getClassMapping(XWikiAttachment.class.getName());
        String attachmentTableName = attachmentClass.getTable().getName();
        String attachmentSizeColumn =
            ((Column) attachmentClass.getProperty("size").getColumnIterator().next()).getName();

        final StringBuilder sb = new StringBuilder(12000);

        sb.append("  <changeSet id=\"R90000-modifyDataType-attachment-size\" author=\"xwiki\">\n");
        sb.append("    <comment>Upgrade size comlumn type [");
        sb.append(attachmentSizeColumn);
        sb.append("] from table [");
        sb.append(attachmentTableName);
        sb.append("] to BIGINT type</comment >\n");

        sb.append("    <modifyDataType tableName=\"");
        sb.append(attachmentTableName);
        sb.append("\"  columnName=\"");
        sb.append(attachmentSizeColumn);
        sb.append("\" newDataType=\"BIGINT\"/>\n");

        sb.append("  </changeSet>\n");

        return sb.toString();
    }
}

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
package org.xwiki.index.migration;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrate XWikiDocumentIndexingTask by copying the table to a new temporary table. Then, after hibernate migration,
 * when XWikiDocumentIndexingTask is created again, the rows from the temporary table are copied to the new table.
 * Finally, the temporary table is dropped.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
@Named(R140300001XWIKI19571DataMigration.HINT)
public class R140300001XWIKI19571DataMigration extends AbstractHibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "R140300001XWIKI19571";

    @Override
    public String getDescription()
    {
        return "Copy the content of XWikiDocumentIndexingTask in a temporary table, let hibernate initialize "
            + "XWikiDocumentIndexingTask with a new schema, copy the content of the temporary table in "
            + "XWikiDocumentIndexingTask, then drop the temporary table.";
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, all the work is done as Liquibase changes
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140300001);
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        // If the 
        // TASK_TIMESTAMP Needs to be dropped, otherwise xwikidocumentindexingqueue cannot be created again since the 
        // index already exists
        return "<changeSet author=\"xwikiorg\" id=\"" + HINT + "0\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <and>\n"
            + "      <tableExists tableName=\"xwikidocumentindexingqueue\"/>\n"
            + "      <not>\n"
            + "        <sqlCheck expectedResult=\"0\">SELECT count(1) FROM xwikidocumentindexingqueue;</sqlCheck>\n"
            + "      </not>\n"
            + "    </and>\n"
            + "  </preConditions>\n"
            + "  <renameTable oldTableName=\"xwikidocumentindexingqueue\" "
            + "newTableName=\"xwikidocumentindexingqueuetmp\"/>\n"
            + "   <dropIndex tableName=\"xwikidocumentindexingqueuetmp\" indexName=\"TASK_TIMESTAMP\"/>\n"
            + "</changeSet>\n"
            + "<changeSet author=\"xwikiorg\" id=\"" + HINT + "1\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <tableExists tableName=\"xwikidocumentindexingqueue\"/>\n"
            + "    <sqlCheck expectedResult=\"0\">SELECT count(1) FROM xwikidocumentindexingqueue;</sqlCheck>\n"
            + "  </preConditions>\n"
            + "  <dropTable tableName=\"xwikidocumentindexingqueue\"/>\n"
            + "</changeSet>\n\n";
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        return "<changeSet author=\"xwikiorg\" id=\"" + HINT + "\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <and>\n"
            + "      <tableExists tableName=\"xwikidocumentindexingqueuetmp\"/>\n"
            + "      <not>\n"
            + "        <sqlCheck expectedResult=\"0\">SELECT count(1) FROM xwikidocumentindexingqueuetmp;</sqlCheck>\n"
            + "      </not>\n"
            + "    </and>\n"
            + "  </preConditions>\n"
            + "  <sql>" 
            + "INSERT INTO xwikidocumentindexingqueue (XWT_DOC_ID, XWT_VERSION, XWT_TYPE, XWT_INSTANCE_ID, XWT_TIMESTAMP) "
            + "SELECT XWT_DOC_ID, XWT_VERSION, XWT_TYPE, XWT_INSTANCE_ID, XWT_TIMESTAMP " 
            + "FROM xwikidocumentindexingqueuetmp" 
            + "</sql>\n"
            + "  <dropTable tableName= \"xwikidocumentindexingqueuetmp\"/>\n"
            + "</changeSet>\n\n";
    }
}

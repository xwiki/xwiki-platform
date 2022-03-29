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

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link R140300001XWIKI19571DataMigration}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class R140300001XWIKI19571DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140300001XWIKI19571DataMigration dataMigration;

    @Test
    void getPreHibernateLiquibaseChangeLog() throws Exception
    {
        assertEquals("<changeSet author=\"xwikiorg\" id=\"R140300001XWIKI195710\">\n"
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
            + "<changeSet author=\"xwikiorg\" id=\"R140300001XWIKI195711\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <tableExists tableName=\"xwikidocumentindexingqueue\"/>\n"
            + "    <sqlCheck expectedResult=\"0\">SELECT count(1) FROM xwikidocumentindexingqueue;</sqlCheck>\n"
            + "  </preConditions>\n"
            + "  <dropTable tableName=\"xwikidocumentindexingqueue\"/>\n"
            + "</changeSet>\n"
            + "\n", this.dataMigration.getPreHibernateLiquibaseChangeLog());
    }

    @Test
    void getLiquibaseChangeLog() throws Exception
    {
        assertEquals("<changeSet author=\"xwikiorg\" id=\"R140300001XWIKI19571\">\n"
            + "  <preConditions onFail=\"MARK_RAN\">\n"
            + "    <and>\n"
            + "      <tableExists tableName=\"xwikidocumentindexingqueuetmp\"/>\n"
            + "      <not>\n"
            + "        <sqlCheck expectedResult=\"0\">SELECT count(1) FROM xwikidocumentindexingqueuetmp;</sqlCheck>\n"
            + "      </not>\n"
            + "    </and>\n"
            + "  </preConditions>\n"
            + "  <sql>" 
            + "INSERT INTO xwikidocumentindexingqueue (XWT_DOC_ID, XWT_VERSION, XWT_TYPE, XWT_INSTANCE_ID, XWT_INSTANCE_ID) "
            + "SELECT XWT_DOC_ID, XWT_VERSION, XWT_TYPE, XWT_INSTANCE_ID, XWT_INSTANCE_ID " 
            + "FROM xwikidocumentindexingqueuetmp" 
            + "</sql>\n"
            + "  <dropTable tableName= \"xwikidocumentindexingqueuetmp\"/>\n"
            + "</changeSet>\n"
            + "\n", this.dataMigration.getLiquibaseChangeLog());
    }
}

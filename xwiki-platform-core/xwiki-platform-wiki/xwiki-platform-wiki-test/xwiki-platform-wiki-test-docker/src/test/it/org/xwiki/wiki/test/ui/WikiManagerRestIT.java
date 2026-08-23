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
package org.xwiki.wiki.test.ui;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.rest.WikiManagerREST;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the Wiki manager REST API.
 *
 * @version $Id$
 */
@UITest(properties = {
    // The row format check below reads the store APIs from a Groovy script, in a page created by
    // TestUtils#executeWikiPlain() which thus needs Programming Rights.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*"
})
class WikiManagerRestIT
{
    private static final String WIKI_ID = "foo";

    @Test
    @Order(1)
    void testCreateWiki(TestUtils setup) throws Exception
    {
        setup.createUser("CreateWikiTest", "CreateWikiTestPWD", null);
        setup.login("CreateWikiTest", "CreateWikiTestPWD");

        Wiki wiki = new Wiki();
        wiki.setId(WIKI_ID);
        wiki.setName("test");
        wiki.setName("Some description");
        PostMethod postMethod = setup.rest().executePost(WikiManagerREST.class, wiki);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, postMethod.getStatusCode());

        // Need admin right to create a wiki
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        postMethod = setup.rest().executePost(WikiManagerREST.class, wiki);
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        try (InputStream stream = postMethod.getResponseBodyAsStream()) {
            wiki = setup.rest().toResource(stream);
        }
        assertEquals(WIKI_ID, wiki.getId());

        // Back to guest
        setup.setDefaultCredentials(null);
        Wikis wikis = setup.rest().get(WikisResource.class, true);

        boolean found = false;
        for (Wiki w : wikis.getWikis()) {
            if (WIKI_ID.equals(w.getId())) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    /**
     * Verify that the row format of the tables of a newly created wiki is updated in the database of that wiki. The
     * statement is executed on a session taken straight from the session factory, so an unqualified table name is
     * resolved against the database currently selected in the pooled JDBC connection, i.e. the database of whichever
     * wiki borrowed that connection last.
     * <p>
     * This covers the case where the row format is silently applied to another wiki's table. The case where the other
     * database has been dropped in the meantime, which makes the wiki initialization fail, depends on the state of the
     * connection pool and is not covered.
     * <p>
     * A subwiki is required: the main wiki passes even when the table name is not qualified, since the catalog of a
     * fresh connection comes from the JDBC URL and is already the right database.
     */
    @Test
    @Order(2)
    void checkTableRowFormat(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Only the MySQL and MariaDB adapters update the row formats. Note that the database cannot be forced with
        // @UITest(database = ...) since -Dxwiki.test.ui.database takes precedence over it.
        Database database = testConfiguration.getDatabase();
        assumeTrue(database == Database.MYSQL || database == Database.MARIADB,
            () -> String.format("The row format is not updated on [%s]", database));

        setup.loginAsSuperAdmin();

        // The script is executed in the main wiki, where the {{groovy}} macro is provisioned, and switches the context
        // to the created wiki so that the store APIs target that wiki's database. The row formats are read through the
        // store API so that the difference between the MySQL and the MariaDB information schema tables is handled, and
        // they are compared with the format the configuration asks for rather than with a hardcoded one since
        // compression can be disabled.
        assertEquals("The row format of table [xwikircs] is the expected one.", setup.executeWikiPlain("""
            {{groovy wiki="false"}}
            import com.xpn.xwiki.internal.store.hibernate.HibernateStore

            def store = services.component.getInstance(HibernateStore.class)
            def adapter = store.getAdapter()
            def context = xcontext.context
            def previousWikiId = context.getWikiId()
            context.setWikiId('%s')
            def session = store.getSessionFactory().openSession()
            try {
              def rowFormat = adapter.getRowFormats(session).get('xwikircs')
              def expectedRowFormat = adapter.isCompressionAllowed()
                ? adapter.getCompressedRowFormat() : adapter.getDefaultRowFormat()
              if (expectedRowFormat.equalsIgnoreCase(rowFormat)) {
                print("The row format of table [xwikircs] is the expected one.")
              } else {
                print("The row format of table [xwikircs] is [${rowFormat}] instead of [${expectedRowFormat}].")
              }
            } finally {
              session.close()
              context.setWikiId(previousWikiId)
            }
            {{/groovy}}
            """.formatted(WIKI_ID), Syntax.XWIKI_2_1));
    }
}

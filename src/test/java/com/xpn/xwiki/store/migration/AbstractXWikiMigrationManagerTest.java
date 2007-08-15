/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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
package com.xpn.xwiki.store.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiEngineContext;
/**
 * Test for {@link AbstractXWikiMigrationManager}
 * @version $Id: $
 */
public class AbstractXWikiMigrationManagerTest extends TestCase
{
    XWikiContext context;
    /** {@inheritDoc} */
    protected void setUp() throws Exception
    {
        super.setUp();
        context = new XWikiContext();
        XWikiConfig config = new XWikiConfig();
        context.setWiki(new XWiki(config, context) {
            public void initXWiki(XWikiConfig config, XWikiContext context,
                XWikiEngineContext engine_context, boolean noupdate) throws XWikiException
            {}
        });
        context.getWiki().setConfig(config);
    }
    /** mocked migration manager */
    private static class TestMigrationManager extends AbstractXWikiMigrationManager {
        public TestMigrationManager(XWikiContext context)
        {
            super(context);
        }
        private XWikiMigratorInterface createMigrator(final int ver) {
            return new XWikiMigratorInterface() {
                public XWikiDBVersion getVersion()
                {
                    return new XWikiDBVersion(ver);
                }
                public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
                    throws XWikiException
                {
                }
            };
        }
        protected List getAllMigrations(XWikiContext context) throws XWikiException
        {
            List lst = new ArrayList();
            lst.add(createMigrator(123));
            lst.add(createMigrator(234));
            lst.add(createMigrator(345));
            lst.add(createMigrator(456));
            return lst;
        }
        XWikiDBVersion curversion;
        protected void setDBVersion(XWikiDBVersion version, XWikiContext context)
            throws XWikiException
        {
            this.curversion = version;
        }
    }
    /** test migration if there are no data version */
    public void testMigrationWhenNoVersion() throws Exception {
        TestMigrationManager mm = new TestMigrationManager(context);
        Collection neededMigration = mm.getNeededMigrations(context);
        assertEquals(4, neededMigration.size());
        mm.startMigrations(context);
        assertEquals(457, mm.curversion.getVersion());
    }
    /** test parameters "xwiki.store.migration.version", "xwiki.store.migration.ignored"
     *   and migrations order */ 
    public void testMigrationOrderAndIgnore() throws Exception {
        XWikiConfig config = context.getWiki().getConfig();
        config.setProperty("xwiki.store.migration.version", "234");
        config.setProperty("xwiki.store.migration.ignored", "345");
        TestMigrationManager mm = new TestMigrationManager(context);
        Collection neededMigration = mm.getNeededMigrations(context);
        assertEquals(2, neededMigration.size());
        XWikiMigratorInterface[] actual = new XWikiMigratorInterface[2];
        neededMigration.toArray(actual);
        assertEquals(234, actual[0].getVersion().getVersion());
        assertEquals(456, actual[1].getVersion().getVersion());
    }
    
    public static class TestForceMigratior implements XWikiMigratorInterface {
        public XWikiDBVersion getVersion()
        {
            return new XWikiDBVersion(567);
        }
        public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
            throws XWikiException
        { }
    }
    /** test "xwiki.store.migration.force" parameter */
    public void testMigrationForce() throws Exception {
        XWikiConfig config = context.getWiki().getConfig();
        config.setProperty("xwiki.store.migration.version", "234");
        config.setProperty("xwiki.store.migration.force", TestForceMigratior.class.getName());
        TestMigrationManager mm = new TestMigrationManager(context);
        Collection neededMigration = mm.getNeededMigrations(context);
        assertEquals(1, neededMigration.size());
        assertEquals(567, ((XWikiMigratorInterface)neededMigration.toArray()[0]).getVersion().getVersion());
    }
}

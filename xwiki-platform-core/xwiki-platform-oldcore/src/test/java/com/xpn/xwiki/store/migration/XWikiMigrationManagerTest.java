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
package com.xpn.xwiki.store.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Test for {@link AbstractDataMigrationManager}
 * 
 * @version $Id$
 */
public class XWikiMigrationManagerTest extends AbstractBridgedXWikiComponentTestCase
{
    /** mocked migration manager */
    @Component(staticRegistration = false)
    @Named("TestDataMigration")
    @Singleton
    public static class TestDataMigrationManager extends AbstractDataMigrationManager
    {
        private DataMigration createMigrator(final int ver)
        {
            return new DataMigration()
            {
                @Override
                public String getName()
                {
                    return "Test";
                }

                @Override
                public String getDescription()
                {
                    return "Test";
                }

                @Override
                public XWikiDBVersion getVersion()
                {
                    return new XWikiDBVersion(ver);
                }

                @Override
                public boolean shouldExecute(XWikiDBVersion startupVersion)
                {
                    return true;
                }

                @Override
                public void migrate()
                {
                }
            };
        }

        @Override
        protected List<DataMigration> getAllMigrations()
        {
            List<DataMigration> lst = new ArrayList<DataMigration>();
            lst.add(createMigrator(345));
            lst.add(createMigrator(123));
            lst.add(createMigrator(456));
            lst.add(createMigrator(234));

            return lst;
        }

        XWikiDBVersion curversion;

        @Override
        protected void initializeEmptyDB() throws DataMigrationException
        {
        }

        @Override
        protected void setDBVersionToDatabase(XWikiDBVersion version)
        {
            this.curversion = version;
        }

        @Override
        protected void updateSchema(Collection<XWikiMigration> migrations)
        {
        }
    }

    private void registerComponent(Class<?> klass) throws Exception
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDescriptor> descriptors = loader.getComponentsDescriptors(klass);

        for (ComponentDescriptor<?> descriptor : descriptors) {
            getComponentManager().registerComponent(descriptor);
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        getContext().setWiki(new XWiki() {
            @Override
            public List<String> getVirtualWikisDatabaseNames(XWikiContext context) throws XWikiException
            {
                return Arrays.asList("xwiki");
            }
        });

        registerComponent(TestDataMigrationManager.class);
    }

    /** test migration if there are no data version */
    public void testMigrationWhenNoVersion() throws Exception
    {
        TestDataMigrationManager mm = (TestDataMigrationManager) getComponentManager().getInstance(
            DataMigrationManager.class,"TestDataMigration");
        Collection neededMigration = mm.getNeededMigrations();
        assertEquals(0, neededMigration.size());
        mm.startMigrations();
        assertEquals(456, mm.curversion.getVersion());
    }

    /**
     * test parameters "xwiki.store.migration.version", "xwiki.store.migration.ignored" and migrations order
     */
    public void testMigrationOrderAndIgnore() throws Exception
    {
        getConfigurationSource().setProperty("xwiki.store.migration.version", "123");
        getConfigurationSource().setProperty("xwiki.store.migration.ignored", "345");
        TestDataMigrationManager mm = getComponentManager().getInstance(
            DataMigrationManager.class,"TestDataMigration");
        Collection neededMigration = mm.getNeededMigrations();
        assertEquals(2, neededMigration.size());
        AbstractDataMigrationManager.XWikiMigration[] actual = new AbstractDataMigrationManager.XWikiMigration[2];
        neededMigration.toArray(actual);
        assertEquals(234, actual[0].dataMigration.getVersion().getVersion());
        assertEquals(456, actual[1].dataMigration.getVersion().getVersion());
    }

    @Component(staticRegistration = false)
    @Named("TestForcedMigration")
    @Singleton
    public static class TestForceMigration implements DataMigration
    {
        @Override
        public String getName()
        {
            return "Test";
        }

        @Override
        public String getDescription()
        {
            return "Test";
        }

        @Override
        public XWikiDBVersion getVersion()
        {
            return new XWikiDBVersion(567);
        }

        @Override
        public boolean shouldExecute(XWikiDBVersion startupVersion)
        {
            return true;
        }

        @Override
        public void migrate() throws DataMigrationException
        {
        }
    }

    /** test "xwiki.store.migration.force" parameter */
    public void testMigrationForce() throws Exception
    {
        getConfigurationSource().setProperty("xwiki.store.migration.version", "234");
        getConfigurationSource().setProperty("xwiki.store.migration.force", "TestForcedMigration");
        registerComponent(TestForceMigration.class);

        TestDataMigrationManager mm = getComponentManager().getInstance(
            DataMigrationManager.class,"TestDataMigration");
        Collection neededMigration = mm.getNeededMigrations();
        assertEquals(1, neededMigration.size());
        assertEquals(567, ((AbstractDataMigrationManager.XWikiMigration) neededMigration.toArray()[0])
            .dataMigration.getVersion().getVersion());
    }
}

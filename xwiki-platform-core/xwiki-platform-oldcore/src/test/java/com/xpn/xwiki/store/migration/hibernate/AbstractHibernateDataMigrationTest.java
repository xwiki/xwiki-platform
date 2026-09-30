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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.store.migration.XWikiDBVersion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AbstractHibernateDataMigration}.
 *
 * @version $Id$
 */
class AbstractHibernateDataMigrationTest
{
    private static class TestDataMigration extends AbstractHibernateDataMigration
    {
        private final List<XWikiDBVersion> backportVersions;

        TestDataMigration(int... backportVersions)
        {
            this.backportVersions = Arrays.stream(backportVersions).mapToObj(XWikiDBVersion::new).toList();
        }

        @Override
        public String getDescription()
        {
            return "Test migration";
        }

        @Override
        public XWikiDBVersion getVersion()
        {
            return new XWikiDBVersion(180100000);
        }

        @Override
        protected List<XWikiDBVersion> getBackportVersions()
        {
            return this.backportVersions;
        }

        @Override
        protected void hibernateMigrate()
        {
            // Nothing to migrate.
        }
    }

    @Test
    void shouldExecuteWhenNoBackport()
    {
        TestDataMigration migration = new TestDataMigration();

        assertTrue(migration.shouldExecute(new XWikiDBVersion(0)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(171014000)));
    }

    @Test
    void shouldExecuteWhenBackportedToLastMinorOfCycle()
    {
        // 17.10 is the last branch of the 17.x cycle, so the next branch is 18.0.
        TestDataMigration migration = new TestDataMigration(171014000);

        assertTrue(migration.shouldExecute(new XWikiDBVersion(171013000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(171014000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(171015000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(180000000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(180002000)));
    }

    @Test
    void shouldExecuteWhenBackportedToIntermediateMinor()
    {
        // 16.4 is followed by the 16.5 branch.
        TestDataMigration migration = new TestDataMigration(160401000);

        assertTrue(migration.shouldExecute(new XWikiDBVersion(160400000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(160401000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(160499000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(160500000)));
    }

    @Test
    void shouldExecuteWhenBackportedToSeveralBranches()
    {
        TestDataMigration migration = new TestDataMigration(131008000, 140403000);

        assertTrue(migration.shouldExecute(new XWikiDBVersion(131007000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(131008000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(140000000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(140402000)));
        assertFalse(migration.shouldExecute(new XWikiDBVersion(140403000)));
        assertTrue(migration.shouldExecute(new XWikiDBVersion(140500000)));
    }
}

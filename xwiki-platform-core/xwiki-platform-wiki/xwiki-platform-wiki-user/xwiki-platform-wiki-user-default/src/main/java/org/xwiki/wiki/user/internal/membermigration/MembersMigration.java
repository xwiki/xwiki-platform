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
package org.xwiki.wiki.user.internal.membermigration;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Component that create a XWiki.XWikiMemberGroup for subwikis initialized with the list of current users (located in
 * XWiki.XWikiAllGroup). It also update rights given to XWikiAllGroup to occurs on XWikiMemberGroup and move candidacies
 * to XWiki.XWikiMemberGroup.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@Component
@Named("R54000MembersMigration")
public class MembersMigration extends AbstractHibernateDataMigration
{
    @Inject
    private MemberGroupMigrator memberGroupMigrator;

    @Inject
    private MemberRightsMigrator memberRightsMigrator;

    @Inject
    private MemberCandidaciesMigrator memberCandidaciesMigrator;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "http://jira.xwiki.org/browse/XWIKI-9886";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We migrate only subwikis
        return !wikiDescriptorManager.getCurrentWikiId().equals(wikiDescriptorManager.getMainWikiId());
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();
        memberGroupMigrator.migrateGroups(currentWikiId);
        memberRightsMigrator.upgradeRights(currentWikiId);
        memberCandidaciesMigrator.migrateCandidacies(currentWikiId);
    }
}

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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.xwiki.index.internal.DefaultLinksTaskConsumer.LINKS_TASK_TYPE;

/**
 * Migrates the links by reindexing all the documents of the farm.
 *
 * @version $Id$
 * @since 14.2RC1
 */
// TODO: Implement DataMigration once XWIKI-19399 is fixed.
@Component
@Singleton
@Named(R14010XWIKI19352DataMigration.HINT)
public class R14010XWIKI19352DataMigration implements HibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "R14010XWIKI19352";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private TaskManager taskManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public String getDescription()
    {
        return "Queue all the document of the wiki for links indexing.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140100000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        XWikiContext context = this.contextProvider.get();

        try {
            for (String wikiId : this.wikiDescriptorManager.getAllIds()) {
                context.setWikiId(wikiId);
                // No need to migrate if the wiki does not support backlinks.
                if (context.getWiki().hasBacklinks(context)) {
                    migrateWiki(wikiId);
                }
            }
        } catch (WikiManagerException e) {
            throw new DataMigrationException("Failed retrieve the list of wiki identifiers.", e);
        }
    }

    private void migrateWiki(String wikiId) throws DataMigrationException
    {
        try {
            List<Object[]> rows =
                this.queryManager.createQuery("SELECT doc.id, doc.version FROM XWikiDocument doc",
                    Query.HQL).setWiki(wikiId).execute();
            for (Object[] row : rows) {
                this.taskManager.replaceTask(wikiId, (long) row[0], (String) row[1], LINKS_TASK_TYPE);
            }
        } catch (QueryException e) {
            throw new DataMigrationException(
                String.format("Failed retrieve the list of all the documents for wiki [%s].", wikiId), e);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return true;
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog()
    {
        // TODO: Remove once XWIKI-19399 is fixed.
        return null;
    }

    @Override
    public String getLiquibaseChangeLog()
    {
        // TODO: Remove once XWIKI-19399 is fixed.
        return null;
    }
}

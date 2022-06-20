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
 * @deprecated links indexing move to Solr, see org.xwiki.refactoring.internal.solr.* in
 *             xwiki-platform-refactoring-default module
 */
// TODO: Implement DataMigration once XWIKI-19399 is fixed.
@Component
@Singleton
@Named(R140300000XWIKI19614DataMigration.HINT)
@Deprecated(since = "14.8RC1")
public class R140300000XWIKI19614DataMigration implements HibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "R140300000XWIKI19614";

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
        return new XWikiDBVersion(140300000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        XWikiContext context = this.contextProvider.get();
        // No need to migrate if the wiki does not support backlinks.
        if (context.getWiki().hasBacklinks(context)) {
            String wikiId = context.getWikiId();
            try {
                List<Long> ids =
                    this.queryManager.createQuery("SELECT doc.id FROM XWikiDocument doc", Query.HQL).setWiki(wikiId)
                        .execute();
                for (Long id : ids) {
                    this.taskManager.addTask(wikiId, id, LINKS_TASK_TYPE);
                }
            } catch (QueryException e) {
                throw new DataMigrationException(
                    String.format("Failed retrieve the list of all the documents for wiki [%s].", wikiId), e);
            }
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

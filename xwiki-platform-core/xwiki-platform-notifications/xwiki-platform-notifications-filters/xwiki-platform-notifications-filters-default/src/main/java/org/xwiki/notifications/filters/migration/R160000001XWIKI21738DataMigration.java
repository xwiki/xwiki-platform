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
package org.xwiki.notifications.filters.migration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrate old WatchListClass xobjects to save them as proper notification filters. The migration doesn't directly
 * remove the xobjects but asks {@link WatchListObjectsRemovalTaskConsumer} to do it.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@Component
@Named("R160000000XWIKI17243")
@Singleton
public class R160000001XWIKI21738DataMigration extends AbstractHibernateDataMigration
{
    private static final int BATCH_SIZE = 100;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("unique")
    private QueryFilter uniqueFilter;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Migrate filters next to users.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160000000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // This migration only needs to be performed on main wiki: if we have filters on local wikis it's because
        // local filters was enabled.
        // Migration steps:
        //   - Retrieve all filters from main wiki where owner belongs to a subwiki
        //   - Store that filter on the subwiki DB and remove it from main wiki

        String statement = "from doc.object(XWiki.WatchListClass) as user";
    }

}

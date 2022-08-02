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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSpace;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-12228: Provide API and probably storage for optimized space related queries.
 * <p>
 * Make sure xwikidocument and xwikispace tables are in sync.
 *
 * @version $Id$
 * @since 7.2M2
 */
@Component
@Named("R72001XWIKI12228")
@Singleton
public class R72001XWIKI12228DataMigration extends AbstractHibernateDataMigration
{
    /**
     * We don't really care what is the wiki since it's only used to go trough XWikiSpace utility methods.
     */
    private static final WikiReference WIKI = new WikiReference("wiki");

    @Inject
    private SpaceReferenceResolver<String> spaceResolver;

    @Override
    public String getDescription()
    {
        return "Make sure xwikidocument and xwikispace tables are in sync";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(72001);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                // Copy visible spaces
                Collection<SpaceReference> visibleSpaces = createVisibleSpaces(session);

                // Copy hidden spaces
                createHiddenSpaces(visibleSpaces, session);

                return Boolean.TRUE;
            }
        });
    }

    private String createSpaceQuery(boolean hidden)
    {
        StringBuilder query = new StringBuilder("select DISTINCT doc.space from XWikiDocument as doc where");
        if (hidden) {
            query.append(" doc.space not in (" + createSpaceQuery(false) + ")");
        } else {
            query.append(" doc.hidden <> true OR doc.hidden IS NULL");
        }

        return query.toString();
    }

    private Collection<SpaceReference> getVisibleSpaces(Session session)
    {
        Query<String> query = session.createQuery(createSpaceQuery(false), String.class);

        Collection<SpaceReference> databaseSpaces = new ArrayList<>();
        for (String space : query.list()) {
            databaseSpaces.add(this.spaceResolver.resolve(space, WIKI));
        }

        // Resolve nested spaces
        Set<SpaceReference> spaces = new HashSet<>(databaseSpaces);
        for (SpaceReference space : databaseSpaces) {
            for (EntityReference parent = space.getParent(); parent instanceof SpaceReference; parent =
                parent.getParent()) {
                spaces.add((SpaceReference) parent);
            }
        }

        return spaces;
    }

    private Collection<SpaceReference> getHiddenSpaces(Collection<SpaceReference> visibleSpaces, Session session)
    {
        Query<String> query = session.createQuery(createSpaceQuery(true), String.class);

        Collection<SpaceReference> databaseSpaces = new ArrayList<>();
        for (String space : query.list()) {
            databaseSpaces.add(this.spaceResolver.resolve(space, WIKI));
        }

        // Resolve nested spaces
        Set<SpaceReference> spaces = new HashSet<>(databaseSpaces);
        for (SpaceReference space : databaseSpaces) {
            for (EntityReference parent = space.getParent(); parent instanceof SpaceReference; parent =
                parent.getParent()) {
                if (!visibleSpaces.contains(parent)) {
                    spaces.add((SpaceReference) parent);
                }
            }
        }

        return spaces;
    }

    private Collection<SpaceReference> createVisibleSpaces(Session session)
    {
        // Get spaces
        Collection<SpaceReference> spaces = getVisibleSpaces(session);

        // Create spaces
        createSpaces(spaces, false, session);

        return spaces;
    }

    private void createHiddenSpaces(Collection<SpaceReference> visibleSpaces, Session session)
    {
        // Get spaces
        Collection<SpaceReference> spaces = getHiddenSpaces(visibleSpaces, session);

        // Create spaces
        createSpaces(spaces, true, session);
    }

    private void createSpaces(Collection<SpaceReference> spaces, boolean hidden, Session session)
    {
        // Create spaces in the xwikispace table
        for (SpaceReference spaceReference : spaces) {
            XWikiSpace space = new XWikiSpace(spaceReference);

            space.setHidden(hidden);

            session.save(space);
        }
    }
}

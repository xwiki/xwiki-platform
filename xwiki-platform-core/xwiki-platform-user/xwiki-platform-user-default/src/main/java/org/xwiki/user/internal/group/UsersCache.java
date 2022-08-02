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
package org.xwiki.user.internal.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Various user related helpers.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
// TODO: replace by a proper public API
@Component(roles = UsersCache.class)
@Singleton
public class UsersCache implements Initializable, Disposable
{
    private static final int DEFAULT_CAPACITY = 50;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private QueryManager queries;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private EntityReferenceFactory referenceFactory;

    @Inject
    private Logger logger;

    private Cache<List<DocumentReference>> cache;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration cacheConfig = new LRUCacheConfiguration("user.users", DEFAULT_CAPACITY);
        try {
            this.cache = this.cacheManager.createNewCache(cacheConfig);
        } catch (Exception e) {
            throw new InitializationException("Failed to create the user cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.cache != null) {
            this.cache.dispose();
        }
    }

    /**
     * Search for the users belonging to the passed wiki.
     * 
     * @param wiki the wiki were to search for users
     * @param activeOnly true if only active users should be returned
     * @return the users references
     */
    public List<DocumentReference> getUsers(WikiReference wiki, boolean activeOnly)
    {
        String cacheKey = activeOnly + wiki.getName();

        List<DocumentReference> cached = this.cache.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        List<String> result;
        try {
            StringBuilder statement = new StringBuilder(
                "select distinct doc.fullName from Document doc, doc.object(XWiki.XWikiUsers) as user");

            if (activeOnly) {
                statement.append(" where user.active = 1");
            }

            Query query = this.queries.createQuery(statement.toString(), Query.XWQL);
            query.setWiki(wiki.getName());

            result = query.execute();
        } catch (QueryException e) {
            this.logger.error("Failed to get users from the wiki [{}]", wiki, e);

            // Return an empty list but don't cache it in case the problem is temporary
            return Collections.emptyList();
        }

        List<DocumentReference> users;

        if (result.isEmpty()) {
            users = Collections.emptyList();
        } else {
            List<DocumentReference> list = new ArrayList<>(result.size());
            result.forEach(e -> list.add(this.referenceFactory.getReference(this.resolver.resolve(e, wiki))));
            users = Collections.unmodifiableList(list);
        }

        this.cache.set(cacheKey, users);

        return users;
    }

    /**
     * @param wiki the id of of the wiki to clean
     */
    public void cleanCache(String wiki)
    {
        this.cache.remove("true" + wiki);
        this.cache.remove("false" + wiki);
    }
}

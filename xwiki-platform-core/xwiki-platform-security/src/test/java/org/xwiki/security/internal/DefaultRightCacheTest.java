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
 *
 */
package org.xwiki.security.internal;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.AccessLevel;
import org.xwiki.security.ConflictingInsertionException;
import org.xwiki.security.ParentEntryEvictedException;
import org.xwiki.security.Right;
import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheEntry;
import org.xwiki.security.RightCacheKey;
import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.assertTrue;

public class DefaultRightCacheTest extends AbstractComponentTestCase
{
    DocumentReferenceResolver<String> resolver = null;
    RightCache cache = null;

    private final RightCacheEntry[] testEntries = {
        new AccessLevel() {{ allow(Right.VIEW); }},
        new AccessLevel() {{ deny(Right.VIEW); }},
        new AccessLevel() {{ allow(Right.EDIT); }},
        new AccessLevel() {{ deny(Right.EDIT); }},
        new AccessLevel() {{ allow(Right.DELETE); }},
        new AccessLevel() {{ deny(Right.DELETE); }},
        new AccessLevel() {{ allow(Right.ADMIN); }},
        new AccessLevel() {{ deny(Right.ADMIN); }},
        new AccessLevel() {{ allow(Right.PROGRAM); }},
        new AccessLevel() {{ deny(Right.PROGRAM); }},
        new AccessLevel() {{ allow(Right.COMMENT); }},
        new AccessLevel() {{ deny(Right.COMMENT); }},
        new AccessLevel() {{ allow(Right.REGISTER); }},
        new AccessLevel() {{ deny(Right.REGISTER); }},
    };

    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(DefaultRightCacheTest.class);

    private static RightCacheKey k(RightCache c, EntityReference e)
    {
        return c.getRightCacheKey(e);
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        try {
            Utils.setComponentManager(getComponentManager());
            final Execution execution = getComponentManager().lookup(Execution.class);

            final ExecutionContext context = execution.getContext();

            //        execution.setContext(context);

            XWikiContext xwikiContext = new XWikiContext();

            xwikiContext.setMainXWiki("mainxwiki");
            context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
            xwikiContext.setWiki(new XWiki());

            cache = getComponentManager().lookup(RightCache.class);
            resolver = getComponentManager().lookup(DocumentReferenceResolver.class);

        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw e;
        }
    }

    @Test
    public void testRightCache()
    {

        DocumentReference guestUser = resolver.resolve("XWiki.XWikiGuest");
        DocumentReference document = resolver.resolve("page");

        try {
            RightCacheKey k = cache.getRightCacheKey(document);
            for (EntityReference e : k.getEntityReference().getReversedReferenceChain()) {
                cache.add(k(cache, e), AccessLevel.getDefaultAccessLevel());
            }

            insertToCache(guestUser, null, cache, AccessLevel.getDefaultAccessLevel());
            cache.addUserAtEntity(k(cache, guestUser), k(cache, document), AccessLevel.getDefaultAccessLevel());
            RightCacheEntry rce = cache.get(k(cache, guestUser), k(cache, document));
            assertTrue(rce == AccessLevel.getDefaultAccessLevel());
            cache.remove(k(cache, guestUser), k(cache, document));
            rce = cache.get(k(cache, guestUser), k(cache, document));
            assertTrue(rce == null);
            rce = cache.get(k(cache, document));
            assertTrue(rce == AccessLevel.getDefaultAccessLevel());
            cache.remove(k(cache, document.getRoot()));
            rce = cache.get(k(cache, document.getParent()));
            assertTrue(rce == null);
            rce = cache.get(k(cache, document));
            assertTrue(rce == null);
        } catch(ParentEntryEvictedException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testInsertions()
    {
        String[] wikis = {"wiki1", "wiki2", "wiki3", "wiki4"};
        String[] spaces = {"space1", "spac2", "space3", "space4"};
        String[] pages = {"page1", "Page2", "page3", "page4"};
        List<EntityReference> entities = new LinkedList<EntityReference>();
        for (String wiki : wikis) {
            for (String space : spaces) {
                for (String page : pages) {
                    entities.add(resolver.resolve(wiki + ":" + space + "." + page));
                }
            }
        }

        int i = 0;
        for (EntityReference e : entities) {
            insertToCache(e, null, cache, testEntries[i]);
            i = (i + 1) % testEntries.length;
        }

        i = 0;
        int noNull = 0;
        int other = 0;
        for (EntityReference e : entities) {
            RightCacheEntry entry = cache.get(k(cache, e));
            assertTrue(entry == null || entry == testEntries[i]);
            if (entry == null) {
                noNull++;
            } else {
                other++;
            }
            i = (i + 1) % testEntries.length;
        }
        System.out.println("Number null: " + noNull + ", other: " + other);
    }

    @Test
    public void testMultiParent()
    {
        String[] wikis = {"wiki1", "wiki2", "wiki3", "wiki4"};
        String[] spaces = {"space1", "spac2", "space3", "space4"};
        String[] pages = {"page1", "Page2", "page3", "page4"};
        List<EntityReference> entities = new LinkedList<EntityReference>();
        for (String wiki : wikis) {
            for (String space : spaces) {
                for (String page : pages) {
                    entities.add(resolver.resolve(wiki + ":" + space + "." + page));
                }
            }
        }
        try {
            int i = 0;
            for (EntityReference e : entities) {
                if (e.getType() != EntityType.DOCUMENT || !e.getName().equals("page4")) {
                    insertToCache(e, null, cache, testEntries[i]);
                } else {
                    int index = entities.indexOf(e);
                    List<EntityReference> parents = entities.subList(index - 3, index - 1);
                    insertToCache(e, parents, cache, testEntries[i]);
                }
                i = (i + 1) % testEntries.length;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        int i = 0;
        int noNull = 0;
        int other = 0;
        for (EntityReference e : entities) {
            RightCacheEntry entry = cache.get(k(cache, e));
            assertTrue(entry == null || entry == testEntries[i]);
            if (entry == null) {
                noNull++;
            } else {
                other++;
            }
            i = (i + 1) % testEntries.length;
        }
        System.out.println("Number null: " + noNull + ", other: " + other);
    }

    private void insertToCache(EntityReference e, List<EntityReference> parents, RightCache c, RightCacheEntry x)
    {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                if (parents != null) {
                    for (EntityReference p : parents) {
                        insertToCache(p, null, c, x);
                    }
                }
                RightCacheKey key = k(c, e);
                for (EntityReference r : key.getEntityReference().getReversedReferenceChain()) {
                    RightCacheEntry entry = c.get(k(cache, r));
                    if (entry == null) {
                        c.add(c.getRightCacheKey(r), x);
                    }
                }
            } catch (ParentEntryEvictedException e0) {
                if (attempts > 10) {
                    LOG.error("Failed to insert entry on " + attempts + " attempts.");
                    throw new RuntimeException("Failed to insert entry on " + attempts + " attempts.");
                }
                continue;
            } catch (ConflictingInsertionException e0) {
                if (attempts > 10) {
                    LOG.error("Failed to insert entry on " + attempts + " attempts.");
                    throw new RuntimeException("Failed to insert entry on " + attempts + " attempts.");
                }
                continue;
            }
            break;
        }
        if (attempts > 1) {
            System.out.println("Inserted entry in " + attempts + " attempts.");
        }
    }
}

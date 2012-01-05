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
package org.xwiki.security.authorization.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AccessLevel;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityEntry;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

public class DefaultSecurityCacheTest extends AbstractComponentTestCase
{
    protected XWikiContext xwikiContext;
    DocumentReferenceResolver<String> resolver = null;
    SecurityCache cache = null;
    private SecurityReferenceFactory factory;

    private final AccessLevel[] testEntries = {
        new XWikiAccessLevel() {{ allow(Right.VIEW); }},
        new XWikiAccessLevel() {{ deny(Right.VIEW); }},
        new XWikiAccessLevel() {{ allow(Right.EDIT); }},
        new XWikiAccessLevel() {{ deny(Right.EDIT); }},
        new XWikiAccessLevel() {{ allow(Right.DELETE); }},
        new XWikiAccessLevel() {{ deny(Right.DELETE); }},
        new XWikiAccessLevel() {{ allow(Right.ADMIN); }},
        new XWikiAccessLevel() {{ deny(Right.ADMIN); }},
        new XWikiAccessLevel() {{ allow(Right.PROGRAM); }},
        new XWikiAccessLevel() {{ deny(Right.PROGRAM); }},
        new XWikiAccessLevel() {{ allow(Right.COMMENT); }},
        new XWikiAccessLevel() {{ deny(Right.COMMENT); }},
        new XWikiAccessLevel() {{ allow(Right.REGISTER); }},
        new XWikiAccessLevel() {{ deny(Right.REGISTER); }},
    };

    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(org.xwiki.security.authorization.internal.DefaultSecurityCacheTest.class);

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getComponentManager());

        this.xwikiContext = new XWikiContext();

        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setMainXWiki("xwiki");

        // We need to initialize the Component Manager so that the components can be looked up
        this.xwikiContext.put(ComponentManager.class.getName(), getComponentManager());

        xwikiContext.setWiki(new XWiki());

        // Bridge with old XWiki Context, required for old code.
        Execution execution = getComponentManager().lookup(Execution.class);
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);

        factory = getComponentManager().lookup(SecurityReferenceFactory.class);
        cache = getComponentManager().lookup(SecurityCache.class);
        resolver = (DocumentReferenceResolver<String>) getComponentManager().lookup(DocumentReferenceResolver.class);
    }

    @After
    public void tearDown() throws Exception
    {
        Utils.setComponentManager(null);
        super.tearDown();
    }

    @Test
    public void testSecurityCache() throws Exception
    {
        UserSecurityReference guestUser = factory.newUserReference(resolver.resolve("XWiki.XWikiGuest"));
        SecurityReference document = factory.newEntityReference(resolver.resolve("page"));

        Assert.assertEquals(newSecurityRuleEntry(document, XWikiAccessLevel.getDefaultAccessLevel()),
            newSecurityRuleEntry(document, XWikiAccessLevel.getDefaultAccessLevel()));

        for (SecurityReference e : document.getReversedSecurityReferenceChain()) {
            cache.add(newSecurityRuleEntry(e, XWikiAccessLevel.getDefaultAccessLevel()));
        }

        insertToCache(guestUser, null, cache, XWikiAccessLevel.getDefaultAccessLevel());
        cache.add(new TestSecurityAccessEntry(guestUser, document, XWikiAccessLevel.getDefaultAccessLevel()));
        SecurityEntry rce = cache.get(guestUser, document);
        Assert.assertEquals(new TestSecurityAccessEntry(guestUser, document, XWikiAccessLevel.getDefaultAccessLevel()), rce);
        cache.remove(guestUser, document);
        rce = cache.get(guestUser,document);
        Assert.assertNull(rce);
        rce = cache.get(document);
        Assert.assertEquals(newSecurityRuleEntry(document, XWikiAccessLevel.getDefaultAccessLevel()), rce);
        cache.remove(factory.newEntityReference(document.getRoot()));
        rce = cache.get(document.getParentSecurityReference());
        Assert.assertNull(rce);
        rce = cache.get(document);
        Assert.assertNull(rce);
    }

    @Test
    public void testInsertions()
    {
        String[] wikis = {"wiki1", "wiki2", "wiki3", "wiki4"};
        String[] spaces = {"space1", "spac2", "space3", "space4"};
        String[] pages = {"page1", "Page2", "page3", "page4"};
        List<SecurityReference> entities = new LinkedList<SecurityReference>();
        for (String wiki : wikis) {
            for (String space : spaces) {
                for (String page : pages) {
                    entities.add(factory.newEntityReference(resolver.resolve(wiki + ":" + space + "." + page)));
                }
            }
        }

        int i = 0;
        for (SecurityReference e : entities) {
            insertToCache(e, null, cache, testEntries[i]);
            i = (i + 1) % testEntries.length;
        }

        i = 0;
        int noNull = 0;
        int other = 0;
        for (SecurityReference e : entities) {
            SecurityRuleEntry entry = cache.get(e);
            if (entry != null) {
                Assert.assertEquals(newSecurityRuleEntry(e,testEntries[i]), entry);
            }
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
        String[] spaces = {"space1", "space2", "space3", "space4"};
        String[] pages = {"page1", "Page2", "page3", "page4"};
        List<SecurityReference> entities = new LinkedList<SecurityReference>();
        for (String wiki : wikis) {
            for (String space : spaces) {
                for (String page : pages) {
                    entities.add(factory.newEntityReference(resolver.resolve(wiki + ":" + space + "." + page)));
                }
            }
        }
        try {
            int i = 0;
            for (SecurityReference e : entities) {
                if (e.getType() != EntityType.DOCUMENT || !e.getName().equals("page4")) {
                    insertToCache(e, null, cache, testEntries[i]);
                } else {
                    int index = entities.indexOf(e);
                    List<SecurityReference> parents = entities.subList(index - 3, index - 1);
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
        for (SecurityReference e : entities) {
            SecurityEntry entry = cache.get(e);
            if (entry != null) {
                Assert.assertEquals(newSecurityRuleEntry(e,testEntries[i]), entry);
            }
            if (entry == null) {
                noNull++;
            } else {
                other++;
            }
            i = (i + 1) % testEntries.length;
        }
        System.out.println("Number null: " + noNull + ", other: " + other);
    }

    private void insertToCache(SecurityReference e, List<SecurityReference> parents, SecurityCache c, AccessLevel x)
    {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                if (parents != null) {
                    for (SecurityReference p : parents) {
                        insertToCache(p, null, c, x);
                    }
                }
                for (SecurityReference r : e.getReversedSecurityReferenceChain()) {
                    SecurityEntry entry = c.get(r);
                    if (entry == null) {
                        c.add(newSecurityRuleEntry(r, x));
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

    private SecurityRuleEntry newSecurityRuleEntry(SecurityReference r, AccessLevel x)
    {
        UserSecurityReference guestUser = factory.newUserReference(resolver.resolve("XWiki.XWikiGuest"));
        TestSecurityRule allowRules = new TestSecurityRule(RuleState.ALLOW);
        TestSecurityRule denyRules = new TestSecurityRule(RuleState.DENY);
        allowRules.add(guestUser);
        denyRules.add(guestUser);
        for (Right right : Right.getEnabledRights(r.getType())) {
            switch(x.get(right)) {
                case ALLOW:
                    allowRules.add(right);
                    break;
                case DENY:
                    denyRules.add(right);
                    break;
            }
        }
        Collection<SecurityRule> rules = new ArrayList<SecurityRule>(2);
        if (!allowRules.isEmpty()) {
            rules.add(allowRules);
            rules.add(denyRules);
        }
        return new TestSecurityRuleEntry(r, rules);
    }
}

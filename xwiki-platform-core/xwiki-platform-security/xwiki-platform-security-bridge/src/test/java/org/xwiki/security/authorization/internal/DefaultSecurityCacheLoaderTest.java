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

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.SecurityEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;

import junit.framework.Assert;

import static java.util.Arrays.asList;
import static org.xwiki.security.authorization.Right.COMMENT;
import static org.xwiki.security.authorization.Right.EDIT;

public class DefaultSecurityCacheLoaderTest extends AbstractTestCase
{
    @Test 
    public void testSecurityCacheLoader() throws Exception
    {
        final UserSecurityReference userX = referenceFactory.newUserReference(docRefResolver.resolve("wikiY:XWiki.userX"));
        final UserSecurityReference userY = referenceFactory.newUserReference(docRefResolver.resolve("wikiY:XWiki.userY"));

        MockDocument wikiDocument = new MockDocument("xwiki:XWiki.XWikiPreferences", "xwiki:XWiki.Admin");
        MockDocument allGroupDocument = MockDocument.newGroupDocument("xwiki:XWiki.XWikiAllGroup", 
                                                                      new String[]{"wikiY:XWiki.userX", 
                                                                                   "wikiY:XWiki.userY" });
        wiki.add(new MockDocument(userX.getOriginalReference(), "xwiki:XWiki.Admin")
                 .allowLocal(asList(EDIT),
                             asList("wikiY:XWiki.userX"),
                             Collections.<String>emptyList()))
            .add(new MockDocument(userY.getOriginalReference(), "xwiki:XWiki.Admin")
                 .allowLocal(asList(EDIT),
                             asList("wikiY:XWiki.userY"),
                             Collections.<String>emptyList()))
            .add(wikiDocument)
            .add(allGroupDocument);

        getMockery().checking(new Expectations()
        {{
                allowing(mockGroupService)
                    .getAllGroupsReferencesForMember(userX.getOriginalReference(), 0, 0, xwikiContext);
                will(Expectations.returnValue(asList(new DocumentReference("wikiY","XWiki","XWikiAllGroup"))));
                allowing(mockGroupService)
                    .getAllGroupsReferencesForMember(userY.getOriginalReference(), 0, 0, xwikiContext);
                will(Expectations.returnValue(asList(new DocumentReference("wikiY","XWiki","XWikiAllGroup"))));
            }});

        SecurityCacheLoader loader = getComponentManager().lookup(SecurityCacheLoader.class);
        SecurityCache cache  = getComponentManager().lookup(SecurityCache.class);
        SecurityCacheRulesInvalidator rulesInvalidator = getComponentManager().lookup(SecurityCacheRulesInvalidator.class);

        XWikiSecurityAccess edit = XWikiSecurityAccess.getDefaultAccess().clone();
        edit.allow(EDIT);

        SecurityAccess access = loader.load(userX, userX).getAccess();
        System.out.println("Level is " + access + ", expected " + edit);
        Assert.assertEquals(edit, access);

        SecurityEntry entry = cache.get(userX, userX);
        Assert.assertNotNull(entry);
        Assert.assertEquals(new TestSecurityAccessEntry(userX, userX, access), entry);

        entry = cache.get(userX);
        Assert.assertNotNull(entry);
        Assert.assertTrue(((SecurityRuleEntry)entry).getRules().size() > 0);

        entry = cache.get(userX.getParentSecurityReference());
        Assert.assertNotNull(entry);
        Assert.assertEquals(0, ((SecurityRuleEntry) entry).getRules().size());

        entry = cache.get(userX.getParentSecurityReference().getParentSecurityReference());
        Assert.assertNotNull(entry);
        Assert.assertEquals(0, ((SecurityRuleEntry) entry).getRules().size());

        ((EventListener) rulesInvalidator).onEvent(null, wikiDocument, null);

        entry = cache.get(userX, userX);
        Assert.assertNull(entry);

        entry = cache.get(userX);
        Assert.assertNull(entry);

        entry = cache.get(userX.getParentSecurityReference());
        Assert.assertNull(entry);

        entry = cache.get(userX.getParentSecurityReference().getParentSecurityReference());
        Assert.assertNull(entry);

        wikiDocument.denyGlobal(asList(COMMENT),
                                Collections.<String>emptyList(),
                                asList("wikiY:XWiki.XWikiAllGroup"));

        XWikiSecurityAccess editNoComment = edit.clone();
        editNoComment.deny(COMMENT);
        access = loader.load(userX, userX).getAccess();
        Assert.assertEquals(editNoComment, access);

        getMockery().checking(new Expectations()
        {{
                allowing(mockGroupService).getAllMembersNamesForGroup("xwiki:XWiki.GroupX", 100, 0, xwikiContext);
                will(Expectations.returnValue(asList("wikiY:XWiki.userX")));
            }});
        MockDocument group = MockDocument.newGroupDocument("XWiki.GroupX", new String[] {"wikiY:XWiki.userX" } );
        wiki.add(group);
        ((EventListener) rulesInvalidator).onEvent(null, group, null);

        entry = cache.get(userX);
        Assert.assertNull("Invalidating cache after group update",entry);

        entry = cache.get(userX, userX);
        Assert.assertNull("Invalidating cache after group update",entry);

        entry = cache.get(userX.getParentSecurityReference());
        Assert.assertNotNull("Invalidating cache after group update",entry);

        entry = cache.get(userX.getParentSecurityReference().getParentSecurityReference());
        Assert.assertNotNull("Invalidating cache after group update",entry);
    }
}

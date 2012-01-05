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
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AccessLevel;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;

import junit.framework.Assert;

import static java.util.Arrays.asList;
import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.DELETE;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.RuleState.ALLOW;


public class DefaultSecuritySettlerTest extends AbstractTestCase
{
    private List<GroupSecurityReference> asGroupList(DocumentReference[] groups) {
        List<GroupSecurityReference> results = new ArrayList<GroupSecurityReference>(groups.length);
        for (DocumentReference group : groups) {
            results.add(referenceFactory.newGroupReference(group));
        }
        return results;
    }

    @Test
    public void testSecuritySettler()
    {
        try {
            wiki.add(new MockDocument(docRefResolver.resolve("wikiY:SpaceX.DocY"), "wikiY:XWiki.UserY"));

            Right[] rights = { ADMIN, PROGRAM };
            DocumentReference[] groupDocs = { uResolver.resolve("GroupX", new WikiReference("wikiY")),
                                           uResolver.resolve("SpaceY.GroupY") };
            DocumentReference[] userDocs  = { uResolver.resolve("UserX", new WikiReference("wikiY")) };

            List<GroupSecurityReference> groups = asGroupList(groupDocs);

            SecurityRule o = new MockSecurityRule(new RightSet(asList(rights)),
                                                  ALLOW,
                                                  new HashSet<DocumentReference>(asList(userDocs)),
                                                  new HashSet<DocumentReference>(asList(groupDocs)));

            final SecurityReference doc = referenceFactory.newEntityReference(docRefResolver.resolve("wikiY:SpaceX.DocY"));
            UserSecurityReference userX = referenceFactory.newUserReference(uResolver.resolve("UserX", new WikiReference("wikiY")));
            UserSecurityReference userY = referenceFactory.newUserReference(uResolver.resolve("UserY", new WikiReference("wikiY")));

            Deque<SecurityRuleEntry> docLevel = new LinkedList<SecurityRuleEntry>();
            docLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference().getParentSecurityReference(),
                new LinkedList<SecurityRule>()));
            docLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference(), new LinkedList<SecurityRule>()));
            Collection<SecurityRule> pageRights = new LinkedList<SecurityRule>();
            pageRights.add(o);
            docLevel.push(new TestSecurityRuleEntry(doc, pageRights));

            AccessLevel level = settler.settle(userX, new LinkedList<GroupSecurityReference>(), docLevel).getAccessLevel();
            Assert.assertEquals(XWikiAccessLevel.getDefaultAccessLevel(), level);

            level = settler.settle(userY, groups, docLevel).getAccessLevel();
            XWikiAccessLevel delete = XWikiAccessLevel.getDefaultAccessLevel().clone();
            delete.allow(DELETE);
            Assert.assertEquals(delete,level);

            level = settler.settle(userY, new LinkedList<GroupSecurityReference>(), docLevel).getAccessLevel();
            Assert.assertEquals(delete,level);

            Deque<SecurityRuleEntry> spaceLevel = new LinkedList<SecurityRuleEntry>();
            spaceLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference().getParentSecurityReference(),
                new LinkedList<SecurityRule>()));
            Collection<SecurityRule> spaceRights = new LinkedList<SecurityRule>();
            spaceRights.add(o);
            spaceLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference(), spaceRights));
            spaceLevel.push(new TestSecurityRuleEntry(doc, new LinkedList<SecurityRule>()));

            level = settler.settle(userX, new LinkedList<GroupSecurityReference>(), spaceLevel).getAccessLevel();
            XWikiAccessLevel expected = XWikiAccessLevel.getDefaultAccessLevel().clone();
            expected.allow(ADMIN);
            expected.allow(DELETE);
            Assert.assertEquals(expected,level);

            level = settler.settle(userY, groups, spaceLevel).getAccessLevel();
            Assert.assertEquals(expected,level);

            level = settler.settle(userY, new LinkedList<GroupSecurityReference>(), spaceLevel).getAccessLevel();
            Assert.assertEquals(delete,level);

            Deque<SecurityRuleEntry> wikiLevel = new LinkedList<SecurityRuleEntry>();
            Collection<SecurityRule> wikiRights = new LinkedList<SecurityRule>();
            wikiRights.add(o);
            wikiLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference().getParentSecurityReference(),
                wikiRights));
            wikiLevel.push(new TestSecurityRuleEntry(doc.getParentSecurityReference(),
                new LinkedList<SecurityRule>()));
            wikiLevel.push(new TestSecurityRuleEntry(doc, new LinkedList<SecurityRule>()));

            level = settler.settle(userX, new LinkedList<GroupSecurityReference>(), wikiLevel).getAccessLevel();
            //expected.allow(PROGRAM);
            Assert.assertEquals(expected,level);

            level = settler.settle(userY, groups, wikiLevel).getAccessLevel();
            Assert.assertEquals(expected,level);

            level = settler.settle(userY, new LinkedList<GroupSecurityReference>(), wikiLevel).getAccessLevel();
            Assert.assertEquals(delete,level);
        } catch (Exception e) {
            LOG.error("Caught exception!", e);
            assert false;
        }
    }
}

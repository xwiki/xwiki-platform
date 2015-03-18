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

package org.xwiki.security.authorization.cache.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.security.internal.UserBridge;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.LogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSecurityCacheLoader}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultSecurityCacheLoader.class, DefaultSecurityReferenceFactory.class})
public class DefaultSecurityCacheLoaderTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public LogRule logCapture = new LogRule();

    private SecurityCacheLoader securityCacheLoader;

    private SecurityReferenceFactory securityReferenceFactory;

    @Before
    public void setUp() throws Exception
    {
        XWikiBridge bridge = mocker.registerMockComponent(XWikiBridge.class);
        when(bridge.getMainWikiReference()).thenReturn(new WikiReference("wiki"));
        securityReferenceFactory = mocker.getInstance(SecurityReferenceFactory.class);

        mocker.registerMockComponent(SecurityCache.class);
        mocker.registerMockComponent(SecurityCacheRulesInvalidator.class);
        mocker.registerMockComponent(SecurityEntryReader.class);
        mocker.registerMockComponent(UserBridge.class);
        mocker.registerMockComponent(AuthorizationSettler.class);
        securityCacheLoader = mocker.getInstance(SecurityCacheLoader.class);
    }

    @Test
    public void loadWithConflictingInsertionException() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "Users", "mflorea");
        UserSecurityReference user = securityReferenceFactory.newUserReference(userReference);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");
        SecurityReference entity = securityReferenceFactory.newEntityReference(documentReference);

        SecurityRuleEntry documentEntry = mock(SecurityRuleEntry.class, "document");
        when(documentEntry.getReference()).thenReturn(entity);
        when(documentEntry.isEmpty()).thenReturn(true);

        SecurityRuleEntry spaceEntry = mock(SecurityRuleEntry.class, "space");
        when(spaceEntry.getReference()).thenReturn(entity.getParentSecurityReference());
        when(spaceEntry.isEmpty()).thenReturn(true);

        SecurityRuleEntry wikiEntry = mock(SecurityRuleEntry.class, "wiki");
        when(wikiEntry.getReference()).thenReturn(entity.getParentSecurityReference().getParentSecurityReference());
        when(wikiEntry.isEmpty()).thenReturn(true);

        SecurityCache securityCache = mocker.getInstance(SecurityCache.class);
        when(securityCache.get(entity)).thenReturn(documentEntry);
        when(securityCache.get(entity.getParentSecurityReference())).thenReturn(spaceEntry);
        when(securityCache.get(entity.getParentSecurityReference().getParentSecurityReference())).thenReturn(wikiEntry);
        when(securityCache.getGroupsFor(user, null)).thenReturn(null);

        UserBridge userBridge = mocker.getInstance(UserBridge.class);
        DocumentReference groupReference = new DocumentReference("wiki", "Groups", "AllGroup");
        Set<GroupSecurityReference> groups =
            Collections.singleton(securityReferenceFactory.newGroupReference(groupReference));
        when(userBridge.getAllGroupsFor(user, userReference.getWikiReference())).thenReturn(groups);

        SecurityAccessEntry securityAccessEntry = mock(SecurityAccessEntry.class);

        AuthorizationSettler authorizationSettler = mocker.getInstance(AuthorizationSettler.class);
        Deque<SecurityRuleEntry> securityRuleEntries =
            new LinkedList<SecurityRuleEntry>(Arrays.asList(documentEntry, spaceEntry, wikiEntry));
        when(authorizationSettler.settle(user, groups, securityRuleEntries)).thenReturn(securityAccessEntry);

        doThrow(ConflictingInsertionException.class).when(securityCache).add(securityAccessEntry);
        doThrow(ConflictingInsertionException.class).when(securityCache).add(securityAccessEntry, null);

        try {
            securityCacheLoader.load(user, entity);
            Assert.fail();
        } catch (AuthorizationException e) {
            Assert.assertEquals("Failed to load the cache in 5 attempts.  Giving up. when checking  "
                + "access to [wiki:Space.Document] for user [wiki:Users.mflorea]", e.getMessage());
        }
    }
}

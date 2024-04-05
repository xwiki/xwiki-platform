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
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.security.internal.UserBridge;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSecurityCacheLoader}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultSecurityCacheLoader.class, DefaultSecurityReferenceFactory.class, EntityReferenceFactory.class})
public class DefaultSecurityCacheLoaderTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public AllLogRule logRule = new AllLogRule();

    private SecurityCacheLoader securityCacheLoader;

    private SecurityReferenceFactory securityReferenceFactory;

    @Before
    public void setUp() throws Exception
    {
        XWikiBridge bridge = mocker.registerMockComponent(XWikiBridge.class);
        when(bridge.getMainWikiReference()).thenReturn(new WikiReference("wiki"));
        when(bridge.toCompatibleEntityReference(any(EntityReference.class))).thenAnswer(new Answer<EntityReference>()
        {
            @Override
            public EntityReference answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(0);
            }
        });
        securityReferenceFactory = mocker.getInstance(SecurityReferenceFactory.class);

        mocker.registerComponent(org.xwiki.security.authorization.cache.SecurityCache.class, mock(SecurityCache.class));
        mocker.registerMockComponent(SecurityCacheRulesInvalidator.class);
        mocker.registerMockComponent(SecurityEntryReader.class);
        mocker.registerMockComponent(UserBridge.class);
        mocker.registerMockComponent(AuthorizationSettler.class);
        securityCacheLoader = mocker.getInstance(SecurityCacheLoader.class);
    }

    /**
     * Test that after fixing XWIKI-18508 the security cache loader ignores exceptions from the cache.
     */
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

        SecurityCache securityCache = mocker.getInstance(org.xwiki.security.authorization.cache.SecurityCache.class);
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

        assertEquals(securityAccessEntry, this.securityCacheLoader.load(user, entity));

        verify(securityCache).add(securityAccessEntry, null);
    }

    /**
     * Tests that the security cache loader doesn't load a rule with empty rules when loading the rules for the guest
     * user.
     */
    @Test
    public void loadForGuestLoadsRulesForMainWiki()
        throws ComponentLookupException, AuthorizationException, ParentEntryEvictedException,
        ConflictingInsertionException
    {
        UserSecurityReference user = this.securityReferenceFactory.newUserReference(null);
        SecurityReference entity = this.securityReferenceFactory.newEntityReference(new DocumentReference("wiki",
            "Space", "Document"));

        // Mock the security access entry returned by the authorization settler
        SecurityAccessEntry securityAccessEntry = mock(SecurityAccessEntry.class);
        AuthorizationSettler authorizationSettler = mocker.getInstance(AuthorizationSettler.class);
        when(authorizationSettler.settle(eq(user), any(), any())).thenReturn(securityAccessEntry);

        SecurityEntryReader securityEntryReader = mocker.getInstance(SecurityEntryReader.class);
        // Store SecurityRuleEntry instances for each level of the hierarchy
        SecurityRule documentRule = mock(SecurityRule.class);
        SecurityRuleEntry documentRuleEntry = mock();
        when(documentRuleEntry.getReference()).thenReturn(entity);
        when(documentRuleEntry.getRules()).thenReturn(List.of(documentRule));

        SecurityRule spaceRule = mock(SecurityRule.class);
        SecurityRuleEntry spaceRuleEntry = mock();
        when(spaceRuleEntry.getReference()).thenReturn(entity.getParentSecurityReference());
        when(spaceRuleEntry.getRules()).thenReturn(List.of(spaceRule));

        SecurityRule mainWikiRule = mock(SecurityRule.class);
        SecurityRuleEntry mainWikiRuleEntry = mock();
        when(mainWikiRuleEntry.getReference()).thenReturn(entity.getWikiReference());
        when(mainWikiRuleEntry.getRules()).thenReturn(List.of(mainWikiRule));

        Deque<SecurityRuleEntry> hierarchy = new LinkedList<>();
        hierarchy.push(mainWikiRuleEntry);
        hierarchy.push(spaceRuleEntry);
        hierarchy.push(documentRuleEntry);

        // Special entry for the guest user
        SecurityRuleEntry guestUserSecurityEntry = mock();
        when(guestUserSecurityEntry.getReference()).thenReturn(user);
        when(guestUserSecurityEntry.getRules()).thenReturn(List.of());

        when(securityEntryReader.read(any())).thenAnswer(invocation -> {
            SecurityReference reference = invocation.getArgument(0);
            SecurityRuleEntry result;
            if (reference.getOriginalReference() == null) {
                result = guestUserSecurityEntry;
            } else if (reference.getType() == EntityType.WIKI) {
                result = mainWikiRuleEntry;
            } else if (reference.getType() == EntityType.SPACE) {
                result = spaceRuleEntry;
            } else if (reference.getType() == EntityType.DOCUMENT) {
                result = documentRuleEntry;
            } else {
                throw new IllegalArgumentException("Unexpected reference type: " + reference.getType());
            }

            assertEquals(reference, result.getReference());
            return result;
        });

        assertEquals(securityAccessEntry, this.securityCacheLoader.load(user, entity));

        // Verify that rules were loaded in the correct order
        SecurityCache securityCache = mocker.getInstance(org.xwiki.security.authorization.cache.SecurityCache.class);
        InOrder inOrder = inOrder(securityCache);
        inOrder.verify(securityCache).get(entity.getWikiReference());
        inOrder.verify(securityCache).add(mainWikiRuleEntry);
        inOrder.verify(securityCache).get(entity.getParentSecurityReference());
        inOrder.verify(securityCache).add(spaceRuleEntry);
        inOrder.verify(securityCache).get(entity);
        inOrder.verify(securityCache).add(documentRuleEntry);
        inOrder.verify(securityCache).add(securityAccessEntry, null);
        inOrder.verifyNoMoreInteractions();
        verify(authorizationSettler).settle(user, Collections.emptySet(), hierarchy);
    }
}

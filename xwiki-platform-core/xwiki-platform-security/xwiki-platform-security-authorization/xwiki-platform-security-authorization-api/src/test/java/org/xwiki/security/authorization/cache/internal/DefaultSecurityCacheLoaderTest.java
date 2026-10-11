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

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
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
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.security.internal.UserBridge;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSecurityCacheLoader}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({DefaultSecurityCacheLoader.class, DefaultSecurityReferenceFactory.class, EntityReferenceFactory.class})
class DefaultSecurityCacheLoaderTest
{
    @MockComponent
    private XWikiBridge xwikiBridge;

    @MockComponent
    private SecurityCacheRulesInvalidator securityCacheRulesInvalidator;

    @MockComponent
    private SecurityEntryReader securityEntryReader;

    @MockComponent
    private UserBridge userBridge;

    @MockComponent
    private AuthorizationSettler authorizationSettler;

    @InjectMockComponents
    private DefaultSecurityCacheLoader securityCacheLoader;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private SecurityCache securityCache;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension();

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.securityCache = mock(SecurityCache.class);
        this.componentManager.registerComponent(
            org.xwiki.security.authorization.cache.SecurityCache.class, this.securityCache);

        when(this.xwikiBridge.getMainWikiReference()).thenReturn(new WikiReference("wiki"));
        when(this.xwikiBridge.toCompatibleEntityReference(any(EntityReference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test that after fixing XWIKI-18508 the security cache loader ignores exceptions from the cache.
     */
    @Test
    void loadWithConflictingInsertionException() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "Users", "mflorea");
        UserSecurityReference user = this.securityReferenceFactory.newUserReference(userReference);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");
        SecurityReference entity = this.securityReferenceFactory.newEntityReference(documentReference);

        SecurityRuleEntry documentEntry = mock(SecurityRuleEntry.class, "document");
        when(documentEntry.getReference()).thenReturn(entity);
        when(documentEntry.isEmpty()).thenReturn(true);

        SecurityRuleEntry spaceEntry = mock(SecurityRuleEntry.class, "space");
        when(spaceEntry.getReference()).thenReturn(entity.getParentSecurityReference());
        when(spaceEntry.isEmpty()).thenReturn(true);

        SecurityRuleEntry wikiEntry = mock(SecurityRuleEntry.class, "wiki");
        when(wikiEntry.getReference()).thenReturn(entity.getParentSecurityReference().getParentSecurityReference());
        when(wikiEntry.isEmpty()).thenReturn(true);

        long invalidationCounter = 42;

        when(this.securityCache.get(entity)).thenReturn(documentEntry);
        when(this.securityCache.get(entity.getParentSecurityReference())).thenReturn(spaceEntry);
        when(this.securityCache.get(entity.getParentSecurityReference().getParentSecurityReference()))
            .thenReturn(wikiEntry);
        when(this.securityCache.getGroupsFor(user, null)).thenReturn(null);
        when(this.securityCache.getInvalidationCounter()).thenReturn(invalidationCounter);

        DocumentReference groupReference = new DocumentReference("wiki", "Groups", "AllGroup");
        Set<GroupSecurityReference> groups =
            Collections.singleton(this.securityReferenceFactory.newGroupReference(groupReference));
        when(this.userBridge.getAllGroupsFor(user, userReference.getWikiReference())).thenReturn(groups);

        SecurityAccessEntry securityAccessEntry = mock(SecurityAccessEntry.class);

        Deque<SecurityRuleEntry> securityRuleEntries =
            new LinkedList<>(List.of(documentEntry, spaceEntry, wikiEntry));
        when(this.authorizationSettler.settle(user, groups, securityRuleEntries)).thenReturn(securityAccessEntry);

        doThrow(ConflictingInsertionException.class).when(this.securityCache)
            .add(eq(securityAccessEntry), anyLong());
        doThrow(ConflictingInsertionException.class).when(this.securityCache)
            .add(eq(securityAccessEntry), isNull(), anyLong());

        assertEquals(securityAccessEntry, this.securityCacheLoader.load(user, entity));

        verify(this.securityCache).add(securityAccessEntry, null, invalidationCounter);
    }

    /**
     * Tests that the security cache loader doesn't load a rule with empty rules when loading the rules for the guest
     * user.
     */
    @Test
    void loadForGuestLoadsRulesForMainWiki() throws Exception
    {
        UserSecurityReference user = this.securityReferenceFactory.newUserReference(null);
        SecurityReference entity = this.securityReferenceFactory.newEntityReference(new DocumentReference("wiki",
            "Space", "Document"));

        // Mock the security access entry returned by the authorization settler
        SecurityAccessEntry securityAccessEntry = mock(SecurityAccessEntry.class);
        when(this.authorizationSettler.settle(eq(user), any(), any())).thenReturn(securityAccessEntry);

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

        when(this.securityEntryReader.read(any())).thenAnswer(invocation -> {
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

        when(this.securityCache.getInvalidationCounter()).thenReturn(0L, 1L);

        assertEquals(securityAccessEntry, this.securityCacheLoader.load(user, entity));

        // Verify that rules were loaded in the correct order
        InOrder inOrder = inOrder(this.securityCache);
        inOrder.verify(this.securityCache, times(2)).getInvalidationCounter();
        inOrder.verify(this.securityCache).get(entity.getWikiReference());
        inOrder.verify(this.securityCache).add(mainWikiRuleEntry, 1);
        inOrder.verify(this.securityCache).get(entity.getParentSecurityReference());
        inOrder.verify(this.securityCache).add(spaceRuleEntry, 1);
        inOrder.verify(this.securityCache).get(entity);
        inOrder.verify(this.securityCache).add(documentRuleEntry, 1);
        inOrder.verify(this.securityCache).add(securityAccessEntry, null, 0);
        inOrder.verifyNoMoreInteractions();
        verify(this.authorizationSettler).settle(user, Set.of(), hierarchy);
    }
}

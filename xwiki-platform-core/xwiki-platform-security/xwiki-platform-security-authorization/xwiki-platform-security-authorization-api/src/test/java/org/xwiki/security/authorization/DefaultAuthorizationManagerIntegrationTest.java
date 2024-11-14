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

package org.xwiki.security.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.security.authorization.cache.internal.DefaultSecurityCache;
import org.xwiki.security.authorization.cache.internal.DefaultSecurityCacheLoader;
import org.xwiki.security.authorization.cache.internal.TestCache;
import org.xwiki.security.authorization.internal.AbstractSecurityRuleEntry;
import org.xwiki.security.authorization.internal.DefaultAuthorizationSettler;
import org.xwiki.security.authorization.testwikis.SecureTestEntity;
import org.xwiki.security.authorization.testwikis.TestAccessRule;
import org.xwiki.security.authorization.testwikis.TestDefinition;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestGroup;
import org.xwiki.security.authorization.testwikis.TestUserDocument;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.security.internal.UserBridge;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.COMMENT;
import static org.xwiki.security.authorization.Right.CREATE_WIKI;
import static org.xwiki.security.authorization.Right.CREATOR;
import static org.xwiki.security.authorization.Right.DELETE;
import static org.xwiki.security.authorization.Right.EDIT;
import static org.xwiki.security.authorization.Right.ILLEGAL;
import static org.xwiki.security.authorization.Right.LOGIN;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.REGISTER;
import static org.xwiki.security.authorization.Right.SCRIPT;
import static org.xwiki.security.authorization.Right.VIEW;
import static org.xwiki.security.authorization.Right.values;

/**
 * Test XWiki Authorization policy against the authentication module.
 * 
 * @since 5.0M2
 */
@ComponentTest
@ComponentList({DefaultSecurityCache.class, DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class, DefaultEntityReferenceProvider.class, DefaultModelConfiguration.class,
    AuthorizationManagerConfiguration.class, DefaultSecurityReferenceFactory.class, DefaultSecurityCacheLoader.class,
    DefaultAuthorizationSettler.class, DefaultAuthorizationManager.class, DefaultSymbolScheme.class,
    EntityReferenceFactory.class})
class DefaultAuthorizationManagerIntegrationTest extends AbstractAuthorizationTestCase
{
    @InjectMockComponents
    private DefaultAuthorizationManager authorizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    /** Mocked xWikiBridge */
    @MockComponent
    private XWikiBridge xWikiBridge;

    /** Mocked userBridge */
    @MockComponent
    private UserBridge userBridge;

    @MockComponent
    private ModelContext modelContext;

    /** Mocked securityEntryReader */
    @MockComponent
    private SecurityEntryReader securityEntryReader;

    /** Mocked securityCacheRulesInvalidator */
    @MockComponent
    private SecurityCacheRulesInvalidator securityCacheRulesInvalidator;

    /** Mocked cache */
    private TestCache<Object> cache;

    /** Factory for security reference */
    private SecurityReferenceFactory securityReferenceFactory;

    private DocumentReference currentEntityReference = new DocumentReference("xwiki", "Page", "Space");

    @BeforeComponent
    void initializeMocks() throws Exception
    {
        cache = new TestCache<Object>();
        final CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        when(xWikiBridge.toCompatibleEntityReference(any(EntityReference.class)))
            .thenAnswer(new Answer<EntityReference>()
            {
                @Override
                public EntityReference answer(InvocationOnMock invocation) throws Throwable
                {
                    return invocation.getArgument(0);
                }
            });

        when(xWikiBridge.getMainWikiReference()).thenReturn(new WikiReference("xwiki"));
    }

    @BeforeEach
    void setUp() throws Exception
    {
        securityReferenceFactory = componentManager.getInstance(SecurityReferenceFactory.class);
        authorizationManager = componentManager.getInstance(AuthorizationManager.class);
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentEntityReference);
    }

    /**
     * Assert an allowed access for a given right for a given user on a given entity.
     * 
     * @param message the assert message
     * @param right the right to check for allowance
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessTrue(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        assertTrue(authorizationManager.hasAccess(right, userReference, entityReference), message);
    }

    /**
     * Assert a denied access for a given right for a given user on a given entity.
     * 
     * @param message the assert message
     * @param right the right to check for denial
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessFalse(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        assertFalse(authorizationManager.hasAccess(right, userReference, entityReference), message);
    }

    /**
     * Check all rights for access by given user on a given entity.
     * 
     * @param allowedRights the set of rights that should be allowed.
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccess(RightSet allowedRights, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        for (Right right : values()) {
            if (allowedRights != null && allowedRights.contains(right)) {
                if (!authorizationManager.hasAccess(right, userReference, entityReference)) {
                    fail(String.format("[%s] should have [%s] right on [%s].", getUserReadableName(userReference),
                        right, getEntityReadableName(entityReference)));
                }
            } else {
                if (authorizationManager.hasAccess(right, userReference, entityReference)) {
                    fail(String.format("[%s] should not have [%s] right on [%s].", getUserReadableName(userReference),
                        right, getEntityReadableName(entityReference)));
                }
            }
        }
    }

    private boolean compareReferenceNullSafe(EntityReference entity1, EntityReference entity2)
    {
        return entity1 == entity2 || (entity1 != null && entity1.equals(entity2));
    }

    private SecurityRule mockSecurityRule(SecurityReference reference, final Right right, RuleState state,
        final DocumentReference userOrGroup, boolean isUser)
    {
        SecurityRule mockedRule = mock(SecurityRule.class,
            String.format("Rule for [%s] %s [%s] right to %s [%s]", reference.toString(),
                state == RuleState.ALLOW ? "allowing" : "denying", right.getName(), (isUser) ? "user" : "group",
                userOrGroup.toString()));
        when(mockedRule.getState()).thenReturn(state);
        when(mockedRule.match(any(Right.class))).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                Right reqRight = (Right) invocationOnMock.getArguments()[0];
                return (reqRight == right);
            }
        });
        if (isUser) {
            when(mockedRule.match(any(UserSecurityReference.class))).thenAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    SecurityReference reference = (UserSecurityReference) invocationOnMock.getArguments()[0];
                    return compareReferenceNullSafe(userOrGroup, reference.getOriginalDocumentReference());
                }
            });
            // when(mockedRule.match(any(GroupSecurityReference.class))).thenReturn(false);
        } else {
            when(mockedRule.match(any(GroupSecurityReference.class))).thenAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    SecurityReference reference = (GroupSecurityReference) invocationOnMock.getArguments()[0];
                    return compareReferenceNullSafe(userOrGroup, reference.getOriginalDocumentReference());
                }
            });
            // when(mockedRule.match(any(UserSecurityReference.class))).thenReturn(false);
        }
        return mockedRule;
    }

    @Override
    public TestDefinition initialiseWikiMock(String filename) throws Exception
    {
        super.initialiseWikiMock(filename);

        when(xWikiBridge.getMainWikiReference()).thenReturn(testDefinition.getMainWiki().getWikiReference());
        when(xWikiBridge.isWikiReadOnly()).thenReturn(false);

        when(userBridge.getAllGroupsFor(any(UserSecurityReference.class), any(WikiReference.class)))
            .thenAnswer(new Answer<Collection<GroupSecurityReference>>()
            {
                @Override
                public Collection<GroupSecurityReference> answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    UserSecurityReference userReference = (UserSecurityReference) invocationOnMock.getArguments()[0];
                    WikiReference wikiReference = (WikiReference) invocationOnMock.getArguments()[1];

                    if (userReference.getOriginalReference() == null) {
                        // Public users (not logged in) may not appears in any group
                        return Collections.emptyList();
                    }

                    TestWiki wiki = testDefinition.getWiki(userReference.getOriginalReference().getWikiReference());
                    if (wiki == null) {
                        throw new AuthorizationException(
                            String.format("Failed to get groups for user or group [%s] in wiki [%s]. Unknown wiki.",
                                userReference, wikiReference),
                            null);
                    }

                    TestUserDocument user = wiki.getUser(userReference.getName());
                    if (user == null) {
                        return Collections.emptyList();
                    }

                    Collection<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>();
                    for (TestGroup group : user.getGroups()) {
                        // Ensure we return only group of the requested wiki
                        if (group.getGroupReference().getWikiReference().equals(wikiReference)) {
                            groups.add(securityReferenceFactory.newGroupReference(group.getGroupReference()));
                        }
                    }
                    return groups;
                }
            });

        when(securityEntryReader.read(any(SecurityReference.class))).thenAnswer(new Answer<SecurityRuleEntry>()
        {
            @Override
            public SecurityRuleEntry answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                final SecurityReference reference = (SecurityReference) invocationOnMock.getArguments()[0];

                TestEntity entity = testDefinition.searchEntity(reference);

                Collection<TestAccessRule> rules = (entity != null && entity instanceof SecureTestEntity)
                    ? ((SecureTestEntity) entity).getAccessRules() : Collections.<TestAccessRule>emptyList();

                final Collection<SecurityRule> mockedRules = new ArrayList<SecurityRule>();
                for (final TestAccessRule rule : rules) {
                    mockedRules.add(
                        mockSecurityRule(reference, rule.getRight(), rule.getState(), rule.getUser(), rule.isUser()));
                }

                if (entity instanceof TestWiki) {
                    TestWiki wiki = (TestWiki) entity;
                    if (wiki.getOwner() != null) {
                        mockedRules
                            .add(mockSecurityRule(reference, Right.ADMIN, RuleState.ALLOW, wiki.getOwner(), true));
                    }
                }

                if (entity instanceof TestDocument) {
                    TestDocument document = (TestDocument) entity;
                    if (document.getCreator() != null) {
                        mockedRules.add(
                            mockSecurityRule(reference, Right.CREATOR, RuleState.ALLOW, document.getCreator(), true));
                    }
                }

                // This mock should be barely comparable for #testLoadUserInAnotherWikiAfterUserDoc()
                /*
                 * SecurityRuleEntry accessEntry = mock(SecurityRuleEntry.class,
                 * String.format("Rule entry for %s containing %d rules", reference.toString(), mockedRules.size()));
                 * when(accessEntry.getReference()).thenReturn(reference);
                 * when(accessEntry.isEmpty()).thenReturn(mockedRules.isEmpty());
                 * when(accessEntry.getRules()).thenReturn(mockedRules);
                 */

                return new AbstractSecurityRuleEntry()
                {
                    @Override
                    public Collection<SecurityRule> getRules()
                    {
                        return mockedRules;
                    }

                    @Override
                    public SecurityReference getReference()
                    {
                        return reference;
                    }

                    @Override
                    public String toString()
                    {
                        return String.format("Rule entry for %s containing %d rules", reference.toString(),
                            mockedRules.size());
                    }

                    @Override
                    public boolean equals(Object object)
                    {
                        if (object == this) {
                            return true;
                        }
                        if (!(object instanceof SecurityRuleEntry)) {
                            return false;
                        }
                        SecurityRuleEntry other = (SecurityRuleEntry) object;

                        return compareReferenceNullSafe(other.getReference(), reference)
                            && other.getRules().size() == mockedRules.size();
                    }
                };
            }
        });

        return testDefinition;
    }

    @Test
    void defaultAccessOnEmptyWikis() throws Exception
    {
        initialiseWikiMock("emptyWikis");

        // Public access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN), null,
            getXDoc("an empty main wiki", "anySpace"));

        // SuperAdmin access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, SCRIPT, COMMENT, DELETE, CREATOR, REGISTER, LOGIN, ADMIN, PROGRAM,
            CREATE_WIKI, ILLEGAL), SUPERADMIN, getXDoc("an empty main wiki", "anySpace"));

        // Any Global user without access rules on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            getXUser("a global user without any access rule"), getXDoc("main wiki", "anySpace"));

        // Any Local user on main wiki
        assertAccess(null, getUser("a local user", "any SubWiki"), getXDoc("main wiki", "anySpace"));

        // Public access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN), null,
            getDoc("an empty sub wiki", "anySpace", "any SubWiki"));

        // SuperAdmin access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, SCRIPT, COMMENT, DELETE, CREATOR, REGISTER, LOGIN, ADMIN, PROGRAM,
            CREATE_WIKI, ILLEGAL), SUPERADMIN, getDoc("an empty sub wiki", "anySpace", "any SubWiki"));

        // Any Global user without access rules on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            getXUser("a global user without any access rule"), getDoc("a subwiki", "anySpace", "any SubWiki"));

        // Any Local user on another subwiki
        assertAccess(null, getUser("a local user", "any SubWiki"),
            getDoc("an another subwiki", "anySpace", "any Other SubWiki"));
    }

    @Test
    void verifyNeedsAuthentication() throws Exception
    {
        initialiseWikiMock("emptyWikis");
        when(this.xWikiBridge.needsAuthentication(VIEW)).thenReturn(true);
        DocumentReference documentReference = getXDoc("an empty main wiki", "anySpace");
        assertFalse(authorizationManager.hasAccess(VIEW, null, documentReference));
        verify(this.modelContext).setCurrentEntityReference(documentReference);
        verify(this.modelContext).setCurrentEntityReference(currentEntityReference);

        when(this.xWikiBridge.needsAuthentication(VIEW)).thenReturn(false);
        assertTrue(authorizationManager.hasAccess(VIEW, null, documentReference));
    }

    @Test
    void inheritancePolicyForFullFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyFullFarmAccess");

        // Main wiki allowing all access to A
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docAllowA", "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docDenyA", "any space"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA", "any space", "wikiNoRules"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA", "any space", "wikiDenyA"));
    }

    @Test
    void inheritancePolicyForGlobalFullWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForGlobalFullWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_SPACE_RIGHTS, getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(ALL_SPACE_RIGHTS, getXUser("userA"), getXDoc("docDenyA", "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getXDoc("docAllowA", "any space"));

        assertAccess(null, getXUser("userA"), getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS, getXUser("userA"), getDoc("docDenyA", "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getDoc("docAllowA", "any space", "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowA", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docDenyA", "any space", "wikiAllowA"));
    }

    @Test
    void inheritancePolicyForLocalWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForLocalWikiAccess");

        // Main wiki denying all access to A
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"),
            getDoc("any document", "any space", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"),
            getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"),
            getDoc("docAllowA", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"),
            getDoc("docDenyA", "any space", "wikiAllowA"));

        assertAccess(null, getUser("userA", "wikiDenyA"), getDoc("any document", "any space", "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS, getUser("userA", "wikiDenyA"),
            getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS, getUser("userA", "wikiDenyA"), getDoc("docDenyA", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getUser("userA", "wikiDenyA"),
            getDoc("any document", "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(null, getUser("userA", "wikiDenyA"), getDoc("docDenyA", "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getUser("userA", "wikiDenyA"), getDoc("docAllowA", "any space", "wikiDenyA"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userA", "wikiAllowNoAdminA"),
            getDoc("any document", "any space", "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"),
            getDoc("any document", "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userA", "wikiAllowNoAdminA"),
            getDoc("docAllowA", "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"),
            getDoc("docDenyA", "any space", "wikiAllowNoAdminA"));
    }

    @Test
    void inheritancePolicyForNoAdminFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminFarmAccess");

        // Main wiki allowing all but admin access to A
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"), getXDoc("docAllowA", "spaceDenyA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyA", "any space"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"),
            getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowA", "spaceDenyA", "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA", "any space", "wikiNoRules"));

        assertAccess(null, getXUser("userA"), getDoc("any document", "any space", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(null, getXUser("userA"), getDoc("docDenyA", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getDoc("docAllowA", "any space", "wikiDenyA"));
    }

    @Test
    void inheritancePolicyForNoAdminWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(null, getXUser("userA"), getXDoc("docDenyA", "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getXDoc("docAllowA", "any space"));

        assertAccess(null, getXUser("userA"), getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(null, getXUser("userA"), getDoc("docDenyA", "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS, getXUser("userA"), getDoc("docAllowA", "any space", "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"),
            getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowA", "spaceDenyA", "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA", "any space", "wikiAllowA"));
    }

    @Test
    void tieResolutionPolicy() throws Exception
    {
        initialiseWikiMock("tieResolutionPolicy");

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserAllowDeny"),
            getWiki("wikiUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserDenyAllow"),
            getWiki("wikiUserDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupAllowDeny"),
            getWiki("wikiGroupAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupDenyAllow"),
            getWiki("wikiGroupDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserGroupAllowDeny"),
            getWiki("wikiUserGroupAllowDeny"));
        assertAccess(null, getUser("userA", "wikiUserGroupDenyAllow"), getWiki("wikiUserGroupDenyAllow"));
        assertAccess(null, getUser("userA", "wikiGroupUserAllowDeny"), getWiki("wikiGroupUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupUserDenyAllow"),
            getWiki("wikiGroupUserDenyAllow"));

        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiUserAllowDenyNoAdmin"),
            getWiki("wikiUserAllowDenyNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiUserDenyAllowNoAdmin"),
            getWiki("wikiUserDenyAllowNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiGroupAllowDenyNoAdmin"),
            getWiki("wikiGroupAllowDenyNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiGroupDenyAllowNoAdmin"),
            getWiki("wikiGroupDenyAllowNoAdmin"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userA", "wikiUserGroupAllowDenyNoAdmin"),
            getWiki("wikiUserGroupAllowDenyNoAdmin"));
        assertAccess(null, getUser("userA", "wikiUserGroupDenyAllowNoAdmin"), getWiki("wikiUserGroupDenyAllowNoAdmin"));
        assertAccess(null, getUser("userA", "wikiGroupUserAllowDenyNoAdmin"), getWiki("wikiGroupUserAllowDenyNoAdmin"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userA", "wikiGroupUserDenyAllowNoAdmin"),
            getWiki("wikiGroupUserDenyAllowNoAdmin"));
    }

    @Test
    void documentCreator() throws Exception
    {
        initialiseWikiMock("documentCreator");

        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, LOGIN, REGISTER), getXUser("userA"),
            getXDoc("userAdoc", "space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, LOGIN, REGISTER), getXUser("userA"),
            getXDoc("userBdoc", "space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, LOGIN, REGISTER), getXUser("userB"),
            getXDoc("userAdoc", "space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, LOGIN, REGISTER), getXUser("userB"),
            getXDoc("userBdoc", "space"));
    }

    @Test
    void ownerAccess() throws Exception
    {
        initialiseWikiMock("ownerAccess");

        // Owner of main wiki has admin access to all wikis whatever the wiki access is
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiLocalUserA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("any document", "any space", "wikiDenyLocalUserA"));

        // Local owner has admin access whatever the wiki access is
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiLocalUserA"),
            getDoc("any document", "any space", "wikiLocalUserA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiDenyLocalUserA"),
            getDoc("any document", "any space", "wikiDenyLocalUserA"));
    }

    @Test
    void groupAccess() throws Exception
    {
        initialiseWikiMock("groupAccess");

        assertAccess(DEFAULT_DOCUMENT_RIGHTS, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getXDoc("docAllowGroupA", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getXDoc("docAllowGroupB", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupB", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getXDoc("docDenyGroupAAllowUserA", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getXDoc("docDenyGroupBAllowUserA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"),
            getXDoc("docDenyGroupBAllowGroupA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"),
            getXDoc("docDenyGroupAAllowGroupB", "any space"));

        assertAccess(DEFAULT_DOCUMENT_RIGHTS, getUser("userB", "subwiki"),
            getDoc("any document", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userB", "subwiki"),
            getDoc("docAllowGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getUser("userB", "subwiki"),
            getDoc("docAllowGroupB", "any space", "subwiki"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userB", "subwiki"),
            getDoc("docAllowGroupC", "any space", "subwiki"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowGlobalGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowGlobalGroupB", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowGroupB", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userA"),
            getDoc("docAllowGroupC", "any space", "subwiki"));

        /** Test XWIKI-13574 **/
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userD"),
            getXDoc("docAllowGroupB", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userC"),
            getXDoc("docAllowGroupB", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI, getXUser("userB"),
            getXDoc("docAllowGroupB", "any space"));

        assertAccess(new RightSet(List.of(LOGIN, REGISTER, VIEW, DELETE)), getXUser("userA") ,
            getXDoc("docDeleteAllowA", "any space"));

        /* In the situation where a global user (userA) is in a global group (groupA) that is in another global group
         (groupB) that is in a local group (groupC) that is in a subwiki (subwiki), ensure that removing the local
         group (groupC) from the cache removes the access entries for the global user (userA) in the subwiki as
         otherwise permissions based on groupC are not recalculated when a member of groupC is removed. This
         verifies that memberships between groups are correctly mirrored in the shadow entries for the subwiki. */
        SecurityCache securityCache = this.componentManager.getInstance(SecurityCache.class);
        UserSecurityReference subwikiGroupC =
            this.securityReferenceFactory.newUserReference(getUser("groupC", "subwiki"));
        UserSecurityReference userA = this.securityReferenceFactory.newUserReference(getXUser("userA"));
        SecurityReference docC =
            this.securityReferenceFactory.newEntityReference(getDoc("docAllowGroupC", "any space", "subwiki"));

        assertNotNull(securityCache.get(subwikiGroupC));
        assertNotNull(securityCache.get(userA, docC));
        securityCache.remove(subwikiGroupC);
        assertNull(securityCache.get(userA, docC));
        assertNotNull(securityCache.get(userA));
    }

    @Test
    void checkAccess() throws Exception
    {
        initialiseWikiMock("emptyWikis");

        authorizationManager.checkAccess(VIEW, null, getXDoc("an empty main wiki", "anySpace"));

        AccessDeniedException accessDeniedException = assertThrows(AccessDeniedException.class, () -> {
            authorizationManager.checkAccess(ADMIN, null, getXDoc("an empty main wiki", "anySpace"));
        });
        assertNull(accessDeniedException.getCause(),
            "checkAccess should throw access denied exception without any cause when access is denied");
    }

    @Test
    void loadUserAfterUserDoc() throws Exception
    {
        initialiseWikiMock("loadUserAfterUserDoc");

        // Check access to the userA document => introduce the userA as a simple document into the cache
        authorizationManager.checkAccess(VIEW, getXUser("userB"), getXUser("userA"));

        // Check access of userA to the userB document => it prove userA is in groupA and has access
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getXUser("userB"));

        SecurityCache securityCache = componentManager.getInstance(SecurityCache.class);

        // a userA entry is in the cache
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), notNullValue());

        assertThat("UserA should be considered as a user by the cache, else the group cache will not be used",
            securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))).getReference(),
            instanceOf(UserSecurityReference.class));

        // remove group A from the cache => this should also remove all members of groupA
        securityCache.remove(securityReferenceFactory.newUserReference(getXUser("groupA")));

        // check that userA was seen as a member of groupA by the cache => implies document userA was considered as a
        // user (and not simply a document) after the second check.
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), nullValue());
    }

    // Test XWIKI-12016
    @Test
    void groupCacheLoadingUserAfterGroupDoc() throws Exception
    {
        initialiseWikiMock("loadUserAfterUserDoc");

        // Check access to the groupA document => introduce the groupA as a simple document into the cache
        authorizationManager.checkAccess(VIEW, getXUser("userB"), getXUser("groupA"));

        // Check access of userA to the userA document => it transform group document into a true group
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getXUser("userA"));

        // Check access of userA to the userB document => it prove userA is in groupA (from cache) and has access
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getXUser("userB"));

        SecurityCache securityCache = componentManager.getInstance(SecurityCache.class);

        // a userA entry is in the cache
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), notNullValue());

        assertThat("UserA should be considered as a user by the cache, else the group cache will not be used",
            securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))).getReference(),
            instanceOf(UserSecurityReference.class));

        // a userA entry in the cache is know as a user, else the group cache is inactive for her
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))).getReference(),
            instanceOf(UserSecurityReference.class));

        // remove group A from the cache => this should also remove all members of groupA
        securityCache.remove(securityReferenceFactory.newUserReference(getXUser("groupA")));

        // check that userA was seen as a member of groupA by the cache => implies document userA was considered as a
        // user (and not simply a document) after the second check.
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), nullValue());
    }

    @Test
    void loadUserInAnotherWikiAfterUserDoc() throws Exception
    {
        initialiseWikiMock("loadUserAfterUserDoc");

        // Check access to the userA document => introduce the userA as a simple document into the cache
        authorizationManager.checkAccess(VIEW, getXUser("userB"), getXUser("userA"));

        // Check access of userA to the userB document => it prove userA is in groupA and has access
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getDoc("any document", "any space", "subwiki"));

        // Check access of userA to the userB document => it prove userA is in groupA and has access
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getXUser("userB"));

        SecurityCache securityCache = componentManager.getInstance(SecurityCache.class);

        // a userA entry is in the cache
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), notNullValue());

        // a userA accessEntry is still in the cache for the document in the subwiki
        assertThat(
            securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA")),
                securityReferenceFactory.newEntityReference(getDoc("any document", "any space", "subwiki"))),
            notNullValue());

        // remove group A from the cache => this should also remove all members of groupA
        securityCache.remove(securityReferenceFactory.newUserReference(getXUser("groupA")));

        // check that userA was seen as a member of groupA by the cache => implies document userA is now a user
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), nullValue());

        // check that the shadow userA has also been affected
        assertThat(
            securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA")),
                securityReferenceFactory.newEntityReference(getDoc("any document", "any space", "subwiki"))),
            nullValue());
    }

    private final class CustomRightDescription implements RightDescription
    {
        @Override
        public String getName()
        {
            return "foo";
        }

        @Override
        public RuleState getDefaultState()
        {
            return RuleState.DENY;
        }

        @Override
        public RuleState getTieResolutionPolicy()
        {
            return RuleState.DENY;
        }

        @Override
        public boolean getInheritanceOverridePolicy()
        {
            return true;
        }

        @Override
        public Set<Right> getImpliedRights()
        {
            return Collections.singleton(EDIT);
        }

        @Override
        public Set<EntityType> getTargetedEntityType()
        {
            return Collections.singleton(EntityType.DOCUMENT);
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }
    }

    @Test
    void register() throws AuthorizationException
    {
        Right registeredRight = null;
        try {
            registeredRight =
                this.authorizationManager.register(new CustomRightDescription(), Collections.singleton(SCRIPT));
            assertEquals("foo", registeredRight.getName());
            assertEquals(RuleState.DENY, registeredRight.getDefaultState());
            assertEquals(Collections.singleton(EDIT), registeredRight.getImpliedRights());
            assertEquals(RuleState.DENY, registeredRight.getTieResolutionPolicy());
            assertEquals(Collections.singleton(EntityType.DOCUMENT), registeredRight.getTargetedEntityType());
            assertTrue(registeredRight.getInheritanceOverridePolicy());
            assertFalse(registeredRight.isReadOnly());
            assertTrue(SCRIPT.getImpliedRights().contains(registeredRight));
            assertTrue(PROGRAM.getImpliedRights().contains(registeredRight));

            Right otherRegisteredRight = this.authorizationManager.register(new CustomRightDescription());
            assertSame(registeredRight, otherRegisteredRight);
            assertSame(registeredRight, Right.toRight("foo"));
        } finally {
            if (registeredRight != null) {
                this.authorizationManager.unregister(registeredRight);
            }
        }
    }

    @Test
    void unregister() throws AuthorizationException
    {
        AuthorizationException authorizationException = assertThrows(AuthorizationException.class, () -> {
            this.authorizationManager.unregister(SCRIPT);
        });
        assertEquals("Attempt to unregister the static right [script]", authorizationException.getMessage());

        Right registeredRight =
            this.authorizationManager.register(new CustomRightDescription(), Collections.singleton(SCRIPT));
        this.authorizationManager.unregister(registeredRight);
        assertFalse(SCRIPT.getImpliedRights().contains(registeredRight));
        assertFalse(PROGRAM.getImpliedRights().contains(registeredRight));

        assertEquals(ILLEGAL, Right.toRight("foo"));
    }
}

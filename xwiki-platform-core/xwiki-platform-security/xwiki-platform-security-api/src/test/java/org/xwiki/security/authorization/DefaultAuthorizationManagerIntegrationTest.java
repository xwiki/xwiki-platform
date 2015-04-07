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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
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
import org.xwiki.test.LogRule;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import static org.xwiki.security.authorization.Right.VIEW;
import static org.xwiki.security.authorization.Right.values;

/**
 * Test XWiki Authorization policy against the authentication module.
 * @since 5.0M2
 */
@ComponentList({DefaultSecurityCache.class, DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class, DefaultEntityReferenceValueProvider.class,
    DefaultModelConfiguration.class, AuthorizationManagerConfiguration.class,
    DefaultSecurityReferenceFactory.class, DefaultSecurityCacheLoader.class,
    DefaultAuthorizationSettler.class, DefaultAuthorizationManager.class})
public class DefaultAuthorizationManagerIntegrationTest extends AbstractAuthorizationTestCase
{
    private AuthorizationManager authorizationManager;

    @Rule
    public final LogRule logCapture = new LogRule();

    /** Mocked xWikiBridge */
    private XWikiBridge xWikiBridge;

    /** Mocked userBridge */
    private UserBridge userBridge;

    /** Mocked securityEntryReader */
    private SecurityEntryReader securityEntryReader;

    /** Mocked securityCacheRulesInvalidator */
    private SecurityCacheRulesInvalidator securityCacheRulesInvalidator;

    /** Mocked cache */
    private TestCache<Object> cache;

    /** Factory for security reference */
    private SecurityReferenceFactory securityReferenceFactory;

    @BeforeComponent
    public void initializeMocks() throws Exception {
        cache = new TestCache<Object>();
        final CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        xWikiBridge = componentManager.registerMockComponent(XWikiBridge.class);
        userBridge = componentManager.registerMockComponent(UserBridge.class);
        securityEntryReader = componentManager.registerMockComponent(SecurityEntryReader.class);
        securityCacheRulesInvalidator = componentManager.registerMockComponent(SecurityCacheRulesInvalidator.class);
    }

    @Before
    public void setUp() throws Exception
    {
        securityReferenceFactory = componentManager.getInstance(SecurityReferenceFactory.class);
        authorizationManager = componentManager.getInstance(AuthorizationManager.class);
    }


    /**
     * Assert an allowed access for a given right for a given user on a given entity.
     * @param message the assert message
     * @param right the right to check for allowance
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessTrue(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        Assert.assertTrue(message,
            authorizationManager.hasAccess(right, userReference, entityReference));
    }

    /**
     * Assert a denied access for a given right for a given user on a given entity.
     * @param message the assert message
     * @param right the right to check for denial
     * @param userReference the reference of the user to test.
     * @param entityReference the reference of the entity to test.
     * @throws Exception on error.
     */
    protected void assertAccessFalse(String message, Right right, DocumentReference userReference,
        EntityReference entityReference) throws Exception
    {
        Assert.assertFalse(message, authorizationManager.hasAccess(right, userReference, entityReference));
    }

    /**
     * Check all rights for access by given user on a given entity.
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
                    fail(String.format("[%s] should have [%s] right on [%s].",
                        getUserReadableName(userReference), right, getEntityReadableName(entityReference)));
                }
            } else {
                if (authorizationManager.hasAccess(right, userReference, entityReference)) {
                    fail(String.format("[%s] should not have [%s] right on [%s].",
                        getUserReadableName(userReference), right, getEntityReadableName(entityReference)));
                }
            }
        }
    }

    private boolean compareReferenceNullSafe(EntityReference entity1, EntityReference entity2) {
        return entity1 == entity2 || (entity1 != null && entity1.equals(entity2));
    }

    private SecurityRule mockSecurityRule(SecurityReference reference, final Right right, RuleState state,
        final DocumentReference userOrGroup, boolean isUser) {
        SecurityRule mockedRule = mock(SecurityRule.class,
            String.format("Rule for [%s] %s [%s] right to %s [%s]", reference.toString(),
                state == RuleState.ALLOW ? "allowing" : "denying",
                right.getName(), (isUser) ? "user" : "group", userOrGroup.toString()));
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
                    SecurityReference reference =
                        (UserSecurityReference) invocationOnMock.getArguments()[0];
                    return compareReferenceNullSafe(userOrGroup,
                        reference.getOriginalDocumentReference());
                }
            });
            //when(mockedRule.match(any(GroupSecurityReference.class))).thenReturn(false);
        } else {
            when(mockedRule.match(any(GroupSecurityReference.class))).thenAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    SecurityReference reference =
                        (GroupSecurityReference) invocationOnMock.getArguments()[0];
                    return compareReferenceNullSafe(userOrGroup,
                        reference.getOriginalDocumentReference());
                }
            });
            //when(mockedRule.match(any(UserSecurityReference.class))).thenReturn(false);
        }
        return mockedRule;
    }

    @Override
    public TestDefinition initialiseWikiMock(String filename) throws Exception
    {
        super.initialiseWikiMock(filename);

        when(xWikiBridge.getMainWikiReference()).thenReturn(testDefinition.getMainWiki().getWikiReference());
        when(xWikiBridge.isWikiReadOnly()).thenReturn(false);

        when(userBridge.getAllGroupsFor(any(UserSecurityReference.class), any(WikiReference.class))).thenAnswer(
            new Answer<Collection<GroupSecurityReference>>()
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
                                userReference, wikiReference), null);
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
            }
        );

        when(securityEntryReader.read(any(SecurityReference.class))).thenAnswer(
            new Answer<SecurityRuleEntry>()
            {
                @Override
                public SecurityRuleEntry answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    final SecurityReference reference = (SecurityReference) invocationOnMock.getArguments()[0];

                    TestEntity entity = testDefinition.searchEntity(reference);

                    Collection<TestAccessRule> rules = (entity != null && entity instanceof SecureTestEntity)
                        ? ((SecureTestEntity) entity).getAccessRules()
                        : Collections.<TestAccessRule>emptyList();

                    final Collection<SecurityRule> mockedRules = new ArrayList<SecurityRule>();
                    for (final TestAccessRule rule : rules) {
                        mockedRules.add(mockSecurityRule(reference, rule.getRight(), rule.getState(), rule.getUser(),
                            rule.isUser()));
                    }

                    if (entity instanceof TestWiki) {
                        TestWiki wiki = (TestWiki) entity;
                        if (wiki.getOwner() != null) {
                            mockedRules.add(mockSecurityRule(reference, Right.ADMIN, RuleState.ALLOW, wiki.getOwner(),
                                true));
                        }
                    }

                    if (entity instanceof TestDocument) {
                        TestDocument document = (TestDocument) entity;
                        if (document.getCreator() != null) {
                            mockedRules.add(mockSecurityRule(reference, Right.CREATOR, RuleState.ALLOW,
                                document.getCreator(), true));
                        }
                    }

                    // This mock should be barely comparable for #testLoadUserInAnotherWikiAfterUserDoc()
                    /*
                    SecurityRuleEntry accessEntry = mock(SecurityRuleEntry.class,
                        String.format("Rule entry for %s containing %d rules", reference.toString(), mockedRules.size()));
                    when(accessEntry.getReference()).thenReturn(reference);
                    when(accessEntry.isEmpty()).thenReturn(mockedRules.isEmpty());
                    when(accessEntry.getRules()).thenReturn(mockedRules);
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

                        public String toString()
                        {
                            return String.format("Rule entry for %s containing %d rules", reference.toString(), mockedRules.size());
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
            }
        );

        return testDefinition;
    }

    @Test
    public void testDefaultAccessOnEmptyWikis() throws Exception
    {
        initialiseWikiMock("emptyWikis");

        // Public access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            null, getXDoc("an empty main wiki", "anySpace"));

        // SuperAdmin access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, REGISTER, LOGIN, ADMIN, PROGRAM, CREATE_WIKI, ILLEGAL),
            SUPERADMIN, getXDoc("an empty main wiki", "anySpace"));

        // Any Global user without access rules on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            getXUser("a global user without any access rule"), getXDoc("main wiki", "anySpace"));

        // Any Local user on main wiki
        assertAccess(null,
            getUser("a local user", "any SubWiki"), getXDoc("main wiki", "anySpace"));

        // Public access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            null, getDoc("an empty sub wiki", "anySpace", "any SubWiki"));

        // SuperAdmin access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, REGISTER, LOGIN, ADMIN, PROGRAM, CREATE_WIKI, ILLEGAL),
            SUPERADMIN, getDoc("an empty sub wiki", "anySpace", "any SubWiki"));

        // Any Global user without access rules on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            getXUser("a global user without any access rule"), getDoc("a subwiki", "anySpace", "any SubWiki"));

        // Any Local user on another subwiki
        assertAccess(null,
            getUser("a local user", "any SubWiki"), getDoc("an another subwiki", "anySpace", "any Other SubWiki"));

    }

    @Test
    public void testInheritancePolicyForFullFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyFullFarmAccess");

        // Main wiki allowing all access to A
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docAllowA",    "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docDenyA",     "any space"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space",  "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiNoRules"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));
    }

    @Test
    public void testInheritancePolicyForGlobalFullWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForGlobalFullWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null,                         getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getXDoc("docDenyA",     "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getXUser("userA"), getXDoc("docAllowA",    "any space"));

        assertAccess(null,                         getXUser("userA"), getDoc("any document", "any space",   "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));
    }

    @Test
    public void testInheritancePolicyForLocalWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForLocalWikiAccess");

        // Main wiki denying all access to A
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiAllowA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));

        assertAccess(null,                         getUser("userA", "wikiDenyA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS,             getUser("userA", "wikiDenyA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS,             getUser("userA", "wikiDenyA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getUser("userA", "wikiDenyA"), getDoc("any document", "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(null,                         getUser("userA", "wikiDenyA"), getDoc("docDenyA",     "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getUser("userA", "wikiDenyA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userA", "wikiAllowNoAdminA"), getDoc("any document", "any space",  "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"), getDoc("any document", "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userA", "wikiAllowNoAdminA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"), getDoc("docDenyA",     "any space",  "wikiAllowNoAdminA"));
    }

    @Test
    public void testInheritancePolicyForNoAdminFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminFarmAccess");

        // Main wiki allowing all but admin access to A
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("docAllowA",    "spaceDenyA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyA",     "any space"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("any document", "any space",  "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiNoRules"));

        assertAccess(null,                          getXUser("userA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(null,                          getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));
    }

    @Test
    public void testInheritancePolicyForNoAdminWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null,                          getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(null,                          getXUser("userA"), getXDoc("docDenyA",     "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getXDoc("docAllowA",    "any space"));

        assertAccess(null,                          getXUser("userA"), getDoc("any document", "any space",   "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(null,                          getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));
    }

    @Test
    public void testTieResolutionPolicy() throws Exception
    {
        initialiseWikiMock("tieResolutionPolicy");

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserAllowDeny"), getWiki("wikiUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserDenyAllow"), getWiki("wikiUserDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupAllowDeny"), getWiki("wikiGroupAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupDenyAllow"), getWiki("wikiGroupDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiUserGroupAllowDeny"), getWiki("wikiUserGroupAllowDeny"));
        assertAccess(null,                         getUser("userA", "wikiUserGroupDenyAllow"), getWiki("wikiUserGroupDenyAllow"));
        assertAccess(null,                         getUser("userA", "wikiGroupUserAllowDeny"), getWiki("wikiGroupUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiGroupUserDenyAllow"), getWiki("wikiGroupUserDenyAllow"));

        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiUserAllowDenyNoAdmin"), getWiki("wikiUserAllowDenyNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiUserDenyAllowNoAdmin"), getWiki("wikiUserDenyAllowNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiGroupAllowDenyNoAdmin"), getWiki("wikiGroupAllowDenyNoAdmin"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiGroupDenyAllowNoAdmin"), getWiki("wikiGroupDenyAllowNoAdmin"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userA", "wikiUserGroupAllowDenyNoAdmin"), getWiki("wikiUserGroupAllowDenyNoAdmin"));
        assertAccess(null,                          getUser("userA", "wikiUserGroupDenyAllowNoAdmin"), getWiki("wikiUserGroupDenyAllowNoAdmin"));
        assertAccess(null,                          getUser("userA", "wikiGroupUserAllowDenyNoAdmin"), getWiki("wikiGroupUserAllowDenyNoAdmin"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userA", "wikiGroupUserDenyAllowNoAdmin"), getWiki("wikiGroupUserDenyAllowNoAdmin"));
    }

    @Test
    public void testDocumentCreator() throws Exception
    {
        initialiseWikiMock("documentCreator");

        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, LOGIN, REGISTER), getXUser("userA"), getXDoc("userAdoc","space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, LOGIN, REGISTER), getXUser("userA"), getXDoc("userBdoc","space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, LOGIN, REGISTER), getXUser("userB"), getXDoc("userAdoc","space"));
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, CREATOR, LOGIN, REGISTER), getXUser("userB"), getXDoc("userBdoc","space"));
    }

    @Test
    public void testOwnerAccess() throws Exception
    {
        initialiseWikiMock("ownerAccess");

        // Owner of main wiki has admin access to all wikis whatever the wiki access is
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("any document", "any space", "wikiNoRules"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("any document", "any space", "wikiLocalUserA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getXUser("userA"), getDoc("any document", "any space", "wikiDenyLocalUserA"));

        // Local owner has admin access whatever the wiki access is
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiLocalUserA"), getDoc("any document", "any space", "wikiLocalUserA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING_AND_CREATE_WIKI, getUser("userA", "wikiDenyLocalUserA"), getDoc("any document", "any space", "wikiDenyLocalUserA"));
    }

    @Test
    public void testGroupAccess() throws Exception
    {
        initialiseWikiMock("groupAccess");

        assertAccess(DEFAULT_DOCUMENT_RIGHTS,       getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("docAllowGroupA", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("docAllowGroupeB", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupB", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("docDenyGroupAAllowUserA", "any space"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getXDoc("docDenyGroupBAllowUserA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupBAllowGroupA", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyGroupAAllowGroupB", "any space"));

        assertAccess(DEFAULT_DOCUMENT_RIGHTS,       getUser("userB","subwiki"), getDoc("any document", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userB","subwiki"), getDoc("docAllowGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getUser("userB","subwiki"), getDoc("docAllowGroupB", "any space", "subwiki"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userB","subwiki"), getDoc("docAllowGroupC", "any space", "subwiki"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowGlobalGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowGlobalGroupB", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowGroupA", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowGroupB", "any space", "subwiki"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN_AND_CREATE_WIKI,       getXUser("userA"), getDoc("docAllowGroupC", "any space", "subwiki"));
    }

    @Test
    public void testCheckAccess() throws Exception
    {
        initialiseWikiMock("emptyWikis");

        authorizationManager.checkAccess(VIEW, null, getXDoc("an empty main wiki", "anySpace"));

        try {
            authorizationManager.checkAccess(ADMIN, null, getXDoc("an empty main wiki", "anySpace"));
            fail("checkAccess should throw access denied exception when access is denied.");
        } catch (AccessDeniedException e) {
            assertThat("checkAccess should throw access denied exception without any cause when access is denied",
                e.getCause(), nullValue());
        }
    }

    @Test
    public void testLoadUserAfterUserDoc() throws Exception
    {
        initialiseWikiMock("loadUserAfterUserDoc");

        // Check access to the userA document => introduce the userA as a simple document into the cache
        authorizationManager.checkAccess(VIEW, getXUser("userB"), getXUser("userA"));

        // Check access of userA to the userB document => it prove userA is in groupA and has access
        authorizationManager.checkAccess(VIEW, getXUser("userA"), getXUser("userB"));

        SecurityCache securityCache = componentManager.getInstance(SecurityCache.class);

        // a userA entry is in the cache
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), notNullValue());

        // remove group A from the cache => this should also remove all members of groupA
        securityCache.remove(securityReferenceFactory.newUserReference(getXUser("groupA")));

        // check that userA was seen as a member of groupA by the cache => implies document userA was considered as a 
        // user (and not simply a document) after the second check.
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), nullValue());
    }

    @Test
    public void testLoadUserInAnotherWikiAfterUserDoc() throws Exception
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
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA")),
            securityReferenceFactory.newEntityReference(getDoc("any document", "any space", "subwiki"))),
            notNullValue());

        // remove group A from the cache => this should also remove all members of groupA
        securityCache.remove(securityReferenceFactory.newUserReference(getXUser("groupA")));

        // check that userA was seen as a member of groupA by the cache => implies document userA is now a user
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA"))), nullValue());

        // check that the shadow userA has also been affected
        assertThat(securityCache.get(securityReferenceFactory.newUserReference(getXUser("userA")),
            securityReferenceFactory.newEntityReference(getDoc("any document", "any space", "subwiki"))),
            nullValue());
    }
}

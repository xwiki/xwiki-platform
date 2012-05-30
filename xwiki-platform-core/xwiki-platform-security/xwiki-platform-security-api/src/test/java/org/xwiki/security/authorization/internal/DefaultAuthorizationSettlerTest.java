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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AbstractAuthorizationTestCase;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.internal.EntityBridge;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.annotation.MockingRequirement;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.DENY;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Default authorization settler unit tests.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class DefaultAuthorizationSettlerTest extends AbstractAuthorizationTestCase
{
    @MockingRequirement
    private DefaultAuthorizationSettler authorizationSettler;

    private XWikiSecurityAccess defaultAccess;
    private XWikiSecurityAccess denyAllAccess;
    private XWikiSecurityAccess initialImportAccess;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        final EntityBridge entityBridge= getComponentManager().getInstance(EntityBridge.class);
        final XWikiBridge xwikiBridge = getComponentManager().getInstance(XWikiBridge.class);

        getMockery().checking(new Expectations() {{
            allowing (entityBridge).isDocumentCreator(with(creatorRef),
                with(any(SecurityReference.class))); will(returnValue(true));
            allowing (entityBridge).isDocumentCreator(with(any(UserSecurityReference.class)), 
                with(any(SecurityReference.class))); will(returnValue(false));

            allowing (xwikiBridge).isWikiOwner(with(ownerRef),
                with(any(WikiReference.class))); will(returnValue(true));
            allowing (xwikiBridge).isWikiOwner(with(any(UserSecurityReference.class)),
                with(any(WikiReference.class))); will(returnValue(false));
        }});

        defaultAccess = XWikiSecurityAccess.getDefaultAccess();
        denyAllAccess = new XWikiSecurityAccess();
        for (Right right : Right.values()) {
            denyAllAccess.deny(right);
        }
        initialImportAccess = new XWikiSecurityAccess();
        for (Right right : Right.values()) {
            if (Right.ADMIN.getImpliedRights().contains(right)) {
                initialImportAccess.allow(right);
            } else {
                initialImportAccess.set(right, right.getDefaultState());
            }
        }
        initialImportAccess.allow(Right.ADMIN);
    }

    private Deque<SecurityRuleEntry> getMockedSecurityRuleEntries(String name, final SecurityReference reference,
                                                                  final List<List<SecurityRule>> ruleEntries)
    {
        return getMockedSecurityRuleEntries(name, reference, ruleEntries, true);
    }

    private Deque<SecurityRuleEntry> getMockedSecurityRuleEntries(final String name, final SecurityReference reference,
                                                                  final List<List<SecurityRule>> ruleEntries,
                                                                  final boolean addMainWikiDefaultRules)
    {
        final Deque<SecurityReference> refs = reference.getReversedSecurityReferenceChain();
        final Deque<SecurityRuleEntry> entries = new ArrayDeque<SecurityRuleEntry>(refs.size());

        for (SecurityReference ref : refs) {
            entries.push(getMockery().mock(SecurityRuleEntry.class, name + ref));
        }

        getMockery().checking(new Expectations() {{
            int i = 0;
            SecurityReference ref = reference;
            for (SecurityRuleEntry entry : entries) {
                List<SecurityRule> rules;

                if (i < ruleEntries.size()) {
                    rules = ruleEntries.get(i);
                } else {
                    rules = Collections.emptyList();
                }

                if (ref.getParentSecurityReference() == null && rules.size() == 0 && addMainWikiDefaultRules) {
                    // Add some rule at the main wiki level to avoid that the wiki is determined to be in its initial
                    // import state.  By setting DENY on all, this rule will not have any other side effects than
                    // indicating that the initial import have been made.
                    rules = Arrays.asList(getMockedSecurityRule(name + " non-initial import state indicator security rule",
                                                                Arrays.asList(defaultUserRef),
                                                                Collections.<GroupSecurityReference>emptyList(),
                                                                allTestRights, DENY));
                    
                }

                allowing (entry).getReference(); will(returnValue(ref));
                allowing (entry).getRules(); will(returnValue(rules));

                ref = ref.getParentSecurityReference();
                i++;
            }

        }});

        return entries;
    }

    private SecurityRule getMockedSecurityRule(String name, Iterable<UserSecurityReference> users,
        Iterable<GroupSecurityReference> groups, Iterable<Right> rights, final RuleState state) {
        final SecurityRule rule = getMockery().mock(SecurityRule.class, name);

        final List<Matcher<? extends UserSecurityReference>> userMatchers
            = new ArrayList<Matcher<? extends UserSecurityReference>>();
        final List<Matcher<? extends GroupSecurityReference>> groupMatchers
            = new ArrayList<Matcher<? extends GroupSecurityReference>>();
        final List<Matcher<? extends Right>> rightMatchers = new ArrayList<Matcher<? extends Right>>();

        for (UserSecurityReference user : users) {
            userMatchers.add(is(user));
        }
        for (GroupSecurityReference group : groups) {
            groupMatchers.add(is(group));
        }
        for (Right right : rights) {
            rightMatchers.add(is(right));
        }

        getMockery().checking(new Expectations() {{
            allowing (rule).match(with(anyOf(userMatchers))); will(returnValue(true));
            allowing (rule).match(with(anyOf(groupMatchers))); will(returnValue(true));
            allowing (rule).match(with(anyOf(rightMatchers))); will(returnValue(true));
            allowing (rule).match(with(any(UserSecurityReference.class))); will(returnValue(false));
            allowing (rule).match(with(any(GroupSecurityReference.class))); will(returnValue(false));
            allowing (rule).match(with(any(Right.class))); will(returnValue(false));
            allowing (rule).getState(); will(returnValue(state));
        }});

        return rule;
    }


    private void assertAccess(String message, UserSecurityReference user, SecurityReference entity,
        SecurityAccess access, SecurityAccessEntry actual)
    {
        assertThat(message + " - user", actual.getUserReference(), equalTo(user));
        assertThat(message + " - entity", actual.getReference(), equalTo(entity));
        for (Right right : Right.values()) {
            if (access.get(right) != UNDETERMINED) {
                assertThat(message + " - Right(" + right.getName() + ")",
                    actual.getAccess().get(right), equalTo(access.get(right)));
            }
        }
    }

    @Test
    public void testSettleNoRulesOnMainWiki() throws Exception
    {
        Deque<SecurityRuleEntry> emptyXdocRules
            = getMockedSecurityRuleEntries("emptyXdocRules", xdocRef, Collections.<List<SecurityRule>>emptyList(), false);

        assertAccess("When no rules are defined, return initial import access for main wiki user on main wiki doc",
            xuserRef, xdocRef, initialImportAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(), emptyXdocRules));

        assertAccess("When no rules are defined, deny all access for local wiki user on main wiki doc",
            userRef, xdocRef, denyAllAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                emptyXdocRules));

        assertAccess("When no rules are defined, deny all access for another wiki user on main wiki doc",
            anotherWikiUserRef, xdocRef, denyAllAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                emptyXdocRules));
    }

    @Test
    public void testSettleNoRulesOnLocalWiki() throws Exception
    {
        Deque<SecurityRuleEntry> emptydocRules
            = getMockedSecurityRuleEntries("emptydocRules", docRef, Collections.<List<SecurityRule>>emptyList());

        assertAccess("When no rules are defined, return default access for local wiki user on local wiki doc",
            userRef, docRef, defaultAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(), emptydocRules));

        assertAccess("When no rules are defined, deny all access for main wiki user on local wiki doc",
            xuserRef, docRef, denyAllAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(), emptydocRules));

        assertAccess("When no rules are defined, deny all access for another wiki user on local wiki doc",
            anotherWikiUserRef, docRef, denyAllAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                emptydocRules));
    }

    @Test
    public void testSettleInheritancePolicy() throws Exception
    {
        SecurityRule allowAllTestRightsRulesToXuser = getMockedSecurityRule("allowAllTestRightsRulesToXuser",
            Arrays.asList(xuserRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsRulesToXuser = getMockedSecurityRule("denyAllTestRightsRulesToXuser",
            Arrays.asList(xuserRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, DENY);
        SecurityRule allowAllTestRightsRulesToUser = getMockedSecurityRule("allowAllTestRightsRulesToUser",
            Arrays.asList(userRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsRulesToUser = getMockedSecurityRule("denyAllTestRightsRulesToUser",
            Arrays.asList(userRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, DENY);
        SecurityRule allowAllTestRightsRulesToAnotherWikiUser = getMockedSecurityRule("allowAllTestRightsRulesToAnotherWikiUser",
            Arrays.asList(anotherWikiUserRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsRulesToAnotherWikiUser = getMockedSecurityRule(
            "denyAllTestRightsRulesToAnotherWikiUser",
            Arrays.asList(anotherWikiUserRef), Collections.<GroupSecurityReference>emptyList(), allTestRights, DENY);

        Deque<SecurityRuleEntry> allowThenDenyRulesForXdocSpace
            = getMockedSecurityRuleEntries("allowThenDenyRulesForXdocSpace", xdocRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser,
                allowAllTestRightsRulesToAnotherWikiUser),
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser,
                denyAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> denyThenAllowRulesForXdocSpace
            = getMockedSecurityRuleEntries("denyThenAllowRulesForXdocSpace", xdocRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser),
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> allowThenDenyRulesForDocSpace
            = getMockedSecurityRuleEntries("allowThenDenyRulesForDocSpace", docRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser,
                allowAllTestRightsRulesToAnotherWikiUser),
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> denyThenAllowRulesForDocSpace
            = getMockedSecurityRuleEntries("denyThenAllowRulesForDocSpace", docRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser,
                denyAllTestRightsRulesToAnotherWikiUser),
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> allowThenDenyRulesForXDocWiki
            = getMockedSecurityRuleEntries("allowThenDenyRulesForXDocWiki", xdocRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser,
                allowAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser,
                denyAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> denyThenAllowRulesForXdocWiki
            = getMockedSecurityRuleEntries("denyThenAllowRulesForXdocWiki", xdocRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> allowThenDenyRulesForDocWiki
            = getMockedSecurityRuleEntries("allowThenDenyRulesForDocWiki", docRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> denyThenAllowRulesForDocWiki
            = getMockedSecurityRuleEntries("denyThenAllowRulesForDocWiki", docRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> allowThenDenyRulesForDocXWiki
            = getMockedSecurityRuleEntries("allowThenDenyRulesForDocXWiki", docRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser)));

        Deque<SecurityRuleEntry> denyThenAllowRulesForDocXWiki
            = getMockedSecurityRuleEntries("denyThenAllowRulesForDocXWiki", docRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsRulesToXuser, denyAllTestRightsRulesToUser, denyAllTestRightsRulesToAnotherWikiUser),
            Collections.<SecurityRule>emptyList(),
            Collections.<SecurityRule>emptyList(),
            Arrays.asList(allowAllTestRightsRulesToXuser, allowAllTestRightsRulesToUser, allowAllTestRightsRulesToAnotherWikiUser)));

        XWikiSecurityAccess allowDenyAccess = new XWikiSecurityAccess();
        for (Right right : allTestRights) {
            allowDenyAccess.allow(right);
        }

        XWikiSecurityAccess denyAllowAccess = new XWikiSecurityAccess();
        for (Right right : allTestRights) {
            denyAllowAccess.set(right, right.getInheritanceOverridePolicy() ? DENY : ALLOW);
        }

        assertAccess("When allowed right on doc are denied on space from main wiki for main wiki user, use inheritance policy",
            xuserRef, xdocRef, allowDenyAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForXdocSpace));

        assertAccess("When denied right on doc are allowed on space from main wiki for main wiki user, use inheritance policy",
            xuserRef, xdocRef, denyAllowAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForXdocSpace));

        assertAccess("When allowed right on doc are denied on space from local wiki for main wiki user, use inheritance policy",
            xuserRef, docRef, allowDenyAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocSpace));

        assertAccess("When denied right on doc are allowed on space from local wiki for main wiki user, use inheritance policy",
            xuserRef, docRef, denyAllowAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocSpace));

        assertAccess("When allowed right on doc are denied on space from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, allowDenyAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocSpace));

        assertAccess("When denied right on doc are allowed on space from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, denyAllowAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocSpace));

        assertAccess("When allowed right on doc are denied on space from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, allowDenyAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocSpace));

        assertAccess("When denied right on doc are allowed on space from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, denyAllowAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocSpace));

        //
        assertAccess("When allowed right on doc are denied on wiki from main wiki for main wiki user, use inheritance policy",
            xuserRef, xdocRef, allowDenyAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForXDocWiki));

        assertAccess("When denied right on doc are allowed on wiki from main wiki for main wiki user, use inheritance policy",
            xuserRef, xdocRef, denyAllowAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForXdocWiki));

        assertAccess("When allowed right on doc are denied on wiki from local wiki for main wiki user, use inheritance policy",
            xuserRef, docRef, allowDenyAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocWiki));

        assertAccess("When denied right on doc are allowed on wiki from local wiki for main wiki user, use inheritance policy",
            xuserRef, docRef, denyAllowAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocWiki));

        assertAccess("When allowed right on doc are denied on wiki from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, allowDenyAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocWiki));

        assertAccess("When denied right on doc are allowed on wiki from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, denyAllowAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocWiki));

        assertAccess("When allowed right on doc are denied on wiki from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, allowDenyAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocWiki));

        assertAccess("When denied right on doc are allowed on wiki from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, denyAllowAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocWiki));

        //
        assertAccess("When allowed right on doc are denied on main wiki from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, allowDenyAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocXWiki));

        assertAccess("When denied right on doc are allowed on main wiki from local wiki for local wiki user, use inheritance policy",
            userRef, docRef, denyAllowAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocXWiki));

        assertAccess("When allowed right on doc are denied on main wiki from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, allowDenyAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                allowThenDenyRulesForDocXWiki));

        assertAccess("When denied right on doc are allowed on main wiki from local wiki for another wiki user, use inheritance policy",
            anotherWikiUserRef, docRef, denyAllowAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                denyThenAllowRulesForDocXWiki));

    }

    @Test
    public void testSettleTieResolutionPolicy() throws Exception
    {
        SecurityRule allowAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("allowAllTestRightsUserAndAnotherGroup",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("denyAllTestRightsUserAndAnotherGroup",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), allTestRights, DENY);
        SecurityRule denyAllTestRightsAnotherUserAndGroup = getMockedSecurityRule("denyAllTestRightsAnotherUserAndGroup",
            Arrays.asList(anotherUserRef), Arrays.asList(groupRef), allTestRights, DENY);


        Deque<SecurityRuleEntry> conflictAllowDenySameTarget
            = getMockedSecurityRuleEntries("conflictAllowDenySameTarget", docRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsUserAndAnotherGroup, denyAllTestRightsUserAndAnotherGroup)));
        Deque<SecurityRuleEntry> conflictDenyAllowSameTarget
            = getMockedSecurityRuleEntries("conflictDenyAllowSameTarget", docRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsUserAndAnotherGroup, allowAllTestRightsUserAndAnotherGroup)));
        Deque<SecurityRuleEntry> conflictAllowDenyUserGroup
            = getMockedSecurityRuleEntries("conflictAllowDenyUserGroup", docRef, Arrays.asList(
            Arrays.asList(allowAllTestRightsUserAndAnotherGroup, denyAllTestRightsAnotherUserAndGroup)));
        Deque<SecurityRuleEntry> conflictDenyAllowUserGroup
            = getMockedSecurityRuleEntries("conflictDenyAllowUserGroup", docRef, Arrays.asList(
            Arrays.asList(denyAllTestRightsAnotherUserAndGroup, allowAllTestRightsUserAndAnotherGroup)));

        XWikiSecurityAccess allowAccess = defaultAccess.clone();
        for (Right right : allTestRights) {
            allowAccess.allow(right);
        }

        XWikiSecurityAccess denyAccess = defaultAccess.clone();
        for (Right right : allTestRights) {
            denyAccess.deny(right);
        }

        XWikiSecurityAccess tieAccess = defaultAccess.clone();
        for (Right right : allTestRights) {
            tieAccess.set(right, right.getTieResolutionPolicy());
        }

        assertAccess("When allowed right for user is denied for same user in another rule, use tie resolution policy",
            userRef, docRef, tieAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenySameTarget));

        assertAccess("When denied right for user is allowed for same user in another rule, use tie resolution policy",
            userRef, docRef, tieAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictDenyAllowSameTarget));

        assertAccess("When allowed right for group is denied for same group in another rule, use tie resolution policy",
            anotherUserRef, docRef, tieAccess,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenySameTarget));

        assertAccess("When denied right for group is allowed for same group in another rule, use tie resolution policy",
            anotherUserRef, docRef, tieAccess,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictDenyAllowSameTarget));

        assertAccess("When allowed right for user is denied for its group in another rule, allow it.",
            userRef, docRef, allowAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenyUserGroup));

        assertAccess("When allowed right for group is denied for one of its user in another rule, deny it.",
            anotherUserRef, docRef, denyAccess,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenyUserGroup));

        assertAccess("When denied right for group is allowed for one of its user in another rule, allow it.",
            userRef, docRef, allowAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictDenyAllowUserGroup));

        assertAccess("When denied right for user is allowed for its group in another rule, deny it.",
            anotherUserRef, docRef, denyAccess,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictDenyAllowUserGroup));

    }

    @Test
    public void testSettleOneAllowImpliesDenyForAllOthers() throws Exception
    {
        XWikiSecurityAccess defaultAllowRight0 = defaultAccess.clone();
        XWikiSecurityAccess defaultDenyRight0 = defaultAccess.clone();
        XWikiSecurityAccess defaultAllowRight6 = defaultAccess.clone();
        defaultAllowRight0.allow(allTestRights.get(0));
        defaultDenyRight0.deny(allTestRights.get(0));
        defaultAllowRight6.allow(allTestRights.get(6));

        assertAccess("When an allow rules is found, deny any not matching user",
            anotherUserRef, docRef, defaultDenyRight0,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef),
                getMockedSecurityRuleEntries("onlyRight0",
                    docRef,
                    Arrays.asList(Arrays.asList(getMockedSecurityRule(
                        "onlyRight0",
                        Arrays.asList(userRef),
                        Collections.<GroupSecurityReference>emptyList(),
                        Arrays.asList(allTestRights.get(0)),
                        RuleState.ALLOW))))));


        assertAccess("When an allow rules is found, do not deny a user matching in another rule",
            anotherUserRef, docRef, defaultAllowRight6,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6ToAnotherUser",
                    docRef,
                    Arrays.asList(Arrays.asList(
                        getMockedSecurityRule(
                            "allowRight6ToUser",
                            Arrays.asList(userRef),
                            Collections.<GroupSecurityReference>emptyList(),
                            Arrays.asList(allTestRights.get(6)),
                            RuleState.ALLOW),
                        getMockedSecurityRule(
                            "allowRight6ToAnotherUser",
                            Arrays.asList(anotherUserRef),
                            Collections.<GroupSecurityReference>emptyList(),
                            Arrays.asList(allTestRights.get(6)),
                            RuleState.ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching in another rule",
            anotherUserRef, docRef, defaultAllowRight6,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6ToAnotherGroup",
                    docRef,
                    Arrays.asList(Arrays.asList(
                        getMockedSecurityRule(
                            "allowRight6ToUserAndGroup",
                            Arrays.asList(userRef),
                            Arrays.asList(groupRef),
                            Arrays.asList(allTestRights.get(6)),
                            RuleState.ALLOW),
                        getMockedSecurityRule(
                            "allowRight6ToAnotherGroup",
                            Collections.<UserSecurityReference>emptyList(),
                            Arrays.asList(anotherGroupRef),
                            Arrays.asList(allTestRights.get(6)),
                            RuleState.ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching the rule",
            userRef, docRef, defaultAllowRight6,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("onlyRight6",
                    docRef,
                    Arrays.asList(Arrays.asList(getMockedSecurityRule(
                        "onlyRight6",
                        Arrays.asList(userRef),
                        Collections.<GroupSecurityReference>emptyList(),
                        Arrays.asList(allTestRights.get(6)),
                        RuleState.ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching a group in the rule",
            anotherUserRef, docRef, defaultAllowRight6,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6",
                    docRef,
                    Arrays.asList(Arrays.asList(
                        getMockedSecurityRule(
                            "allowRight6ToUserAndAnotherGroup",
                            Arrays.asList(userRef),
                            Arrays.asList(anotherGroupRef),
                            Arrays.asList(allTestRights.get(6)),
                            RuleState.ALLOW))))));

    }

    @Test
    public void testSettleRightWithImpliedRights() throws Exception
    {
        SecurityRule allowImpliedADT = getMockedSecurityRule("allowImpliedADT",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), Arrays.asList(impliedTestRightsADT), ALLOW);
        SecurityRule denyImpliedADT = getMockedSecurityRule("denyImpliedADT",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), Arrays.asList(impliedTestRightsADT), DENY);

        SecurityRule allowImpliedDAF = getMockedSecurityRule("allowImpliedDAF",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), Arrays.asList(impliedTestRightsDAF), ALLOW);
        SecurityRule denyImpliedDAF = getMockedSecurityRule("denyImpliedDAF",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), Arrays.asList(impliedTestRightsDAF), DENY);


        XWikiSecurityAccess allowAccessADT = defaultAccess.clone();
        allowAccessADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            allowAccessADT.allow(right);
        }
        XWikiSecurityAccess tieADT = defaultAccess.clone();
        tieADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            tieADT.set(right, right.getTieResolutionPolicy());
        }
        XWikiSecurityAccess allowAccessDAF = defaultAccess.clone();
        allowAccessDAF.set(impliedTestRightsDAF, ALLOW);
        for (Right right : allTestRights) {
            allowAccessDAF.allow(right);
        }
        XWikiSecurityAccess denyADTAccess = defaultAccess.clone();
        denyADTAccess.deny(impliedTestRightsADT);
        XWikiSecurityAccess denyDAFAccess = defaultAccess.clone();
        denyDAFAccess.deny(impliedTestRightsDAF);

        XWikiSecurityAccess denyAccessADT = defaultAccess.clone();
        denyAccessADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            denyAccessADT.deny(right);
        }
        XWikiSecurityAccess denyAccessDAF = defaultAccess.clone();
        denyAccessDAF.set(impliedTestRightsDAF, ALLOW);
        for (Right right : allTestRights) {
            denyAccessDAF.deny(right);
        }



        assertAccess("When a right implying others rights is allowed, imply those rights (ADT)",
            userRef, docRef, allowAccessADT,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("allowAccessADT", docRef,
                    Arrays.asList(Arrays.asList(allowImpliedADT)))));

        assertAccess("When a right implying others rights is allowed, imply those rights (DAF)",
            userRef, docRef, allowAccessDAF,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("allowAccessDAF", docRef,
                    Arrays.asList(Arrays.asList(allowImpliedDAF)))));

        assertAccess("When a right implying others rights is denied, do not denied implied rights (ADT)",
            userRef, docRef, denyADTAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("denyAccessADT", docRef,
                    Arrays.asList(Arrays.asList(denyImpliedADT)))));

        assertAccess("When a right implying others rights is denied, do not denied implied rights (ADT)",
            userRef, docRef, denyDAFAccess,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("denyAccessDAF", docRef,
                    Arrays.asList(Arrays.asList(denyImpliedDAF)))));

        SecurityRule allowAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("allowAllTestRightsUserAndAnotherGroup",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("denyAllTestRightsUserAndAnotherGroup",
            Arrays.asList(userRef), Arrays.asList(anotherGroupRef), allTestRights, DENY);
        SecurityRule denyAllTestRightsAnotherUserAndGroup = getMockedSecurityRule("denyAllTestRightsAnotherUserAndGroup",
            Arrays.asList(anotherUserRef), Arrays.asList(groupRef), allTestRights, DENY);


        Deque<SecurityRuleEntry> conflictAllowDenySameTargetADT
            = getMockedSecurityRuleEntries("conflictAllowDenySameTargetADT", docRef, Arrays.asList(
            Arrays.asList(allowImpliedADT, denyAllTestRightsUserAndAnotherGroup)));
        Deque<SecurityRuleEntry> conflictAllowDenySameTargetDAF
            = getMockedSecurityRuleEntries("conflictAllowDenySameTargetDAF", docRef, Arrays.asList(
            Arrays.asList(allowImpliedDAF, denyAllTestRightsUserAndAnotherGroup)));

        Deque<SecurityRuleEntry> conflictAllowDenyUserGroupADT
            = getMockedSecurityRuleEntries("conflictAllowDenyUserGroupADT", docRef, Arrays.asList(
            Arrays.asList(allowImpliedADT, denyAllTestRightsAnotherUserAndGroup)));
        Deque<SecurityRuleEntry> conflictAllowDenyUserGroupDAF
            = getMockedSecurityRuleEntries("conflictAllowDenyUserGroupDAF", docRef, Arrays.asList(
            Arrays.asList(allowImpliedDAF, denyAllTestRightsAnotherUserAndGroup)));

        assertAccess("When allowed implied right for user is denied for same user in another rule, use most favorable tie resolution policy (ADT)",
            userRef, docRef, tieADT,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenySameTargetADT));

        assertAccess("When allowed implied right for user is denied for same user in another rule, use most favorable tie resolution policy (DAF)",
            userRef, docRef, allowAccessDAF,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenySameTargetDAF));

        assertAccess("When allowed implied right for group is denied for same group in another rule, use most favorable tie resolution policy (ADT)",
            anotherUserRef, docRef, tieADT,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenySameTargetADT));

        assertAccess("When allowed implied right for group is denied for same group in another rule, use most favorable tie resolution policy (DAF)",
            anotherUserRef, docRef, allowAccessDAF,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenySameTargetDAF));

        assertAccess("When allowed implied right for user is denied for its group in another rule, allow it. (ADT)",
            userRef, docRef, allowAccessADT,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenyUserGroupADT));

        assertAccess("When allowed implied right for user is denied for its group in another rule, allow it. (DAF)",
            userRef, docRef, allowAccessDAF,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef), conflictAllowDenyUserGroupDAF));

        assertAccess("When allowed implied right for group is denied for one of its user in another rule, deny it. (ADT)",
            anotherUserRef, docRef, denyAccessADT,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenyUserGroupADT));

        assertAccess("When allowed implied right for group is denied for one of its user in another rule, deny it. (DAF)",
            anotherUserRef, docRef, denyAccessDAF,
            authorizationSettler.settle(anotherUserRef, Arrays.asList(anotherGroupRef), conflictAllowDenyUserGroupDAF));
    }

    @Test
    public void testSettleAllowDeleteToCreator() throws Exception
    {
        XWikiSecurityAccess allowDelete = defaultAccess.clone();
        allowDelete.allow(Right.DELETE);

        assertAccess("Allow delete right to creator",
            creatorRef, docRef, allowDelete,
            authorizationSettler.settle(creatorRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("emptydocRules", docRef, Collections.<List<SecurityRule>>emptyList())));
    }

    @Test
    public void testSettleAllowAdminToWikiOwner() throws Exception
    {
        XWikiSecurityAccess allowAdmin = defaultAccess.clone();
        Set<Right> implied = Right.ADMIN.getImpliedRights();
        allowAdmin.allow(Right.ADMIN);
        for (Right right : Right.getEnabledRights(EntityType.DOCUMENT)) {
            if (implied.contains(right)) {
                allowAdmin.allow(right);
            }
        }

        assertAccess("Allow admin right to wiki owner",
            ownerRef, xdocRef, allowAdmin,
            authorizationSettler.settle(ownerRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("emptydocRules", xdocRef, Collections.<List<SecurityRule>>emptyList())));
    }

    @Test
    public void testSettleNewRightJustAdded() throws Exception
    {
        Right newRight = getNewTestRight("RightAddedLater",DENY,DENY,true);
        XWikiSecurityAccess defaultNewRight = defaultAccess.clone();
        defaultNewRight.allow(newRight);

        assertAccess("Allow a new right just added now",
            userRef, docRef, defaultNewRight,
            authorizationSettler.settle(userRef, Arrays.asList(groupRef),
                getMockedSecurityRuleEntries("onlyNewRight",
                    docRef,
                    Arrays.asList(Arrays.asList(getMockedSecurityRule(
                        "onlyNewRight",
                        Arrays.asList(userRef),
                        Collections.<GroupSecurityReference>emptyList(),
                        Arrays.asList(newRight),
                        RuleState.ALLOW))))));
    }
}

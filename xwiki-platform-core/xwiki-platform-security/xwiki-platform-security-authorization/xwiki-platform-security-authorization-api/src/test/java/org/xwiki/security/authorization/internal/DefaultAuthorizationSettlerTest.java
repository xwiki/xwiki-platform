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

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AbstractAdditionalRightsTestCase;
import org.xwiki.security.authorization.DefaultAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.DENY;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Default authorization settler unit tests.
 *
 * @version $Id$
 * @since 4.0M2
 */
@ComponentTest
class DefaultAuthorizationSettlerTest extends AbstractAdditionalRightsTestCase
{
    @InjectMockComponents
    private DefaultAuthorizationSettler authorizationSettler;

    @InjectMockComponents
    private DefaultAuthorizationManager authorizationManager;

    private XWikiSecurityAccess defaultAccess;
    private XWikiSecurityAccess denyAllAccess;

    @BeforeEach
    public void configure() throws Exception
    {
        // TODO: convert to junit 5 and add the execution context.
        defaultAccess = XWikiSecurityAccess.getDefaultAccess();
        denyAllAccess = new XWikiSecurityAccess();
        for (Right right : Right.values()) {
            denyAllAccess.deny(right);
        }
    }

    private Deque<SecurityRuleEntry> getMockedSecurityRuleEntries(String name, final SecurityReference reference,
                                                                  final List<List<SecurityRule>> ruleEntries)
    {
        final Deque<SecurityReference> refs = reference.getReversedSecurityReferenceChain();
        final Deque<SecurityRuleEntry> entries = new ArrayDeque<SecurityRuleEntry>(refs.size());

        for (SecurityReference ref : refs) {
            entries.push(mock(SecurityRuleEntry.class, name + ref));
        }

        int i = 0;
        SecurityReference ref = reference;
        for (SecurityRuleEntry entry : entries) {
            List<SecurityRule> rules;

            if (i < ruleEntries.size()) {
                rules = ruleEntries.get(i);
            } else {
                rules = Collections.emptyList();
            }

            when(entry.getReference()).thenReturn(ref);
            when(entry.getRules()).thenReturn(rules);
            when(entry.isEmpty()).thenReturn(rules.size() == 0);

            ref = ref.getParentSecurityReference();
            i++;
        }

        return entries;
    }

    private SecurityRule getMockedSecurityRule(String name, Iterable<UserSecurityReference> users,
        Iterable<GroupSecurityReference> groups, Iterable<Right> rights, final RuleState state)
    {
        final SecurityRule rule = mock(SecurityRule.class, name);

        final List<Matcher<? super UserSecurityReference>> userMatchers
            = new ArrayList<Matcher<? super UserSecurityReference>>();
        final List<Matcher<? super GroupSecurityReference>> groupMatchers
            = new ArrayList<Matcher<? super GroupSecurityReference>>();
        final List<Matcher<? super Right>> rightMatchers = new ArrayList<Matcher<? super Right>>();

        for (UserSecurityReference user : users) {
            userMatchers.add(is(user));
        }
        for (GroupSecurityReference group : groups) {
            groupMatchers.add(is(group));
        }
        for (Right right : rights) {
            rightMatchers.add(is(right));
        }

        when(rule.match(argThat(anyOf(userMatchers)))).thenReturn(true);
        when(rule.match(argThat(anyOf(groupMatchers)))).thenReturn(true);
        when(rule.match(argThat(anyOf(rightMatchers)))).thenReturn(true);
        when(rule.match(argThat(not(anyOf(userMatchers))))).thenReturn(false);
        when(rule.match(argThat(not(anyOf(groupMatchers))))).thenReturn(false);
        when(rule.match(argThat(not(anyOf(rightMatchers))))).thenReturn(false);
        when(rule.getState()).thenReturn(state);

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
    void testSettleNoRulesOnMainWiki() throws Exception
    {
        Deque<SecurityRuleEntry> emptyXdocRules
            = getMockedSecurityRuleEntries("emptyXdocRules", xdocRef, Collections.<List<SecurityRule>>emptyList());

        assertAccess("When no rules are defined, return default access for main wiki user on main wiki doc",
            xuserRef, xdocRef.getParentSecurityReference().getParentSecurityReference(), defaultAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(), emptyXdocRules, null, false));

        assertAccess("When no rules are defined, deny all access for local wiki user on main wiki doc",
            userRef, xdocRef.getParentSecurityReference().getParentSecurityReference(), denyAllAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(),
                emptyXdocRules));

        assertAccess("When no rules are defined, deny all access for another wiki user on main wiki doc",
            anotherWikiUserRef, xdocRef.getParentSecurityReference().getParentSecurityReference(), denyAllAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                emptyXdocRules));
    }

    @Test
    void testSettleNoRulesOnLocalWiki() throws Exception
    {
        Deque<SecurityRuleEntry> emptydocRules
            = getMockedSecurityRuleEntries("emptydocRules", docRef, Collections.<List<SecurityRule>>emptyList());

        assertAccess("When no rules are defined, return default access for local wiki user on local wiki doc",
            userRef, docRef.getParentSecurityReference().getParentSecurityReference(), defaultAccess,
            authorizationSettler.settle(userRef, Collections.<GroupSecurityReference>emptyList(), emptydocRules));

        assertAccess("When no rules are defined, return default access for main wiki on local wiki doc",
            xuserRef, docRef.getParentSecurityReference().getParentSecurityReference(), defaultAccess,
            authorizationSettler.settle(xuserRef, Collections.<GroupSecurityReference>emptyList(), emptydocRules));

        assertAccess("When no rules are defined, deny all access for another wiki user on local wiki doc",
            anotherWikiUserRef, docRef.getParentSecurityReference().getParentSecurityReference(), denyAllAccess,
            authorizationSettler.settle(anotherWikiUserRef, Collections.<GroupSecurityReference>emptyList(),
                emptydocRules));
    }

    @Test
    void testSettleInheritancePolicy() throws Exception
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
    void testSettleTieResolutionPolicy() throws Exception
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
    void testSettleOneAllowImpliesDenyForAllOthers() throws Exception
    {
        XWikiSecurityAccess defaultAllowRight0 = defaultAccess.clone();
        XWikiSecurityAccess defaultDenyRight0 = this.defaultAccess.clone();
        XWikiSecurityAccess defaultAllowRight6 = this.defaultAccess.clone();
        defaultAllowRight0.allow(allTestRights.get(0));
        defaultDenyRight0.deny(allTestRights.get(0));
        defaultAllowRight6.allow(allTestRights.get(6));

        assertAccess("When an allow rules is found, deny any not matching user",
            this.anotherUserRef, this.docRef, defaultDenyRight0,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef),
                getMockedSecurityRuleEntries("onlyRight0",
                    this.docRef,
                    List.of(List.of(getMockedSecurityRule(
                        "onlyRight0",
                        Collections.singletonList(this.userRef),
                        Collections.emptyList(),
                        Collections.singletonList(allTestRights.get(0)),
                        ALLOW))))));


        assertAccess("When an allow rules is found, do not deny a user matching in another rule",
            this.anotherUserRef, this.docRef, defaultAllowRight6,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6ToAnotherUser",
                    this.docRef,
                    List.of(Arrays.asList(
                        getMockedSecurityRule(
                            "allowRight6ToUser",
                            Collections.singletonList(this.userRef),
                            Collections.emptyList(),
                            Collections.singletonList(allTestRights.get(6)),
                            ALLOW),
                        getMockedSecurityRule(
                            "allowRight6ToAnotherUser",
                            Collections.singletonList(this.anotherUserRef),
                            Collections.emptyList(),
                            Collections.singletonList(allTestRights.get(6)),
                            ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching in another rule",
            this.anotherUserRef, this.docRef, defaultAllowRight6,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6ToAnotherGroup",
                    this.docRef,
                    List.of(Arrays.asList(
                        getMockedSecurityRule(
                            "allowRight6ToUserAndGroup",
                            Collections.singletonList(this.userRef),
                            Collections.singletonList(this.groupRef),
                            Collections.singletonList(allTestRights.get(6)),
                            ALLOW),
                        getMockedSecurityRule(
                            "allowRight6ToAnotherGroup",
                            Collections.emptyList(),
                            Collections.singletonList(this.anotherGroupRef),
                            Collections.singletonList(allTestRights.get(6)),
                            ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching the rule",
            this.userRef, this.docRef, defaultAllowRight6,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("onlyRight6",
                    this.docRef,
                    List.of(List.of(getMockedSecurityRule(
                        "onlyRight6",
                        Collections.singletonList(this.userRef),
                        Collections.emptyList(),
                        Collections.singletonList(allTestRights.get(6)),
                        ALLOW))))));

        assertAccess("When an allow rules is found, do not deny a user matching a group in the rule",
            this.anotherUserRef, this.docRef, defaultAllowRight6,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef),
                getMockedSecurityRuleEntries("allowRight6",
                    this.docRef,
                    List.of(List.of(
                        getMockedSecurityRule(
                            "allowRight6ToUserAndAnotherGroup",
                            Collections.singletonList(this.userRef),
                            Collections.singletonList(this.anotherGroupRef),
                            Collections.singletonList(allTestRights.get(6)),
                            ALLOW))))));

    }

    @Test
    void testSettleRightWithImpliedRights() throws Exception
    {
        SecurityRule allowImpliedADT = getMockedSecurityRule("allowImpliedADT",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), Collections.singletonList(impliedTestRightsADT), ALLOW);
        SecurityRule denyImpliedADT = getMockedSecurityRule("denyImpliedADT",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), Collections.singletonList(impliedTestRightsADT), DENY);

        SecurityRule allowImpliedDAF = getMockedSecurityRule("allowImpliedDAF",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), Collections.singletonList(impliedTestRightsDAF), ALLOW);
        SecurityRule denyImpliedDAF = getMockedSecurityRule("denyImpliedDAF",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), Collections.singletonList(impliedTestRightsDAF), DENY);


        XWikiSecurityAccess allowAccessADT = this.defaultAccess.clone();
        allowAccessADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            allowAccessADT.allow(right);
        }
        XWikiSecurityAccess tieADT = this.defaultAccess.clone();
        tieADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            tieADT.set(right, right.getTieResolutionPolicy());
        }
        XWikiSecurityAccess allowAccessDAF = this.defaultAccess.clone();
        allowAccessDAF.set(impliedTestRightsDAF, ALLOW);
        for (Right right : allTestRights) {
            allowAccessDAF.allow(right);
        }
        XWikiSecurityAccess denyADTAccess = this.defaultAccess.clone();
        denyADTAccess.deny(impliedTestRightsADT);
        XWikiSecurityAccess denyDAFAccess = this.defaultAccess.clone();
        denyDAFAccess.deny(impliedTestRightsDAF);

        XWikiSecurityAccess denyAccessADT = this.defaultAccess.clone();
        denyAccessADT.set(impliedTestRightsADT, ALLOW);
        for (Right right : allTestRights) {
            denyAccessADT.deny(right);
        }
        XWikiSecurityAccess denyAccessDAF = this.defaultAccess.clone();
        denyAccessDAF.set(impliedTestRightsDAF, ALLOW);
        for (Right right : allTestRights) {
            denyAccessDAF.deny(right);
        }



        assertAccess("When a right implying others rights is allowed, imply those rights (ADT)",
            this.userRef, this.docRef, allowAccessADT,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("allowAccessADT", this.docRef,
                    List.of(List.of(allowImpliedADT)))));

        assertAccess("When a right implying others rights is allowed, imply those rights (DAF)",
            this.userRef, this.docRef, allowAccessDAF,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("allowAccessDAF", this.docRef,
                    List.of(List.of(allowImpliedDAF)))));

        assertAccess("When a right implying others rights is denied, do not denied implied rights (ADT)",
            this.userRef, this.docRef, denyADTAccess,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("denyAccessADT", this.docRef,
                    List.of(List.of(denyImpliedADT)))));

        assertAccess("When a right implying others rights is denied, do not denied implied rights (DAF)",
            this.userRef, this.docRef, denyDAFAccess,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("denyAccessDAF", this.docRef,
                    List.of(List.of(denyImpliedDAF)))));

        SecurityRule allowAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("allowAllTestRightsUserAndAnotherGroup",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), allTestRights, ALLOW);
        SecurityRule denyAllTestRightsUserAndAnotherGroup = getMockedSecurityRule("denyAllTestRightsUserAndAnotherGroup",
            Collections.singletonList(this.userRef), Collections.singletonList(this.anotherGroupRef), allTestRights, DENY);
        SecurityRule denyAllTestRightsAnotherUserAndGroup = getMockedSecurityRule("denyAllTestRightsAnotherUserAndGroup",
            Collections.singletonList(this.anotherUserRef), Collections.singletonList(this.groupRef), allTestRights, DENY);


        Deque<SecurityRuleEntry> conflictAllowDenySameTargetADT
            = getMockedSecurityRuleEntries("conflictAllowDenySameTargetADT", this.docRef, List.of(
            Arrays.asList(allowImpliedADT, denyAllTestRightsUserAndAnotherGroup)));
        Deque<SecurityRuleEntry> conflictAllowDenySameTargetDAF
            = getMockedSecurityRuleEntries("conflictAllowDenySameTargetDAF", this.docRef, List.of(
            Arrays.asList(allowImpliedDAF, denyAllTestRightsUserAndAnotherGroup)));

        Deque<SecurityRuleEntry> conflictAllowDenyUserGroupADT
            = getMockedSecurityRuleEntries("conflictAllowDenyUserGroupADT", this.docRef, List.of(
            Arrays.asList(allowImpliedADT, denyAllTestRightsAnotherUserAndGroup)));
        Deque<SecurityRuleEntry> conflictAllowDenyUserGroupDAF
            = getMockedSecurityRuleEntries("conflictAllowDenyUserGroupDAF", this.docRef, List.of(
            Arrays.asList(allowImpliedDAF, denyAllTestRightsAnotherUserAndGroup)));

        assertAccess("When allowed implied right for user is denied for same user in another rule, use most favorable tie resolution policy (ADT)",
            this.userRef, this.docRef, tieADT,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef), conflictAllowDenySameTargetADT));

        assertAccess("When allowed implied right for user is denied for same user in another rule, use most favorable tie resolution policy (DAF)",
            this.userRef, this.docRef, allowAccessDAF,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef), conflictAllowDenySameTargetDAF));

        assertAccess("When allowed implied right for group is denied for same group in another rule, use most favorable tie resolution policy (ADT)",
            this.anotherUserRef, this.docRef, tieADT,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef), conflictAllowDenySameTargetADT));

        assertAccess("When allowed implied right for group is denied for same group in another rule, use most favorable tie resolution policy (DAF)",
            this.anotherUserRef, this.docRef, allowAccessDAF,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef), conflictAllowDenySameTargetDAF));

        assertAccess("When allowed implied right for user is denied for its group in another rule, allow it. (ADT)",
            this.userRef, this.docRef, allowAccessADT,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef), conflictAllowDenyUserGroupADT));

        assertAccess("When allowed implied right for user is denied for its group in another rule, allow it. (DAF)",
            this.userRef, this.docRef, allowAccessDAF,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef), conflictAllowDenyUserGroupDAF));

        assertAccess("When allowed implied right for group is denied for one of its user in another rule, deny it. (ADT)",
            this.anotherUserRef, this.docRef, denyAccessADT,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef), conflictAllowDenyUserGroupADT));

        assertAccess("When allowed implied right for group is denied for one of its user in another rule, deny it. (DAF)",
            this.anotherUserRef, this.docRef, denyAccessDAF,
            this.authorizationSettler.settle(this.anotherUserRef, Collections.singletonList(this.anotherGroupRef), conflictAllowDenyUserGroupDAF));
    }

    @Test
    void testSettleNewRightJustAdded() throws Exception
    {
        Right newRight = getNewTestRight("RightAddedLater",DENY,DENY,true);
        XWikiSecurityAccess defaultNewRight = this.defaultAccess.clone();
        defaultNewRight.allow(newRight);

        assertAccess("Allow a new right just added now",
            this.userRef, this.docRef, defaultNewRight,
            this.authorizationSettler.settle(this.userRef, Collections.singletonList(this.groupRef),
                getMockedSecurityRuleEntries("onlyNewRight",
                    this.docRef,
                    List.of(List.of(getMockedSecurityRule(
                        "onlyNewRight",
                        Collections.singletonList(this.userRef),
                        Collections.emptyList(),
                        List.of(newRight),
                        ALLOW))))));

        this.authorizationManager.unregister(newRight);
    }

    @Test
    void testSettleEntityTypeWithoutAnyEnabledRight() throws Exception
    {
        SecurityRule allowAllTestRightsRulesToXuser = getMockedSecurityRule("allowAllTestRightsRulesToXuser",
            Collections.singletonList(this.xuserRef), Collections.emptyList(), allTestRights, ALLOW);

        assertAccess("Allow rights to entity without any acceptable right on itself but having some (XWIKI-12552)",
            this.xuserRef, this.xattachmentRef, this.defaultAccess,
            this.authorizationSettler.settle(this.xuserRef, Collections.emptyList(),
                getMockedSecurityRuleEntries("allrights",
                    this.xattachmentRef,
                    Collections.singletonList(Collections.singletonList(allowAllTestRightsRulesToXuser)))));
    }
}

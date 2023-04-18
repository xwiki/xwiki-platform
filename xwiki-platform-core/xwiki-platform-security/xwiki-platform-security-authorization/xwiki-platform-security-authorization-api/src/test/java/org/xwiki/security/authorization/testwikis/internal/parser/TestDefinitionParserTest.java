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
package org.xwiki.security.authorization.testwikis.internal.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.testwikis.TestAccessRule;
import org.xwiki.security.authorization.testwikis.TestDefinition;
import org.xwiki.security.authorization.testwikis.TestDefinitionParser;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestGroup;
import org.xwiki.security.authorization.testwikis.TestSpace;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test of the test definition parser itself.
 *
 * @version $Id$
 * @since 5.0M2
 */
@ComponentTest
@ComponentList({
    DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultSymbolScheme.class
})
class TestDefinitionParserTest
{
    @InjectComponentManager
    private ComponentManager componentManager;

    @Test
    void definitionTestParser() throws Exception
    {
        TestDefinitionParser parser = new DefaultTestDefinitionParser();

        EntityReferenceResolver<String> resolver =
            componentManager.getInstance(EntityReferenceResolver.TYPE_STRING);
        EntityReferenceSerializer<String> serializer =
            componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);

        TestDefinition testDefinition =
            parser.parse("testwikis" + File.separatorChar + "parserTester.xml", resolver, serializer);

        Collection<TestWiki> testWikis = testDefinition.getWikis();

        assertThat("There should be some wikis", testWikis, notNullValue());
        assertThat("The wikis count should be 3", testWikis.size(), equalTo(3));

        TestWiki mainwiki = testDefinition.getMainWiki();

        assertThat("Main wiki should be defined", mainwiki, notNullValue());
        assertThat("Main wiki should be named 'wiki'", mainwiki.getWikiReference(), equalTo(new WikiReference("wiki")));
        assertThat("Main wiki should be main wiki", mainwiki.isMainWiki(), is(true));
        assertThat("Main wiki owner should be XWiki.Admin", mainwiki.getOwner(), equalTo(
            new DocumentReference("wiki", "XWiki", "Admin")));
        assertThat("Main wiki should have 4 users (2 groups, and 2 users)", mainwiki.getUsers().size(), equalTo(4));
        assertThat("Main wiki should have 2 groups", mainwiki.getGroups().size(), equalTo(2));
        assertThat("Main wiki should have a groupA", mainwiki.getGroup("groupA"), notNullValue());
        assertThat("Main wiki should have a userA", mainwiki.getUser("userA"), notNullValue());

        Collection<TestGroup> groups = mainwiki.getUser("userA").getGroups();

        assertThat("UserA of Main wiki should be in 2 groups", groups.size(), equalTo(2));

        List<DocumentReference> groupRefs = new ArrayList<DocumentReference>();
        for (TestGroup group : groups) {
            groupRefs.add(group.getGroupReference());
        }

        assertThat("User A is in GroupA of the main wiki and the subwiki", groupRefs,
            hasItems(new DocumentReference("wiki", "XWiki", "groupA"),
                new DocumentReference("wiki1", "XWiki", "groupA")));

        Collection<TestAccessRule> rules = mainwiki.getAccessRules();

        assertThat("There must be 26 access rules on main wiki", rules.size(), equalTo(26));

        List<DocumentReference> userRefs = new ArrayList<DocumentReference>();
        List<Right> rights = new ArrayList<Right>();
        List<RuleState> states = new ArrayList<RuleState>();

        for (TestAccessRule rule : rules) {
            userRefs.add(rule.getUser());
            rights.add(rule.getRight());
            states.add(rule.getState());
        }

        assertThat("Users in access rules of main wiki mismatch", userRefs, hasItems(
            new DocumentReference("wiki", "XWiki", "userA"),
            new DocumentReference("wiki", "XWiki", "userB"),
            new DocumentReference("wiki", "XWiki", "groupA"),
            new DocumentReference("wiki", "XWiki", "groupB")
        ));
        assertThat("Rights in access rules of main wiki mismatch", rights, hasItems(
            Right.VIEW, Right.LOGIN, Right.EDIT, Right.COMMENT, Right.DELETE, Right.REGISTER, Right.ADMIN, Right.PROGRAM
        ));
        assertThat("State in access rules of main wiki mismatch", states, hasItems(
            RuleState.ALLOW, RuleState.DENY
        ));

        assertThat("Main wiki should have 3 spaces (2 plus XWiki)", mainwiki.getSpaces().size(), equalTo(3));

        TestSpace space = mainwiki.getSpace("space1");

        assertThat("Main wiki should have a space named 'space1'", space, notNullValue());
        assertThat("'space1' of main wiki should have description 'space 1'", space.getDescription(),
            equalTo("space 1"));

        rules = space.getAccessRules();

        assertThat("There must be 8 access rules on space 1", rules.size(), equalTo(8));

        userRefs = new ArrayList<DocumentReference>();
        rights = new ArrayList<Right>();
        states = new ArrayList<RuleState>();

        for (TestAccessRule rule : rules) {
            userRefs.add(rule.getUser());
            rights.add(rule.getRight());
            states.add(rule.getState());
        }

        assertThat("Users in access rules of space 1 of main wiki mismatch", userRefs, hasItems(
            new DocumentReference("wiki", "XWiki", "userA"),
            new DocumentReference("wiki", "XWiki", "userB"),
            new DocumentReference("wiki", "XWiki", "groupB")
        ));
        assertThat("Rights in access rules of space 1 of main wiki mismatch", rights, hasItems(
            Right.VIEW, Right.EDIT, Right.COMMENT, Right.DELETE, Right.ADMIN
        ));
        assertThat("State in access rules of space 1 of main wiki mismatch", states, hasItems(
            RuleState.DENY
        ));

        assertThat("Space 1 of main wiki should have 2 documents", space.getDocuments().size(), equalTo(2));

        TestDocument document = space.getDocument("document1");

        assertThat("Space 1 of main wiki should have a document named 'document1'", document, notNullValue());
        assertThat("'document1' of 'space1' of main wiki should have description 'Document 1'",
            document.getDescription(), equalTo("Document 1"));

        rules = document.getAccessRules();

        assertThat("There must be 7 access rules on document 1", rules.size(), equalTo(7));

        userRefs = new ArrayList<>();
        rights = new ArrayList<>();
        states = new ArrayList<>();

        for (TestAccessRule rule : rules) {
            userRefs.add(rule.getUser());
            rights.add(rule.getRight());
            states.add(rule.getState());
        }

        assertThat("Users in access rules of document 1 of space 1 of main wiki mismatch", userRefs, hasItems(
            new DocumentReference("wiki", "XWiki", "userA"),
            new DocumentReference("wiki", "XWiki", "userB"),
            new DocumentReference("wiki", "XWiki", "groupA")
        ));
        assertThat("Rights in access rules of document 1 of space 1 of main wiki mismatch", rights, hasItems(
            Right.VIEW, Right.EDIT, Right.COMMENT, Right.DELETE
        ));
        assertThat("State in access rules of document 1 of space 1 of main wiki mismatch", states, hasItems(
            RuleState.ALLOW
        ));
    }
}


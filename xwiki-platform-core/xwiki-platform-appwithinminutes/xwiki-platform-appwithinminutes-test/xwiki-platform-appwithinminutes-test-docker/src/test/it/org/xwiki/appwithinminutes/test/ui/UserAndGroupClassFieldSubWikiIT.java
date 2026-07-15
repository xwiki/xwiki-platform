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
package org.xwiki.appwithinminutes.test.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.SuggestClassFieldEditPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;
import org.xwiki.xclass.test.po.ClassSheetPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the App Within Minutes User and Group pickers on both the main wiki and a subwiki, checking that when a
 * subwiki is configured with the {@code local_and_global} user scope ("Both global and local users are available in
 * the wiki") the pickers suggest both the local (subwiki) and global (main wiki) users and groups, and that a picked
 * entry is displayed properly in view mode.
 *
 * @version $Id$
 * @since 17.10.10
 * @since 18.4.3
 * @since 18.6.0RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class UserAndGroupClassFieldSubWikiIT
{
    private static final String MAIN_WIKI = "xwiki";

    private static final String USERS_SPACE = "XWiki";

    private static final String GLOBAL_USER = "GlobalPickerUser";

    private static final String GLOBAL_USER_FIRST_NAME = "Global";

    private static final String GLOBAL_GROUP = "GlobalPickerGroup";

    private static final String LOCAL_USER = "LocalPickerUser";

    private static final String LOCAL_USER_FIRST_NAME = "Local";

    private static final String LOCAL_GROUP = "LocalPickerGroup";

    // Both users share this last name and both groups share this substring so that a single search fragment matches
    // the local and the global entity at once.
    private static final String USER_LAST_NAME = "Pickerson";

    private static final String GROUP_SEARCH = "PickerGroup";

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.createAdminUser();

        // Create a global user and a global group in the main wiki.
        setup.setCurrentWiki(MAIN_WIKI);
        setup.loginAsSuperAdmin();
        setup.createUser(GLOBAL_USER, GLOBAL_USER, null, "first_name", GLOBAL_USER_FIRST_NAME, "last_name",
            USER_LAST_NAME);
        createGroup(setup, GLOBAL_GROUP);
    }

    /**
     * Creates an empty group, i.e. a document holding a single (empty) {@code XWiki.XWikiGroups} object, in the current
     * wiki.
     */
    private void createGroup(TestUtils setup, String groupName) throws Exception
    {
        LocalDocumentReference groupReference = new LocalDocumentReference(USERS_SPACE, groupName);
        setup.rest().savePage(groupReference);
        setup.rest().addObject(groupReference, "XWiki.XWikiGroups", "member", "");
    }

    @ParameterizedTest
    @WikisSource(extensions = { "org.xwiki.platform:xwiki-platform-appwithinminutes-ui" })
    void userAndGroupPickersShowLocalAndGlobal(WikiReference wiki, TestUtils setup, TestReference testReference)
        throws Exception
    {
        boolean isSubWiki = !MAIN_WIKI.equals(wiki.getName());
        setup.loginAsSuperAdmin();

        if (isSubWiki) {
            // Configure the subwiki so that both global and local users/groups are available. The scope is stored in
            // a WikiManager.WikiUserClass object on the WikiManager.WikiUserConfiguration document of the subwiki
            // itself. A subwiki with no such object defaults to GLOBAL_ONLY, so we set it explicitly (the
            // updateOrCreate object policy creates the object if it doesn't exist yet).
            setup.setCurrentWiki(wiki.getName());
            setup.updateObject("WikiManager", "WikiUserConfiguration", "WikiManager.WikiUserClass", 0, "userScope",
                "local_and_global");

            // Create a local user and a local group inside the subwiki.
            setup.createUser(LOCAL_USER, LOCAL_USER, null, "first_name", LOCAL_USER_FIRST_NAME, "last_name",
                USER_LAST_NAME);
            createGroup(setup, LOCAL_GROUP);
        }

        setup.setCurrentWiki(wiki.getName());
        setup.loginAsSuperAdmin();

        // The class document must live in the current wiki (the injected test reference is always rooted in the main
        // wiki), so re-root it explicitly.
        List<String> spaceNames = testReference.getSpaceReferences().stream().map(EntityReference::getName).toList();
        DocumentReference classReference = new DocumentReference(wiki.getName(), spaceNames, "Class");
        setup.deletePage(classReference, true);

        // Build an App Within Minutes class with a User picker field and a Group picker field.
        ApplicationClassEditPage editor = ApplicationClassEditPage.goToEditor(classReference);
        SuggestClassFieldEditPane userField = new SuggestClassFieldEditPane(editor.addField("User").getName());
        SuggestClassFieldEditPane groupField = new SuggestClassFieldEditPane(editor.addField("Group").getName());
        String userProperty = userField.getName();
        String groupProperty = groupField.getName();

        // Edit mode: the picker suggestions must honour the wiki's user scope.
        SuggestInputElement userPicker = userField.getPicker();
        List<SuggestionElement> userSuggestions =
            userPicker.sendKeys(USER_LAST_NAME).waitForNonTypedSuggestions().getSuggestions();
        assertTrue(hasSuggestionEndingWith(userSuggestions, USERS_SPACE + '.' + GLOBAL_USER),
            "The global user should always be suggested. Got: " + values(userSuggestions));
        assertEquals(isSubWiki, hasSuggestionEndingWith(userSuggestions, USERS_SPACE + '.' + LOCAL_USER),
            "The local user should be suggested only on the subwiki. Got: " + values(userSuggestions));
        userPicker.hideSuggestions();

        SuggestInputElement groupPicker = groupField.getPicker();
        List<SuggestionElement> groupSuggestions =
            groupPicker.sendKeys(GROUP_SEARCH).waitForNonTypedSuggestions().getSuggestions();
        assertTrue(hasSuggestionEndingWith(groupSuggestions, USERS_SPACE + '.' + GLOBAL_GROUP),
            "The global group should always be suggested. Got: " + values(groupSuggestions));
        assertEquals(isSubWiki, hasSuggestionEndingWith(groupSuggestions, USERS_SPACE + '.' + LOCAL_GROUP),
            "The local group should be suggested only on the subwiki. Got: " + values(groupSuggestions));
        groupPicker.hideSuggestions();
        editor.clickSaveAndView();

        // View mode: create an entry, pick the local user/group on the subwiki (the global ones on the main wiki) and
        // check they are displayed properly.
        String pickedUser = isSubWiki ? LOCAL_USER : GLOBAL_USER;
        String pickedUserName = (isSubWiki ? LOCAL_USER_FIRST_NAME : GLOBAL_USER_FIRST_NAME) + ' ' + USER_LAST_NAME;
        String pickedGroup = isSubWiki ? LOCAL_GROUP : GLOBAL_GROUP;

        ClassSheetPage classSheetPage = new ClassSheetPage();
        String location = setup.serializeLocalReference(testReference.getLastSpaceReference());
        InlinePage entryEditPage = classSheetPage.createNewDocument(location, "Entry");

        String className = setup.serializeReference(classReference.getLocalDocumentReference());
        SuggestInputElement entryUserPicker =
            new SuggestInputElement(setup.getDriver().findElement(By.id(className + "_0_" + userProperty)));
        entryUserPicker.sendKeys(pickedUser).waitForNonTypedSuggestions().selectByValue(USERS_SPACE + '.' + pickedUser);
        SuggestInputElement entryGroupPicker =
            new SuggestInputElement(setup.getDriver().findElement(By.id(className + "_0_" + groupProperty)));
        entryGroupPicker.sendKeys(pickedGroup).waitForNonTypedSuggestions()
            .selectByValue(USERS_SPACE + '.' + pickedGroup);
        entryEditPage.clickSaveAndView();

        List<WebElement> users = setup.getDriver().findElements(By.className("user"));
        assertEquals(1, users.size());
        assertEquals(pickedUserName, users.get(0).getText());
        assertTrue(setup.getDriver().getPageSource().contains(pickedGroup),
            "The selected group [" + pickedGroup + "] should be displayed in view mode.");
    }

    private boolean hasSuggestionEndingWith(List<SuggestionElement> suggestions, String valueSuffix)
    {
        return suggestions.stream().map(SuggestionElement::getValue).anyMatch(value -> value != null
            && value.endsWith(valueSuffix));
    }

    private List<String> values(List<SuggestionElement> suggestions)
    {
        return suggestions.stream().map(SuggestionElement::getValue).toList();
    }
}

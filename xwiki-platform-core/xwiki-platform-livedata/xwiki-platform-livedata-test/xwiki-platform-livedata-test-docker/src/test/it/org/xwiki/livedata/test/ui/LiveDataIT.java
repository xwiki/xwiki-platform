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
package org.xwiki.livedata.test.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.StaticListClassEditElement;
import org.xwiki.text.StringUtils;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.Keys.BACK_SPACE;
import static org.openqa.selenium.Keys.CONTROL;
import static org.openqa.selenium.Keys.chord;
import static org.xwiki.livedata.test.po.TableLayoutElement.FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS;

/**
 * Tests of the Live Data macro, in view and edit modes.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@UITest
class LiveDataIT
{
    private static final String NAME_COLUMN = "name";

    private static final String CHOICE_COLUMN = "choice";

    private static final String NAME_LYNDA = "Lynda";

    private static final String NAME_ESTHER = "Esther";

    private static final String NAME_CHARLY = "Charly";

    private static final String NAME_NIKOLAY = "Nikolay";

    private static final String CHOICE_A = "value1";

    private static final String CHOICE_B = "value2";

    private static final String CHOICE_C = "value3";

    private static final String CHOICE_D = "value4";

    private static final String CHOICE_EMPTY = "(empty)";

    private static final String CHOICE_L = "value_l";

    private static final String CHOICE_L_LABEL = "Display & Label";

    private static final String CHOICE_T = "value_t";

    private static final String CHOICE_T_TRANSLATION = "Translated & Label";

    private static final String BIRTHDAY_COLUMN = "birthday";

    private static final String USER_COLUMN = "user";

    private static final String BIRTHDAY_DATETIME = "11/05/2021 16:00:00";

    private static final String CANCELED_BIRTHDAY_DATETIME = "11/05/2021 16:00:10";

    private static final String IS_ACTIVE_COLUMN = "isActive";

    private static final String YES = "Yes";
    private static final String NO = "No";

    private static final String DOC_TITLE_COLUMN = "doc.title";

    private static final String FOOTNOTE_COMPUTED_TITLE =
        "(1) Some pages have a computed title. Filtering and sorting by title will not work as expected for these "
            + "pages.";

    public static final String ACTIONS_COLUMN = "_actions";

    /**
     * Test the view and edition of the cells of a live data in table layout with a liveTable source. Creates an XClass
     * and two XObjects, then edit the XObjects properties from the live data.
     */
    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-livetable-ui")
    @Order(1)
    void livedataLivetableTableLayout(WikiReference wikiReference, TestUtils testUtils,
        TestLocalReference testLocalReference) throws Exception
    {
        testUtils.setCurrentWiki(wikiReference.getName());

        DocumentReference testReference = new DocumentReference(testLocalReference, wikiReference);

        // Make sure an icon theme is configured.
        testUtils.setWikiPreference("iconTheme", "IconThemes.Silk");

        // Login as super admin because guest user cannot remove pages.
        testUtils.loginAsSuperAdmin();
        testUtils.createUser("U1", "U1", null);
        testUtils.createUser("U2", "U2", null);
        // Wipes the test space.
        testUtils.deletePage(testReference, true);

        initLocalization(testUtils, testReference);

        // Initializes the page content.
        List<String> properties =
            List.of(NAME_COLUMN, CHOICE_COLUMN, BIRTHDAY_COLUMN, USER_COLUMN, IS_ACTIVE_COLUMN, DOC_TITLE_COLUMN);
        createClassNameLiveDataPage(testUtils, testReference, properties, "");

        // Creates the XClass.
        createXClass(testUtils, testReference);

        // Creates corresponding XObjects.
        createXObjects(testUtils, testReference);

        testUtils.gotoPage(testReference);

        LiveDataElement liveDataElement = new LiveDataElement("test");
        TableLayoutElement tableLayout = liveDataElement.getTableLayout();

        // Verify that at least one Live Data icon is displayed to prevent XWIKI-19086 to happen again.
        assertTrue(tableLayout.getDropDownButton().findElement(By.tagName("img")).getAttribute("src")
            .contains("bullet_arrow_down.png"));

        // Test the Live Data content.
        assertEquals(3, tableLayout.countRows());

        assertEquals(List.of(5, 15, 25, 50, 100), liveDataElement.getPaginationPageSizes());

        // Check that the list of page sizes is preserved when we select a standard page size (see XWIKI-20650)
        liveDataElement = liveDataElement.setPagination(15);
        assertEquals(3, liveDataElement.getTableLayout().countRows());
        assertEquals(List.of(5, 15, 25, 50, 100), liveDataElement.getPaginationPageSizes());

        tableLayout.assertRow(DOC_TITLE_COLUMN, "O1");
        tableLayout.assertRow(DOC_TITLE_COLUMN, "O2 1");
        tableLayout.assertRow(DOC_TITLE_COLUMN, "O3 1");
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.assertRow(NAME_COLUMN, NAME_ESTHER);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_A);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_B);
        tableLayout.editCell(NAME_COLUMN, 1, NAME_COLUMN, NAME_CHARLY);
        tableLayout.editCell(CHOICE_COLUMN, 2, CHOICE_COLUMN, CHOICE_C);
        tableLayout.editCell(BIRTHDAY_COLUMN, 1, BIRTHDAY_COLUMN, BIRTHDAY_DATETIME);
        tableLayout.editAndCancel(BIRTHDAY_COLUMN, 2, BIRTHDAY_COLUMN, CANCELED_BIRTHDAY_DATETIME);
        // Edits the choice column of Nikolay, to assert that a cell with an empty content can be edited.
        tableLayout.editCell(CHOICE_COLUMN, 3, CHOICE_COLUMN, CHOICE_D);
        assertEquals(3, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_CHARLY);
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_B);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_C);
        tableLayout.assertRow(BIRTHDAY_COLUMN, BIRTHDAY_DATETIME);
        tableLayout.assertRow(IS_ACTIVE_COLUMN, YES);
        tableLayout.assertRow(IS_ACTIVE_COLUMN, NO);
        // The canceled birthday date shouldn't appear on the table since it has been canceled. 
        tableLayout
            .assertRow(BIRTHDAY_COLUMN, not(hasItem(tableLayout.getWebElementTextMatcher(CANCELED_BIRTHDAY_DATETIME))));
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_D);
        SpaceReference xWikiSpace = new SpaceReference("XWiki", wikiReference);
        tableLayout
            .assertCellWithLink(USER_COLUMN, "U1", testUtils.getURL(new DocumentReference("U1", xWikiSpace)));
        tableLayout
            .assertCellWithLink(USER_COLUMN, "U2", testUtils.getURL(new DocumentReference("U2", xWikiSpace)));
        tableLayout.assertRow(USER_COLUMN, "");
        assertEquals(1, liveDataElement.countFootnotes());
        assertEquals(FOOTNOTE_COMPUTED_TITLE, liveDataElement.getFootnotesText().get(0));
        tableLayout.filterColumn(USER_COLUMN, "U1");
        assertEquals(1, tableLayout.countRows());
        // The footnotes are supposed to disappear after the filter because the related entries are not displayed.
        assertEquals(0, liveDataElement.countFootnotes());
        tableLayout
            .assertCellWithLink(USER_COLUMN, "U1", testUtils.getURL(new DocumentReference("U1", xWikiSpace)));

        // Reload the page to verify that the persisted filters are taken into account after reload.
        testUtils.getDriver().navigate().refresh();
        tableLayout.waitUntilReady();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.filterColumn(CHOICE_COLUMN, CHOICE_EMPTY);
        // Reload to test if the empty filter is displayed again.
        testUtils.getDriver().navigate().refresh();
        tableLayout.waitUntilReady();
        assertEquals(CHOICE_EMPTY, tableLayout.getFilterValues(CHOICE_COLUMN).get(0));

        // Become guest because the tests does not need specific rights.
        testUtils.forceGuestUser();

        testUtils.gotoPage(testReference);

        liveDataElement = new LiveDataElement("test");
        tableLayout = liveDataElement.getTableLayout();
        assertEquals(2, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.assertRow(NAME_COLUMN, NAME_NIKOLAY);
        assertEquals(1, liveDataElement.countFootnotes());
        assertThat(liveDataElement.getFootnotesText(), containsInAnyOrder(FOOTNOTE_COMPUTED_TITLE));

        // Test text filters. Verify that all keystrokes are preserved when typing a value (non-regression test for
        // XWIKI-23789).
        WebElement nameFilter = tableLayout.getFilter(NAME_COLUMN);
        String longstring = "0123456789012345678901234567890123456789";
        // Simulate a human-like keyboard input since selenium setting the value all at once, which does not make the
        // potential problem visible. With a long enough input string, the change of losing char are very high when
        // the issue exists.
        longstring.chars().forEach(c -> {
            nameFilter.sendKeys(Character.toString((char) c));
            try {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(0, 300));
            } catch (InterruptedException e) {
                // Ignore
            }
        });
        assertEquals(0, tableLayout.countRows());
        assertEquals(longstring, nameFilter.getAttribute("value"));
        nameFilter.sendKeys(chord(CONTROL, "a", BACK_SPACE));

        // Testing the selectize filters
        // Test filtering by label
        SuggestInputElement suggestInputElement = new SuggestInputElement(tableLayout.getFilter(CHOICE_COLUMN));
        suggestInputElement.sendKeys(CHOICE_L_LABEL);
        suggestInputElement.waitForNonTypedSuggestions();
        List<SuggestInputElement.SuggestionElement> suggestionElements = suggestInputElement.getSuggestions();
        assertEquals(1, suggestionElements.size());
        assertEquals(CHOICE_L, suggestionElements.get(0).getValue());
        assertEquals(CHOICE_L_LABEL, suggestionElements.get(0).getLabel());

        // Test filtering by translation
        suggestInputElement.clear();
        suggestInputElement.sendKeys(CHOICE_T_TRANSLATION);
        suggestInputElement.waitForNonTypedSuggestions();
        suggestionElements = suggestInputElement.getSuggestions();
        assertEquals(1, suggestionElements.size());
        assertEquals(CHOICE_T, suggestionElements.get(0).getValue());
        assertEquals(CHOICE_T_TRANSLATION, suggestionElements.get(0).getLabel());

        // Test filter on boolean values
        suggestInputElement.clear().hideSuggestions();
        liveDataElement.waitUntilReady();
        tableLayout.waitUntilRowCountEqualsTo(2);
        assertEquals(2, tableLayout.countRows());

        // Take the focus on the is active filter.
        suggestInputElement = new SuggestInputElement(tableLayout.getFilter(IS_ACTIVE_COLUMN));
        suggestInputElement.sendKeys(Boolean.TRUE.toString());
        suggestInputElement.waitForNonTypedSuggestions();
        suggestionElements = suggestInputElement.getSuggestions();
        assertEquals(1, suggestionElements.size());
        suggestionElements.get(0).select();
        liveDataElement.waitUntilReady();
        tableLayout.waitUntilRowCountEqualsTo(1);
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);

        // Refresh the page and ensure the filter is still there
        testUtils.getDriver().navigate().refresh();
        tableLayout.waitUntilReady();
        liveDataElement.waitUntilReady();

        WebElement isActiveFilter = tableLayout.getFilter(IS_ACTIVE_COLUMN);
        assertEquals("1", isActiveFilter.getAttribute("value"));
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);

        suggestInputElement = new SuggestInputElement(isActiveFilter);
        suggestInputElement.clear();
        suggestInputElement.sendKeys(Boolean.FALSE.toString());
        suggestInputElement.waitForNonTypedSuggestions();
        suggestionElements = suggestInputElement.getSuggestions();
        assertEquals(1, suggestionElements.size());
        suggestionElements.get(0).select();
        liveDataElement.waitUntilReady();
        tableLayout.waitUntilRowCountEqualsTo(1);
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_NIKOLAY);

        suggestInputElement.clear().hideSuggestions();
        liveDataElement.waitUntilReady();
        tableLayout.waitUntilRowCountEqualsTo(2);
        assertEquals(2, tableLayout.countRows());

        // Switch to another language to assert that the text is still correctly translated.
        try {
            testUtils.setWikiPreference("default_language", "fr");
            testUtils.getDriver().navigate().refresh();
            liveDataElement.waitUntilReady();
            assertEquals(List.of(
                    "(1) Le titre de certaines pages est calculé. Filtrer et trier sur ces titres ne fonctionnera pas normalement pour ces pages."),
                liveDataElement.getFootnotesText());
        } finally {
            testUtils.setWikiPreference("default_language", "en");
        }
    }

    private void createXObjects(TestUtils testUtils, DocumentReference testReference)
    {
        String className = testUtils.serializeReference(testReference);
        DocumentReference o1 = new DocumentReference("O1", (SpaceReference) testReference.getParent());
        testUtils.createPage(o1, "", "O1");
        addXObject(testUtils, o1, className, NAME_LYNDA, CHOICE_A, "U1", true);
        DocumentReference o2 = new DocumentReference("O2", (SpaceReference) testReference.getParent());
        // Make 02 not viewable by guests to test the footnotes.
        testUtils.setRights(o2, null, "XWiki.XWikiGuest", "view", false);
        addXObject(testUtils, o2, className, NAME_ESTHER, CHOICE_B, "U2", false);
        DocumentReference o3 = new DocumentReference("O3", (SpaceReference) testReference.getParent());
        // Set a localized title on O3 to test the footnotes.
        testUtils.createPage(o3, "", "$services.localization.render('computedTitle')");
        addXObject(testUtils, o3, className, NAME_NIKOLAY, "", null, false);
    }

    /**
     * @since 12.10.9
     */
    @Test
    @Order(2)
    void livedataLivetableTableLayoutResultPage(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // Login as super admin because guest user cannot remove pages.
        testUtils.loginAsSuperAdmin();
        // Wipes the test space.
        testUtils.deletePage(testReference, true);

        DocumentReference resultPageDocumentReference =
            new DocumentReference("resultPage", testReference.getLastSpaceReference());

        initResultPage(testUtils, resultPageDocumentReference);

        String resultPage = testUtils.serializeReference(resultPageDocumentReference).replaceFirst("xwiki:", "");

        createResultPageLiveDataPage(testUtils, testReference, resultPage);

        testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("count", "1");
        tableLayout.assertRow("label", "first result");

        assertEquals(Set.of("100", "25", "15", "50"), tableLayout.getPaginationSizes());
    }

    @Test
    @Order(3)
    void asyncAction(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference.getLastSpaceReference(), true);

        List<String> properties = List.of(NAME_COLUMN, ACTIONS_COLUMN);
        createClassNameLiveDataPage(testUtils, testReference, properties, """
            {
              "meta": {
                "actions": [{
                    "id": "delete",
                    "async": {
                      "httpMethod": "POST",
                      "loadingMessage": "Loading",
                      "successMessage": "Delete Success",
                      "failureMessage": "Failed",
                      "body": "newBacklinkTarget=&updateLinks=false&autoRedirect=false&form_token=${services.csrf.token}&confirm=1&async=false",
                      "headers": {
                        "Content-Type": "application/x-www-form-urlencoded"
                      }
                    }
                  }]
              }
            }
            """);
        createXClass(testUtils, testReference);
        createXObjects(testUtils, testReference);
        ViewPage viewPage = testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        assertEquals(3, tableLayout.countRows());
        tableLayout.clickAction(1, "delete");
        viewPage.waitForNotificationSuccessMessage("Delete Success");
        tableLayout.waitUntilRowCountEqualsTo(2);
        assertEquals(2, tableLayout.countRows());
    }

    @Test
    @Order(4)
    void filterWithRelationalDBList(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference, true);

        ViewPage viewPage = testUtils.createPage(testReference, "My Class", "");
        ClassEditPage classEditPage = viewPage.editClass();
        classEditPage.addProperty("myList", "DBList");
        DatabaseListClassEditElement dbListClassEditElement =
            classEditPage.getDatabaseListClassEditElement("myList");
        dbListClassEditElement.setMultiSelect(true);
        dbListClassEditElement.setMetaProperty("relationalStorage", "true");
        dbListClassEditElement.setMetaProperty("displayType", "input");
        classEditPage.clickSaveAndView();

        // Create a few XObjects with multiple selections and no selections.
        // First, first two options selected.
        DocumentReference doc1Ref = new DocumentReference("Doc1", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc1Ref, testUtils.serializeReference(testReference), "myList", "Option1|Option2");
        // Second, third option selected.
        DocumentReference doc2Ref = new DocumentReference("Doc2", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc2Ref, testUtils.serializeReference(testReference), "myList", "Option3");
        // Third, no option selected.
        DocumentReference doc3Ref = new DocumentReference("Doc3", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc3Ref, testUtils.serializeReference(testReference));

        // Create a Live Data page to list those objects.
        List<String> properties = List.of("doc.name", "myList");
        createClassNameLiveDataPage(testUtils, testReference, properties, "");

        testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(3);

        // Filter by Option1 should return only Doc1.
        tableLayout.filterColumn("myList", "Option1", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("myList", "Option1 Option2");
        // Filter by Option3 should return only Doc2.
        tableLayout.filterColumn("myList", "Option3", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("myList", "Option3");
    }

    /**
     * Test filtering with a StaticList property that has multiple selection enabled but doesn't use relational
     * storage. This uses StringListProperty which stores values in a CLOB column on Oracle.
     */
    @Test
    @Order(5)
    void filterWithNonRelationalStaticList(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference, true);

        ViewPage viewPage = testUtils.createPage(testReference, "My Class", "");
        ClassEditPage classEditPage = viewPage.editClass();
        classEditPage.addProperty("myList", "StaticList");
        StaticListClassEditElement staticListClassEditElement =
            classEditPage.getStaticListClassEditElement("myList");
        staticListClassEditElement.setMultiSelect(true);

        staticListClassEditElement.setMetaProperty("displayType", "input");
        // Don't configure any static values to explicitly test the used values fetching.
        classEditPage.clickSaveAndView();

        // Create a few XObjects with multiple selections and no selections.
        // First, the first two options are selected.
        DocumentReference doc1Ref = new DocumentReference("Doc1", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc1Ref, testUtils.serializeReference(testReference), "myList", "Option1|Option2");
        // Second, third option selected.
        DocumentReference doc2Ref = new DocumentReference("Doc2", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc2Ref, testUtils.serializeReference(testReference), "myList", "Option3");
        // Third, no option selected.
        DocumentReference doc3Ref = new DocumentReference("Doc3", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc3Ref, testUtils.serializeReference(testReference));
        // Third, many options selected to test large CLOB value filtering.
        DocumentReference doc4Ref = new DocumentReference("Doc4", testReference.getLastSpaceReference());
        StringJoiner manyOptions = new StringJoiner("|");
        for (int i = 1; i <= 1000; i++) {
            manyOptions.add("Option" + i);
        }
        testUtils.rest().addObject(doc4Ref, testUtils.serializeReference(testReference), "myList",
            manyOptions.toString());

        // Create a Live Data page to list those objects.
        List<String> properties = List.of("doc.name", "myList");
        createClassNameLiveDataPage(testUtils, testReference, properties, "");

        testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(4);

        // Filter by Option1 should return Doc1 and Doc4.
        tableLayout.filterColumn("myList", "Option1", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(2);
        tableLayout.assertRow("myList", "Option1 Option2");
        tableLayout.assertRow("doc.name", "Doc4");

        // Filter by Option3 should return Doc2 and Doc4.
        tableLayout.filterColumn("myList", "Option3", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(2);
        tableLayout.assertRow("myList", "Option3");
        tableLayout.assertRow("doc.name", "Doc4");

        // Filter by (empty) should return only Doc3.
        tableLayout.filterColumn("myList", CHOICE_EMPTY, true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("doc.name", "Doc3");

        // Filter by Option100 should return only Doc4.
        tableLayout.filterColumn("myList", "Option100", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("doc.name", "Doc4");
    }

    /**
     * Test filtering with a single-select StringProperty. This is stored in a VARCHAR column for small values or CLOB
     * for large values (on Oracle).
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Order(6)
    void filterWithSingleSelectStringProperty(boolean largeStorage, TestUtils testUtils, TestReference testReference)
        throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference, true);

        ViewPage viewPage = testUtils.createPage(testReference, "My Class", "");
        ClassEditPage classEditPage = viewPage.editClass();
        classEditPage.addProperty("myList", "StaticList");
        StaticListClassEditElement staticListClassEditElement =
            classEditPage.getStaticListClassEditElement("myList");
        // Single select - this will use StringProperty.
        staticListClassEditElement.setMultiSelect(false);
        staticListClassEditElement.setMetaProperty("displayType", "input");
        // Using large storage will make the property use CLOB storage on databases that differentiate between
        // VARCHAR and CLOB (like Oracle).
        staticListClassEditElement.setMetaProperty("largeStorage", Boolean.toString(largeStorage));
        // Don't configure any static values to explicitly test the used values fetching.
        classEditPage.clickSaveAndView();

        // Create a few XObjects with different selections.
        DocumentReference doc1Ref = new DocumentReference("Doc1", testReference.getLastSpaceReference());
        testUtils.rest().addObject(doc1Ref, testUtils.serializeReference(testReference), "myList", "Option1");
        DocumentReference doc2Ref = new DocumentReference("Doc2", testReference.getLastSpaceReference());
        String option2 = "Option2";
        if (largeStorage) {
            // Use a large value to test that it actually works with a very large value with more than 4k characters.
            option2 += StringUtils.repeat("A", 5000);
        }
        testUtils.rest().addObject(doc2Ref, testUtils.serializeReference(testReference), "myList", option2);

        DocumentReference doc3Ref = new DocumentReference("Doc3", testReference.getLastSpaceReference());
        String option3 = "Option3";
        if (largeStorage) {
            // Use a large value that has less than 4k characters but more than 4k bytes.
            // We don't just append Unicode characters because this would make the URI too large when filtering the Live
            // Data as the filters are passed in the URL (should be fixed in the future). So we append a mix of 3.6k
            // ASCII and 200 Unicode characters.
            option3 += StringUtils.repeat("B", 3600) + StringUtils.repeat("⟷", 200);
            // Sanity check to ensure we have more than 4k bytes.
            assertTrue(option3.getBytes().length > 4000);
        }
        testUtils.rest().addObject(doc3Ref, testUtils.serializeReference(testReference), "myList", option3);

        // Create a Live Data page to list those objects.
        List<String> properties = List.of("doc.name", "myList");
        createClassNameLiveDataPage(testUtils, testReference, properties, "");

        testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(3);

        // Filter by Option1 should return only Doc1.
        tableLayout.filterColumn("myList", "Option1", true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("doc.name", "Doc1");

        // For the long values, we only use a part of the value to filter.
        // Users won't type such long values in a real filter - the long value support is mostly for storing several
        // values in the multiselect scenario, not really for single select.
        String option2Filter = largeStorage ? option2.substring(0, 20) : option2;
        // We can't filter directly using filterColumn as it expects an exact match. So we use the SuggestInputElement
        // directly.
        SuggestInputElement suggestInputElement = new SuggestInputElement(tableLayout.getFilter("myList"));
        suggestInputElement.clearSelectedSuggestions().sendKeys(option2Filter).waitForNonTypedSuggestions();
        suggestInputElement.selectByVisibleText(option2);
        // Filter by Option2 should return only Doc2.
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("doc.name", "Doc2");

        String option3Filter = largeStorage ? option3.substring(0, 20) : option3;
        // Filter by Option3 should return only Doc3.
        suggestInputElement.clearSelectedSuggestions().sendKeys(option3Filter).waitForNonTypedSuggestions();
        suggestInputElement.selectByVisibleText(option3);
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow("doc.name", "Doc3");
    }

    private void initLocalization(TestUtils testUtils, DocumentReference testReference) throws Exception
    {
        DocumentReference translationDocumentReference =
            new DocumentReference("Translation", testReference.getLastSpaceReference());
        testUtils.addObject(translationDocumentReference, "XWiki.TranslationDocumentClass",
            singletonMap("scope", "WIKI"));
        String choiceTTranslationKey =
            StringUtils.joinWith("_", testUtils.serializeReference(testReference.getLocalDocumentReference()),
                CHOICE_COLUMN, CHOICE_T);
        testUtils.rest().savePage(translationDocumentReference,
            "emptyvalue=\ncomputedTitle=O3\n" + choiceTTranslationKey + "=" + CHOICE_T_TRANSLATION,
            "translation");
    }

    /**
     * Creates an XObject of type {@code className} and stores it in {@code documentReference}.
     *
     * @param testUtils the {@link TestUtils} instance of the test
     * @param documentReference the reference of the document storing the created XObject
     * @param className the type of the created XObject
     * @param name the value of the name field
     * @param choice the value of the choice field
     * @param username the username of the user field (e.g., {@code "U1"})
     */
    private void addXObject(TestUtils testUtils, DocumentReference documentReference, String className, String name,
        String choice, String username, boolean isActive)
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put(NAME_COLUMN, name);
        properties.put(CHOICE_COLUMN, choice);
        if (username != null) {
            properties.put(USER_COLUMN, "XWiki." + username);
        }
        properties.put(IS_ACTIVE_COLUMN, isActive);
        testUtils.addObject(documentReference, className, properties);
    }

    private void createXClass(TestUtils testUtils, DocumentReference testReference)
    {
        testUtils.addClassProperty(testReference, NAME_COLUMN, "String");
        testUtils.addClassProperty(testReference, CHOICE_COLUMN, "StaticList");
        ClassEditPage classEditPage = new ClassEditPage();
        StaticListClassEditElement propertyList = classEditPage.getStaticListClassEditElement(CHOICE_COLUMN);
        StringJoiner choices = new StringJoiner("|");
        choices.add(CHOICE_A);
        choices.add(CHOICE_B);
        choices.add(CHOICE_C);
        choices.add(CHOICE_D);
        // Add padding values to ensure that the last values are not initially loaded
        IntStream.range(10, 20).mapToObj(i -> "padding" + i).forEach(choices::add);
        choices.add(StringUtils.joinWith("=", CHOICE_L, CHOICE_L_LABEL));
        choices.add(CHOICE_T);
        propertyList.setValues(choices.toString());
        classEditPage.clickSaveAndView();
        testUtils.addClassProperty(testReference, BIRTHDAY_COLUMN, "Date");
        testUtils.addClassProperty(testReference, USER_COLUMN, "Users");
        testUtils.addClassProperty(testReference, IS_ACTIVE_COLUMN, "Boolean");
    }

    private static void createClassNameLiveDataPage(TestUtils testUtils, DocumentReference testReference,
        List<String> properties, String body) throws Exception
    {
        TestUtils.RestTestUtils rest = testUtils.rest();
        Page page = rest.page(testReference);
        page.setContent("{{velocity}}\n"
            + "{{liveData\n"
            + "  id=\"test\"\n"
            + "  properties=\"" + String.join(",", properties) + "\"\n"
            + "  limit=\"5\"\n"
            + "  source=\"liveTable\"\n"
            + "  sourceParameters=\"translationPrefix=&className=" + testUtils.serializeReference(
            testReference.getLocalDocumentReference()) + "\"\n"
            + "}}" + body + "{{/liveData}}\n"
            + "{{/velocity}}");
        rest.save(page);
    }

    private void createResultPageLiveDataPage(TestUtils testUtils, DocumentReference testReference, String resultPage)
        throws Exception
    {
        TestUtils.RestTestUtils rest = testUtils.rest();
        Page page = rest.page(testReference);
        page.setContent("{{velocity}}\n"
            + "#set ($liveDataConfig={'meta': {'propertyDescriptors':[{'id': 'count', 'type': 'Number'}]}})\n"
            + "\n"
            + "{{liveData\n"
            + "  id=\"test\"\n"
            + "  properties=\"count,label\"\n"
            + "  source=\"liveTable\"\n"
            + "  sourceParameters=\"translationPrefix=&resultPage=" + resultPage + "\"\n"
            + "}}$jsontool.serialize($liveDataConfig){{/liveData}}\n"
            + "{{/velocity}}");
        rest.save(page);
    }

    private void initResultPage(TestUtils testUtils, DocumentReference resultPageDocumentReference)
        throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("totalrows", 1);
        objectNode.put("returnedrows", 1);
        objectNode.put("offset", 1);
        objectNode.put("reqNo", 1);
        ArrayNode rows = objectNode.putArray("rows");
        ObjectNode jsonNodes = rows.addObject();
        jsonNodes.put("count", 1);
        jsonNodes.put("label", "first result");
        testUtils.createPage(resultPageDocumentReference, objectMapper.writeValueAsString(objectNode));
    }
}

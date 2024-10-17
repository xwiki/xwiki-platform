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
package org.xwiki.rest.test;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Spaces;
import org.xwiki.rest.model.jaxb.Translation;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.pages.PageChildrenResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.rest.test.framework.TestConstants;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PageResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private String space;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    private String childPageName;

    private DocumentReference childReference;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.wikiName = getWiki();
        this.space = getTestClassName();
        this.spaces = List.of(this.space);
        this.pageName = getTestMethodName();
        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        this.childPageName = "child";
        this.childReference = new DocumentReference(this.wikiName, this.spaces, this.childPageName);

        // Create a clean test page.
        this.testUtils.rest().delete(this.reference);
        Page page = this.testUtils.rest().page(this.reference);
        page.setComment("Test page");
        this.testUtils.rest().save(page);

        // Create a clean test page child.
        this.testUtils.rest().delete(this.childReference);
        Page childPage = this.testUtils.rest().page(this.childReference);
        childPage.setComment("Test page child");
        childPage.setParent(this.space + '.' + this.pageName);
        this.testUtils.rest().save(childPage);
    }

    private Page getFirstPage() throws Exception
    {
        CloseableHttpResponse response = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Wikis wikis = (Wikis) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(wikis.getWikis().size() > 0);
        Wiki wiki = wikis.getWikis().get(0);

        // Get a link to an index of spaces (http://localhost:8080/xwiki/rest/wikis/xwiki/spaces)
        Link spacesLink = getFirstLinkByRelation(wiki, Relations.SPACES);
        Assert.assertNotNull(spacesLink);
        response = executeGet(spacesLink.getHref());
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        Spaces spaces = (Spaces) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(spaces.getSpaces().size() > 0);

        Space space = null;
        for (final Space s : spaces.getSpaces()) {
            if (this.space.equals(s.getName())) {
                space = s;
                break;
            }
        }

        // get the pages list for the space
        // eg: http://localhost:8080/xwiki/rest/wikis/xwiki/spaces/Main/pages
        Link pagesInSpace = getFirstLinkByRelation(space, Relations.PAGES);
        Assert.assertNotNull(pagesInSpace);
        response = executeGet(pagesInSpace.getHref());
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        Pages pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(pages.getPageSummaries().size() > 0);

        Link pageLink = null;
        for (final PageSummary ps : pages.getPageSummaries()) {
            if (this.pageName.equals(ps.getName())) {
                pageLink = getFirstLinkByRelation(ps, Relations.PAGE);
                Assert.assertNotNull(pageLink);
                break;
            }
        }
        Assert.assertNotNull(pageLink);

        response = executeGet(pageLink.getHref());
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        return (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        Page page = getFirstPage();

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        Assert.assertNotNull(link);

        checkLinks(page);
    }

    @Test
    public void whiteSpaceEncoding() throws Exception
    {
        LocalDocumentReference localDocumentReference = new LocalDocumentReference("Space", "Page with space");

        this.testUtils.rest().savePage(localDocumentReference);
        Page page = this.testUtils.rest().get(localDocumentReference);

        assertEquals("Page with space", page.getName());

        // Make sure that the page can be accessed with the white space characters encoded as +
        URI uri = new URI(getBaseURL() + "/wikis/xwiki/spaces/Space/pages/Page+with+space");
        CloseableHttpResponse response = this.testUtils.rest().executeGet(uri);

        assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        try (InputStream stream = response.getEntity().getContent()) {
            page = this.testUtils.rest().toResource(stream);

            assertEquals("Page with space", page.getName());
        }
    }

    @Test
    public void testGETNotExistingPage() throws Exception
    {
        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), Arrays.asList("NOTEXISTING"), "NOTEXISTING"));
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testPUTGETPage() throws Exception
    {
        final String title = String.format("Title (%s)", UUID.randomUUID());
        final String content = String.format("This is a content (%d)", System.currentTimeMillis());
        final String comment = String.format("Updated title and content (%d)", System.currentTimeMillis());

        Page originalPage = getFirstPage();

        Page newPage = this.objectFactory.createPage();
        newPage.setTitle(title);
        newPage.setContent(content);
        newPage.setComment(comment);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        Assert.assertNotNull(link);

        // PUT
        CloseableHttpResponse response = executePutXml(link.getHref(), newPage,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());

        // GET
        response = executeGet(link.getHref());
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());
    }

    @Test
    public void testPUTGETWithObject() throws Exception
    {
        String pageURI = buildURI(PageResource.class, getWiki(), List.of("RESTTest"), "PageWithObject");

        final String title = String.format("Title (%s)", UUID.randomUUID());
        final String content = String.format("This is a content (%d)", System.currentTimeMillis());
        final String comment = String.format("Updated title and content (%d)", System.currentTimeMillis());

        Page newPage = this.objectFactory.createPage();
        newPage.setTitle(title);
        newPage.setContent(content);
        newPage.setComment(comment);

        // Add object
        final String TAG_VALUE = "TAG";
        Property property = new Property();
        property.setName("tags");
        property.setValue(TAG_VALUE);
        Object object = this.objectFactory.createObject();
        object.setClassName("XWiki.TagClass");
        object.getProperties().add(property);
        newPage.setObjects(this.objectFactory.createObjects());
        newPage.getObjects().getObjectSummaries().add(object);

        // PUT
        CloseableHttpResponse response = executePutXml(pageURI, newPage,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertThat(getHttpResponseInfo(response), response.getCode(),
            isIn(Arrays.asList(HttpStatus.SC_ACCEPTED, HttpStatus.SC_CREATED)));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());

        // GET
        response = executeGet(pageURI + "?objects=true");
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());

        Assert.assertEquals(TAG_VALUE,
            getProperty((Object) modifiedPage.getObjects().getObjectSummaries().get(0), "tags").getValue());

        // Send again but with empty object list

        modifiedPage.getObjects().getObjectSummaries().clear();

        // PUT
        response = executePutXml(pageURI, modifiedPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertThat(getHttpResponseInfo(response), response.getCode(), isIn(Arrays.asList(HttpStatus.SC_ACCEPTED)));

        modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());

        // GET
        response = executeGet(pageURI + "?objects=true");
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(title, modifiedPage.getTitle());
        Assert.assertEquals(content, modifiedPage.getContent());
        Assert.assertEquals(comment, modifiedPage.getComment());

        Assert.assertTrue(modifiedPage.getObjects().getObjectSummaries().isEmpty());
    }

    public Property getProperty(Object object, String propertyName)
    {
        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }

        return null;
    }

    @Test
    public void testPUTPageWithTextPlain() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        Assert.assertNotNull(link);

        CloseableHttpResponse response = executePut(link.getHref(), CONTENT, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(CONTENT, modifiedPage.getContent());
    }

    @Test
    public void testPUTPageUnauthorized() throws Exception
    {
        Page page = getFirstPage();
        page.setContent("New content");

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        Assert.assertNotNull(link);

        CloseableHttpResponse putMethod = executePutXml(link.getHref(), page);
        Assert.assertEquals(getHttpResponseInfo(putMethod), HttpStatus.SC_UNAUTHORIZED, putMethod.getCode());
    }

    @Test
    public void testPUTNonExistingPage() throws Exception
    {
        final List<String> SPACE_NAME = List.of("Test");
        final String PAGE_NAME = String.format("Test-%d", System.currentTimeMillis());
        final String CONTENT = String.format("Content %d", System.currentTimeMillis());
        final String TITLE = String.format("Title %d", System.currentTimeMillis());
        final String PARENT = "Main.WebHome";

        Page page = this.objectFactory.createPage();
        page.setContent(CONTENT);
        page.setTitle(TITLE);
        page.setParent(PARENT);

        CloseableHttpResponse response = executePutXml(buildURI(PageResource.class, getWiki(), SPACE_NAME, PAGE_NAME),
            page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_CREATED, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(CONTENT, modifiedPage.getContent());
        Assert.assertEquals(TITLE, modifiedPage.getTitle());

        Assert.assertEquals(PARENT, modifiedPage.getParent());
    }

    /**
     * Note that logs output are expected related to this test with a stacktrace, but we cannot capture it easily in the
     * test, since it's not an exception which is directly thrown in the current thread. The stacktrace appear because
     * of an event triggered which is then captured by {@link javax.xml.bind.helpers.DefaultValidationEventHandler}
     * which immediately output the message.
     */
    @Test
    public void testPUTWithInvalidRepresentation() throws Exception
    {
        Page page = getFirstPage();
        Link link = getFirstLinkByRelation(page, Relations.SELF);

        CloseableHttpResponse putMethod = executePut(link.getHref(),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><invalidPage><content/></invalidPage>", MediaType.TEXT_XML);
        Assert.assertEquals(getHttpResponseInfo(putMethod), HttpStatus.SC_BAD_REQUEST, putMethod.getCode());

        this.validateConsole.getLogCaptureConfiguration()
            .registerExpected("unexpected element (uri:\"\", local:\"invalidPage\"). Expected elements are");
    }

    @Test
    public void testPUTGETTranslation() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, "Translations");

        // PUT
        String[] languages = Locale.getISOLanguages();
        final String languageId = languages[this.random.nextInt(languages.length)];

        Page page = this.objectFactory.createPage();
        page.setContent(languageId);

        CloseableHttpResponse response = executePutXml(
            buildURI(PageTranslationResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME, languageId),
            page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_CREATED, response.getCode());

        // GET
        response = executeGet(buildURI(PageTranslationResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
            TestConstants.TRANSLATIONS_PAGE_NAME, languageId));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        // Some of the language codes returned by Locale#getISOLanguages() are deprecated and Locale's constructors map
        // the new codes to the old ones which means the language code we have submitted can be different than the
        // actual language code used when the Locale object is created on the server side. Let's go through the Locale
        // constructor to be safe.
        String expectedLanguage = LocaleUtils.toLocale(languageId).getLanguage();
        Assert.assertEquals(expectedLanguage, modifiedPage.getLanguage());
        Assert.assertTrue(modifiedPage.getTranslations().getTranslations().size() > 0);

        for (Translation translation : modifiedPage.getTranslations().getTranslations()) {
            response = executeGet(getFirstLinkByRelation(translation, Relations.PAGE).getHref());
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

            modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

            Assert.assertEquals(modifiedPage.getLanguage(), translation.getLanguage());

            checkLinks(translation);
        }
    }

    @Test
    public void testGETNotExistingTranslation() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, "Translations");

        CloseableHttpResponse response = executeGet(buildURI(PageResource.class, getWiki(),
            TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        response = executeGet(buildURI(PageTranslationResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
            TestConstants.TRANSLATIONS_PAGE_NAME, "NOT_EXISTING"));
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testDELETEPage() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, this.pageName, "Test page");

        CloseableHttpResponse deleteMethod =
            executeDelete(buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(deleteMethod), HttpStatus.SC_NO_CONTENT, deleteMethod.getCode());

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testDELETEPageNoRights() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, this.pageName, "Test page");

        CloseableHttpResponse deleteMethod =
            executeDelete(buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        Assert.assertEquals(getHttpResponseInfo(deleteMethod), HttpStatus.SC_UNAUTHORIZED, deleteMethod.getCode());

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
    }

    @Test
    public void testPageHistory() throws Exception
    {
        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));

        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Page originalPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertEquals(this.spaces.get(0), originalPage.getSpace());

        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), this.spaces, originalPage.getName());

        response = executeGet(pageHistoryUri);
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        History history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());

        HistorySummary firstVersion = null;
        for (HistorySummary historySummary : history.getHistorySummaries()) {
            if ("1.1".equals(historySummary.getVersion())) {
                firstVersion = historySummary;
            }

            response = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

            Page page = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

            checkLinks(page);

            for (Translation translation : page.getTranslations().getTranslations()) {
                checkLinks(translation);
            }
        }

        Assert.assertNotNull(firstVersion);
        Assert.assertEquals("Test page", firstVersion.getComment());
    }

    @Test
    public void testPageTranslationHistory() throws Exception
    {
        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
            TestConstants.TRANSLATIONS_PAGE_NAME);

        CloseableHttpResponse response = executeGet(pageHistoryUri);
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        History history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            response = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

            Page page = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

            checkLinks(page);
            checkLinks(page.getTranslations());
        }
    }

    @Test
    public void testGETPageChildren() throws Exception
    {
        CloseableHttpResponse response =
            executeGet(buildURI(PageChildrenResource.class, getWiki(), this.spaces, this.pageName));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Pages pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(pages.getPageSummaries().size() > 0);

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }
    }

    @Test
    public void testPOSTPageFormUrlEncoded() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        Assert.assertNotNull(link);

        List<NameValuePair> nameValuePairs =
            List.of(new BasicNameValuePair("title", TITLE), new BasicNameValuePair("content", CONTENT));

        CloseableHttpResponse response = executePostForm(String.format("%s?method=PUT", link.getHref()), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(CONTENT, modifiedPage.getContent());
        Assert.assertEquals(TITLE, modifiedPage.getTitle());
    }

    @Test
    public void testPOSTPageFormUrlEncodedNoCSRF() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        Assert.assertNotNull(link);

        List<NameValuePair> nameValuePairs =
            List.of(new BasicNameValuePair("title", TITLE), new BasicNameValuePair("content", CONTENT));

        CloseableHttpResponse response = executePostForm(String.format("%s?method=PUT", link.getHref()), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_FORBIDDEN, response.getCode());
        Assert.assertEquals("Invalid or missing form token.", EntityUtils.toString(response.getEntity()));

        // Assert that the page hasn't been modified.
        response = executeGet(link.getHref());
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(originalPage.getContent(), modifiedPage.getContent());
        Assert.assertEquals(originalPage.getTitle(), modifiedPage.getTitle());
    }

    @Test
    public void testPUTPageSyntax() throws Exception
    {
        Page originalPage = getFirstPage();

        // Use the plain/1.0 syntax since we are sure that the test page does not already use it.
        String newSyntax = "plain/1.0";

        originalPage.setSyntax(newSyntax);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        Assert.assertNotNull(link);

        CloseableHttpResponse response = executePutXml(link.getHref(), originalPage,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(newSyntax, modifiedPage.getSyntax());
    }

    @Test
    public void testPageChildrenResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create a parent page and two children
        String spaceName = getTestClassName();
        String parentPage = getTestMethodName() + "Parent";
        String child1 = getTestMethodName() + "ChildA";
        String child2 = getTestMethodName() + "ChildB";
        DocumentReference parentRef = new DocumentReference(getWiki(), spaceName, parentPage);
        DocumentReference childRef1 = new DocumentReference(getWiki(), spaceName, child1);
        DocumentReference childRef2 = new DocumentReference(getWiki(), spaceName, child2);
        try {
            this.testUtils.rest().delete(parentRef);
            this.testUtils.rest().delete(childRef1);
            this.testUtils.rest().delete(childRef2);
            this.testUtils.rest().savePage(parentRef, "parent content", "parent title");
            Page childPageObj1 = this.testUtils.rest().page(childRef1);
            childPageObj1.setParent(spaceName + "." + parentPage);
            childPageObj1.setContent("child1 content");
            childPageObj1.setTitle("child1 title");
            this.testUtils.rest().save(childPageObj1);
            Page childPageObj2 = this.testUtils.rest().page(childRef2);
            childPageObj2.setParent(spaceName + "." + parentPage);
            childPageObj2.setContent("child2 content");
            childPageObj2.setTitle("child2 title");
            this.testUtils.rest().save(childPageObj2);

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(
                "%s?number=-1".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet(
                "%s?number=1001".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet(
                "%s?number=1".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, pages.getPageSummaries().size());

            String firstName = pages.getPageSummaries().get(0).getName();

            // Test: pagination with number=1 and start=1
            response = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            Assert.assertNotEquals(firstName, pages.getPageSummaries().get(0).getName());
        } finally {
            this.testUtils.rest().delete(parentRef);
            this.testUtils.rest().delete(childRef1);
            this.testUtils.rest().delete(childRef2);
        }
    }

    @Test
    public void testPageHistoryResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create a page and several versions
        try {
            this.testUtils.rest().delete(this.reference);
            this.testUtils.rest().savePage(this.reference, "v1", "title1");
            this.testUtils.rest().savePage(this.reference, "v2", "title2");
            this.testUtils.rest().savePage(this.reference, "v3", "title3");

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(
                "%s?number=-1".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet(
                "%s?number=1001".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet(
                "%s?number=1".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            History history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, history.getHistorySummaries().size());

            String firstVersion = history.getHistorySummaries().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            response = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, history.getHistorySummaries().size());
            Assert.assertNotEquals(firstVersion, history.getHistorySummaries().get(0).getVersion());
        } finally {
            this.testUtils.rest().delete(this.reference);
        }
    }
}

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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PageResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private String space;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    private String childPageName;

    private DocumentReference childReference;

    @BeforeEach
    @Override
    protected void setUp(TestUtils setup, TestInfo info) throws Exception
    {
        super.setUp(setup, info);

        this.wikiName = getWiki();
        this.space = getTestClassName();
        this.spaces = List.of(this.space);
        this.pageName = getTestMethodName();
        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        this.childPageName = "child";
        this.childReference = new DocumentReference(this.wikiName, this.spaces, this.childPageName);

        // Create a clean test page.
        getUtil().rest().delete(this.reference);
        Page page = getUtil().rest().page(this.reference);
        page.setComment("Test page");
        getUtil().rest().save(page);

        // Create a clean test page child.
        getUtil().rest().delete(this.childReference);
        Page childPage = getUtil().rest().page(this.childReference);
        childPage.setComment("Test page child");
        childPage.setParent(this.space + '.' + this.pageName);
        getUtil().rest().save(childPage);
    }

    private Page getFirstPage() throws Exception
    {
        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Wikis wikis = (Wikis) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(!wikis.getWikis().isEmpty());
        Wiki wiki = wikis.getWikis().get(0);

        // Get a link to an index of spaces (http://localhost:8080/xwiki/rest/wikis/xwiki/spaces)
        Link spacesLink = getFirstLinkByRelation(wiki, Relations.SPACES);
        assertNotNull(spacesLink);
        getMethod = executeGet(spacesLink.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        Spaces spaces = (Spaces) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(!spaces.getSpaces().isEmpty());

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
        assertNotNull(pagesInSpace);
        getMethod = executeGet(pagesInSpace.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(!pages.getPageSummaries().isEmpty());

        Link pageLink = null;
        for (final PageSummary ps : pages.getPageSummaries()) {
            if (this.pageName.equals(ps.getName())) {
                pageLink = getFirstLinkByRelation(ps, Relations.PAGE);
                assertNotNull(pageLink);
                break;
            }
        }
        assertNotNull(pageLink);

        getMethod = executeGet(pageLink.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        return page;
    }

    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
        Page page = getFirstPage();

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        assertNotNull(link);

        checkLinks(page);
    }

    @Test
    void whiteSpaceEncoding() throws Exception
    {
        LocalDocumentReference localDocumentReference = new LocalDocumentReference("Space", "Page with space");

        getUtil().rest().savePage(localDocumentReference);
        Page page = getUtil().rest().get(localDocumentReference);

        assertEquals("Page with space", page.getName());

        // Make sure that the page can be accessed with the white space characters encoded as +
        URI uri = new URI(getBaseURL() + "/wikis/xwiki/spaces/Space/pages/Page+with+space");
        GetMethod getMethod = getUtil().rest().executeGet(uri);

        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        try (InputStream stream = getMethod.getResponseBodyAsStream()) {
            page = getUtil().rest().toResource(stream);

            assertEquals("Page with space", page.getName());
        }
    }

    @Test
    void testGETNotExistingPage() throws Exception
    {
        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), List.of("NOTEXISTING"), "NOTEXISTING"));
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testPUTGETPage() throws Exception
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
        assertNotNull(link);

        // PUT
        PutMethod putMethod = executePutXml(link.getHref(), newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        // GET
        GetMethod getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());
    }

    @Test
    void testPUTGETWithObject() throws Exception
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
        PutMethod putMethod = executePutXml(pageURI, newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertThat(getHttpMethodInfo(putMethod), putMethod.getStatusCode(),
            isIn(Arrays.asList(HttpStatus.SC_ACCEPTED, HttpStatus.SC_CREATED)));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        // GET
        GetMethod getMethod = executeGet(pageURI + "?objects=true");
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        assertEquals(TAG_VALUE, getProperty((Object) modifiedPage.getObjects().getObjectSummaries().get(0), "tags").getValue());

        // Send again but with empty object list

        modifiedPage.getObjects().getObjectSummaries().clear();

        // PUT
        putMethod = executePutXml(pageURI, modifiedPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertThat(getHttpMethodInfo(putMethod), putMethod.getStatusCode(),
            isIn(List.of(HttpStatus.SC_ACCEPTED)));

        modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        // GET
        getMethod = executeGet(pageURI + "?objects=true");
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        assertTrue(modifiedPage.getObjects().getObjectSummaries().isEmpty());
    }

    private Property getProperty(Object object, String propertyName)
    {
        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }

        return null;
    }

    @Test
    void testPUTPageWithTextPlain() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePut(link.getHref(), CONTENT, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(CONTENT, modifiedPage.getContent());
    }

    @Test
    void testPUTPageUnauthorized() throws Exception
    {
        Page page = getFirstPage();
        page.setContent("New content");

        Link link = getFirstLinkByRelation(page, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePutXml(link.getHref(), page);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));
    }

    @Test
    void testPUTNonExistingPage() throws Exception
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

        PutMethod putMethod = executePutXml(buildURI(PageResource.class, getWiki(), SPACE_NAME, PAGE_NAME),
            page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(CONTENT, modifiedPage.getContent());
        assertEquals(TITLE, modifiedPage.getTitle());

        assertEquals(PARENT, modifiedPage.getParent());
    }

    /**
     * Note that logs output are expected related to this test with a stacktrace, but we cannot capture it easily
     * in the test, since it's not an exception which is directly thrown in the current thread.
     * The stacktrace appear because of an event triggered which is then captured by
     * {@link javax.xml.bind.helpers.DefaultValidationEventHandler} which immediately output the message.
     */
    @Test
    void testPUTWithInvalidRepresentation(LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        Page page = getFirstPage();
        Link link = getFirstLinkByRelation(page, Relations.SELF);

        PutMethod putMethod = executePut(link.getHref(),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><invalidPage><content/></invalidPage>", MediaType.TEXT_XML);
        assertEquals(HttpStatus.SC_BAD_REQUEST, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        logCaptureConfiguration.registerExpected(
            "unexpected element (uri:\"\", local:\"invalidPage\"). Expected elements are"
        );
    }

    @Test
    void testPUTGETTranslation() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, "Translations");

        // PUT
        String[] languages = Locale.getISOLanguages();
        final String languageId = languages[this.random.nextInt(languages.length)];

        Page page = this.objectFactory.createPage();
        page.setContent(languageId);

        PutMethod putMethod = executePutXml(
            buildURI(PageTranslationResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
                TestConstants.TRANSLATIONS_PAGE_NAME, languageId),
            page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        // GET
        GetMethod getMethod = executeGet(buildURI(PageTranslationResource.class, getWiki(),
            TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, languageId));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        // Some of the language codes returned by Locale#getISOLanguages() are deprecated and Locale's constructors map
        // the new codes to the old ones which means the language code we have submitted can be different than the
        // actual language code used when the Locale object is created on the server side. Let's go through the Locale
        // constructor to be safe.
        String expectedLanguage = LocaleUtils.toLocale(languageId).getLanguage();
        assertEquals(expectedLanguage, modifiedPage.getLanguage());
        assertTrue(!modifiedPage.getTranslations().getTranslations().isEmpty());

        for (Translation translation : modifiedPage.getTranslations().getTranslations()) {
            getMethod = executeGet(getFirstLinkByRelation(translation, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            assertEquals(modifiedPage.getLanguage(), translation.getLanguage());

            checkLinks(translation);
        }
    }

    @Test
    void testGETNotExistingTranslation() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME, "Translations");

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TRANSLATIONS_PAGE_NAME));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        getMethod = executeGet(buildURI(PageTranslationResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
            TestConstants.TRANSLATIONS_PAGE_NAME, "NOT_EXISTING"));
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testDELETEPage() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, this.pageName, "Test page");

        DeleteMethod deleteMethod = executeDelete(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode(), getHttpMethodInfo(deleteMethod));

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testDELETEPageNoRights() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, this.pageName, "Test page");

        DeleteMethod deleteMethod = executeDelete(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, deleteMethod.getStatusCode(), getHttpMethodInfo(deleteMethod));

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, this.pageName));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testPageHistory() throws Exception
    {
        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));

        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page originalPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(this.spaces.get(0), originalPage.getSpace());

        String pageHistoryUri =
            buildURI(PageHistoryResource.class, getWiki(), this.spaces, originalPage.getName());

        getMethod = executeGet(pageHistoryUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        History history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        HistorySummary firstVersion = null;
        for (HistorySummary historySummary : history.getHistorySummaries()) {
            if ("1.1".equals(historySummary.getVersion())) {
                firstVersion = historySummary;
            }

            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Page page = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(page);

            for (Translation translation : page.getTranslations().getTranslations()) {
                checkLinks(translation);
            }
        }

        assertNotNull(firstVersion);
        assertEquals("Test page", firstVersion.getComment());
    }

    @Test
    void testPageTranslationHistory() throws Exception
    {
        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
            TestConstants.TRANSLATIONS_PAGE_NAME);

        GetMethod getMethod = executeGet(pageHistoryUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        History history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Page page = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(page);
            checkLinks(page.getTranslations());
        }
    }

    @Test
    void testGETPageChildren() throws Exception
    {
        GetMethod getMethod =
            executeGet(buildURI(PageChildrenResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(!pages.getPageSummaries().isEmpty());

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }
    }

    @Test
    void testPOSTPageFormUrlEncoded() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new NameValuePair("title", TITLE);
        nameValuePairs[1] = new NameValuePair("content", CONTENT);

        PostMethod postMethod = executePostForm(String.format("%s?method=PUT", link.getHref()), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        assertEquals(CONTENT, modifiedPage.getContent());
        assertEquals(TITLE, modifiedPage.getTitle());
    }

    @Test
    void testPOSTPageFormUrlEncodedNoCSRF() throws Exception
    {
        final String CONTENT = String.format("This is a content (%d)", System.currentTimeMillis());
        final String TITLE = String.format("Title (%s)", UUID.randomUUID());

        Page originalPage = getFirstPage();

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new NameValuePair("title", TITLE);
        nameValuePairs[1] = new NameValuePair("content", CONTENT);

        PostMethod postMethod = executePostForm(String.format("%s?method=PUT", link.getHref()), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
        assertEquals("Invalid or missing form token.", postMethod.getResponseBodyAsString());

        // Assert that the page hasn't been modified.
        GetMethod getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(originalPage.getContent(), modifiedPage.getContent());
        assertEquals(originalPage.getTitle(), modifiedPage.getTitle());
    }

    @Test
    void testPUTPageSyntax() throws Exception
    {
        Page originalPage = getFirstPage();

        // Use the plain/1.0 syntax since we are sure that the test page does not already use it.
        String newSyntax = "plain/1.0";

        originalPage.setSyntax(newSyntax);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        PutMethod putMethod = executePutXml(link.getHref(), originalPage,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(newSyntax, modifiedPage.getSyntax());
    }

    @Test
    void testPageChildrenResourcePaginationAndErrors() throws Exception
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
            getUtil().rest().delete(parentRef);
            getUtil().rest().delete(childRef1);
            getUtil().rest().delete(childRef2);
            getUtil().rest().savePage(parentRef, "parent content", "parent title");
            Page childPageObj1 = getUtil().rest().page(childRef1);
            childPageObj1.setParent(spaceName + "." + parentPage);
            childPageObj1.setContent("child1 content");
            childPageObj1.setTitle("child1 title");
            getUtil().rest().save(childPageObj1);
            Page childPageObj2 = getUtil().rest().page(childRef2);
            childPageObj2.setParent(spaceName + "." + parentPage);
            childPageObj2.setContent("child2 content");
            childPageObj2.setTitle("child2 title");
            getUtil().rest().save(childPageObj2);

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                "%s?number=-1".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                "%s?number=1001".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                "%s?number=1".formatted(buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, pages.getPageSummaries().size());

            String firstName = pages.getPageSummaries().get(0).getName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageChildrenResource.class, getWiki(), spaceName, parentPage)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, pages.getPageSummaries().size());
            assertNotEquals(firstName, pages.getPageSummaries().get(0).getName());
        } finally {
            getUtil().rest().delete(parentRef);
            getUtil().rest().delete(childRef1);
            getUtil().rest().delete(childRef2);
        }
    }

    @Test
    void testPageHistoryResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create a page and several versions
        try {
            getUtil().rest().delete(this.reference);
            getUtil().rest().savePage(this.reference, "v1", "title1");
            getUtil().rest().savePage(this.reference, "v2", "title2");
            getUtil().rest().savePage(this.reference, "v3", "title3");

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                "%s?number=-1".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                "%s?number=1001".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                "%s?number=1".formatted(buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            History history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, history.getHistorySummaries().size());

            String firstVersion = history.getHistorySummaries().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageHistoryResource.class, getWiki(), this.space, this.pageName)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, history.getHistorySummaries().size());
            assertNotEquals(firstVersion, history.getHistorySummaries().get(0).getVersion());
        } finally {
            getUtil().rest().delete(this.reference);
        }
    }

    @Test
    void testGETRenderedContent() throws Exception
    {
        long time = System.currentTimeMillis();
        final String title = String.format("Title (%s)", UUID.randomUUID());
        final String content = String.format("Here is an attachment: image:Attachment.png (%d)", time);
        // Build the expected attachment URL from the HTTP client base URL: the host and port depend on the (possibly
        // containerized) instance and aren't necessarily localhost:8080, and the server renders the URL using the host
        // of the incoming REST request (the HTTP client host), which isn't necessarily the browser-facing host either.
        final String attachmentURL = String.format("%s%sdownload/%s/%s/Attachment.png",
            getUtil().getCurrentExecutor().getHttpClientBaseURL(), getUtil().getBaseBinPath(null), this.space,
            this.pageName);
        final String renderedContent = String.format("<p>Here is an attachment: <img "
            + "src=\"%s\" "
            + "class=\"wikimodel-freestanding wikigeneratedid\" id=\"IAttachment.png\" alt=\"Attachment.png\""
            + "/>&nbsp;(%d)</p>", attachmentURL, time);
        final String comment = String.format("Updated title and content (%d)", System.currentTimeMillis());

        Page originalPage = getFirstPage();

        Page newPage = this.objectFactory.createPage();
        newPage.setTitle(title);
        newPage.setContent(content);
        newPage.setComment(comment);

        Link link = getFirstLinkByRelation(originalPage, Relations.SELF);
        assertNotNull(link);

        // PUT
        PutMethod putMethod = executePutXml(link.getHref(), newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(comment, modifiedPage.getComment());

        // GET
        GetMethod getMethod = executeGet(link.getHref() + "?supportedSyntax=markdown/1.2");
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(title, modifiedPage.getTitle());
        assertEquals(content, modifiedPage.getContent());
        assertEquals(renderedContent, modifiedPage.getRenderedContent());
        assertEquals(comment, modifiedPage.getComment());
    }
}

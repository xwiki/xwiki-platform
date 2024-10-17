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
package org.xwiki.rest.test.framework;

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.LinkCollection;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractHttpIT
{
    protected static final String INVALID_LIMIT_MINUS_1 =
        "Invalid limit value: -1. The limit must be a positive integer and less than or equal to 1000.";

    protected static final String INVALID_LIMIT_1001 =
        "Invalid limit value: 1001. The limit must be a positive integer and less than or equal to 1000.";

    protected Random random;

    protected Marshaller marshaller;

    protected Unmarshaller unmarshaller;

    protected ObjectFactory objectFactory;

    protected SolrTestUtils solrUtils;

    /**
     * The {@link TestUtils} instance injected by the Docker test framework, kept as a field so that the helper methods
     * ported from the legacy framework can keep using {@link #getUtil()}.
     */
    protected TestUtils testUtils;

    /**
     * Used to access the name of the current test (replaces the JUnit 4 {@code TestName} rule).
     */
    protected TestInfo testInfo;

    static {
        try {
            initializeSystem();
        } catch (Exception e) {

        }
    }

    @BeforeEach
    protected void setUp(TestUtils setup, TestInfo info) throws Exception
    {
        this.testUtils = setup;
        this.testInfo = info;

        // REST operations performed below and by the various helper methods rely on the default credentials being
        // those of the superadmin user. The former JUnit 4 functional test framework set this up automatically; the
        // Docker framework does not, so we set it explicitly here (before each test).
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        random = new Random();

        JAXBContext context = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
        marshaller = context.createMarshaller();
        unmarshaller = context.createUnmarshaller();
        objectFactory = new ObjectFactory();

        // Make sure guest does not have edit right
        Page page = getUtil().rest().page(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        org.xwiki.rest.model.jaxb.Object rightObject = getUtil().rest().object("XWiki.XWikiGlobalRights");
        rightObject.withProperties(getUtil().rest().property("users", "XWiki.XWikiGuest"),
            getUtil().rest().property("levels", "edit"), getUtil().rest().property("allow", "0"));
        Objects objects = new Objects();
        objects.withObjectSummaries(rightObject);
        page.setObjects(objects);
        getUtil().rest().save(page);

        // Init solr utils
        this.solrUtils = new SolrTestUtils(getUtil());
    }

    /**
     * @return the {@link TestUtils} instance injected by the Docker test framework
     */
    protected TestUtils getUtil()
    {
        return this.testUtils;
    }

    private static void initializeSystem() throws Exception
    {
        ComponentManager componentManager = new EmbeddableComponentManager();

        // Only load the minimal number of components required for the test framework, for both performance reasons
        // and for avoiding having to declare dependencies such as HttpServletRequest.
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDeclaration> componentDeclarations = new ArrayList<>();
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceSerializer.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(RelativeStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultEntityReferenceProvider.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultModelConfiguration.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultSymbolScheme.class.getName()));
        loader.initialize(componentManager, AbstractHttpIT.class.getClassLoader(), componentDeclarations);

        TestUtils.initializeComponent(componentManager);
    }

    protected Link getFirstLinkByRelation(LinkCollection linkCollection, String relation)
    {
        if (linkCollection.getLinks() == null) {
            return null;
        }

        for (Link link : linkCollection.getLinks()) {
            if (link.getRel().equals(relation)) {
                return link;
            }
        }

        return null;
    }

    protected List<Link> getLinksByRelation(LinkCollection linkCollection, String relation)
    {
        List<Link> result = new ArrayList<Link>();

        if (linkCollection.getLinks() == null) {
            return result;
        }

        for (Link link : linkCollection.getLinks()) {
            if (link.getRel().equals(relation)) {
                result.add(link);
            }
        }

        return result;
    }

    protected String getBaseURL()
    {
        return getUtil().rest().getBaseURL();
    }

    protected String getFullUri(Class<?> resourceClass)
    {
        return getUtil().rest().createUri(resourceClass, null).toString();
    }

    protected abstract void testRepresentation() throws Exception;

    protected CloseableHttpResponse executeGet(String uri) throws Exception
    {
        return executeGet(uri, (XWikiCredentials) null);
    }

    protected CloseableHttpResponse executeGet(String uri, String userName, String password) throws Exception
    {
        return executeGet(uri, new XWikiCredentials(userName, password));
    }

    private CloseableHttpResponse executeGet(String uri, XWikiCredentials credentials) throws Exception
    {
        HttpGet getMethod = new HttpGet(uri);
        getMethod.addHeader("Accept", MediaType.APPLICATION_XML);

        return execute(getMethod, credentials);
    }

    protected CloseableHttpResponse executePostXml(String uri, Object object) throws Exception
    {
        return executePostXml(uri, object, (XWikiCredentials) null);
    }

    protected CloseableHttpResponse executePostXml(String uri, Object object, String userName, String password)
        throws Exception
    {
        return executePostXml(uri, object, new XWikiCredentials(userName, password));
    }

    private CloseableHttpResponse executePostXml(String uri, Object object, XWikiCredentials credentials)
        throws Exception
    {
        HttpPost postMethod = new HttpPost(uri);
        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        postMethod.setEntity(toXmlEntity(object));

        return execute(postMethod, credentials);
    }

    protected CloseableHttpResponse executePost(String uri, InputStream is, String userName, String password)
        throws Exception
    {
        HttpPost postMethod = new HttpPost(uri);
        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        postMethod.setEntity(new InputStreamEntity(is, null));

        return execute(postMethod, new XWikiCredentials(userName, password));
    }

    protected String getFormToken(String userName, String password) throws Exception
    {
        CloseableHttpResponse response = executeGet(getFullUri(WikisResource.class), userName, password);
        assertEquals(HttpStatus.SC_OK, response.getCode(), getHttpResponseInfo(response));

        return response.getHeader("XWiki-Form-Token").getValue();
    }

    protected CloseableHttpResponse executePost(String uri, String string, String mediaType, String userName,
        String password) throws Exception
    {
        return executePost(uri, string, mediaType, userName, password, getFormToken(userName, password));
    }

    protected CloseableHttpResponse executePost(String uri, String string, String mediaType, String userName,
        String password, String formToken) throws Exception
    {
        HttpPost postMethod = new HttpPost(uri);
        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        if (formToken != null) {
            postMethod.addHeader("XWiki-Form-Token", formToken);
        }
        postMethod.setEntity(new StringEntity(string, toContentType(mediaType)));

        return execute(postMethod, new XWikiCredentials(userName, password));
    }

    protected CloseableHttpResponse executePostForm(String uri, NameValuePair[] nameValuePairs, String userName,
        String password) throws Exception
    {
        return executePostForm(uri, nameValuePairs, userName, password, getFormToken(userName, password));
    }

    protected CloseableHttpResponse executePostForm(String uri, NameValuePair[] nameValuePairs, String userName,
        String password, String formToken) throws Exception
    {
        HttpPost postMethod = new HttpPost(uri);
        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        if (formToken != null) {
            postMethod.addHeader("XWiki-Form-Token", formToken);
        }
        postMethod.setEntity(new UrlEncodedFormEntity(Arrays.asList(nameValuePairs), StandardCharsets.UTF_8));

        return execute(postMethod, new XWikiCredentials(userName, password));
    }

    protected CloseableHttpResponse executePutXml(String uri, Object object) throws Exception
    {
        return executePutXml(uri, object, (XWikiCredentials) null);
    }

    protected CloseableHttpResponse executePutXml(String uri, Object object, String userName, String password)
        throws Exception
    {
        return executePutXml(uri, object, new XWikiCredentials(userName, password));
    }

    private CloseableHttpResponse executePutXml(String uri, Object object, XWikiCredentials credentials)
        throws Exception
    {
        HttpPut putMethod = new HttpPut(uri);
        putMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        putMethod.setEntity(toXmlEntity(object));

        return execute(putMethod, credentials);
    }

    protected CloseableHttpResponse executePut(String uri, String string, String mediaType) throws Exception
    {
        return executePut(uri, string, mediaType, (XWikiCredentials) null);
    }

    protected CloseableHttpResponse executePut(String uri, String string, String mediaType, String userName,
        String password) throws Exception
    {
        return executePut(uri, string, mediaType, new XWikiCredentials(userName, password));
    }

    private CloseableHttpResponse executePut(String uri, String string, String mediaType,
        XWikiCredentials credentials) throws Exception
    {
        HttpPut putMethod = new HttpPut(uri);
        putMethod.setEntity(new StringEntity(string, toContentType(mediaType)));

        return execute(putMethod, credentials);
    }

    protected CloseableHttpResponse executeDelete(String uri) throws Exception
    {
        return executeDelete(uri, (XWikiCredentials) null);
    }

    protected CloseableHttpResponse executeDelete(String uri, String userName, String password) throws Exception
    {
        return executeDelete(uri, new XWikiCredentials(userName, password));
    }

    private CloseableHttpResponse executeDelete(String uri, XWikiCredentials credentials) throws Exception
    {
        return execute(new HttpDelete(uri), credentials);
    }

    protected CloseableHttpResponse execute(ClassicHttpRequest request, String userName, String password)
        throws Exception
    {
        return execute(request, new XWikiCredentials(userName, password));
    }

    /**
     * Executes the passed request with the passed credentials, and restores the credentials used by the rest of the
     * test framework afterwards. Passing null credentials sends the request as guest.
     */
    private CloseableHttpResponse execute(ClassicHttpRequest request, XWikiCredentials credentials) throws Exception
    {
        XWikiCredentials previousCredentials = getUtil().setDefaultCredentials(credentials);

        try {
            return getUtil().execute(request);
        } finally {
            getUtil().setDefaultCredentials(previousCredentials);
        }
    }

    private HttpEntity toXmlEntity(Object object) throws Exception
    {
        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);

        return new StringEntity(writer.toString(), toContentType(MediaType.APPLICATION_XML));
    }

    private ContentType toContentType(String mediaType)
    {
        return ContentType.parse(mediaType).withCharset(StandardCharsets.UTF_8);
    }

    protected String getWiki() throws Exception
    {
        CloseableHttpResponse getMethod = executeGet(getFullUri(WikisResource.class));
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        assertTrue(!wikis.getWikis().isEmpty());

        return wikis.getWikis().get(0).getName();
    }

    protected String getContentFromURI(String uri) throws Exception
    {
        CloseableHttpResponse getMethod = executeGet(uri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        return EntityUtils.toString(getMethod.getEntity());
    }

    protected void checkLinks(LinkCollection linkCollection) throws Exception
    {
        if (linkCollection.getLinks() != null) {
            for (Link link : linkCollection.getLinks()) {
                CloseableHttpResponse getMethod = executeGet(link.getHref());
                if (getMethod.getCode() != HttpStatus.SC_UNAUTHORIZED) {
                    assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));
                }
            }
        }
    }

    protected String buildURI(Class<?> resource, Object... pathParameters) throws Exception
    {
        return Utils.createURI(new URI(getBaseURL()), resource, pathParameters).toString();
    }

    /**
     * @since 18.8.0RC1
     */
    protected String buildURI(Class<?> resource, List<Object> pathSegments, Map<String, Object> queryParameters)
        throws Exception
    {
        return Utils.createURI(new URI(getBaseURL()), resource, pathSegments, queryParameters).toString();
    }

    private Page getPage(String wikiName, List<String> spaceName, String pageName) throws Exception
    {
        String uri = buildURI(PageResource.class, wikiName, spaceName, pageName).toString();

        CloseableHttpResponse getMethod = executeGet(uri);

        return (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
    }

    protected String getPageContent(String wikiName, List<String> spaceName, String pageName) throws Exception
    {
        Page page = getPage(wikiName, spaceName, pageName);

        return page.getContent();
    }

    protected int setPageContent(String wikiName, List<String> spaceName, String pageName, String content)
        throws Exception
    {
        String uri = buildURI(PageResource.class, wikiName, spaceName, pageName).toString();

        CloseableHttpResponse putMethod = executePut(uri, content, javax.ws.rs.core.MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        int code = putMethod.getCode();
        assertTrue(code == HttpStatus.SC_ACCEPTED || code == HttpStatus.SC_CREATED,
            String.format("Failed to set page content, %s", getHttpResponseInfo(putMethod)));

        return code;
    }

    protected String getHttpResponseInfo(ClassicHttpResponse response) throws Exception
    {
        return String.format("\nStatus code: %d\nStatus text: %s", response.getCode(), response.getReasonPhrase());
    }

    protected String getAttachmentsInfo(Attachments attachments)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Attachments: %d\n", attachments.getAttachments().size()));
        for (Attachment attachment : attachments.getAttachments()) {
            sb.append(String.format("* %s\n", attachment.getName()));
        }

        return sb.toString();
    }

    protected String getPagesInfo(Pages pages)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Pages: %d\n", pages.getPageSummaries().size()));
        for (PageSummary pageSummary : pages.getPageSummaries()) {
            sb.append(String.format("* %s\n", pageSummary.getFullName()));
        }

        return sb.toString();
    }

    protected List<String> toRestSpaces(List<String> spaces)
    {
        List<String> restSpaces = new ArrayList<>(spaces.size());
        spaces.forEach(s -> {
            if (!restSpaces.isEmpty()) {
                restSpaces.add("spaces");
            }
            restSpaces.add(s);
        });

        return restSpaces;
    }

    protected void createPage(List<String> spaces, String pageName, String content) throws Exception
    {
        String uri = buildURI(PageResource.class, getWiki(), toRestSpaces(spaces), pageName);

        Page page = this.objectFactory.createPage();
        page.setContent(content);

        CloseableHttpResponse putMethod = executePutXml(uri, page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));
    }

    protected boolean createPageIfDoesntExist(List<String> spaces, String pageName, String content) throws Exception
    {
        String uri = buildURI(PageResource.class, getWiki(), toRestSpaces(spaces), pageName);

        CloseableHttpResponse getMethod = executeGet(uri);

        if (getMethod.getCode() == HttpStatus.SC_NOT_FOUND) {
            createPage(spaces, pageName, content);

            getMethod = executeGet(uri);
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            return true;
        }

        return false;
    }

    protected String getTestMethodName()
    {
        return this.testInfo.getTestMethod().map(Method::getName).orElse(null);
    }

    protected String getTestClassName()
    {
        // The tests run as @Nested classes named "Nested<OriginalClassName>" inside AllIT. Strip the "Nested" prefix
        // so that the space/page names built from the class name (and the assertions that hardcode it) match the
        // original IT class name, exactly as in the former JUnit 4 suite.
        String simpleName = getClass().getSimpleName();
        return simpleName.startsWith("Nested") ? simpleName.substring("Nested".length()) : simpleName;
    }
}

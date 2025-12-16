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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
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
import org.xwiki.test.integration.junit4.ValidateConsoleRule;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractHttpIT
{
    /**
     * Validate stdout/stderr for problems.
     */
    @ClassRule
    public static final ValidateConsoleRule validateConsole = new ValidateConsoleRule();

    protected static final String INVALID_LIMIT_MINUS_1 =
        "Invalid limit value: -1. The limit must be a positive integer and less than or equal to 1000.";

    protected static final String INVALID_LIMIT_1001 =
        "Invalid limit value: 1001. The limit must be a positive integer and less than or equal to 1000.";

    /**
     * The object used to access the name of the current test.
     */
    @Rule
    public final TestName testName = new TestName();

    protected Random random;

    protected Marshaller marshaller;

    protected Unmarshaller unmarshaller;

    protected ObjectFactory objectFactory;

    // TODO: Refactor TestUtils to move REST tools to xwiki-platform-test-integration
    protected TestUtils testUtils = new TestUtils();

    protected SolrTestUtils solrUtils;

    static {
        try {
            initializeSystem();
        } catch (Exception e) {

        }
    }

    @Before
    public void setUp() throws Exception
    {
        random = new Random();

        JAXBContext context = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
        marshaller = context.createMarshaller();
        unmarshaller = context.createUnmarshaller();
        objectFactory = new ObjectFactory();

        // Make sure guest does not have edit right
        Page page = this.testUtils.rest().page(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        org.xwiki.rest.model.jaxb.Object rightObject = this.testUtils.rest().object("XWiki.XWikiGlobalRights");
        rightObject.withProperties(this.testUtils.rest().property("users", "XWiki.XWikiGuest"),
            this.testUtils.rest().property("levels", "edit"), this.testUtils.rest().property("allow", "0"));
        Objects objects = new Objects();
        objects.withObjectSummaries(rightObject);
        page.setObjects(objects);
        this.testUtils.rest().save(page);

        // Reset the default credentials
        this.testUtils.setDefaultCredentials(null);

        // Init solr utils
        this.solrUtils = new SolrTestUtils(this.testUtils);
    }

    public static void initializeSystem() throws Exception
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
        loader.initialize(componentManager, AbstractTest.class.getClassLoader(), componentDeclarations);

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
        return this.testUtils.rest().getBaseURL();
    }

    protected String getFullUri(Class<?> resourceClass)
    {
        return this.testUtils.rest().createUri(resourceClass, null).toString();
    }

    public abstract void testRepresentation() throws Exception;

    protected CloseableHttpResponse executeGet(String uri) throws Exception
    {
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(null);

        try {
            // Execute
            return this.testUtils.rest().executeGet(uri);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected CloseableHttpResponse executeGet(String uri, String userName, String password) throws Exception
    {
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(userName, password);

        try {
            // Execute
            return this.testUtils.rest().executeGet(uri);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected String getString(String uri, String userName, String password) throws Exception
    {
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(userName, password);

        try {
            // Execute
            return this.testUtils.rest().getString(uri);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected CloseableHttpResponse executePostXml(String uri, Object object) throws Exception
    {
        return executePostXml(uri, object, null);
    }

    protected CloseableHttpResponse executePostXml(String uri, Object object, String userName, String password)
        throws Exception
    {
        return executePostXml(uri, object, new XWikiCredentials(userName, password));
    }

    protected CloseableHttpResponse executePostXml(String uri, Object object, XWikiCredentials credentials)
        throws Exception
    {
        XWikiCredentials defaultCredentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(credentials);

        try {
            // Execute
            HttpPost postMethod = new HttpPost(uri);
            postMethod.addHeader("Accept", MediaType.APPLICATION_XML);

            StringWriter writer = new StringWriter();
            marshaller.marshal(object, writer);

            postMethod.setEntity(new StringEntity(writer.toString(), ContentType.APPLICATION_XML));
            return this.testUtils.execute(postMethod);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(defaultCredentials);
        }
    }

    protected CloseableHttpResponse executePost(String uri, InputStream is, String userName, String password)
        throws Exception
    {
        return executePost(uri, new InputStreamEntity(is, null), userName, password);
    }

    protected CloseableHttpResponse executePost(String uri, HttpEntity entity, String userName, String password)
        throws Exception
    {
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(userName, password);

        try {
            // Execute
            HttpPost postMethod = new HttpPost(uri);
            postMethod.addHeader("Accept", MediaType.APPLICATION_XML);

            postMethod.setEntity(entity);
            return this.testUtils.execute(postMethod);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected CloseableHttpResponse execute(ClassicHttpRequest request, String userName, String password)
        throws Exception
    {
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(userName, password);

        try {
            return this.testUtils.execute(request);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected String getFormToken(String userName, String password) throws Exception
    {
        CloseableHttpResponse response = executeGet(getFullUri(WikisResource.class), userName, password);
        assertEquals(EntityUtils.toString(response.getEntity()), HttpStatus.SC_OK, response.getCode());
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
        XWikiCredentials credentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(userName, password);

        try {
            // Execute
            HttpPost postMethod = new HttpPost(uri);
            postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
            if (formToken != null) {
                postMethod.addHeader("XWiki-Form-Token", formToken);
            }

            postMethod.setEntity(new StringEntity(string, ContentType.create(mediaType)));
            return this.testUtils.execute(postMethod);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(credentials);
        }
    }

    protected CloseableHttpResponse executePostForm(String uri, Iterable<? extends NameValuePair> parameters,
        String userName, String password) throws Exception
    {
        return executePostForm(uri, parameters, userName, password, getFormToken(userName, password));
    }

    protected CloseableHttpResponse executePostForm(String uri, Iterable<? extends NameValuePair> parameters,
        String userName, String password, String formToken) throws Exception
    {
        HttpPost postMethod = new HttpPost(uri);

        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        postMethod.addHeader("Content-type", MediaType.APPLICATION_FORM_URLENCODED);
        if (formToken != null) {
            postMethod.addHeader("XWiki-Form-Token", formToken);
        }

        postMethod.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));

        this.testUtils.setDefaultCredentials(userName, password);

        return this.testUtils.execute(postMethod);
    }

    protected CloseableHttpResponse executePutXml(String uri, Object object, XWikiCredentials credentials)
        throws Exception
    {
        XWikiCredentials defaultCredentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(credentials);

        try {
            // Execute
            HttpPut method = new HttpPut(uri);
            method.addHeader("Accept", MediaType.APPLICATION_XML);

            StringWriter writer = new StringWriter();
            marshaller.marshal(object, writer);

            method.setEntity(new StringEntity(writer.toString(), ContentType.APPLICATION_XML));
            return this.testUtils.execute(method);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(defaultCredentials);
        }
    }

    protected CloseableHttpResponse executePutXml(String uri, Object object) throws Exception
    {
        return executePutXml(uri, object, null);
    }

    protected CloseableHttpResponse executePutXml(String uri, Object object, String userName, String password)
        throws Exception
    {
        return executePutXml(uri, object, new XWikiCredentials(userName, password));
    }

    protected CloseableHttpResponse executePut(String uri, String string, String mediaType, XWikiCredentials credentials)
        throws Exception
    {
        XWikiCredentials defaultCredentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(credentials);

        try {
            // Execute
            HttpPut method = new HttpPut(uri);
            method.addHeader("Accept", MediaType.APPLICATION_XML);

            method.setEntity(new StringEntity(string, ContentType.parse(mediaType)));
            return this.testUtils.execute(method);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(defaultCredentials);
        }
    }

    protected CloseableHttpResponse executePut(String uri, String string, String mediaType) throws Exception
    {
        return executePut(uri, string, mediaType, null);
    }

    protected CloseableHttpResponse executePut(String uri, String string, String mediaType, String userName,
        String password) throws Exception
    {
        return executePut(uri, string, mediaType, new XWikiCredentials(userName, password));
    }

    protected CloseableHttpResponse executeDelete(String uri, XWikiCredentials credentials) throws Exception
    {
        XWikiCredentials defaultCredentials = this.testUtils.getDefaultCredentials();

        // Switch default credentials
        this.testUtils.setDefaultCredentials(credentials);

        try {
            // Execute
            return this.testUtils.rest().executeDelete(uri);
        } finally {
            // Restore default credentials
            this.testUtils.setDefaultCredentials(defaultCredentials);
        }
    }

    protected CloseableHttpResponse executeDelete(String uri) throws Exception
    {
        return executeDelete(uri, null);
    }

    protected CloseableHttpResponse executeDelete(String uri, String userName, String password) throws Exception
    {
        return executeDelete(uri, new XWikiCredentials(userName, password));
    }

    protected String getWiki() throws Exception
    {
        Wikis wikis = this.testUtils.rest().get(getFullUri(WikisResource.class), true);

        assertTrue(wikis.getWikis().size() > 0);

        return wikis.getWikis().get(0).getName();
    }

    protected void checkLinks(LinkCollection linkCollection) throws Exception
    {
        if (linkCollection.getLinks() != null) {
            for (Link link : linkCollection.getLinks()) {
                try (CloseableHttpResponse response = executeGet(link.getHref())) {
                    if (response.getCode() != HttpStatus.SC_UNAUTHORIZED) {
                        assertEquals(EntityUtils.toString(response.getEntity()), HttpStatus.SC_OK, response.getCode());
                    }
                }
            }
        }
    }

    protected String buildURI(Class<?> resource, Object... pathParameters) throws Exception
    {
        return Utils.createURI(new URI(getBaseURL()), resource, pathParameters).toString();
    }

    /**
     * @since 17.3.0RC1
     */
    protected String buildURI(Class<?> resource, List<Object> pathSegments, Map<String, Object> queryString) throws Exception
    {
        return Utils.createURI(new URI(getBaseURL()), resource, pathSegments, queryString).toString();
    }

    private Page getPage(String wikiName, List<String> spaceName, String pageName) throws Exception
    {
        return this.testUtils.rest().get(new DocumentReference(wikiName, spaceName, pageName));
    }

    protected String getPageContent(String wikiName, List<String> spaceName, String pageName) throws Exception
    {
        Page page = getPage(wikiName, spaceName, pageName);

        return page.getContent();
    }

    protected void setPageContent(String wikiName, List<String> spaceName, String pageName, String content)
        throws Exception
    {
        this.testUtils.rest().savePage(new DocumentReference(wikiName, spaceName, pageName), content, null);
    }

    protected String getHttpResponseInfo(ClassicHttpResponse response) throws Exception
    {
        return String.format("Status code: %d\nStatus text: %s", response.getCode(), response.getReasonPhrase());
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

        try (CloseableHttpResponse response = executePutXml(uri, page, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword())) {
            assertEquals(EntityUtils.toString(response.getEntity()), HttpStatus.SC_CREATED, response.getCode());
        }
    }

    protected boolean createPageIfDoesntExist(List<String> spaces, String pageName, String content) throws Exception
    {
        String uri = buildURI(PageResource.class, getWiki(), toRestSpaces(spaces), pageName);

        CloseableHttpResponse response = executeGet(uri);

        if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
            createPage(spaces, pageName, content);

            response = executeGet(uri);
            assertEquals(EntityUtils.toString(response.getEntity()), HttpStatus.SC_OK, response.getCode());

            return true;
        }

        return false;
    }

    protected String getTestMethodName()
    {
        return this.testName.getMethodName();
    }

    protected String getTestClassName()
    {
        return getClass().getSimpleName();
    }
}

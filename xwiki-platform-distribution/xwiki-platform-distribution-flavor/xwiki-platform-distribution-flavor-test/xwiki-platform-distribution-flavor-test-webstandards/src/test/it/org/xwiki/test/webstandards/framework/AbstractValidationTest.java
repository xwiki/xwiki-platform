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
package org.xwiki.test.webstandards.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.validator.Validator;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarPackage;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AbstractValidationTest extends TestCase
{
    private static final DefaultStringEntityReferenceSerializer SERIALIZER =
        new DefaultStringEntityReferenceSerializer();

    protected HttpClient client;

    protected Target target;

    protected String credentials;

    public AbstractValidationTest(String name, Target target, HttpClient client, String credentials)
    {
        super(name);

        this.target = target;
        this.client = client;
        this.credentials = credentials;
    }

    protected GetMethod createGetMethod() throws UnsupportedEncodingException
    {
        GetMethod getMethod = null;

        if (this.target instanceof DocumentReferenceTarget) {
            DocumentReferenceTarget documentReferenceTarget = (DocumentReferenceTarget) this.target;
            StringBuilder url = new StringBuilder();
            url.append("http://127.0.0.1:8080/xwiki/bin/view/");
            for (SpaceReference spaceReference : documentReferenceTarget.getDocumentReference().getSpaceReferences()) {
                url.append(URLEncoder.encode(spaceReference.getName(), "UTF-8"));
                url.append("/");
            }
            url.append(URLEncoder.encode(documentReferenceTarget.getDocumentReference().getName(), "UTF-8"));
            
            getMethod = new GetMethod(url.toString());
        } else if (this.target instanceof URLPathTarget) {
            String urlPath = ((URLPathTarget) this.target).getUrlPath();

            if (urlPath.startsWith("http://")) {
                getMethod = new GetMethod(urlPath);
            } else {
                getMethod = new GetMethod("http://127.0.0.1:8080" + urlPath);
            }
        }

        return getMethod;
    }

    protected GetMethod getResponse() throws Exception
    {
        GetMethod method = createGetMethod();

        method.setFollowRedirects(true);
        method.getParams().setSoTimeout(30000);

        if (this.credentials != null) {
            method.setDoAuthentication(true);
            method.addRequestHeader("Authorization",
                "Basic " + new String(Base64.encodeBase64(this.credentials.getBytes())));
        }

        // Execute the method.
        try {
            int statusCode = this.client.executeMethod(method);

            assertEquals("Method failed: " + method.getStatusLine(), HttpStatus.SC_OK, statusCode);

            // Read the response body.
            return method;
        } catch (Exception e) {
            method.releaseConnection();

            throw new Exception(String.format("Failed to get response for URL [%s]", method.getURI()), e);
        }
    }

    protected byte[] getResponseBody() throws Exception
    {
        GetMethod method = getResponse();

        try {
            // Read the response body.
            return method.getResponseBody();
        } finally {
            method.releaseConnection();
        }
    }

    public static Test suite(Class< ? extends AbstractValidationTest> validationTest, Validator validator)
        throws Exception
    {
        TestSuite suite = new TestSuite();

        suite.setName(validator.getName());

        HttpClient adminClient = new HttpClient();
        // The code that prevents circular redirects (HttpMethodDirector#processRedirectResponse) ignores the query
        // string when comparing the redirect location with the current location. The browser doesn't behave like this
        // and we have pages that redirect to themselves with different query string parameters.
        adminClient.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        // Limit the number of redirects because we don't detect circular redirect any more.
        adminClient.getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 3);
        Credentials defaultcreds = new UsernamePasswordCredentials("Admin", "admin");
        adminClient.getState().setCredentials(AuthScope.ANY, defaultcreds);

        addXarFiles(validationTest, validator, suite, adminClient);
        addURLsForAdmin(validationTest, validator, suite, adminClient);

        HttpClient guestClient = new HttpClient();

        addURLsForGuest(validationTest, validator, suite, guestClient);

        return suite;
    }

    protected static void addURLsForAdmin(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, HttpClient client) throws Exception
    {
        addURLs("urlsToTestAsAdmin", validationTest, validator, suite, client, "Admin:admin");
    }

    protected static void addURLsForGuest(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, HttpClient client) throws Exception
    {
        addURLs("urlsToTestAsGuest", validationTest, validator, suite, client, null);
    }

    protected static void addURLs(String property, Class< ? extends AbstractValidationTest> validationTest,
        Validator validator, TestSuite suite, HttpClient client, String credentials) throws Exception
    {
        String urlsToTest = System.getProperty(property);

        if (urlsToTest != null) {
            for (String url : urlsToTest.split("\\s")) {
                if (StringUtils.isNotEmpty(url)) {
                    suite.addTest(validationTest.getConstructor(Target.class, HttpClient.class, Validator.class,
                        String.class).newInstance(new URLPathTarget(url), client, validator, credentials));
                }
            }
        }
    }

    protected static void documents(String property, Class< ? extends AbstractValidationTest> validationTest,
        Validator validator, TestSuite suite, HttpClient client, String credentials) throws Exception
    {
        String urlsToTest = System.getProperty(property);

        if (urlsToTest != null) {
            for (String url : urlsToTest.split("\\s")) {
                if (StringUtils.isNotEmpty(url)) {
                    suite.addTest(validationTest.getConstructor(Target.class, HttpClient.class, Validator.class,
                        String.class).newInstance(new URLPathTarget(url), client, validator, credentials));
                }
            }
        }
    }

    protected static void addXarFiles(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, HttpClient client) throws Exception
    {
        String path = System.getProperty("pathToDocuments");
        String patternFilter = System.getProperty("documentsToTest");

        boolean skipTechnicalPages;
        try {
            Field isSkipTechnicalPages = validationTest.getDeclaredField("skipTechnicalPages");
            isSkipTechnicalPages.setAccessible(true);
            skipTechnicalPages = isSkipTechnicalPages.getBoolean(null);
        } catch (NoSuchFieldException e) {
            skipTechnicalPages = false;
        }

        for (DocumentReference documentReference : readXarContents(path, patternFilter, skipTechnicalPages)) {
            suite.addTest(validationTest.getConstructor(Target.class, HttpClient.class, Validator.class, String.class)
                .newInstance(new DocumentReferenceTarget(documentReference), client, validator, "Admin:admin"));
        }
    }

    private static boolean isTechnicalPage(String directoryPath, XarEntry entry, DocumentBuilder documentBuilder)
        throws Exception
    {
        try (InputStream inputStream = new FileInputStream(new File(directoryPath, entry.getEntryName()))) {
            Document parsedDocument = documentBuilder.parse(inputStream);

            NodeList elements = parsedDocument.getElementsByTagName("hidden");

            boolean isHiddenPage = false;
            if (elements.getLength() == 1) {
               isHiddenPage = "true".equals(elements.item(0).getTextContent());
            }
            return isHiddenPage;
        }
    }

    protected static List<DocumentReference> readXarContents(String fileName, String patternFilter) throws Exception
    {
        return readXarContents(fileName, patternFilter, false);
    }

    private static boolean isPageIncluded(Pattern pattern, XarEntry entry)
    {
        return (pattern == null || pattern.matcher(SERIALIZER.serialize(entry)).matches());
    }

    private static boolean shouldSkipPage(boolean skipTechnicalPages, String directoryPath, XarEntry entry,
        DocumentBuilder documentBuilder) throws Exception
    {
        if (!skipTechnicalPages) {
            return false;
        } else {
            return isTechnicalPage(directoryPath, entry, documentBuilder);
        }
    }

    protected static List<DocumentReference> readXarContents(String fileName, String patternFilter,
        boolean skipTechnicalPages) throws Exception
    {
        File file = new File(fileName);
        XarPackage xarPackage = new XarPackage(file);
        Collection<XarEntry> entries = xarPackage.getEntries();

        List<DocumentReference> result = new ArrayList<DocumentReference>(entries.size());

        WikiReference wikiReference = new WikiReference("xwiki");

        Pattern pattern = patternFilter == null ? null : Pattern.compile(patternFilter);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        for (XarEntry entry : entries) {
            if (isPageIncluded(pattern, entry)
                && !shouldSkipPage(skipTechnicalPages, fileName, entry, documentBuilder)) {
                result.add(new DocumentReference(entry, wikiReference));
            }
        }

        return result;
    }
}

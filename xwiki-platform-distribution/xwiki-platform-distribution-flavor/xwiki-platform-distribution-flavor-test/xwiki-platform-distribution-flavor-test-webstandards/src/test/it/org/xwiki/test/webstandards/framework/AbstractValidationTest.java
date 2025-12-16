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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.http.internal.XWikiHTTPClient;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.ui.TestUtils;
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

    private static final Logger logger = LoggerFactory.getLogger(AbstractValidationTest.class);

    protected XWikiHTTPClient client;

    protected Target target;

    public AbstractValidationTest(String name, Target target, XWikiHTTPClient client)
    {
        super(name);

        this.target = target;
        this.client = client;
    }

    protected String getTargetURL(Target target)
    {
        StringBuilder url = new StringBuilder();

        if (target instanceof DocumentReferenceTarget) {
            DocumentReferenceTarget documentReferenceTarget = (DocumentReferenceTarget) target;
            url.append("http://127.0.0.1:8080/xwiki/bin/view/");
            for (SpaceReference spaceReference : documentReferenceTarget.getDocumentReference().getSpaceReferences()) {
                url.append(encode(spaceReference.getName()));
                url.append("/");
            }
            url.append(encode(documentReferenceTarget.getDocumentReference().getName()));
        } else if (target instanceof URLPathTarget) {
            String urlPath = ((URLPathTarget) target).getUrlPath();

            if (urlPath.startsWith("http://")) {
                url.append(urlPath);
            } else {
                url.append("http://127.0.0.1:8080").append(urlPath);
            }
        }

        return url.toString();
    }

    protected String encode(String value)
    {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should not happen since UTF-8 should always be available
            throw new RuntimeException(
                String.format("UTF-8 encoding error for [%s]", value), e);
        }
    }

    protected byte[] getResponseBody() throws Exception
    {
        String originalURI = getTargetURL(this.target);

        return this.client.executeGet(originalURI, (response, context) -> {
            // Get the current URI
            URI uri;
            try {
                uri = context.getRequest().getUri();
            } catch (URISyntaxException e) {
                throw new HttpException("Failed to access latest request URI", e);
            }

            String uriMessage;
            if (!originalURI.equals(uri.toString())) {
                uriMessage = String.format("[%s] (redirected to [%s])", originalURI, uri);
            } else {
                uriMessage = String.format("[%s]", uri);
            }

            // We accept status codes 200 and 423 as valid. HTTP code 423 is sent when trying to perform an action on
            // a protected page and thus it's valid.
            int statusCode = response.getCode();
            assertTrue(String.format("Method %s failed with status [%s]", uriMessage, response.getReasonPhrase()),
                statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_LOCKED);

            return EntityUtils.toByteArray(response.getEntity());
        });
    }

    public static Test suite(Class< ? extends AbstractValidationTest> validationTest, Validator validator)
        throws Exception
    {
        TestSuite suite = new TestSuite();

        suite.setName(validator.getName());

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        // The code that prevents circular redirects (HttpMethodDirector#processRedirectResponse) ignores the query
        // string when comparing the redirect location with the current location. The browser doesn't behave like this
        // and we have pages that redirect to themselves with different query string parameters.
        requestConfigBuilder.setCircularRedirectsAllowed(true);
        // Limit the number of redirects because we don't detect circular redirect any more.
        requestConfigBuilder.setMaxRedirects(3);

        XWikiHTTPClient adminClient =
            new XWikiHTTPClient(XWikiHTTPClient.builder().setDefaultRequestConfig(requestConfigBuilder.build()));
        adminClient.setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);

        addXarFiles(validationTest, validator, suite, adminClient);
        addURLsForAdmin(validationTest, validator, suite, adminClient);

        XWikiHTTPClient guestClient = new XWikiHTTPClient();

        addURLsForGuest(validationTest, validator, suite, guestClient);

        return suite;
    }

    protected static void addURLsForAdmin(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, XWikiHTTPClient client) throws Exception
    {
        addURLs("urlsToTestAsAdmin", validationTest, validator, suite, client);
    }

    protected static void addURLsForGuest(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, XWikiHTTPClient client) throws Exception
    {
        addURLs("urlsToTestAsGuest", validationTest, validator, suite, client);
    }

    protected static void addURLs(String property, Class< ? extends AbstractValidationTest> validationTest,
        Validator validator, TestSuite suite, XWikiHTTPClient client) throws Exception
    {
        String urlsToTest = System.getProperty(property);

        if (urlsToTest != null) {
            for (String url : urlsToTest.split("\\s")) {
                if (StringUtils.isNotEmpty(url)) {
                    suite.addTest(validationTest.getConstructor(Target.class, XWikiHTTPClient.class, Validator.class,
                        String.class).newInstance(new URLPathTarget(url), client, validator));
                }
            }
        }
    }

    protected static void documents(String property, Class< ? extends AbstractValidationTest> validationTest,
        Validator validator, TestSuite suite, XWikiHTTPClient client) throws Exception
    {
        String urlsToTest = System.getProperty(property);

        if (urlsToTest != null) {
            for (String url : urlsToTest.split("\\s")) {
                if (StringUtils.isNotEmpty(url)) {
                    suite.addTest(validationTest.getConstructor(Target.class, XWikiHTTPClient.class, Validator.class,
                        String.class).newInstance(new URLPathTarget(url), client, validator));
                }
            }
        }
    }

    protected static void addXarFiles(Class< ? extends AbstractValidationTest> validationTest, Validator validator,
        TestSuite suite, XWikiHTTPClient client) throws Exception
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

        String whitelistedClasses;
        try {
            Field whiteListedClassesField = validationTest.getDeclaredField("whitelistedClasses");
            whiteListedClassesField.setAccessible(true);
            whitelistedClasses = (String) whiteListedClassesField.get(null);
        } catch (NoSuchFieldException e) {
            whitelistedClasses = "";
        }

        List<String> whitelistedClassesList = Arrays.asList(whitelistedClasses.split("\\s"));

        if (skipTechnicalPages) {
            logger.info(String.format("[%s] will skip technical pages, except those containing the following classes:"
                + " %s", validationTest.getName(), whitelistedClasses));
        }

        for (DocumentReference documentReference : readXarContents(path, patternFilter, skipTechnicalPages,
            whitelistedClassesList)) {
            suite.addTest(validationTest.getConstructor(Target.class, XWikiHTTPClient.class, Validator.class, String.class)
                .newInstance(new DocumentReferenceTarget(documentReference), client, validator));
        }
    }

    private static List<Node> getChildElementsOfName(Node element, String nodeName)
    {
        List<Node> result = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && nodeName.equals(node.getNodeName())) {
                result.add(node);
            }
        }

        return result;
    }

    private static boolean isTechnicalPage(String directoryPath, XarEntry entry, DocumentBuilder documentBuilder,
        List<String> whitelistedClasses)
        throws Exception
    {
        try (InputStream inputStream = new FileInputStream(new File(directoryPath, entry.getEntryName()))) {
            Document parsedDocument = documentBuilder.parse(inputStream);

            List<Node> hiddenElements = getChildElementsOfName(parsedDocument.getDocumentElement(), "hidden");
            boolean isHiddenPage = false;

            // if a page is hidden, then it's considered as a technical page
            if (hiddenElements.size() == 1) {
                isHiddenPage = "true".equals(hiddenElements.get(0).getTextContent());
            }

            // if there's a whitelist, we check the document does not contain any object
            // of the class of any of the whitelisted classes
            if (!whitelistedClasses.isEmpty()) {
                List<Node> objectElements = getChildElementsOfName(parsedDocument.getDocumentElement(), "object");
                for (Node objectElement : objectElements) {
                    List<Node> classNameElements = getChildElementsOfName(objectElement, "className");
                    for (Node objectAttribute : classNameElements) {
                        // if we find an object with a className corresponding to one of those whitelisted
                        // then we immediately consider the page as not technical: it must be checked
                        if (whitelistedClasses.contains(objectAttribute.getTextContent())) {
                            return false;
                        }
                    }
                }
            }

            return isHiddenPage;
        }
    }

    protected static List<DocumentReference> readXarContents(String fileName, String patternFilter) throws Exception
    {
        return readXarContents(fileName, patternFilter, false, Collections.emptyList());
    }

    private static boolean isPageIncluded(Pattern pattern, XarEntry entry)
    {
        return (pattern == null || pattern.matcher(SERIALIZER.serialize(entry)).matches());
    }

    private static boolean shouldSkipPage(boolean skipTechnicalPages, String directoryPath, XarEntry entry,
        DocumentBuilder documentBuilder, List<String> whitelistedClasses) throws Exception
    {
        if (!skipTechnicalPages) {
            return false;
        } else {
            return isTechnicalPage(directoryPath, entry, documentBuilder, whitelistedClasses);
        }
    }

    protected static List<DocumentReference> readXarContents(String fileName, String patternFilter,
        boolean skipTechnicalPages, List<String> whitelistedClasses) throws Exception
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
                && !shouldSkipPage(skipTechnicalPages, fileName, entry, documentBuilder, whitelistedClasses)) {
                result.add(new DocumentReference(entry, wikiReference));
            }
        }

        return result;
    }
}

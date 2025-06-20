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

package org.xwiki.test.escaping.framework;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.InvalidRedirectLocationException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.xwiki.test.escaping.suite.FileTest;
import org.xwiki.validator.ValidationError;

/**
 * Abstract base class for escaping tests. Implements common initialization pattern and some utility methods like URL
 * escaping, retrieving page content by URL etc. Subclasses need to implement parsing and custom tests.
 * <p>
 * Note: JUnit4 requires tests to have one public default constructor, subclasses will need to implement it and pass
 * pattern matcher to match file names they can handle.
 * <p>
 * Starting and stopping XWiki server is handled transparently for all subclasses, tests can be run alone using
 * -Dtest=ClassName, a parent test suite should start XWiki server before running all tests for efficiency using
 * {@link SingleXWikiExecutor}.
 * <p>
 * The following configuration properties are supported (set in maven):
 * <ul>
 * <li>pattern (optional): Additional pattern to select files to be tested (use -Dpattern="substring-regex"). Matches
 * all files if empty.</li>
 * </ul>
 * <p>
 * Automatic tests (see {@link AbstractAutomaticTest}) additionally support:
 * <ul>
 * <li>patternExcludeFiles (optional): List of RegEx patterns to exclude files from the tests</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractEscapingTest implements FileTest
{
    /** Static part of the test URL. */
    private static final String URL_START = "http://127.0.0.1:8080/xwiki/bin/";

    /** Language parameter name. */
    private static final String LANGUAGE = "language";

    /** Secret token parameter name. */
    private static final String SECRET_TOKEN = "form_token";

    /** HTTP client shared between all subclasses. */
    private static HttpClient client;

    /** A flag controlling login. If true, administrator credentials are used. */
    private static boolean loggedIn = true;

    /** Stores two cached tokens, one for each value of loggedIn (false -> 0, true -> 1). */
    private static String[] secretTokens = new String[2];

    private static Set<String> XML_MIMETYPES = new HashSet<>(Arrays.asList("text/html", "text/xml", "application/xml"));

    /** File name of the template to use. */
    protected String name;

    /** User provided data found in the file. */
    protected Set<String> userInput;

    /** Pattern used to match files by name. */
    private Pattern namePattern;

    /**
     * Create new AbstractEscapingTest.
     * 
     * @param fileNameMatcher regex pattern used to filter files by name
     */
    protected AbstractEscapingTest(Pattern fileNameMatcher)
    {
        this.namePattern = fileNameMatcher;
    }

    /**
     * Start XWiki server if run alone.
     * 
     * @throws Exception on errors
     */
    @BeforeClass
    public static void startExecutor() throws Exception
    {
        SingleXWikiExecutor.getExecutor().start();
    }

    /**
     * Stop XWiki server if run alone.
     * 
     * @throws Exception on errors
     */
    @AfterClass
    public static void stopExecutor() throws Exception
    {
        SingleXWikiExecutor.getExecutor().stop();
    }

    /**
     * Change multi-language mode. Note: XWiki server must already be started.
     * 
     * @param enabled enable the multi-language mode if true, disable otherwise
     */
    protected static void setMultiLanguageMode(boolean enabled)
    {
        String url = AbstractEscapingTest.URL_START + "save/XWiki/XWikiPreferences?";
        url += SECRET_TOKEN + "=" + getSecretToken();
        url += "&XWiki.XWikiPreferences_0_languages=&XWiki.XWikiPreferences_0_multilingual=";
        AbstractEscapingTest.getUrlContent(url + (enabled ? 1 : 0));
        // set language=en to prevent false positives coming from the cookies
        String langUrl = AbstractEscapingTest.URL_START + "view/Main/?" + LANGUAGE + "=en";
        AbstractEscapingTest.getUrlContent(langUrl);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation for escaping tests checks if the given file name matches the supported name pattern and parses
     * the file.
     * 
     * @see org.xwiki.test.escaping.suite.FileTest#initialize(java.lang.String, java.io.Reader)
     */
    @Override
    public boolean initialize(String name, final Reader reader)
    {
        this.name = name;
        if (!fileNameMatches(name) || !patternMatches(name) || isExcludedFile(name)) {
            // TODO debug log the reason why the test was skipped
            return false;
        }

        this.userInput = parse(reader);
        return true;
    }

    /**
     * Check if the internal file name pattern matches the given file name.
     * 
     * @param fileName file name to check
     * @return true if the name matches, false otherwise
     */
    protected boolean fileNameMatches(String fileName)
    {
        return this.namePattern != null && this.namePattern.matcher(fileName).matches();
    }

    /**
     * Check if the system property "pattern" matches (substring regular expression) the file name. Empty pattern
     * matches everything.
     * 
     * @param fileName file name to check
     * @return true if the pattern matches, false otherwise
     */
    protected boolean patternMatches(String fileName)
    {
        String pattern = System.getProperty("pattern", "");
        if (pattern == null || pattern.equals("")) {
            return true;
        }
        return Pattern.matches(".*" + pattern + ".*", fileName);
    }

    /**
     * Check if the given file should be excluded from the tests.
     * 
     * @param fileName file name to check
     * @return true if the file should be excluded, false otherwise
     */
    protected abstract boolean isExcludedFile(String fileName);

    /**
     * Parse the file and collect parameters controlled by the user.
     * 
     * @param reader the reader associated with the file
     * @return collection of user-controlled input parameters
     */
    protected abstract Set<String> parse(Reader reader);

    /**
     * Check if the authentication status.
     * 
     * @return true if the requests will be sent authenticated as admin, false otherwise
     */
    protected static boolean isLoggedIn()
    {
        return loggedIn;
    }

    /**
     * Set authentication status.
     * 
     * @param value the value to set
     */
    protected static void setLoggedIn(boolean value)
    {
        loggedIn = value;
    }

    /**
     * Download a page from the server and return its content. Throws a {@link RuntimeException} on connection problems
     * etc.
     * 
     * @param url URL of the page
     * @return content of the page
     */
    protected static URLContent getUrlContent(String url)
    {
        GetMethod get = new GetMethod(url);
        get.setFollowRedirects(true);
        if (isLoggedIn()) {
            get.setDoAuthentication(true);
            get.addRequestHeader("Authorization", "Basic " + new String(Base64.encodeBase64("Admin:admin".getBytes())));
        }

        try {
            int statusCode = AbstractEscapingTest.getClient().executeMethod(get);
            switch (statusCode) {
                case HttpStatus.SC_OK:
                    // everything is fine
                    break;
                case HttpStatus.SC_UNAUTHORIZED:
                    // do not fail on 401 (unauthorized), used in some tests
                    System.out.println("WARNING, Ignoring status 401 (unauthorized) for URL: " + url);
                    break;
                case HttpStatus.SC_CONFLICT:
                    // do not fail on 409 (conflict), used in some templates
                    System.out.println("WARNING, Ignoring status 409 (conflict) for URL: " + url);
                    break;
                case HttpStatus.SC_NOT_FOUND:
                    // ignore 404 (the page is still rendered)
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    // ignore 500 (internal server error)
                    break;
                default:
                    throw new RuntimeException("HTTP GET request returned status " + statusCode + " ("
                        + get.getStatusText() + ") for URL: " + url);
            }

            return new URLContent(get.getResponseHeader("Content-Type").getValue(), get.getResponseBody());
        } catch (IOException exception) {
            throw new RuntimeException("Error retrieving URL: " + url, exception);
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * URL-escape given string.
     * 
     * @param str string to escape, "" is used if null
     * @return URL-escaped {@code str}
     */
    protected static String escapeUrl(String str)
    {
        try {
            return URLEncoder.encode(str == null ? "" : str, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            // should not happen
            throw new RuntimeException("Should not happen: ", exception);
        }
    }

    /**
     * Get an instance of the HTTP client to use.
     * 
     * @return HTTP client initialized with admin credentials
     */
    protected static HttpClient getClient()
    {
        if (AbstractEscapingTest.client == null) {
            HttpClient adminClient = new HttpClient();

            // set up admin credentials
            Credentials defaultcreds = new UsernamePasswordCredentials("Admin", "admin");
            adminClient.getState().setCredentials(AuthScope.ANY, defaultcreds);

            // set up client parameters
            HttpClientParams clientParams = new HttpClientParams();
            clientParams.setSoTimeout(20000);
            // We need to allow circular redirects, because some templates redirect to the same location with different
            // query parameters and the check for circular redirect in HttpClient only checks the URI path without the
            // parameters.
            // Note that actual circular redirects are still aborted after following them for some fixed number of times
            clientParams.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
            adminClient.setParams(clientParams);

            // set up connections parameters
            HttpConnectionManagerParams connectionParams = new HttpConnectionManagerParams();
            connectionParams.setConnectionTimeout(30000);
            adminClient.getHttpConnectionManager().setParams(connectionParams);

            AbstractEscapingTest.client = adminClient;
        }
        return AbstractEscapingTest.client;
    }

    @Override
    public String toString()
    {
        return this.name + ' ' + this.userInput;
    }

    /**
     * Check for unescaped data in the given {@code content}. Throws {@link RuntimeException} on errors.
     * 
     * @param url URL used in the test
     * @return list of found validation errors
     */
    protected List<ValidationError> getUnderEscapingErrors(String url)
    {
        // TODO better use XWiki logging
        System.out.println("Testing URL: " + url);

        URLContent content = null;
        try {
            content = AbstractEscapingTest.getUrlContent(url);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InvalidRedirectLocationException) {
                // Don't fail the test if we can't follow a redirect because the redirect location can be taken from the
                // request parameters which are controlled by the test and most of the tests use values that are not
                // valid URLs. The code that performs the redirect always assumes the redirect URL is valid.
                System.out.println(e.getCause().getMessage());
                return Collections.emptyList();
            } else {
                throw e;
            }
        }

        // TODO: add support for other types than XML
        if (content.getType() == null || XML_MIMETYPES.contains(content.getType().getMimeType())
            || content.getType().getMimeType().endsWith("+xml")) {
            String where = "  Template: " + this.name + "\n  URL: " + url;
            Assert.assertNotNull("Response is null\n" + where, content);
            XMLEscapingValidator validator = new XMLEscapingValidator();
            validator.setDocument(new ByteArrayInputStream(content.getContent()));
            try {
                return validator.validate();
            } catch (EscapingError error) {
                // most probably false positive, generate an error instead of failing the test
                throw new RuntimeException(EscapingError.formatMessage(error.getMessage(), this.name, url, null));
            }
        } else {
            System.err.println("WARN: Unsupported content type [" + content.getType() + "] for URL [" + url + "]");

            return Collections.emptyList();
        }
    }

    /**
     * A convenience method that throws an {@link EscapingError} on failure.
     *
     * @param url URL used in the test
     * @param description description of the test
     */
    protected void checkUnderEscaping(String url, String description)
    {
        List<ValidationError> errors = getUnderEscapingErrors(url);
        if (!errors.isEmpty()) {
            throw new EscapingError("Escaping test for " + description + " failed.", this.name, url, errors);
        }
    }

    /**
     * Create the target URL from the given parameters. URL-escapes everything. Adds language=en if the parameter map
     * does not contain language parameter.
     * 
     * @param action action to use, "view" is used if null
     * @param space space name to use, "Main" is used if null
     * @param page page name to use, "WebHome" is used if null
     * @param parameters list of parameters with values, parameters are omitted if null, "" is used is a value is null
     * @return the resulting absolute URL
     */
    protected static String createUrl(String action, String space, String page, Map<String, String> parameters)
    {
        return createUrl(action, space, page, parameters, true);
    }

    /**
     * Create the target URL from the given parameters. URL-escapes everything. Adds secret token if needed.
     * 
     * @param action action to use, "view" is used if null
     * @param space space name to use, "Main" is used if null
     * @param page page name to use, "WebHome" is used if null
     * @param parameters list of parameters with values, parameters are omitted if null, "" is used is a value is null
     * @param addLanguage add language=en if it is not set in the parameter map
     * @return the resulting absolute URL
     */
    protected static String createUrl(String action, String space, String page, Map<String, String> parameters,
        boolean addLanguage)
    {
        String url = URL_START + escapeUrl(action == null ? "view" : action) + "/";
        url += escapeUrl(space == null ? "Main" : space) + "/";
        url += escapeUrl(page == null ? "WebHome" : page);

        String delimiter = "?";
        if (parameters != null) {
            for (String parameter : parameters.keySet()) {
                if (parameter != null && !parameter.equals("")) {
                    String value = parameters.get(parameter);
                    url += delimiter + escapeUrl(parameter) + "=" + escapeUrl(value);
                }
                delimiter = "&";
            }
        }
        // special handling for language parameter to exclude false positives (language setting is saved in cookies and
        // sent on subsequent requests)
        if (addLanguage && (parameters == null || !parameters.containsKey(LANGUAGE))) {
            url += delimiter + LANGUAGE + "=en";
        }
        // some tests need to create or delete pages, we add secret token to avoid CSRF protection failures
        if ((action == null || !action.equals("edit"))
            && (parameters == null || !parameters.containsKey(SECRET_TOKEN))) {
            url += delimiter + SECRET_TOKEN + "=" + getSecretToken();
        }
        return url;
    }

    /**
     * Get the secret token used for CSRF protection. Caches 2 tokens (for logged in and logged out) on the first call.
     * 
     * @return anti-CSRF secret token, or empty string on error
     * @since 3.2M1
     */
    protected static String getSecretToken()
    {
        int index = isLoggedIn() ? 1 : 0;
        if (secretTokens[index] == null) {
            secretTokens[index] = getSecretTokenFromPage();
        }
        return secretTokens[index];
    }

    /**
     * Parse a wiki page to get the current secret token.
     * 
     * @return secret token
     */
    private static String getSecretTokenFromPage()
    {
        Pattern pattern = Pattern.compile("<input[^>]+" + SECRET_TOKEN + "[^>]+value=('|\")([^'\"]+)");
        try {
            String url = createUrl("edit", "Main", "WebHome", null);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(AbstractEscapingTest.getUrlContent(url).getContent())));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 2) {
                    return matcher.group(2);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        // something went really wrong
        System.out.println("WARNING, Failed to cache anti-CSRF secret token, some tests might fail!");
        return "";
    }
}

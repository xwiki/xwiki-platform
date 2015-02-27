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
package org.xwiki.test.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Xwiki;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

/**
 * Helper methods for testing, not related to a specific Page Object. Also made available to tests classes.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class TestUtils
{
    /**
     * @since 5.0M2
     */
    public static final UsernamePasswordCredentials ADMIN_CREDENTIALS = new UsernamePasswordCredentials("Admin",
        "admin");

    /**
     * @since 5.1M1
     */
    public static final UsernamePasswordCredentials SUPER_ADMIN_CREDENTIALS = new UsernamePasswordCredentials(
        "superadmin", "pass");

    /**
     * @since 5.0M2
     */
    public static final String BASE_URL = XWikiExecutor.URL + ":" + XWikiExecutor.DEFAULT_PORT + "/xwiki/";

    /**
     * @since 5.0M2
     */
    public static final String BASE_BIN_URL = BASE_URL + "bin/";

    /**
     * @since 5.0M2
     */
    public static final String BASE_REST_URL = BASE_URL + "rest/";

    private static PersistentTestContext context;

    /**
     * Used to convert Java object into its REST XML representation.
     */
    private static Marshaller marshaller;

    /**
     * Used to convert REST request XML result into its Java representation.
     */
    private static Unmarshaller unmarshaller;

    /**
     * Used to create REST Java resources.
     */
    private static ObjectFactory objectFactory;

    {
        {
            try {
                // Initialize REST related tools
                JAXBContext context =
                    JAXBContext.newInstance("org.xwiki.rest.model.jaxb"
                        + ":org.xwiki.extension.repository.xwiki.model.jaxb");
                marshaller = context.createMarshaller();
                unmarshaller = context.createUnmarshaller();
                objectFactory = new ObjectFactory();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Cached secret token. TODO cache for each user. */
    private String secretToken = null;

    private HttpClient adminHTTPClient;

    public TestUtils()
    {
        this.adminHTTPClient = new HttpClient();
        this.adminHTTPClient.getState().setCredentials(AuthScope.ANY, ADMIN_CREDENTIALS);
        this.adminHTTPClient.getParams().setAuthenticationPreemptive(true);
    }

    /** Used so that AllTests can set the persistent test context. */
    public static void setContext(PersistentTestContext context)
    {
        TestUtils.context = context;
    }

    protected XWikiWebDriver getDriver()
    {
        return context.getDriver();
    }

    public Session getSession()
    {
        return this.new Session(getDriver().manage().getCookies(), getSecretToken());
    }

    public void setSession(Session session)
    {
        WebDriver.Options options = getDriver().manage();
        options.deleteAllCookies();
        if (session != null) {
            for (Cookie cookie : session.getCookies()) {
                options.addCookie(cookie);
            }
        }
        if (session != null && !StringUtils.isEmpty(session.getSecretToken())) {
            this.secretToken = session.getSecretToken();
        } else {
            recacheSecretToken();
        }
    }

    public void loginAsSuperAdmin()
    {
        login(SUPER_ADMIN_CREDENTIALS.getUserName(), SUPER_ADMIN_CREDENTIALS.getPassword());
    }

    public void loginAsSuperAdminAndGotoPage(String pageURL)
    {
        loginAndGotoPage(SUPER_ADMIN_CREDENTIALS.getUserName(), SUPER_ADMIN_CREDENTIALS.getPassword(), pageURL);
    }

    public void loginAsAdmin()
    {
        login(ADMIN_CREDENTIALS.getUserName(), ADMIN_CREDENTIALS.getPassword());
    }

    public void loginAsAdminAndGotoPage(String pageURL)
    {
        loginAndGotoPage(ADMIN_CREDENTIALS.getUserName(), ADMIN_CREDENTIALS.getPassword(), pageURL);
    }

    public void login(String username, String password)
    {
        if (!username.equals(getLoggedInUserName())) {
            // Log in and direct to a non existent page so that it loads very fast and we don't incur the time cost of
            // going to the home page for example.
            // Also recache the CSRF token
            getDriver().get(getURLToLoginAndGotoPage(username, password, getURL("XWiki", "Register", "register")));
            recacheSecretTokenWhenOnRegisterPage();
            getDriver().get(getURLToNonExistentPage());
        }
    }

    public void loginAndGotoPage(String username, String password, String pageURL)
    {
        if (!username.equals(getLoggedInUserName())) {
            // Log in and direct to a non existent page so that it loads very fast and we don't incur the time cost of
            // going to the home page for example.
            // Also recache the CSRF token
            getDriver().get(getURLToLoginAndGotoPage(username, password, getURL("XWiki", "Register", "register")));
            recacheSecretTokenWhenOnRegisterPage();
            // Go to the page asked
            getDriver().get(pageURL);
        }
    }

    /**
     * Consider using setSession(null) because it will drop the cookies which is faster than invoking a logout action.
     */
    public String getURLToLogout()
    {
        return getURL("XWiki", "XWikiLogin", "logout");
    }

    public String getURLToLoginAsAdmin()
    {
        return getURLToLoginAs(ADMIN_CREDENTIALS.getUserName(), ADMIN_CREDENTIALS.getPassword());
    }

    public String getURLToLoginAsSuperAdmin()
    {
        return getURLToLoginAs(SUPER_ADMIN_CREDENTIALS.getUserName(), SUPER_ADMIN_CREDENTIALS.getPassword());
    }

    public String getURLToLoginAs(final String username, final String password)
    {
        return getURLToLoginAndGotoPage(username, password, null);
    }

    /**
     * @param pageURL the URL of the page to go to after logging in.
     * @return URL to accomplish login and goto.
     */
    public String getURLToLoginAsAdminAndGotoPage(final String pageURL)
    {
        return getURLToLoginAndGotoPage(ADMIN_CREDENTIALS.getUserName(), ADMIN_CREDENTIALS.getPassword(), pageURL);
    }

    /**
     * @param pageURL the URL of the page to go to after logging in.
     * @return URL to accomplish login and goto.
     */
    public String getURLToLoginAsSuperAdminAndGotoPage(final String pageURL)
    {
        return getURLToLoginAndGotoPage(SUPER_ADMIN_CREDENTIALS.getUserName(), SUPER_ADMIN_CREDENTIALS.getPassword(),
            pageURL);
    }

    /**
     * @param username the name of the user to log in as.
     * @param password the password for the user to log in.
     * @param pageURL the URL of the page to go to after logging in.
     * @return URL to accomplish login and goto.
     */
    public String getURLToLoginAndGotoPage(final String username, final String password, final String pageURL)
    {
        Map<String, String> parameters = new HashMap<String, String>()
        {
            {
                put("j_username", username);
                put("j_password", password);
                if (pageURL != null && pageURL.length() > 0) {
                    put("xredirect", pageURL);
                }
            }
        };
        return getURL("XWiki", "XWikiLogin", "loginsubmit", parameters);
    }

    /**
     * @return URL to a non existent page that loads very fast (we are using plain mode so that we don't even have to
     *         display the skin ;))
     */
    public String getURLToNonExistentPage()
    {
        return getURL("NonExistentSpace", "NonExistentPage", "view", "xpage=plain");
    }

    /**
     * After successful completion of this function, you are guaranteed to be logged in as the given user and on the
     * page passed in pageURL.
     */
    public void assertOnPage(final String pageURL)
    {
        final String pageURI = pageURL.replaceAll("\\?.*", "");
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return getDriver().getCurrentUrl().contains(pageURI);
            }
        });
    }

    public String getLoggedInUserName()
    {
        String loggedInUserName = null;
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(By.xpath("(//.[@id='tmUser']//a)[1]"));
        if (!elements.isEmpty()) {
            String href = elements.get(0).getAttribute("href");
            loggedInUserName = href.substring(href.lastIndexOf("/") + 1);
        }
        return loggedInUserName;
    }

    public void createUserAndLogin(final String username, final String password, Object... properties)
    {
        createUserAndLoginWithRedirect(username, password, getURLToNonExistentPage(), properties);
    }

    public void createUserAndLoginWithRedirect(final String username, final String password, String url,
        Object... properties)
    {
        createUser(username, password, getURLToLoginAndGotoPage(username, password, url), properties);
    }

    public void createUser(final String username, final String password, String redirectURL, Object... properties)
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("register", "1");
        parameters.put("xwikiname", username);
        parameters.put("register_password", password);
        parameters.put("register2_password", password);
        parameters.put("register_email", "");
        parameters.put("xredirect", redirectURL);
        parameters.put("form_token", getSecretToken());
        getDriver().get(getURL("XWiki", "Register", "register", parameters));
        recacheSecretToken();
        if (properties.length > 0) {
            updateObject("XWiki", username, "XWiki.XWikiUsers", 0, properties);
        }
    }

    public ViewPage gotoPage(String space, String page)
    {
        gotoPage(space, page, "view");
        return new ViewPage();
    }

    public void gotoPage(String space, String page, String action)
    {
        gotoPage(space, page, action, "");
    }

    /**
     * @since 3.5M1
     */
    public void gotoPage(String space, String page, String action, Object... queryParameters)
    {
        gotoPage(space, page, action, toQueryString(queryParameters));
    }

    public void gotoPage(String space, String page, String action, Map<String, ?> queryParameters)
    {
        gotoPage(space, page, action, toQueryString(queryParameters));
    }

    public void gotoPage(String space, String page, String action, String queryString)
    {
        // Only navigate if the current URL is different from the one to go to, in order to improve performances.
        gotoPage(getURL(space, page, action, queryString));
    }

    public void gotoPage(String url)
    {
        // Only navigate if the current URL is different from the one to go to, in order to improve performances.
        if (!getDriver().getCurrentUrl().equals(url)) {
            getDriver().get(url);
        }
    }

    public String getURLToDeletePage(String space, String page)
    {
        return getURL(space, page, "delete", "confirm=1");
    }

    /**
     * @param space the name of the space to delete
     * @return the URL that can be used to delete the specified pace
     * @since 4.5
     */
    public String getURLToDeleteSpace(String space)
    {
        return getURL(space, "WebHome", "deletespace", "confirm=1");
    }

    public ViewPage createPage(String space, String page, String content, String title)
    {
        return createPage(space, page, content, title, null);
    }

    public ViewPage createPage(String space, String page, String content, String title, String syntaxId)
    {
        return createPage(space, page, content, title, syntaxId, null);
    }

    public ViewPage createPage(String space, String page, String content, String title, String syntaxId,
        String parentFullPageName)
    {
        Map<String, String> queryMap = new HashMap<String, String>();
        if (content != null) {
            queryMap.put("content", content);
        }
        if (title != null) {
            queryMap.put("title", title);
        }
        if (syntaxId != null) {
            queryMap.put("syntaxId", syntaxId);
        }
        if (parentFullPageName != null) {
            queryMap.put("parent", parentFullPageName);
        }
        gotoPage(space, page, "save", queryMap);
        return new ViewPage();
    }

    /**
     * @since 5.1M2
     */
    public ViewPage createPageWithAttachment(String space, String page, String content, String title, String syntaxId,
        String parentFullPageName, String attachmentName, InputStream attachmentData) throws Exception
    {
        return createPageWithAttachment(space, page, content, title, syntaxId, parentFullPageName, attachmentName,
            attachmentData, null);
    }

    /**
     * @since 5.1M2
     */
    public ViewPage createPageWithAttachment(String space, String page, String content, String title, String syntaxId,
        String parentFullPageName, String attachmentName, InputStream attachmentData,
        UsernamePasswordCredentials credentials) throws Exception
    {
        ViewPage vp = createPage(space, page, content, title, syntaxId, parentFullPageName);
        attachFile(space, page, attachmentName, attachmentData, false, credentials);
        return vp;
    }

    /**
     * @since 5.1M2
     */
    public ViewPage createPageWithAttachment(String space, String page, String content, String title,
        String attachmentName, InputStream attachmentData) throws Exception
    {
        return createPageWithAttachment(space, page, content, title, null, null, attachmentName, attachmentData);
    }

    /**
     * @since 5.1M2
     */
    public ViewPage createPageWithAttachment(String space, String page, String content, String title,
        String attachmentName, InputStream attachmentData, UsernamePasswordCredentials credentials) throws Exception
    {
        ViewPage vp = createPage(space, page, content, title);
        attachFile(space, page, attachmentName, attachmentData, false, credentials);
        return vp;
    }

    public void deletePage(String space, String page)
    {
        getDriver().get(getURLToDeletePage(space, page));
    }

    /**
     * Accesses the URL to delete the specified space.
     *
     * @param space the name of the space to delete
     * @since 4.5
     */
    public void deleteSpace(String space)
    {
        getDriver().get(getURLToDeleteSpace(space));
    }

    public boolean pageExists(String space, String page)
    {
        boolean exists;
        try {
            executeGet(getURL(space, page), Status.OK.getStatusCode());
            exists = true;
        } catch (Exception e) {
            exists = false;
        }

        return exists;
    }

    /**
     * Get the URL to view a page.
     *
     * @param space the space in which the page resides.
     * @param page the name of the page.
     */
    public String getURL(String space, String page)
    {
        return getURL(space, page, "view");
    }

    /**
     * Get the URL of an action on a page.
     *
     * @param space the space in which the page resides.
     * @param page the name of the page.
     * @param action the action to do on the page.
     */
    public String getURL(String space, String page, String action)
    {
        return getURL(space, page, action, "");
    }

    /**
     * Get the URL of an action on a page with a specified query string.
     *
     * @param space the space in which the page resides.
     * @param page the name of the page.
     * @param action the action to do on the page.
     * @param queryString the query string to pass in the URL.
     */
    public String getURL(String space, String page, String action, String queryString)
    {
        return getURL(new String[] {space, page}, action, queryString);
    }

    private String getURL(String[] path, String action, String queryString)
    {
        StringBuilder builder = new StringBuilder(TestUtils.BASE_BIN_URL);

        builder.append(action);
        for (String element : path) {
            builder.append('/').append(escapeURL(element));
        }

        boolean needToAddSecretToken = !Arrays.asList("view", "register", "download").contains(action);
        if (needToAddSecretToken || !StringUtils.isEmpty(queryString)) {
            builder.append('?');
        }
        if (needToAddSecretToken) {
            addQueryStringEntry(builder, "form_token", getSecretToken());
            builder.append('&');
        }
        if (!StringUtils.isEmpty(queryString)) {
            builder.append(queryString);
        }

        return builder.toString();
    }

    /**
     * Get the URL of an action on a page with specified parameters. If you need to pass multiple parameters with the
     * same key, this function will not work.
     *
     * @param space the space in which the page resides.
     * @param page the name of the page.
     * @param action the action to do on the page.
     * @param queryParameters the parameters to pass in the URL, these will be automatically URL encoded.
     */
    public String getURL(String space, String page, String action, Map<String, ?> queryParameters)
    {
        return getURL(space, page, action, toQueryString(queryParameters));
    }

    /**
     * @param space the name of the space that contains the page with the specified attachment
     * @param page the name of the page that holds the attachment
     * @param attachment the attachment name
     * @param action the action to perform on the attachment
     * @param queryString the URL query string
     * @return the URL that performs the specified action on the specified attachment
     */
    public String getAttachmentURL(String space, String page, String attachment, String action, String queryString)
    {
        return getURL(new String[] {space, page, attachment}, action, queryString);
    }

    /**
     * @param space the name of the space that contains the page with the specified attachment
     * @param page the name of the page that holds the attachment
     * @param attachment the attachment name
     * @param action the action to perform on the attachment
     * @return the URL that performs the specified action on the specified attachment
     */
    public String getAttachmentURL(String space, String page, String attachment, String action)
    {
        return getAttachmentURL(space, page, attachment, action, "");
    }

    /**
     * @param space the name of the space that contains the page with the specified attachment
     * @param page the name of the page that holds the attachment
     * @param attachment the attachment name
     * @return the URL to download the specified attachment
     */
    public String getAttachmentURL(String space, String page, String attachment)
    {
        return getAttachmentURL(space, page, attachment, "download");
    }

    /**
     * (Re)-cache the secret token used for CSRF protection. A user with edit rights on Main.WebHome must be logged in.
     * This method must be called before {@link #getSecretToken()} is called and after each re-login.
     *
     * @see #getSecretToken()
     */
    public void recacheSecretToken()
    {
        // Save the current URL to be able to get back after we cache the secret token. We're not using the browser's
        // Back button because if the current page is the result of a POST request then by going back we are re-sending
        // the POST data which can have unexpected results. Moreover, some browsers pop up a modal confirmation box
        // which blocks the test.
        String previousURL = getDriver().getCurrentUrl();
        // Go to the registration page because the registration form uses secret token.
        gotoPage("XWiki", "Register", "register");
        recacheSecretTokenWhenOnRegisterPage();
        // Return to the previous page.
        getDriver().get(previousURL);
    }

    private void recacheSecretTokenWhenOnRegisterPage()
    {
        try {
            WebElement tokenInput = getDriver().findElement(By.xpath("//input[@name='form_token']"));
            this.secretToken = tokenInput.getAttribute("value");
        } catch (NoSuchElementException exception) {
            // Something is really wrong if this happens.
            System.out.println("Warning: Failed to cache anti-CSRF secret token, some tests might fail!");
            exception.printStackTrace();
        }
    }

    /**
     * Get the secret token used for CSRF protection. Remember to call {@link #recacheSecretToken()} first.
     *
     * @return anti-CSRF secret token, or empty string if the token was not cached
     * @see #recacheSecretToken()
     */
    public String getSecretToken()
    {
        if (this.secretToken == null) {
            System.out.println("Warning: No cached anti-CSRF token found. "
                + "Make sure to call recacheSecretToken() before getSecretToken(), otherwise this test might fail.");
            return "";
        }
        return this.secretToken;
    }

    /**
     * Encodes a given string so that it may be used as a URL component. Compatable with javascript decodeURIComponent,
     * though more strict than encodeURIComponent: all characters except [a-zA-Z0-9], '.', '-', '*', '_' are converted
     * to hexadecimal, and spaces are substituted by '+'.
     *
     * @param s
     */
    public String escapeURL(String s)
    {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * This class represents all cookies stored in the browser. Use with getSession() and setSession()
     */
    public class Session
    {
        private final Set<Cookie> cookies;

        private final String secretToken;

        private Session(final Set<Cookie> cookies, final String secretToken)
        {
            this.cookies = Collections.unmodifiableSet(new HashSet<Cookie>()
            {
                {
                    addAll(cookies);
                }
            });
            this.secretToken = secretToken;
        }

        private Set<Cookie> getCookies()
        {
            return this.cookies;
        }

        private String getSecretToken()
        {
            return this.secretToken;
        }
    }

    public boolean isInWYSIWYGEditMode()
    {
        return getDriver().findElements(By.xpath("//div[@id='editcolumn' and contains(@class, 'editor-wysiwyg')]"))
            .size() > 0;
    }

    public boolean isInWikiEditMode()
    {
        return getDriver().findElements(By.xpath("//div[@id='editcolumn' and contains(@class, 'editor-wiki')]")).size() > 0;
    }

    public boolean isInViewMode()
    {
        return getDriver().findElements(By.id("tmEdit")).size() > 0;
    }

    public boolean isInSourceViewMode()
    {
        return getDriver().findElements(By.xpath("//textarea[@class = 'wiki-code']")).size() > 0;
    }

    public boolean isInInlineEditMode()
    {
        String currentURL = getDriver().getCurrentUrl();
        // Keep checking the deprecated inline action for backward compatibility.
        return currentURL.contains("editor=inline") || currentURL.contains("/inline/");
    }

    public boolean isInRightsEditMode()
    {
        return getDriver().getCurrentUrl().contains("editor=rights");
    }

    public boolean isInObjectEditMode()
    {
        return getDriver().getCurrentUrl().contains("editor=object");
    }

    public boolean isInClassEditMode()
    {
        return getDriver().getCurrentUrl().contains("editor=class");
    }

    public boolean isInDeleteMode()
    {
        return getDriver().getCurrentUrl().contains("/delete/");
    }

    public boolean isInRenameMode()
    {
        return getDriver().getCurrentUrl().contains("xpage=rename");
    }

    public boolean isInCreateMode()
    {
        return getDriver().getCurrentUrl().contains("/create/");
    }

    /**
     * Forces the current user to be the Guest user by clearing all coookies.
     */
    public void forceGuestUser()
    {
        setSession(null);
    }

    public void addObject(String space, String page, String className, Object... properties)
    {
        gotoPage(space, page, "objectadd", toQueryParameters(className, null, properties));
    }

    public void addObject(String space, String page, String className, Map<String, ?> properties)
    {
        gotoPage(space, page, "objectadd", toQueryParameters(className, null, properties));
    }

    public void deleteObject(String space, String page, String className, int objectNumber)
    {
        StringBuilder queryString = new StringBuilder();

        queryString.append("classname=");
        queryString.append(escapeURL(className));
        queryString.append('&');
        queryString.append("classid=");
        queryString.append(objectNumber);

        gotoPage(space, page, "objectremove", queryString.toString());
    }

    public void updateObject(String space, String page, String className, int objectNumber, Map<String, ?> properties)
    {
        gotoPage(space, page, "save", toQueryParameters(className, objectNumber, properties));
    }

    public void updateObject(String space, String page, String className, int objectNumber, Object... properties)
    {
        // TODO: would be even quicker using REST
        gotoPage(space, page, "save", toQueryParameters(className, objectNumber, properties));
    }

    public ClassEditPage addClassProperty(String space, String page, String propertyName, String propertyType)
    {
        gotoPage(space, page, "propadd", "propname", propertyName, "proptype", propertyType);
        return new ClassEditPage();
    }

    /**
     * @since 3.5M1
     */
    public String toQueryString(Object... queryParameters)
    {
        return toQueryString(toQueryParameters(queryParameters));
    }

    /**
     * @since 3.5M1
     */
    public String toQueryString(Map<String, ?> queryParameters)
    {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, ?> entry : queryParameters.entrySet()) {
            addQueryStringEntry(builder, entry.getKey(), entry.getValue());
            builder.append('&');
        }

        return builder.toString();
    }

    /**
     * @sice 3.2M1
     */
    public void addQueryStringEntry(StringBuilder builder, String key, Object value)
    {
        if (value != null) {
            if (value instanceof Iterable) {
                for (Object element : (Iterable<?>) value) {
                    addQueryStringEntry(builder, key, element.toString());
                    builder.append('&');
                }
            } else {
                addQueryStringEntry(builder, key, value.toString());
            }
        } else {
            addQueryStringEntry(builder, key, (String) null);
        }
    }

    /**
     * @sice 3.2M1
     */
    public void addQueryStringEntry(StringBuilder builder, String key, String value)
    {
        builder.append(escapeURL(key));
        if (value != null) {
            builder.append('=');
            builder.append(escapeURL(value));
        }
    }

    /**
     * @since 3.5M1
     */
    public Map<String, ?> toQueryParameters(Object... properties)
    {
        return toQueryParameters(null, null, properties);
    }

    public Map<String, ?> toQueryParameters(String className, Integer objectNumber, Object... properties)
    {
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        queryParameters.put("classname", className);

        for (int i = 0; i < properties.length; i += 2) {
            int nextIndex = i + 1;
            queryParameters.put(toQueryParameterKey(className, objectNumber, (String) properties[i]),
                nextIndex < properties.length ? properties[nextIndex] : null);
        }

        return queryParameters;
    }

    public Map<String, ?> toQueryParameters(String className, Integer objectNumber, Map<String, ?> properties)
    {
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        if (className != null) {
            queryParameters.put("classname", className);
        }

        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            queryParameters.put(toQueryParameterKey(className, objectNumber, entry.getKey()), entry.getValue());
        }

        return queryParameters;
    }

    public String toQueryParameterKey(String className, Integer objectNumber, String key)
    {
        if (className == null) {
            return key;
        } else {
            StringBuilder keyBuilder = new StringBuilder(className);

            keyBuilder.append('_');

            if (objectNumber != null) {
                keyBuilder.append(objectNumber);
                keyBuilder.append('_');
            }

            keyBuilder.append(key);

            return keyBuilder.toString();
        }
    }

    public ObjectEditPage editObjects(String space, String page)
    {
        gotoPage(space, page, "edit", "editor=object");
        return new ObjectEditPage();
    }

    public ClassEditPage editClass(String space, String page)
    {
        gotoPage(space, page, "edit", "editor=class");
        return new ClassEditPage();
    }

    public String getVersion() throws Exception
    {
        Xwiki xwiki = getRESTResource("", null);

        return xwiki.getVersion();
    }

    public String getMavenVersion() throws Exception
    {
        String version = getVersion();

        int index = version.indexOf('-');
        if (index > 0) {
            version = version.substring(0, index) + "-SNAPSHOT";
        }

        return version;
    }

    public void attachFile(String space, String page, String name, File file, boolean failIfExists) throws Exception
    {
        InputStream is = new FileInputStream(file);
        try {
            attachFile(space, page, name, is, failIfExists);
        } finally {
            is.close();
        }
    }

    /**
     * @since 5.1M2
     */
    public void attachFile(String space, String page, String name, InputStream is, boolean failIfExists,
        UsernamePasswordCredentials credentials) throws Exception
    {
        if (credentials != null) {
            this.adminHTTPClient.getState().setCredentials(AuthScope.ANY, credentials);
        }
        attachFile(space, page, name, is, failIfExists);
    }

    public void attachFile(String space, String page, String name, InputStream is, boolean failIfExists)
        throws Exception
    {
        // make sure xwiki.Import exists
        if (!pageExists(space, page)) {
            createPage(space, page, null, null);
        }

        StringBuilder url = new StringBuilder(BASE_REST_URL);

        url.append("wikis/xwiki/spaces/");
        url.append(escapeURL(space));
        url.append("/pages/");
        url.append(escapeURL(page));
        url.append("/attachments/");
        url.append(escapeURL(name));

        if (failIfExists) {
            executePut(url.toString(), is, MediaType.APPLICATION_OCTET_STREAM, Status.CREATED.getStatusCode());
        } else {
            executePut(url.toString(), is, MediaType.APPLICATION_OCTET_STREAM, Status.CREATED.getStatusCode(),
                Status.ACCEPTED.getStatusCode());
        }
    }

    // FIXME: improve that with a REST API to directly import a XAR
    public void importXar(File file) throws Exception
    {
        // attach file
        attachFile("XWiki", "Import", file.getName(), file, false);

        // import file
        executeGet(
            BASE_BIN_URL + "import/XWiki/Import?historyStrategy=add&importAsBackup=true&ajax&action=import&name="
                + escapeURL(file.getName()), Status.OK.getStatusCode());
    }

    public InputStream getRESTInputStream(String resourceUri, Map<String, Object[]> queryParams, Object... elements)
        throws Exception
    {
        UriBuilder builder =
            UriBuilder.fromUri(BASE_REST_URL.substring(0, BASE_REST_URL.length() - 1)).path(
                !resourceUri.isEmpty() && resourceUri.charAt(0) == '/' ? resourceUri.substring(1) : resourceUri);

        if (queryParams != null) {
            for (Map.Entry<String, Object[]> entry : queryParams.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        String url = builder.build(elements).toString();

        return executeGet(url, Status.OK.getStatusCode()).getResponseBodyAsStream();
    }

    public byte[] getRESTBuffer(String resourceUri, Map<String, Object[]> queryParams, Object... elements)
        throws Exception
    {
        InputStream is = getRESTInputStream(resourceUri, queryParams, elements);

        byte[] buffer;
        try {
            buffer = IOUtils.toByteArray(is);
        } finally {
            is.close();
        }

        return buffer;
    }

    public <T> T getRESTResource(String resourceUri, Map<String, Object[]> queryParams, Object... elements)
        throws Exception
    {
        T resource;
        try (InputStream is = getRESTInputStream(resourceUri, queryParams, elements)) {
            resource = (T) unmarshaller.unmarshal(is);
        }

        return resource;
    }

    protected GetMethod executeGet(String uri, int expectedCode) throws Exception
    {
        GetMethod getMethod = new GetMethod(uri);

        int code = this.adminHTTPClient.executeMethod(getMethod);
        if (code != expectedCode) {
            throw new Exception("Failed to execute get [" + uri + "] with code [" + code + "]");
        }

        return getMethod;
    }

    protected PutMethod executePut(String uri, InputStream content, String mediaType, int... expectedCodes)
        throws Exception
    {
        PutMethod putMethod = new PutMethod(uri);
        RequestEntity entity = new InputStreamRequestEntity(content, mediaType);
        putMethod.setRequestEntity(entity);

        int code = this.adminHTTPClient.executeMethod(putMethod);
        if (!ArrayUtils.contains(expectedCodes, code)) {
            throw new Exception("Failed to execute put [" + uri + "] with code [" + code + "]");
        }

        return putMethod;
    }

    /**
     * Delete the latest version from the history of a page, using the {@code /deleteversions/} action.
     * 
     * @param space the space name of the page
     * @param page the name of the page
     * @since 7.0M2
     */
    public void deleteLatestVersion(String space, String page)
    {
        deleteVersion(space, page, "latest");
    }

    /**
     * Delete a specific version from the history of a page, using the {@code /deleteversions/} action.
     * 
     * @param space the space name of the page
     * @param page the name of the page
     * @param version the version to delete
     * @since 7.0M2
     */
    public void deleteVersion(String space, String page, String version)
    {
        deleteVersions(space, page, version, version);
    }

    /**
     * Delete an interval of versions from the history of a page, using the {@code /deleteversions/} action.
     * 
     * @param space the space name of the page
     * @param page the name of the page
     * @param v1 the starting version to delete
     * @param v2 the ending version to delete
     * @since 7.0M2
     */
    public void deleteVersions(String space, String page, String v1, String v2)
    {
        gotoPage(space, page, "deleteversions", "rev1", v1, "rev2", v2, "confirm", "1");
    }

    /**
     * Roll back a page to the previous version, using the {@code /rollback/} action.
     * 
     * @param space the space name of the page
     * @param page the name of the page
     * @since 7.0M2
     */
    public void rollbackToPreviousVersion(String space, String page)
    {
        rollBackTo(space, page, "previous");
    }

    /**
     * Roll back a page to the specified version, using the {@code /rollback/} action.
     * 
     * @param space the space name of the page
     * @param page the name of the page
     * @param version the version to rollback to
     * @since 7.0M2
     */
    public void rollBackTo(String space, String page, String version)
    {
        gotoPage(space, page, "rollback", "rev", version, "confirm", "1");
    }
}

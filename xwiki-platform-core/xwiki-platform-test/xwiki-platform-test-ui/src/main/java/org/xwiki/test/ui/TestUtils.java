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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.opentest4j.AssertionFailedError;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AbstractLocalizedEntityReference;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.Xwiki;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.classes.ClassPropertyResource;
import org.xwiki.rest.resources.objects.ObjectPropertyResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.stability.Unstable;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
    public static final UsernamePasswordCredentials ADMIN_CREDENTIALS =
        new UsernamePasswordCredentials("Admin", "admin");

    /**
     * @since 5.1M1
     */
    public static final UsernamePasswordCredentials SUPER_ADMIN_CREDENTIALS =
        new UsernamePasswordCredentials("superadmin", "pass");

    /**
     * @since 5.0M2
     * @deprecated since 7.3M1, use {@link #getBaseURL()} instead
     */
    @Deprecated
    public static final String BASE_URL = XWikiExecutor.URL + ":" + XWikiExecutor.DEFAULT_PORT
        + XWikiExecutor.DEFAULT_CONTEXT + "/";

    /**
     * @since 5.0M2
     * @deprecated since 7.3M1, use {@link #getBaseBinURL()} instead
     */
    @Deprecated
    public static final String BASE_BIN_URL = BASE_URL + "bin/";

    /**
     * @since 5.0M2
     * @deprecated since 7.3M1, use {@link #getBaseURL()} instead
     */
    @Deprecated
    public static final String BASE_REST_URL = BASE_URL + "rest/";

    /**
     * @since 7.3M1
     */
    public static final int[] STATUS_OK_NOT_FOUND =
        new int[] { Status.OK.getStatusCode(), Status.NOT_FOUND.getStatusCode() };

    /**
     * @since 7.3M1
     */
    public static final int[] STATUS_OK = new int[] { Status.OK.getStatusCode() };

    /**
     * @since 7.3M1
     */
    public static final int[] STATUS_NO_CONTENT = new int[] { Status.NO_CONTENT.getStatusCode() };

    /**
     * @since 8.3RC1
     */
    public static final int[] STATUS_NO_CONTENT_NOT_FOUND =
        new int[] { Status.NO_CONTENT.getStatusCode(), Status.NOT_FOUND.getStatusCode() };

    /**
     * @since 7.3M1
     */
    public static final int[] STATUS_CREATED_ACCEPTED =
        new int[] { Status.CREATED.getStatusCode(), Status.ACCEPTED.getStatusCode() };

    /**
     * @since 7.3M1
     */
    public static final int[] STATUS_CREATED = new int[] { Status.CREATED.getStatusCode() };

    /**
     * @since 9.5RC1
     */
    public static final int[] STATUS_ACCEPTED = new int[] { Status.ACCEPTED.getStatusCode() };

    private static final String MAIN_WIKI_NAME = "xwiki";

    private static PersistentTestContext context;

    private static ComponentManager componentManager;

    private static EntityReferenceResolver<String> relativeReferenceResolver;

    private static EntityReferenceSerializer<String> referenceSerializer;

    private static EntityReferenceResolver<String> referenceResolver;

    private static EntityReferenceSerializer<String> localReferenceSerializer;

    /**
     * Used to convert Java object into its REST XML representation.
     */
    private static Marshaller marshaller;

    /**
     * Used to convert REST request XML result into its Java representation.
     */
    private static Unmarshaller unmarshaller;

    private static String urlPrefix = XWikiExecutor.URL;

    /**
     * Cached secret token. TODO cache for each user.
     */
    private String secretToken = null;

    private HttpClient httpClient;

    /**
     * @since 15.2RC1
     */
    private WCAGUtils wcagUtils = new WCAGUtils();

    /**
     * @since 8.0M1
     */
    private List<XWikiExecutor> executors;

    /**
     * @since 7.3M1
     */
    private int currentExecutorIndex = 0;

    /**
     * @since 7.3M1
     */
    private String currentWiki = "xwiki";

    private RestTestUtils rest;

    public TestUtils()
    {
        this.httpClient = new HttpClient();

        setDefaultCredentials(SUPER_ADMIN_CREDENTIALS);

        this.rest = new RestTestUtils(this);
    }

    /**
     * @since 8.0M1
     */
    public XWikiExecutor getCurrentExecutor()
    {
        return this.executors != null && this.executors.size() > this.currentExecutorIndex
            ? this.executors.get(this.currentExecutorIndex) : null;
    }

    /**
     * @since 8.0M1
     */
    public void switchExecutor(int index)
    {
        this.currentExecutorIndex = index;
    }

    /**
     * @since 8.0M1
     */
    public void setExecutors(List<XWikiExecutor> executors)
    {
        this.executors = executors;
    }

    /** Used so that AllTests can set the persistent test context. */
    public static void setContext(PersistentTestContext context)
    {
        TestUtils.context = context;
    }

    public static void initializeComponent(ComponentManager componentManager) throws Exception
    {
        TestUtils.componentManager = componentManager;
        TestUtils.relativeReferenceResolver =
            TestUtils.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, "relative");
        TestUtils.referenceResolver = TestUtils.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING);
        TestUtils.referenceSerializer = TestUtils.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        TestUtils.localReferenceSerializer = TestUtils.componentManager.getInstance(
            new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class), "local");
    }

    public XWikiWebDriver getDriver()
    {
        return TestUtils.context.getDriver();
    }

    /**
     * @since 15.2RC1
     * @return the utils concerning wcag.
     */
    public WCAGUtils getWCAGUtils()
    {
        return this.wcagUtils;
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
                // Using a cookie for single component domain (i.e., without '.', like 'localhost' or 'xwikiweb') 
                // apparently triggers the following error in firefox:
                // org.openqa.selenium.UnableToSetCookieException:
                //[Exception... "Component returned failure code: 0x80070057 (NS_ERROR_ILLEGAL_VALUE)
                // [nsICookieManager.add]" nsresult: "0x80070057 (NS_ERROR_ILLEGAL_VALUE)"
                // location: "JS frame :: chrome://marionette/content/cookie.js :: cookie.add :: line 177" data: no]
                //
                // According to the following discussions:
                // - https://stackoverflow.com/questions/1134290/cookies-on-localhost-with-explicit-domain
                // - https://github.com/mozilla/geckodriver/issues/1579
                // a working solution is to put null in the cookie domain.
                // Now we might need to fix this in our real code, but the situation is not quite clear for me.
                if (cookie.getDomain() !=null && !cookie.getDomain().contains(".")) {
                    cookie = new Cookie(cookie.getName(), cookie.getValue(), null, cookie.getPath(),
                        cookie.getExpiry(), cookie.isSecure(), cookie.isHttpOnly());
                }
                options.addCookie(cookie);
            }
        }
        if (session != null && !StringUtils.isEmpty(session.getSecretToken())) {
            this.secretToken = session.getSecretToken();
        } else {
            recacheSecretToken();
        }
    }

    /**
     * @since 7.0RC1
     */
    public void setDefaultCredentials(String username, String password)
    {
        setDefaultCredentials(new UsernamePasswordCredentials(username, password));
    }

    /**
     * @since 7.0RC1
     */
    public UsernamePasswordCredentials setDefaultCredentials(UsernamePasswordCredentials defaultCredentials)
    {
        UsernamePasswordCredentials currentCredentials = getDefaultCredentials();

        if (defaultCredentials != null) {
            this.httpClient.getState().setCredentials(AuthScope.ANY, defaultCredentials);
            this.httpClient.getParams().setAuthenticationPreemptive(true);
        } else {
            this.httpClient.getState().clearCredentials();
            this.httpClient.getParams().setAuthenticationPreemptive(false);
        }

        return currentCredentials;
    }

    public UsernamePasswordCredentials getDefaultCredentials()
    {
        return (UsernamePasswordCredentials) this.httpClient.getState().getCredentials(AuthScope.ANY);
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
        loginAndGotoPage(username, password, null);
    }

    public void loginAndGotoPage(String username, String password, String pageURL)
    {
        loginAndGotoPage(username, password, pageURL, true);
    }

    /**
     * @since 11.6RC1
     */
    public void loginAndGotoPage(String username, String password, String pageURL, boolean checkLoginSuccess)
    {
        if (!username.equals(getLoggedInUserName())) {
            // Log in and direct to a non existent page so that it loads very fast and we don't incur the time cost of
            // going to the home page for example.
            // Also recache the CSRF token
            String destUrl = getURL("XWiki", "Register", "register");
            getDriver().get(getURLToLoginAndGotoPage(username, password, destUrl));

            if (checkLoginSuccess && !getDriver().getCurrentUrl().startsWith(destUrl)) {
                throw new RuntimeException(
                    String.format("Login failed with credentials: [%s] / [%s]. Was expecting to be on URL [%s] but "
                        + "was on [%s]. Page source is [%s]", username, password, destUrl,
                        getDriver().getCurrentUrl(), getDriver().getPageSource()));

            }
            recacheSecretTokenWhenOnRegisterPage();
            if (pageURL != null) {
                // Go to the page asked
                getDriver().get(pageURL);
            } else {
                getDriver().get(getURLToNonExistentPage());
            }

            setDefaultCredentials(username, password);
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
        By userAvatarInDrawer = By.id("tmUser");
        if (!getDriver().hasElementWithoutWaiting(userAvatarInDrawer)) {
            // Guest
            return null;
        }

        WebElement element = getDriver().findElementWithoutWaiting(userAvatarInDrawer);
        String href = element.getAttribute("href");
        String loggedInUserName = href.substring(href.lastIndexOf("/") + 1);

        // Return
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

        setDefaultCredentials(username, password);
    }

    public void createUser(final String username, final String password, String redirectURL, Object... properties)
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("register", "1");
        parameters.put("xwikiname", username);
        parameters.put("register_password", password);
        parameters.put("register2_password", password);
        parameters.put("register_email", "");
        if (!StringUtils.isEmpty(redirectURL)) {
            parameters.put("xredirect", redirectURL);
        }
        parameters.put("form_token", getSecretToken());
        getDriver().get(getURL("XWiki", "Register", "register", parameters));
        recacheSecretToken();
        if (properties.length > 0) {
            updateObject("XWiki", username, "XWiki.XWikiUsers", 0, properties);
        }
    }

    /**
     * Creates the Admin user, add it to the XWikiAdminGroup and login.
     * Note that this method requires to be superadmin to be effective.
     *
     * @since 12.2
     */
    public void createAdminUser()
    {
        createAdminUser(false);
    }

    /**
     * Creates the Admin user, add it to the XWikiAdminGroup and login.
     * Note that this method requires to be superadmin to be effective.
     *
     * @param programming true of the user should also be given programming right
     * @since 15.1RC1
     * @since 14.10.5
     */
    public void createAdminUser(boolean programming)
    {
        String username = ADMIN_CREDENTIALS.getUserName();
        String password = ADMIN_CREDENTIALS.getPassword();
        LocalDocumentReference userReference = new LocalDocumentReference("XWiki", username);
        Page userPage = rest().page(userReference);
        userPage.setObjects(new org.xwiki.rest.model.jaxb.Objects());
        org.xwiki.rest.model.jaxb.Object userObject = RestTestUtils.object("XWiki.XWikiUsers");

        // Set password
        userObject.getProperties().add(RestTestUtils.property("password", password));
        userPage.getObjects().getObjectSummaries().add(userObject);

        // Save the user page
        try {
            rest().save(userPage);
        } catch (Exception e) {
            fail("Failed to save the user with name [" + username + "]", e);
        }

        // Add the user to XWikiAllGroup
        try {
            rest().addObject(new LocalDocumentReference("XWiki", "XWikiAllGroup"), "XWiki.XWikiGroups", "member", serializeReference(userReference));
        } catch (Exception e) {
            fail("Failed to add the user in the XWikiAllGroup group", e);
        }

        // Add the user to XWikiAdminGroup group (before we login as the user does not have admin right at first)
        try {
            rest().addObject(new LocalDocumentReference("XWiki", "XWikiAdminGroup"), "XWiki.XWikiGroups", "member", serializeReference(userReference));
        } catch (Exception e) {
            fail("Failed to add the user in the XWikiAdminGroup group", e);
        }

        // Give ADMIN right (and maybe PROGRAMMING right) to XWikiAdminGroup
        setGlobalRights("XWiki.XWikiAdminGroup", "", programming ? "admin,programming" : "admin", true);

        // Also login as Admin user
        loginAsAdmin();
    }

    /**
     * Add or update a {@code XWikiGlobalRights} xobject to the current wiki's {@code XWikiPreferences} document.
     *
     * @param groups the comma-separated list of groups that will have the rights (e.g. {@code XWiki.XWikiAdminGroup}.
     *               Can be empty or null
     * @param users the comma-separated list of users that will have the rights (e.g. {@code XWiki.Admin}. Can be
     *              empty of null
     * @param rights the comma-separated list of rights to give (e.g. {@code edit,admin})
     * @param enabled true if the rights should be allowed, false if they should be disabled
     * @since 12.2
     */
    public void setGlobalRights(String groups, String users, String rights, boolean enabled)
    {
        setRights(new LocalDocumentReference("XWiki", "XWikiPreferences"), "XWiki.XWikiGlobalRights", groups, users,
            rights, enabled);
    }

    /**
     * Add or update a {@code XWikiRights} xobject to the document specified in the passed entity reference.
     *
     * @param entityReference the reference to the document for which to set rights for
     * @param groups the comma-separated list of groups that will have the rights (e.g. {@code XWiki.XWikiAdminGroup}.
     *               Can be empty or null
     * @param users the comma-separated list of users that will have the rights (e.g. {@code XWiki.Admin}. Can be
     *              empty of null
     * @param rights the comma-separated list of rights to give (e.g. {@code edit,admin})
     * @param enabled true if the rights should be allowed, false if they should be denied
     * @since 12.2
     */
    public void setRights(EntityReference entityReference, String groups, String users, String rights, boolean enabled)
    {
        setRights(entityReference, "XWiki.XWikiRights", groups, users, rights, enabled);
    }

    /**
     * Add or update a {@code XWikiRights} xobject to the specified space reference.
     *
     * @param space the reference to the space for which to set rights for
     * @param groups the comma-separated list of groups that will have the rights (e.g. {@code XWiki.XWikiAdminGroup}.
     *               Can be empty or null
     * @param users the comma-separated list of users that will have the rights (e.g. {@code XWiki.Admin}. Can be
     *              empty of null
     * @param rights the comma-separated list of rights to give (e.g. {@code edit,admin})
     * @param enabled true if the rights should be allowed, false if they should be denied
     * @since 14.10
     */
    public void setRightsOnSpace(SpaceReference space, String groups, String users, String rights, boolean enabled)
    {
        DocumentReference documentReference = new DocumentReference("WebPreferences", space);
        setRights(documentReference, "XWiki.XWikiGlobalRights", groups, users, rights, enabled);
    }

    private void setRights(EntityReference entityReference, String rightClassName, String groups, String users,
        String rights, boolean enabled)
    {
        // Normalize users and groups
        String normalizedUsers = users == null ? "" : users;
        String normalizedGroups = groups == null ? "" : groups;

        // Add new rights object
        try {
            rest().addObject(entityReference, rightClassName,
                "groups", normalizedGroups,
                "users", normalizedUsers,
                "levels", rights,
                "allow", enabled ? 1 : 0);
        } catch (Exception e) {
            fail("Failed to add rights object of class [" + rightClassName + "] with groups [" + normalizedGroups
                + "], users [" + normalizedUsers + "], rights [" + rights + "] and enabled [" + enabled + "]", e);
        }
    }

    public ViewPage gotoPage(String space, String page)
    {
        gotoPage(space, page, "view");
        return new ViewPage();
    }

    /**
     * @since 7.2M2
     */
    public ViewPage gotoPage(EntityReference reference)
    {
        gotoPage(reference, "view");
        return new ViewPage();
    }

    public void gotoPage(String space, String page, String action)
    {
        gotoPage(space, page, action, "");
    }

    /**
     * @since 7.2M2
     */
    public void gotoPage(EntityReference reference, String action)
    {
        gotoPage(reference, action, "");
    }

    /**
     * @since 3.5M1
     */
    public void gotoPage(String space, String page, String action, Object... queryParameters)
    {
        gotoPage(space, page, action, toQueryString(queryParameters));
    }

    /**
     * @since 11.3RC1
     */
    public void gotoPage(EntityReference reference, String action, Object... queryParameters)
    {
        gotoPage(reference, action, toQueryString(queryParameters));
    }

    public void gotoPage(String space, String page, String action, Map<String, ?> queryParameters)
    {
        gotoPage(Collections.singletonList(space), page, action, queryParameters);
    }

    /**
     * @since 7.2M2
     */
    public void gotoPage(List<String> spaces, String page, String action, Map<String, ?> queryParameters)
    {
        gotoPage(spaces, page, action, toQueryString(queryParameters));
    }

    /**
     * @since 7.2M2
     */
    public void gotoPage(EntityReference reference, String action, Map<String, ?> queryParameters)
    {
        gotoPage(reference, action, toQueryString(queryParameters));
    }

    public void gotoPage(String space, String page, String action, String queryString)
    {
        gotoPage(Collections.singletonList(space), page, action, queryString);
    }

    /**
     * @since 7.2M2
     */
    public void gotoPage(List<String> spaces, String page, String action, String queryString)
    {
        gotoPage(getURL(spaces, page, action, queryString));
    }

    /**
     * @since 7.2M2
     */
    public void gotoPage(EntityReference reference, String action, String queryString)
    {
        gotoPage(reference, action, queryString, null);
    }

    /**
     * @since 10.9
     */
    public void gotoPage(EntityReference reference, String action, String queryString, String fragment)
    {
        gotoPage(getURL(reference, action, queryString, fragment));

        // Update current wiki
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
        if (wikiReference != null) {
            this.currentWiki = wikiReference.getName();
        }
    }

    public void gotoPage(String url)
    {
        getDriver().get(url);
    }

    public String getURLToDeletePage(String space, String page)
    {
        return getURL(space, page, "delete", "confirm=1");
    }

    /**
     * @since 7.2M2
     */
    public String getURLToDeletePage(EntityReference reference)
    {
        return getURLToDeletePage(reference, false);
    }

    /**
     * @since 12.9RC1
     */
    public String getURLToDeletePage(EntityReference reference, boolean affectChildren)
    {
        String queryString = "confirm=1&async=false";
        if (affectChildren) {
            queryString += "&affectChildren=true";
        }
        return getURL(reference, "delete", queryString);
    }

    /**
     * @param space the name of the space to delete
     * @return the URL that can be used to delete the specified pace
     * @since 4.5
     */
    public String getURLToDeleteSpace(String space)
    {
        return getURL(space, "WebHome", "deletespace", "confirm=1&async=false&affectChidlren=on");
    }

    /**
     * @param space the reference of the space to delete
     * @return the URL that can be used to delete the specified pace
     * @since 14.1RC1
     */
    public String getURLToDeleteSpace(EntityReference space)
    {
        return getURL(space, "WebHome", "deletespace", "confirm=1&async=false&affectChidlren=on");
    }

    public ViewPage createPage(String space, String page, String content, String title)
    {
        return createPage(Collections.singletonList(space), page, content, title);
    }

    /**
     * @since 11.5RC1
     * @since 11.3.1
     */
    public ViewPage createPage(EntityReference reference, String content)
    {
        return createPage(reference, content, this.serializeReference(reference), null);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(EntityReference reference, String content, String title)
    {
        return createPage(reference, content, title, null);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(List<String> spaces, String page, String content, String title)
    {
        return createPage(spaces, page, content, title, null);
    }

    public ViewPage createPage(String space, String page, String content, String title, String syntaxId)
    {
        return createPage(Collections.singletonList(space), page, content, title, syntaxId);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(EntityReference reference, String content, String title, String syntaxId)
    {
        return createPage(reference, content, title, syntaxId, null);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(List<String> spaces, String page, String content, String title, String syntaxId)
    {
        return createPage(spaces, page, content, title, syntaxId, null);
    }

    public ViewPage createPage(String space, String page, String content, String title, String syntaxId,
        String parentFullPageName)
    {
        return createPage(Collections.singletonList(space), page, content, title, syntaxId, parentFullPageName);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(List<String> spaces, String page, String content, String title, String syntaxId,
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
        gotoPage(spaces, page, "save", queryMap);
        return new ViewPage();
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPage(EntityReference reference, String content, String title, String syntaxId,
        String parentFullPageName)
    {
        Map<String, String> queryMap = new HashMap<>();
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
        gotoPage(reference, "save", queryMap);
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
        return createPageWithAttachment(Collections.singletonList(space), page, content, title, syntaxId,
            parentFullPageName, attachmentName, attachmentData, credentials);
    }

    /**
     * @since 7.2M2
     */
    public ViewPage createPageWithAttachment(List<String> spaces, String page, String content, String title,
        String syntaxId, String parentFullPageName, String attachmentName, InputStream attachmentData,
        UsernamePasswordCredentials credentials) throws Exception
    {
        ViewPage vp = createPage(spaces, page, content, title, syntaxId, parentFullPageName);
        attachFile(spaces, page, attachmentName, attachmentData, false, credentials);
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

    /**
     * @since 12.2
     */
    public ViewPage createPageWithAttachment(EntityReference reference, String content, String title,
        String attachmentName, InputStream attachmentData, UsernamePasswordCredentials credentials) throws Exception
    {
        ViewPage vp = createPage(reference, content, title);
        attachFile(reference, attachmentName, attachmentData, false, credentials);
        return vp;
    }

    /**
     * @since 12.2
     */
    public ViewPage createPageWithAttachment(EntityReference reference, String content, String title,
        String attachmentName, InputStream attachmentData) throws Exception
    {
        return createPageWithAttachment(reference, content, title, attachmentName, attachmentData, null);
    }

    public void deletePage(String space, String page)
    {
        getDriver().get(getURLToDeletePage(space, page));
    }

    /**
     * @since 7.2M2
     */
    public void deletePage(EntityReference reference)
    {
        deletePage(reference, false);
    }

    /**
     * @since 12.9RC1
     */
    public void deletePage(EntityReference reference, boolean affectChildren)
    {
        getDriver().get(getURLToDeletePage(reference, affectChildren));
    }

    /**
     * @since 7.2M2
     */
    public EntityReference resolveDocumentReference(String referenceAsString)
    {
        return referenceResolver.resolve(referenceAsString, EntityType.DOCUMENT);
    }

    /**
     * @since 7.2M3
     */
    public EntityReference resolveSpaceReference(String referenceAsString)
    {
        return referenceResolver.resolve(referenceAsString, EntityType.SPACE);
    }

    /**
     * @since 7.2RC1
     */
    public String serializeReference(EntityReference reference)
    {
        return referenceSerializer.serialize(reference);
    }

    /**
     * @since 16.8.0RC1
     * @since 16.4.5
     * @since 15.10.14
     */
    public String serializeLocalReference(EntityReference reference)
    {
        return localReferenceSerializer.serialize(reference);
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

    /**
     * Accesses the URL to delete the specified space.
     *
     * @param space the reference of the space to delete
     * @since 14.1RC1
     */
    public void deleteSpace(EntityReference space)
    {
        getDriver().get(getURLToDeleteSpace(space));
    }

    public boolean pageExists(String space, String page) throws Exception
    {
        return rest().exists(new LocalDocumentReference(space, page));
    }

    /**
     * @since 7.2M2
     */
    public boolean pageExists(List<String> spaces, String page) throws Exception
    {
        return rest().exists(new LocalDocumentReference(spaces, page));
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
        return getURL(action, new String[] { space, page }, queryString);
    }

    /**
     * @since 7.3M1
     */
    public String getURL(List<String> spaces, String page)
    {
        return getURL(spaces, page, "view", "");
    }

    /**
     * @since 7.2M2
     */
    public String getURL(List<String> spaces, String page, String action, String queryString)
    {
        List<String> path = new ArrayList<>(spaces);
        path.add(page);
        return getURL(action, path.toArray(new String[] {}), queryString);
    }

    /**
     * @since 12.2
     */
    public String getURL(EntityReference reference)
    {
        return getURL(reference, "view", "");
    }

    /**
     * @since 7.2M2
     */
    public String getURL(EntityReference reference, String action, String queryString)
    {
        return getURL(reference, action, queryString, null);
    }

    /**
     * @since 10.9
     */
    public String getURL(EntityReference reference, String action, String queryString, String fragment)
    {
        Serializable locale = reference.getParameters().get("locale");
        if (locale != null) {
            queryString += "&language=" + locale;
        }
        return getURL(action, extractListFromReference(reference).toArray(new String[] {}), queryString, fragment,
            reference.extractReference(EntityType.WIKI).getName());
    }

    /**
     * @since 9.7RC1
     */
    public String executeAndGetBodyAsString(EntityReference reference, Map<String, ?> queryParameters) throws Exception
    {
        gotoPage(getURL(reference, "get", toQueryString(queryParameters)));
        
        return getDriver().findElementWithoutWaiting(By.tagName("body")).getText();
    }

    /**
     * @since 15.1RC1
     * @since 14.10.5
     */
    public String executeWiki(String wikiContent, Syntax wikiSyntax) throws Exception
    {
        return executeWiki(wikiContent, wikiSyntax, null);
    }

    /**
     * @since 16.4.0RC1
     * @since 15.10.11
     * @since 14.10.22
     */
    public String executeWikiPlain(String wikiContent, Syntax wikiSyntax) throws Exception
    {
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("outputSyntax", "plain");

        return executeWiki(wikiContent, wikiSyntax, queryParameters);
    }

    /**
     * @since 16.4.0RC1
     * @since 15.10.11
     * @since 14.10.22
     */
    public String executeWiki(String wikiContent, Syntax wikiSyntax, Map<String, String> queryParameters) throws Exception
    {
        LocalDocumentReference reference =
            new LocalDocumentReference(List.of("Test", "Execute"), UUID.randomUUID().toString());

        // Remember the current credentials
        UsernamePasswordCredentials currentCredentials = getDefaultCredentials();

        try {
            // Make sure the page is saved with superadmin author
            setDefaultCredentials(SUPER_ADMIN_CREDENTIALS);

            // Save the page with the content to execute
            rest().savePage(reference, wikiContent, wikiSyntax.toIdString(), null, null);
        } finally {
            // Restore initial credentials
            setDefaultCredentials(currentCredentials);
        }

        // Execute the content and return the result
        return executeAndGetBodyAsString(reference, queryParameters);
    }

    /**
     * @since 7.2M2
     */
    public String getURLFragment(EntityReference reference)
    {
        return StringUtils.join(extractListFromReference(reference), "/");
    }

    private List<String> extractListFromReference(EntityReference reference)
    {
        List<String> path = new ArrayList<>();
        // Add the spaces
        EntityReference spaceReference = reference.extractReference(EntityType.SPACE);
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
        for (EntityReference singleReference : spaceReference.removeParent(wikiReference).getReversedReferenceChain()) {
            path.add(singleReference.getName());
        }
        // Add the page for attachments
        if (reference.getType() == EntityType.ATTACHMENT) {
            path.add(reference.getParent().getName());
        }
        if (reference.getType() == EntityType.DOCUMENT || reference.getType() == EntityType.ATTACHMENT) {
            path.add(reference.getName());
        }
        return path;
    }

    /**
     * @since 7.3M1
     */
    public String getCurrentWiki()
    {
        return this.currentWiki;
    }

    /**
     * @since 14.5
     */
    public void setCurrentWiki(String currentWiki)
    {
        this.currentWiki = currentWiki;
    }

    /**
     * @since 7.3M1
     */
    public String getBaseURL()
    {
        String baseURL;

        // If the URL has the port specified then consider it's a full URL and use it, otherwise add the port and the
        // webapp context
        if (TestUtils.urlPrefix.matches("http://.*:[0-9]+/.*")) {
            baseURL = TestUtils.urlPrefix;
        } else {
            baseURL = TestUtils.urlPrefix + ":"
                + (getCurrentExecutor() != null ? getCurrentExecutor().getPort() : XWikiExecutor.DEFAULT_PORT)
                + XWikiExecutor.DEFAULT_CONTEXT;
        }

        if (!baseURL.endsWith("/")) {
            baseURL = baseURL + "/";
        }

        return baseURL;
    }

    /**
     * @since 10.6RC1
     */
    public static void setURLPrefix(String urlPrefix)
    {
        TestUtils.urlPrefix = urlPrefix;
    }

    /**
     * @since 7.3M1
     */
    public String getBaseBinURL()
    {
        return getBaseBinURL(this.currentWiki);
    }

    /**
     * @since 11.2RC1
     */
    public String getBaseBinURL(String wiki)
    {
        String wikiName = MAIN_WIKI_NAME;
        if (!StringUtils.isEmpty(wiki)) {
            wikiName = wiki;
        } else if (!StringUtils.isEmpty(this.currentWiki)) {
            wikiName = this.currentWiki;
        }
        return getBaseURL() + (wikiName.equals(MAIN_WIKI_NAME) ? "bin/" : "wiki/" + wikiName + '/');
    }

    /**
     * @since 7.2M1
     */
    public String getURL(String action, String[] path, String queryString)
    {
        return getURL(action, path, queryString, null, null);
    }

    /**
     * @since 10.9
     */
    public String getURL(String action, String[] path, String queryString, String fragment)
    {
        return getURL(action, path, queryString, fragment, null);
    }

    /**
     * @since 16.10.0RC1
     * @since 15.10.14
     * @since 16.4.6
     */
    @Unstable
    public String getURL(String action, String[] path, String queryString, String fragment, String wikiName)
    {
        StringBuilder builder = new StringBuilder(getBaseBinURL(wikiName));

        if (!StringUtils.isEmpty(action)) {
            builder.append(action).append('/');
        }
        List<String> escapedPath = new ArrayList<>();
        for (String element : path) {
            escapedPath.add(escapeURL(element));
        }
        builder.append(StringUtils.join(escapedPath, '/'));

        boolean needToAddSecretToken = !Arrays.asList("view", "register", "download", "export").contains(action);
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

        if (!StringUtils.isEmpty(fragment)) {
            builder.append('#').append(fragment);
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
        return getURL(action, new String[] { space, page, attachment }, queryString);
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
        gotoPage(getCurrentWiki(), "Register", "register");
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
     * Sets the secret token used for CSRF protection. Use this method to restore a token that was previously saved. If
     * you want to cache the current token you should use {@link #recacheSecretToken()} instead.
     *
     * @param secretToken the new secret token to use
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public void setSecretToken(String secretToken)
    {
        this.secretToken = secretToken;
    }

    /**
     *This class represents all cookies stored in the browser. Use with getSession() and setSession()
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
        return getDriver().findElements(By.xpath("//div[@id='editcolumn' and contains(@class, 'editor-wiki')]"))
            .size() > 0;
    }

    public boolean isInViewMode()
    {
        return !getDriver().hasElementWithoutWaiting(By.id("editMeta"));
    }

    public boolean isInSourceViewMode()
    {
        String currentURL = getDriver().getCurrentUrl();
        return currentURL.contains("/view/") && currentURL.contains("viewer=code");
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

    public boolean isInAdminMode()
    {
        return getDriver().getCurrentUrl().contains("/admin/");
    }

    /**
     * Verify if the passed reference corresponds to the current page, independently of the wiki.
     * Throws an {@link AssertionFailedError} if it's not the case.
     *
     * @param reference the reference to the document to check
     * @since 12.2
     */
    public void assertOnPage(EntityReference reference)
    {
        if (EntityType.DOCUMENT.equals(reference.getType())) {
            BasePage bp = new BasePage();
            assertEquals(localReferenceSerializer.serialize(reference), bp.getMetaDataValue("document"));
        } else {
            throw new IllegalArgumentException("You should pass a reference to a document");
        }
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

    /**
     * @since 7.2RC1
     */
    public void addObject(EntityReference reference, String className, Object... properties)
    {
        gotoPage(reference, "objectadd", toQueryParameters(className, null, properties));
    }

    /**
     * @since 7.3M2
     */
    public void addObject(EntityReference reference, String className, Map<String, ?> properties)
    {
        gotoPage(reference, "objectadd", toQueryParameters(className, null, properties));
    }

    public void addObject(String space, String page, String className, Map<String, ?> properties)
    {
        gotoPage(space, page, "objectadd", toQueryParameters(className, null, properties));
    }

    public void deleteObject(String space, String page, String className, int objectNumber) throws Exception
    {
        TestUtils.assertStatusCodes(
            rest().executeDelete(ObjectResource.class, getCurrentWiki(), space, page, className, objectNumber), true,
            STATUS_NO_CONTENT_NOT_FOUND);
    }

    public void updateObject(String space, String page, String className, int objectNumber, Map<String, ?> properties)
    {
        gotoPage(space, page, "save", toQueryParameters(className, objectNumber, properties));
    }

    public void updateObject(String space, String page, String className, int objectNumber, Object... properties)
    {
        updateObject(Collections.singletonList(space), page, className, objectNumber, properties);
    }

    /**
     * @since 11.5RC1
     * @since 11.3.1
     */
    public void updateObject(EntityReference entityReference, String className, int objectNumber,
        Object... properties)
    {
        // TODO: would be even quicker using REST
        Map<String, Object> queryParameters =
            (Map<String, Object>) toQueryParameters(className, objectNumber, properties);

        // Append the updateOrCreate objectPolicy since we always want this in our tests.
        queryParameters.put("objectPolicy", "updateOrCreate");

        gotoPage(entityReference, "save", queryParameters);
    }

    /**
     * @since 8.3RC1
     */
    public void updateObject(List<String> spaces, String page, String className, int objectNumber, Object... properties)
    {
        // TODO: would be even quicker using REST
        Map<String, Object> queryParameters =
            (Map<String, Object>) toQueryParameters(className, objectNumber, properties);

        // Append the updateOrCreate objectPolicy since we always want this in our tests.
        queryParameters.put("objectPolicy", "updateOrCreate");

        gotoPage(spaces, page, "save", queryParameters);
    }

    /**
     * @since 11.3RC1
     */
    public void addClassProperty(EntityReference reference, String propertyName, String propertyType)
    {
        gotoPage(reference, "propadd", "propname", propertyName, "proptype", propertyType);
    }

    /**
     * @since 11.3RC1
     */
    public void updateClassProperty(EntityReference reference, Object... queryParameters)
    {
        gotoPage(reference, "propupdate", queryParameters);
    }

    public void addClassProperty(String space, String page, String propertyName, String propertyType)
    {
        gotoPage(space, page, "propadd", "propname", propertyName, "proptype", propertyType);
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

        if (queryParameters != null) {
            for (Map.Entry<String, ?> entry : queryParameters.entrySet()) {
                addQueryStringEntry(builder, entry.getKey(), entry.getValue());
                builder.append('&');
            }
        }

        return builder.toString();
    }

    /**
     * @since 3.2M1
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
     * @since 3.2M1
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

    /**
     * Goes to a page in edit class mode.
     *
     * @param reference a document reference
     * @return the {@link ClassEditPage} Page Object for the page
     * @since 14.0RC1
     */
    public ClassEditPage editClass(DocumentReference reference)
    {
        gotoPage(reference, "edit", "editor=class");
        return new ClassEditPage();
    }

    public String getVersion() throws Exception
    {
        Xwiki xwiki = rest().getResource("", null);

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
        attachFile(Collections.singletonList(space), page, name, is, failIfExists, credentials);
    }

    /**
     * @since 7.2M2
     */
    public void attachFile(List<String> spaces, String page, String name, InputStream is, boolean failIfExists,
        UsernamePasswordCredentials credentials) throws Exception
    {
        UsernamePasswordCredentials currentCredentials = getDefaultCredentials();

        try {
            if (credentials != null) {
                setDefaultCredentials(credentials);
            }
            attachFile(spaces, page, name, is, failIfExists);
        } finally {
            setDefaultCredentials(currentCredentials);
        }
    }

    public void attachFile(String space, String page, String name, InputStream is, boolean failIfExists)
        throws Exception
    {
        attachFile(Collections.singletonList(space), page, name, is, failIfExists);
    }

    /**
     * @since 7.2M2
     */
    public void attachFile(List<String> spaces, String page, String name, InputStream is, boolean failIfExists)
        throws Exception
    {
        AttachmentReference reference =
            new AttachmentReference(name, new DocumentReference(getCurrentWiki(), spaces, page));

        attachFile(reference, is, failIfExists);
    }

    /**
     * @since 7.3M1
     */
    public void attachFile(EntityReference pageReference, String name, InputStream is, boolean failIfExists)
        throws Exception
    {
        EntityReference reference = new EntityReference(name, EntityType.ATTACHMENT, pageReference);

        attachFile(reference, is, failIfExists);
    }

    /**
     * @since 7.3M1
     */
    public void attachFile(EntityReference reference, Object is, boolean failIfExists) throws Exception
    {
        rest().attachFile(reference, is, failIfExists);
    }

    /**
     * @since 12.2
     */
    public void attachFile(EntityReference pageReference, String name, InputStream is, boolean failIfExists,
        UsernamePasswordCredentials credentials) throws Exception
    {
        UsernamePasswordCredentials currentCredentials = getDefaultCredentials();
        EntityReference reference = new EntityReference(name, EntityType.ATTACHMENT, pageReference);

        try {
            if (credentials != null) {
                setDefaultCredentials(credentials);
            }
            attachFile(reference, is, failIfExists);
        } finally {
            if (credentials != null) {
                setDefaultCredentials(currentCredentials);
            }
        }
    }

    public void deleteAttachement(EntityReference pageReference, String filename) throws Exception
    {
        EntityReference reference = new EntityReference(filename, EntityType.ATTACHMENT, pageReference);
        deleteAttachement(reference);
    }

    public void deleteAttachement(EntityReference reference) throws Exception
    {
        rest().deleteAttachement(reference);
    }

    // FIXME: improve that with a REST API to directly import a XAR
    public void importXar(File file) throws Exception
    {
        // attach file
        attachFile("XWiki", "Import", file.getName(), file, false);

        // import file
        executeGet(
            getBaseBinURL() + "import/XWiki/Import?historyStrategy=add&importAsBackup=true&ajax&action=import&name="
                + escapeURL(file.getName()),
            Status.OK.getStatusCode());
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

    /**
     * Set the hierarchy mode used in the wiki
     *
     * @param mode the mode to use ("reference" or "parentchild")
     * @since 7.2M2
     */
    public void setHierarchyMode(String mode)
    {
        setPropertyInXWikiPreferences("core.hierarchyMode", "String", mode);
    }

    /**
     * Add and set a property into XWiki.XWikiPreferences. Create XWiki.XWikiPreferences if it does not exist.
     *
     * @param propertyName name of the property to set
     * @param propertyType the type of the property to add
     * @param value value to set to the property
     * @since 7.2M2
     */
    public void setPropertyInXWikiPreferences(String propertyName, String propertyType, Object value)
    {
        addClassProperty("XWiki", "XWikiPreferences", propertyName, propertyType);
        gotoPage("XWiki", "XWikiPreferences", "edit", "editor", "object");
        ObjectEditPage objectEditPage = new ObjectEditPage();
        if (objectEditPage.hasObject("XWiki.XWikiPreferences")) {
            updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, propertyName, value);
        } else {
            addObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", propertyName, value);
        }
    }

    /**
     * Set global xwiki configuration options (as if the xwiki.cfg file had been modified). This is useful for testing
     * configuration options. This requires the {@code Test.XWikiConfigurationPageForTest} page to have Programming
     * Rights (if the PR checker is enabled, you'll need to exclude this reference so that it can have PR).
     *
     * @param configuration the configuration in {@link Properties} format. For example "param1=value2\nparam2=value2"
     * @throws IOException if an error occurs while parsing the configuration
     */
    public void setPropertyInXWikiCfg(String configuration) throws IOException
    {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(configuration.getBytes()));
        StringBuilder sb = new StringBuilder();

        sb.append("{{velocity}}\n"
                + "#set($config = $!services.component.getInstance(\"org.xwiki.configuration."
                + "ConfigurationSource\", \"xwikicfg\"))\n"
                + "#set($props = $config.getProperties())\n");

        // Since we don't have access to the XWiki object from Selenium tests and since we don't want to restart XWiki
        // with a different xwiki.cfg file for each test that requires a configuration change, we use the following
        // trick: We create a document, and we access the XWiki object with a Velocity script inside that document.
        for (Map.Entry<Object, Object> param : properties.entrySet()) {
            sb.append("#set($discard = $props.put('").append(param.getKey()).append("', '")
                .append(param.getValue()).append("'))\n");
        }
        sb.append("#set($discard = $config.set($props))\n"
            + "{{/velocity}}");
        createPage("Test", "XWikiConfigurationPageForTest", sb.toString(), "Test page to change xwiki.cfg");
    }

    /**
     * Sets the value of an existing property of XWiki.XWikiPreferences.
     *
     * @param propertyName name of the property to set
     * @param value value to set to the property
     * @return the previous value, if the property was set, {@code null} otherwise
     * @since 9.7RC1
     */
    public String setWikiPreference(String propertyName, String value) throws Exception
    {
        DocumentReference documentReference = new DocumentReference(getCurrentWiki(), "XWiki", "XWikiPreferences");
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiPreferences[0]", documentReference);

        Property property = RestTestUtils.property(propertyName, value);

        org.xwiki.rest.model.jaxb.Object preferenceObject = rest().get(objectReference, false);
        String previousValue = null;

        if (preferenceObject == null) {
            // The object does not exist, create it

            preferenceObject = RestTestUtils.object("XWiki.XWikiPreferences");
            preferenceObject.withProperties(property);

            TestUtils.assertStatusCodes(
                rest().executePost(ObjectsResource.class, preferenceObject, rest().toElements(documentReference)), true,
                STATUS_CREATED);
        } else {
            // The object exist just set the property (faster than updating the whole object)
            ObjectPropertyReference propertyReference = new ObjectPropertyReference(propertyName, objectReference);

            TestUtils.assertStatusCodes(
                rest().executePut(ObjectPropertyResource.class, property, rest().toElements(propertyReference)), true,
                STATUS_ACCEPTED);

            Property unsetProperty = RestTestUtils.property(propertyName, null);
            previousValue =
                preferenceObject.getProperties().stream().filter(prop -> Objects.equals(propertyName, prop.getName()))
                    .findFirst().orElse(unsetProperty).getValue();
        }

        return previousValue;
    }

    /**
     * @since 7.3M1
     */
    public static void assertStatuses(int actualCode, int... expectedCodes)
    {
        if (!ArrayUtils.contains(expectedCodes, actualCode)) {
            fail(String.format("Unexpected code [%s], was expecting one of [%s]",
                actualCode, Arrays.toString(expectedCodes)));
        }
    }

    /**
     * @since 7.3M1
     */
    public static <M extends HttpMethod> M assertStatusCodes(M method, boolean release, int... expectedCodes)
        throws Exception
    {
        if (expectedCodes.length > 0) {
            int actualCode = method.getStatusCode();

            if (!ArrayUtils.contains(expectedCodes, actualCode)) {
                if (actualCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                    String message;
                    try {
                        message = method.getResponseBodyAsString();
                    } catch (IOException e) {
                        message = "";
                    }

                    fail(String.format("Unexpected internal server error with message [%s] for [%s]",
                        message, method.getURI()));
                } else {
                    fail(String.format("Unexpected code [%s], was expecting one of [%s] for [%s]",
                        actualCode, Arrays.toString(expectedCodes), method.getURI()));
                }
            }
        }

        if (release) {
            method.releaseConnection();
        }

        return method;
    }

    // HTTP

    /**
     * Encodes a given string so that it may be used as a URL component. Compatible with javascript decodeURIComponent,
     * though more strict than encodeURIComponent: all characters except [a-zA-Z0-9], '.', '-', '*', '_' are encoded.
     * Uses the same algorithm than the one used to generate URLs as otherwise tests won't find the proper matches...
     * See XWikiServletURLFactory#encodeWithinPath() and #encodeWithinQuery().
     *
     * @param url the url to encode
     */
    public String escapeURL(String url)
    {
        String encodedURL;
        try {
            encodedURL = URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            // Should not happen (UTF-8 is always available)
            throw new RuntimeException("Missing charset [UTF-8]", e);
        }

        // The previous call will convert " " into "+" (and "+" into "%2B") so we need to convert "+" into "%20"
        // It's ok since %20 is allowed in both the URL path and the query string (and anchor).
        encodedURL = encodedURL.replaceAll("\\+", "%20");

        return encodedURL;
    }

    /**
     * Usage example:
     * 
     * <pre>
     * {@code
     * By.xpath("//a[. = " + escapeXPath(value) + "]")
     * }
     * </pre>
     * 
     * @param value the value to escape
     * @return the escaped value
     */
    public String escapeXPath(String value)
    {
        return "concat('" + value.replace("'", "', \"'\", '") + "', '')";
    }

    public InputStream getInputStream(String path, Map<String, ?> queryParams) throws Exception
    {
        return getInputStream(getBaseURL(), path, queryParams);
    }

    public String getString(String path, Map<String, ?> queryParams) throws Exception
    {
        return getString(getBaseURL(), path, queryParams);
    }

    /**
     * Extended version to work in a docker context.
     *
     * @param baseURL the base url
     * @param path an additional path added after the base url
     * @param queryParams additional query parameter added to the computed url
     * @return the context of the computed url
     * @throws Exception in case of error when executing the request
     */
    public String getString(String baseURL, String path, Map<String, ?> queryParams) throws Exception
    {
        try (InputStream inputStream = getInputStream(baseURL, path, queryParams)) {
            return IOUtils.toString(inputStream);
        }
    }

    public InputStream getInputStream(String prefix, String path, Map<String, ?> queryParams, Object... elements)
        throws Exception
    {
        String cleanPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        if (path.startsWith(cleanPrefix)) {
            cleanPrefix = "";
        }

        UriBuilder builder = UriBuilder.fromUri(cleanPrefix).path(path.startsWith("/") ? path.substring(1) : path);

        if (queryParams != null) {
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                if (entry.getValue() instanceof Object[]) {
                    builder.queryParam(entry.getKey(), (Object[]) entry.getValue());
                } else {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }

        String url = builder.build(elements).toString();

        return executeGet(url, Status.OK.getStatusCode()).getResponseBodyAsStream();
    }

    protected GetMethod executeGet(String uri) throws Exception
    {
        GetMethod getMethod = new GetMethod(uri);

        this.httpClient.executeMethod(getMethod);

        return getMethod;
    }

    protected GetMethod executeGet(String uri, int... expectedCodes) throws Exception
    {
        return executeGet(uri, false, expectedCodes);
    }

    /**
     * @since 7.3M1
     */
    protected GetMethod executeGet(String uri, boolean release, int... expectedCodes) throws Exception
    {
        return assertStatusCodes(executeGet(uri), release, expectedCodes);
    }

    /**
     * @since 7.3M1
     */
    protected PostMethod executePost(String uri, InputStream content, String mediaType) throws Exception
    {
        PostMethod postMethod = new PostMethod(uri);
        RequestEntity entity = new InputStreamRequestEntity(content, mediaType);
        postMethod.setRequestEntity(entity);

        this.httpClient.executeMethod(postMethod);

        return postMethod;
    }

    protected PostMethod executePost(String uri, InputStream content, String mediaType, int... expectedCodes)
        throws Exception
    {
        return executePost(uri, content, mediaType, true, expectedCodes);
    }

    /**
     * @since 7.3M1
     */
    protected PostMethod executePost(String uri, InputStream content, String mediaType, boolean release,
        int... expectedCodes) throws Exception
    {
        return assertStatusCodes(executePost(uri, content, mediaType), false, expectedCodes);
    }

    /**
     * @since 7.3M1
     */
    protected DeleteMethod executeDelete(String uri) throws Exception
    {
        DeleteMethod postMethod = new DeleteMethod(uri);

        this.httpClient.executeMethod(postMethod);

        return postMethod;
    }

    /**
     * @since 7.3M1
     */
    protected void executeDelete(String uri, int... expectedCodes) throws Exception
    {
        assertStatusCodes(executeDelete(uri), true, expectedCodes);
    }

    /**
     * @since 7.3M1
     */
    protected PutMethod executePut(String uri, InputStream content, String mediaType) throws Exception
    {
        PutMethod putMethod = new PutMethod(uri);
        RequestEntity entity = new InputStreamRequestEntity(content, mediaType);
        putMethod.setRequestEntity(entity);

        this.httpClient.executeMethod(putMethod);

        return putMethod;
    }

    protected void executePut(String uri, InputStream content, String mediaType, int... expectedCodes) throws Exception
    {
        executePut(uri, content, mediaType, true, expectedCodes);
    }

    protected PutMethod executePut(String uri, InputStream content, String mediaType, boolean release,
        int... expectedCodes) throws Exception
    {
        return assertStatusCodes(executePut(uri, content, mediaType), release, expectedCodes);
    }

    // REST

    public RestTestUtils rest()
    {
        return this.rest;
    }

    /**
     * @since 7.3M1
     */
    // TODO: Refactor TestUtils to move RestTestUtils tools to xwiki-platform-test-integration
    public static class RestTestUtils
    {
        public static final Boolean ELEMENTS_ENCODED = new Boolean(true);

        public static final Map<EntityType, ResourceAPI> RESOURCES_MAP = new IdentityHashMap<>();

        public static String urlPrefix;

        public static class ResourceAPI
        {
            public Class<?> api;

            public Class<?> localeAPI;

            public ResourceAPI(Class<?> api, Class<?> localeAPI)
            {
                this.api = api;
                this.localeAPI = localeAPI;
            }
        }

        /**
         * Used to match number part of the object reference name.
         */
        private static final Pattern OBJECT_NAME_PATTERN = Pattern.compile("(\\\\*)\\[(\\d*)\\]$");

        static {
            try {
                // Initialize REST related tools
                JAXBContext jaxbContext = JAXBContext
                    .newInstance("org.xwiki.rest.model.jaxb:org.xwiki.extension.repository.xwiki.model.jaxb");
                marshaller = jaxbContext.createMarshaller();
                unmarshaller = jaxbContext.createUnmarshaller();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            RESOURCES_MAP.put(EntityType.DOCUMENT, new ResourceAPI(PageResource.class, PageTranslationResource.class));
            RESOURCES_MAP.put(EntityType.OBJECT, new ResourceAPI(ObjectResource.class, null));
            RESOURCES_MAP.put(EntityType.OBJECT_PROPERTY, new ResourceAPI(ObjectPropertyResource.class, null));
            RESOURCES_MAP.put(EntityType.CLASS_PROPERTY, new ResourceAPI(ClassPropertyResource.class, null));
        }

        /**
         * @since 7.3M1
         */
        public static org.xwiki.rest.model.jaxb.Object object(String className)
        {
            org.xwiki.rest.model.jaxb.Object obj = new org.xwiki.rest.model.jaxb.Object();

            obj.setClassName(className);

            return obj;
        }

        /**
         * @since 7.3M1
         */
        public static String toPropertyString(Object value)
        {
            String stringValue;

            if (value instanceof Iterable) {
                StringBuilder builder = new StringBuilder();
                for (Object item : (Iterable) value) {
                    if (builder.length() > 0) {
                        builder.append('|');
                    }

                    builder.append(item);
                }

                stringValue = builder.toString();
            } else if (value != null) {
                stringValue = value.toString();
            } else {
                stringValue = null;
            }

            return stringValue;
        }

        /**
         * @since 7.3M1
         */
        public static Property property(String name, Object value)
        {
            Property property = new Property();

            property.setName(name);
            property.setValue(toPropertyString(value));

            return property;
        }

        private TestUtils testUtils;

        public RestTestUtils(TestUtils testUtils)
        {
            this.testUtils = testUtils;
        }

        public String getBaseURL()
        {
            String prefix;
            if (RestTestUtils.urlPrefix != null) {
                prefix = RestTestUtils.urlPrefix;
            } else {
                prefix = this.testUtils.getBaseURL();
            }
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            return prefix + "rest";
        }

        /**
         * Used when running in a docker container for example and thus when we need a REST URL pointing to a host
         * different than the TestUTils baseURL which is used inside the Selenium docker container and is thus
         * different from a REST URL used outside of any container and that needs to call XWiki running inside a
         * container... ;)
         *
         * @since 10.9
         */
        public void setURLPrefix(String newURLPrefix)
        {
            RestTestUtils.urlPrefix = newURLPrefix;
        }

        protected Object[] toElements(Page page)
        {
            // Get locale
            Locale locale;
            if (StringUtils.isNotEmpty(page.getLanguage())) {
                locale = LocaleUtils.toLocale(page.getLanguage());
            } else {
                locale = null;
            }

            // Wiki
            WikiReference wikiReference;
            if (page.getWiki() != null) {
                wikiReference = new WikiReference(page.getWiki());
            } else {
                wikiReference = new WikiReference(this.testUtils.getCurrentWiki());
            }

            // Spaces
            SpaceReference spaceReference = new SpaceReference(relativeReferenceResolver
                .resolve(page.getSpace(), EntityType.SPACE).replaceParent(null, wikiReference));

            // Document
            DocumentReference documentReference = new DocumentReference(page.getName(), spaceReference, locale);

            return toElements(documentReference);
        }

        public Object[] toElements(org.xwiki.rest.model.jaxb.Object obj, boolean onlyDocument)
        {
            // Wiki
            WikiReference wikiReference;
            if (obj.getWiki() != null) {
                wikiReference = new WikiReference(obj.getWiki());
            } else {
                wikiReference = new WikiReference(this.testUtils.getCurrentWiki());
            }

            // Spaces
            SpaceReference spaceReference = new SpaceReference(relativeReferenceResolver
                .resolve(obj.getSpace(), EntityType.SPACE).replaceParent(null, wikiReference));

            // Document
            DocumentReference documentReference = new DocumentReference(obj.getPageName(), spaceReference);

            // Object
            EntityReference finalReference;
            if (onlyDocument) {
                finalReference = documentReference;
            } else {
                String objectName = obj.getClassName() + '[' + obj.getNumber() + ']';
                finalReference = new ObjectReference(objectName, documentReference);
            }

            return toElements(finalReference);
        }

        public Object[] toElements(EntityReference reference)
        {
            if (reference == null) {
                return ArrayUtils.EMPTY_OBJECT_ARRAY;
            }

            List<EntityReference> references = reference.getReversedReferenceChain();

            List<Object> elements = new ArrayList<>(references.size() + 2);

            // Indicate that elements are already encoded
            elements.add(ELEMENTS_ENCODED);

            // Add current wiki if the reference does not contains any
            if (reference.extractReference(EntityType.WIKI) == null) {
                elements.add(this.testUtils.escapeURL(this.testUtils.getCurrentWiki()));
            }

            // Add reference
            for (EntityReference ref : references) {
                if (ref.getType() == EntityType.SPACE) {
                    // The URI builder does not support multiple elements like space reference so we hack it by doing
                    // the opposite of what is done when reading the URL (generate a value looking like
                    // "space1/spaces/space2")
                    Object value = elements.get(elements.size() - 1);

                    StringBuilder builder;
                    if (value instanceof StringBuilder) {
                        builder = (StringBuilder) value;
                        builder.append("/spaces/");
                    } else {
                        builder = new StringBuilder();
                        elements.add(builder);
                    }

                    builder.append(this.testUtils.escapeURL(ref.getName()));
                } else if (ref.getType() == EntityType.OBJECT) {
                    // The REST API is no in sync with the ObjectReference structure:
                    // was is a unique name in ObjectReference is two separated class name and index in REST API
                    String classReferenceStr;
                    String objectNumberStr;

                    Matcher matcher = OBJECT_NAME_PATTERN.matcher(ref.getName());
                    if (matcher.find()) {
                        if (matcher.group(1).length() % 2 == 0) {
                            classReferenceStr = ref.getName().substring(0, matcher.end(1));
                            objectNumberStr = matcher.group(2);
                        } else {
                            classReferenceStr = ref.getName();
                            objectNumberStr = null;
                        }
                    } else {
                        classReferenceStr = ref.getName();
                        objectNumberStr = null;
                    }

                    elements.add(classReferenceStr);
                    elements.add(objectNumberStr);
                } else {
                    elements.add(this.testUtils.escapeURL(ref.getName()));
                }
            }

            // Add locale
            Locale locale = getLocale(reference);
            if (locale != null) {
                elements.add(locale);
            }

            return elements.toArray();
        }

        /**
         * Add or update.
         */
        public void save(Page page, int... expectedCodes) throws Exception
        {
            save(page, true, expectedCodes);
        }

        public EntityEnclosingMethod save(Page page, boolean release, int... expectedCodes) throws Exception
        {
            if (expectedCodes.length == 0) {
                // Allow create or modify by default
                expectedCodes = STATUS_CREATED_ACCEPTED;
            }

            Class resourceClass =
                StringUtils.isEmpty(page.getLanguage()) ? PageResource.class : PageTranslationResource.class;

            return TestUtils.assertStatusCodes(executePut(resourceClass, page, toElements(page)), release,
                expectedCodes);
        }

        /**
         * @since 7.3M1
         */
        public Page page(EntityReference reference)
        {
            Page page = new Page();

            // Add current wiki if the reference does not contains any
            EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
            if (wikiReference == null) {
                page.setWiki(this.testUtils.getCurrentWiki());
            } else {
                page.setWiki(wikiReference.getName());
            }

            // Add spaces
            EntityReference spaceReference = reference.extractReference(EntityType.SPACE).removeParent(wikiReference);
            page.setSpace(referenceSerializer.serialize(spaceReference));

            // Add page
            EntityReference documentReference = reference.extractReference(EntityType.DOCUMENT);
            page.setName(documentReference.getName());

            // Add locale
            if (reference instanceof AbstractLocalizedEntityReference) {
                Locale locale = getLocale(reference);
                if (locale != null) {
                    page.setLanguage(locale.toString());
                }
            }

            return page;
        }

        /**
         * @since 9.8RC1
         */
        public org.xwiki.rest.model.jaxb.Object object(EntityReference parentReference, String className)
        {
            return object(parentReference, className, 0);
        }

        /**
         * @since 9.8RC1
         */
        public org.xwiki.rest.model.jaxb.Object object(EntityReference parentReference, String className, int number)
        {
            org.xwiki.rest.model.jaxb.Object obj = new org.xwiki.rest.model.jaxb.Object();

            // Add current wiki if the reference does not contains any
            EntityReference wikiReference = parentReference.extractReference(EntityType.WIKI);
            if (wikiReference == null) {
                obj.setWiki(this.testUtils.getCurrentWiki());
            } else {
                obj.setWiki(wikiReference.getName());
            }

            // Add spaces
            EntityReference spaceReference =
                parentReference.extractReference(EntityType.SPACE).removeParent(wikiReference);
            obj.setSpace(referenceSerializer.serialize(spaceReference));

            // Add page
            EntityReference documentReference = parentReference.extractReference(EntityType.DOCUMENT);
            obj.setPageName(documentReference.getName());

            // Add class reference
            obj.setClassName(className);
            // Add object number
            obj.setNumber(number);

            return obj;
        }

        /**
         * @since 7.3M1
         */
        public void savePage(EntityReference reference) throws Exception
        {
            savePage(reference, null, null, null, null);
        }

        /**
         * @since 7.3M1
         */
        public void savePage(EntityReference reference, String content, String title) throws Exception
        {
            savePage(reference, content, null, title, null);
        }

        /**
         * @since 7.3M1
         */
        public void savePage(EntityReference reference, String content, String syntaxId, String title,
            String parentFullPageName) throws Exception
        {
            Page page = page(reference);

            if (content != null) {
                page.setContent(content);
            }
            if (title != null) {
                page.setTitle(title);
            }
            if (syntaxId != null) {
                page.setSyntax(syntaxId);
            }
            if (parentFullPageName != null) {
                page.setParent(parentFullPageName);
            }

            save(page, true);
        }

        /**
         * Add a new object.
         */
        public void add(org.xwiki.rest.model.jaxb.Object obj) throws Exception
        {
            add(obj, true);
        }

        /**
         * Add a new object.
         */
        public EntityEnclosingMethod add(org.xwiki.rest.model.jaxb.Object obj, boolean release) throws Exception
        {
            return TestUtils.assertStatusCodes(executePost(ObjectsResource.class, obj, toElements(obj, true)), release,
                STATUS_CREATED);
        }

        /**
         * @since 15.2RC1
         * @since 15.1
         * @since 14.10.6
         */
        private void addObject(EntityReference documentReference, String rightClassName, Object... properties)
            throws Exception
        {
            // Make sure the page exist (object add fail otherwise)
            // TODO: improve object add API to allow adding an object in a page that does not yet exist
            if (!exists(documentReference)) {
                savePage(documentReference);
            }

            // Create the object
            org.xwiki.rest.model.jaxb.Object rightsObject = object(documentReference, rightClassName);
            for (int i = 0; i < properties.length; i += 2) {
                String name = (String) properties[i + 0];
                Object value = properties[i + 1];

                rightsObject.withProperties(RestTestUtils.property(name, value));
            }

            // Add the object
            add(rightsObject);
        }

        /**
         * Fail if the object does not exist.
         */
        public void update(org.xwiki.rest.model.jaxb.Object obj) throws Exception
        {
            update(obj, true);
        }

        /**
         * Fail if the object does not exist.
         */
        public EntityEnclosingMethod update(org.xwiki.rest.model.jaxb.Object obj, boolean release) throws Exception
        {
            return TestUtils.assertStatusCodes(executePut(ObjectResource.class, obj, toElements(obj, false)), release,
                STATUS_CREATED_ACCEPTED);
        }

        public void delete(EntityReference reference) throws Exception
        {
            Class<?> resource = getResourceAPI(reference);

            TestUtils.assertStatusCodes(executeDelete(resource, toElements(reference)), true,
                STATUS_NO_CONTENT_NOT_FOUND);
        }

        // TODO: make EntityReference#getParameter() public
        private Locale getLocale(EntityReference reference)
        {
            if (reference instanceof AbstractLocalizedEntityReference) {
                return ((AbstractLocalizedEntityReference) reference).getLocale();
            }

            return null;
        }

        public void deletePage(String space, String page) throws Exception
        {
            delete(new LocalDocumentReference(space, page));
        }

        /**
         * @since 9.0RC1
         */
        public void deletePage(String space, String page, Locale locale) throws Exception
        {
            delete(new LocalDocumentReference(space, page, locale));
        }

        /**
         * @since 8.0M1
         */
        public void attachFile(EntityReference reference, Object is, boolean failIfExists) throws Exception
        {
            // make sure the page exist
            if (!exists(reference.getParent())) {
                savePage(reference.getParent());
            }

            if (failIfExists) {
                assertStatusCodes(executePut(AttachmentResource.class, is, toElements(reference)), true,
                    STATUS_CREATED);
            } else {
                assertStatusCodes(executePut(AttachmentResource.class, is, toElements(reference)), true,
                    STATUS_CREATED_ACCEPTED);
            }
        }

        public void deleteAttachement(EntityReference reference) throws Exception
        {
            assertStatusCodes(executeDelete(AttachmentResource.class, toElements(reference)), true, STATUS_NO_CONTENT);
        }

        public boolean exists(EntityReference reference) throws Exception
        {
            GetMethod getMethod = executeGet(reference);

            getMethod.releaseConnection();

            return getMethod.getStatusCode() == Status.OK.getStatusCode();
        }

        /**
         * Return object model of the passed reference. Fail if none could be found.
         * 
         * @since 7.3
         */
        public <T> T get(EntityReference reference) throws Exception
        {
            return get(reference, true);
        }

        /**
         * @since 16.2.0RC1
         * @since 15.10.8
         */
        public <T> T get(EntityReference reference, Map<String, Object[]> queryParams) throws Exception
        {
            return get(reference, queryParams, true);
        }

        /**
         * Return object model of the passed reference or null if none could be found.
         * 
         * @since 8.0M1
         */
        public <T> T get(EntityReference reference, boolean failIfNotFound) throws Exception
        {
            return get(reference, Map.of(), failIfNotFound);
        }

        /**
         * @since 16.2.0RC1
         * @since 15.10.8
         */
        public <T> T get(EntityReference reference, Map<String, Object[]> queryParams, boolean failIfNotFound)
            throws Exception
        {
            Class<?> resource = getResourceAPI(reference);

            return get(resource, queryParams, reference, failIfNotFound);
        }

        /**
         * @since 9.0RC1
         */
        public Class<?> getResourceAPI(EntityReference reference) throws Exception
        {
            ResourceAPI resource = RESOURCES_MAP.get(reference.getType());

            if (resource == null) {
                throw new Exception("Unsuported type [" + reference.getType() + "]");
            }

            return getLocale(reference) != null ? resource.localeAPI : resource.api;
        }

        /**
         * Return object model of the passed reference with the passed resource URI. Fail if none could be found.
         * 
         * @since 8.0M1
         */
        public <T> T get(Object resourceURI, EntityReference reference) throws Exception
        {
            return get(resourceURI, reference, true);
        }

        /**
         * @since 16.2.0RC1
         * @since 15.10.8
         */
        public <T> T get(Object resourceURI, Map<String, Object[]> queryParams, EntityReference reference) throws Exception
        {
            return get(resourceURI, queryParams, reference, true);
        }

        /**
         * Return object model of the passed reference with the passed resource URI or null if none could be found.
         * 
         * @since 8.0M1
         */
        public <T> T get(Object resourceURI, EntityReference reference, boolean failIfNotFound) throws Exception
        {
            return get(resourceURI, Map.of(), reference, failIfNotFound);
        }

        /**
         * @since 16.2.0RC1
         * @since 15.10.8
         */
        public <T> T get(Object resourceURI, Map<String, Object[]> queryParams, EntityReference reference,
            boolean failIfNotFound) throws Exception
        {
            GetMethod getMethod = assertStatusCodes(executeGet(resourceURI, queryParams, reference), false,
                failIfNotFound ? STATUS_OK : STATUS_OK_NOT_FOUND);

            if (getMethod.getStatusCode() == Status.NOT_FOUND.getStatusCode()) {
                return null;
            }

            if (reference != null && reference.getType() == EntityType.ATTACHMENT) {
                return (T) getMethod.getResponseBodyAsStream();
            } else {
                try {
                    try (InputStream stream = getMethod.getResponseBodyAsStream()) {
                        return toResource(stream);
                    }
                } finally {
                    getMethod.releaseConnection();
                }
            }
        }

        public <T> T get(Object resourceURI, boolean failIfNotFound) throws Exception
        {
            return get(resourceURI, null, failIfNotFound);
        }

        public InputStream getInputStream(String resourceUri, Map<String, ?> queryParams, Object... elements)
            throws Exception
        {
            return this.testUtils.getInputStream(getBaseURL(), resourceUri, queryParams, elements);
        }

        public InputStream postRESTInputStream(Object resourceUri, Object restObject, Object... elements)
            throws Exception
        {
            return postInputStream(resourceUri, restObject, Collections.<String, Object[]>emptyMap(), elements);
        }

        public InputStream postInputStream(Object resourceUri, Object restObject, Map<String, Object[]> queryParams,
            Object... elements) throws Exception
        {
            return executePost(resourceUri, restObject, queryParams, elements).getResponseBodyAsStream();
        }

        public <T> T toResource(InputStream is) throws JAXBException
        {
            return (T) unmarshaller.unmarshal(is);
        }

        protected InputStream toResourceInputStream(Object restObject) throws JAXBException
        {
            InputStream resourceStream;
            if (restObject instanceof InputStream) {
                resourceStream = (InputStream) restObject;
            } else if (restObject instanceof byte[]) {
                resourceStream = new ByteArrayInputStream((byte[]) restObject);
            } else {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                marshaller.marshal(restObject, stream);
                resourceStream = new ByteArrayInputStream(stream.toByteArray());
            }

            return resourceStream;
        }

        /**
         * @since 7.3
         */
        public GetMethod executeGet(EntityReference reference) throws Exception
        {
            Class<?> resource = getResourceAPI(reference);

            return executeGet(resource, reference);
        }

        /**
         * @since 8.0M1
         */
        public GetMethod executeGet(Object resourceURI, EntityReference reference) throws Exception
        {
            return executeGet(resourceURI, toElements(reference));
        }

        /**
         * @since 16.2.0RC1
         * @since 15.10.8
         */
        public GetMethod executeGet(Object resourceURI, Map<String, Object[]> queryParams, EntityReference reference)
            throws Exception
        {
            return executeGet(resourceURI, queryParams, toElements(reference));
        }

        public GetMethod executeGet(Object resourceUri, Object... elements) throws Exception
        {
            return executeGet(resourceUri, Collections.<String, Object[]>emptyMap(), elements);
        }

        public GetMethod executeGet(Object resourceUri, Map<String, Object[]> queryParams, Object... elements)
            throws Exception
        {
            // Build URI
            String uri = createUri(resourceUri, queryParams, elements).toString();

            return this.testUtils.executeGet(uri);
        }

        public PostMethod executePost(Object resourceUri, Object restObject, Object... elements) throws Exception
        {
            return executePost(resourceUri, restObject, Collections.<String, Object[]>emptyMap(), elements);
        }

        public PostMethod executePost(Object resourceUri, Object restObject, Map<String, Object[]> queryParams,
            Object... elements) throws Exception
        {
            // Build URI
            String uri = createUri(resourceUri, queryParams, elements).toString();

            try (InputStream resourceStream = toResourceInputStream(restObject)) {
                return this.testUtils.executePost(uri, resourceStream, MediaType.APPLICATION_XML);
            }
        }

        public PutMethod executePut(Object resourceUri, Object restObject, Object... elements) throws Exception
        {
            return executePut(resourceUri, restObject, Collections.<String, Object[]>emptyMap(), elements);
        }

        public PutMethod executePut(Object resourceUri, Object restObject, Map<String, Object[]> queryParams,
            Object... elements) throws Exception
        {
            // Build URI
            String uri = createUri(resourceUri, queryParams, elements).toString();

            try (InputStream resourceStream = toResourceInputStream(restObject)) {
                return this.testUtils.executePut(uri, resourceStream, MediaType.APPLICATION_XML);
            }
        }

        public DeleteMethod executeDelete(Object resourceUri, Object... elements) throws Exception
        {
            return executeDelete(resourceUri, Collections.<String, Object[]>emptyMap(), elements);
        }

        public DeleteMethod executeDelete(Object resourceUri, Map<String, Object[]> queryParams, Object... elements)
            throws Exception
        {
            // Build URI
            String uri = createUri(resourceUri, queryParams, elements).toString();

            return this.testUtils.executeDelete(uri);
        }

        public URI createUri(Object resourceUri, Map<String, Object[]> queryParams, Object... elements)
        {
            if (resourceUri instanceof URI) {
                return (URI) resourceUri;
            }

            // Create URI builder
            UriBuilder builder = getUriBuilder(resourceUri, queryParams);

            // Build URI
            URI uri;
            if (elements.length > 0 && elements[0] == ELEMENTS_ENCODED) {
                uri = builder.buildFromEncoded(Arrays.copyOfRange(elements, 1, elements.length));
            } else {
                uri = builder.build(elements);
            }

            return uri;
        }

        public UriBuilder getUriBuilder(Object resourceUri, Map<String, Object[]> queryParams)
        {
            // Create URI builder
            UriBuilder builder;
            if (resourceUri instanceof Class) {
                builder = getUriBuilder((Class) resourceUri);
            } else {
                String stringResourceUri = (String) resourceUri;
                builder = UriBuilder.fromUri(getBaseURL().substring(0, getBaseURL().length() - 1))
                    .path(!stringResourceUri.isEmpty() && stringResourceUri.charAt(0) == '/'
                        ? stringResourceUri.substring(1) : stringResourceUri);
            }

            // Add query parameters
            if (queryParams != null) {
                for (Map.Entry<String, Object[]> entry : queryParams.entrySet()) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }

            return builder;
        }

        protected UriBuilder getUriBuilder(Class<?> resource)
        {
            return UriBuilder.fromUri(getBaseURL()).path(resource);
        }

        public byte[] getBuffer(String resourceUri, Map<String, Object[]> queryParams, Object... elements)
            throws Exception
        {
            InputStream is = getInputStream(resourceUri, queryParams, elements);

            byte[] buffer;
            try {
                buffer = IOUtils.toByteArray(is);
            } finally {
                is.close();
            }

            return buffer;
        }

        public <T> T getResource(String resourceUri, Map<String, Object[]> queryParams, Object... elements)
            throws Exception
        {
            T resource;
            try (InputStream is = getInputStream(resourceUri, queryParams, elements)) {
                resource = (T) unmarshaller.unmarshal(is);
            }

            return resource;
        }

        public static Property getProperty(String name, org.xwiki.rest.model.jaxb.Object preferencesObject,
            boolean create)
        {
            for (Property property : preferencesObject.getProperties()) {
                if (property.getName().equals(name)) {
                    return property;
                }
            }

            if (create) {
                Property property = property(name, null);
                preferencesObject.getProperties().add(property);

                return property;
            }

            return null;
        }
    }

    /**
     * Disable Syntax Highlighting.
     *
     * @since 9.7RC1
     */
    public void disableSyntaxHighlighting() throws Exception
    {
        ObjectPropertyReference enabledPropertyReference =
            new ObjectPropertyReference("enabled", new ObjectReference("SyntaxHighlighting.ConfigurationClass[0]",
                new DocumentReference(getCurrentWiki(), "SyntaxHighlighting", "Configuration")));

        Property property = new Property();
        property.setValue("0");

        TestUtils.assertStatusCodes(
            rest().executePut(ObjectPropertyResource.class, property, rest().toElements(enabledPropertyReference)),
            true, STATUS_ACCEPTED);
    }

    /**
     * @since 11.5RC1
     */
    public void switchTab(String tabHandle)
    {
        getDriver().switchTo().window(tabHandle);
    }

    /**
     * @since 11.5RC1
     */
    public String getCurrentTabHandle()
    {
        return getDriver().getWindowHandle();
    }

    /**
     * @since 11.5RC1
     */
    public String openLinkInTab(By by, String... existingTabHandles)
    {
        getDriver().findElement(by).sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));

        // It might take a bit of time for the driver to know there's another window.
        getDriver().waitUntilCondition(input -> input.getWindowHandles().size() == existingTabHandles.length + 1);
        Set<String> windowHandles = getDriver().getWrappedDriver().getWindowHandles();
        String newTabHandle = null;
        List<String> tabHandles = Arrays.asList(existingTabHandles);
        for (String handle : windowHandles) {
            if (!tabHandles.contains(handle)) {
                newTabHandle = handle;
                break;
            }
        }
        return newTabHandle;
    }

    /**
     * @since 11.5
     * @since 11.6RC1
     */
    public void closeTab(String secondTabHandle)
    {
        String currentTab = getCurrentTabHandle();
        switchTab(secondTabHandle);
        getDriver().close();
        switchTab(currentTab);
    }
}

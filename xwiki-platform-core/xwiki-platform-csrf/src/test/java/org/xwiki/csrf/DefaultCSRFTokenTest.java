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
package org.xwiki.csrf;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Random;
import java.security.SecureRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrf.internal.DefaultCSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class DefaultCSRFTokenTest extends AbstractMockingComponentTestCase
{
    /** URL of the current document. */
    private static final String mockDocumentUrl = "http://host/xwiki/bin/save/Main/Test";

    /** Resubmission URL. */
    private static final String resubmitUrl = mockDocumentUrl;

    /** Tested CSRF token component. */
    @MockingRequirement
    private InsecureCSRFToken csrf;

    /**
     * This class is here because it doesn't require a SecureRandom generator
     * seed on each startup. Seeding a SecureRandom generator can take a very long time,
     * especially many time over which depleats the random pool on the server.
     */
    public static class InsecureCSRFToken extends DefaultCSRFToken
    {
        @Override
        public void initialize()
        {
            final Random random = new Random(System.nanoTime());
            this.setRandom(new SecureRandom() {
                private static final long serialVersionUID = 3;
                @Override
                public void nextBytes(byte[] out)
                {
                    random.nextBytes(out);
                }
            });
        }
    }

    @Override
    public void configure() throws Exception
    {
        // set up mocked dependencies

        // document access bridge
        final DocumentAccessBridge mockDocumentAccessBridge =
            getComponentManager().getInstance(DocumentAccessBridge.class);
        final CopyStringMatcher returnValue = new CopyStringMatcher(resubmitUrl + "?", "");
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentAccessBridge).getCurrentUser();
                will(returnValue("XWiki.Admin"));
                allowing(mockDocumentAccessBridge).getDocumentURL(with(aNonNull(DocumentReference.class)),
                    with("view"), with(returnValue), with(aNull(String.class)));
                will(returnValue);
                allowing(mockDocumentAccessBridge).getDocumentURL(with(aNull(DocumentReference.class)), with("view"),
                    with(aNull(String.class)), with(aNull(String.class)));
                will(returnValue(mockDocumentUrl));
                allowing(mockDocumentAccessBridge).getCurrentDocumentReference();
                will(returnValue(null));
            }
        });
        // configuration
        final CSRFTokenConfiguration mockConfiguration =
            getComponentManager().getInstance(CSRFTokenConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockConfiguration).isEnabled();
                will(returnValue(true));
            }
        });
        // request
        final HttpSession mockSession = getMockery().mock(HttpSession.class);
        final HttpServletRequest httpRequest = getMockery().mock(HttpServletRequest.class);
        final ServletRequest servletRequest = new ServletRequest(httpRequest);
        getMockery().checking(new Expectations()
        {
            {
                allowing(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer(mockDocumentUrl)));
                allowing(httpRequest).getRequestURI();
                will(returnValue(mockDocumentUrl));
                allowing(httpRequest).getParameterMap();
                will(returnValue(new HashMap<String, String[]>()));
                allowing(httpRequest).getSession();
                will(returnValue(mockSession));
            }
        });
        // session
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockSession).getAttribute(with(any(String.class)));
                will(returnValue(new HashMap<String, Object>()));
            }
        });
        // container
        final Container mockContainer = getComponentManager().getInstance(Container.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockContainer).getRequest();
                will(returnValue(servletRequest));
            }
        });
        // logging
        getMockery().checking(new Expectations() {{
            // Ignore all calls to debug()
            ignoring(any(Logger.class)).method("debug");
        }});
    }

    /**
     * Test that the secret token is a non-empty string.
     */
    @Test
    public void testToken()
    {
        String token = this.csrf.getToken();
        Assert.assertNotNull("CSRF token is null", token);
        Assert.assertNotSame("CSRF token is empty string", "", token);
        Assert.assertTrue("CSRF token is too short: \"" + token + "\"", token.length() > 20);
    }

    /**
     * Test that the same secret token is returned on subsequent calls.
     */
    @Test
    public void testTokenTwice()
    {
        String token1 = this.csrf.getToken();
        String token2 = this.csrf.getToken();
        Assert.assertNotNull("CSRF token is null", token1);
        Assert.assertNotSame("CSRF token is empty string", "", token1);
        Assert.assertEquals("Subsequent calls returned different tokens", token1, token2);
    }

    /**
     * Test that the produced valid secret token is indeed valid.
     */
    @Test
    public void testTokenValidity()
    {
        String token = this.csrf.getToken();
        Assert.assertTrue("Valid token did not pass the check", this.csrf.isTokenValid(token));
    }

    /**
     * Test that null is not valid.
     */
    @Test
    public void testNullNotValid() throws Exception
    {
        // Verify that the correct message is logged
        final Logger logger = getMockLogger();

        getMockery().checking(new Expectations() {{
            oneOf(logger).warn(with(startsWith("CSRFToken: Secret token verification failed, token: \"null\", stored "
                + "token:")));
        }});

        Assert.assertFalse("Null passed validity check", this.csrf.isTokenValid(null));
    }

    /**
     * Test that empty string is not valid.
     */
    @Test
    public void testEmptyNotValid() throws Exception
    {
        // Verify that the correct message is logged
        final Logger logger = getMockLogger();

        getMockery().checking(new Expectations() {{
            oneOf(logger).warn(with(startsWith("CSRFToken: Secret token verification failed, token: \"\", stored "
                + "token:")));
        }});

        Assert.assertFalse("Empty string passed validity check", this.csrf.isTokenValid(""));
    }

    /**
     * Test that the prefix of the valid token is not valid.
     */
    @Test
    public void testPrefixNotValid() throws Exception
    {
        // Verify that the correct message is logged
        final Logger logger = getMockLogger();

        getMockery().checking(new Expectations() {{
            oneOf(logger).warn(with(startsWith("CSRFToken: Secret token verification failed, token:")));
        }});

        String token = this.csrf.getToken();
        if (token != null) {
            token = token.substring(0, token.length() - 2);
        }
        Assert.assertFalse("Null passed validity check", this.csrf.isTokenValid(token));
    }

    /**
     * Test that the resubmission URL is correct.
     */
    @Test
    public void testResubmissionURL()
    {
        String url = this.csrf.getResubmissionURL();
        try {
            // srid is random, extract it from the url
            Matcher matcher = Pattern.compile(".*srid%3D([a-zA-Z0-9]+).*").matcher(url);
            String srid = matcher.matches() ? matcher.group(1) : "asdf";
            String resubmit = URLEncoder.encode(mockDocumentUrl + "?srid=" + srid, "utf-8");
            String back = URLEncoder.encode(mockDocumentUrl, "utf-8");
            String expected = resubmitUrl + "?resubmit=" + resubmit + "&xback=" + back + "&xpage=resubmit";
            Assert.assertEquals("Invalid resubmission URL", expected, url);
        } catch (UnsupportedEncodingException exception) {
            Assert.fail("Should not happen: " + exception.getMessage());
        }
    }

    /**
     * Tests if the token contains any special characters that have a potential to break the layout when used in places
     * where XWiki-syntax is allowed.
     */
    @Test
    public void testXWikiSyntaxCompatibility()
    {
        // We cannot easily control the value of the token, so we just test if it contains any "bad" characters and hope
        // for the best. Since the probability that the token contains some specific character is about 1/3, this test
        // will start to flicker (instead of always failing) if something like XWIKI-5996 is reintroduced
        for (int i = 0; i < 30; ++i) {
            this.csrf.clearToken();
            String token = this.csrf.getToken();
            Assert.assertFalse("The token \"" + token + "\" contains a character that might break the layout",
                token.matches(".*[&?*_/#^,.({\\[\\]})~!=+-].*"));
        }
    }
}

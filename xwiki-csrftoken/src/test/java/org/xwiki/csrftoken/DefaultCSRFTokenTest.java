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
 *
 */
package org.xwiki.csrftoken;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrftoken.internal.DefaultCSRFToken;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class DefaultCSRFTokenTest extends AbstractMockingComponentTestCase
{
    /** Resubmission URL. */
    private static final String resubmitUrl = "http://host/xwiki/bin/view/XWiki/Resubmit";

    /** URL of the current document. */
    private static final String mockDocumentUrl = "http://host/xwiki/bin/save/Main/Test";

    /** Tested CSRF token component. */
    @MockingRequirement
    private DefaultCSRFToken csrf;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractMockingComponentTest#setUp()
     */
    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // set up mocked dependencies

        // document access bridge
        final DocumentAccessBridge mockDocumentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
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
        final CSRFTokenConfiguration mockConfiguration = getComponentManager().lookup(CSRFTokenConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockConfiguration).isEnabled();
                will(returnValue(true));
            }
        });
        // request
        final HttpServletRequest httpRequest = getMockery().mock(HttpServletRequest.class);
        final ServletRequest servletRequest = new ServletRequest(httpRequest);
        // tree map preserves order
        final Map<String, String[]> mockParams = new TreeMap<String, String[]>();
        mockParams.put("a", new String[] {"b"});
        mockParams.put("form_token", new String[] {"", null, "xyz", "xyz"});
        mockParams.put("c", new String[] {"d e/f"});
        getMockery().checking(new Expectations()
        {
            {
                allowing(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer(mockDocumentUrl)));
                allowing(httpRequest).getParameterMap();
                will(returnValue(mockParams));
            }
        });
        // container
        final Container mockContainer = getComponentManager().lookup(Container.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockContainer).getRequest();
                will(returnValue(servletRequest));
            }
        });
        // model
        final ModelContext mockModel = getComponentManager().lookup(ModelContext.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockModel).getCurrentEntityReference();
                will(returnValue(new WikiReference("wiki")));
            }
        });
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
    public void testNullNotValid()
    {
        Assert.assertFalse("Null passed validity check", this.csrf.isTokenValid(null));
    }

    /**
     * Test that empty string is not valid.
     */
    @Test
    public void testEmptyNotValid()
    {
        Assert.assertFalse("Empty string passed validity check", this.csrf.isTokenValid(""));
    }

    /**
     * Test that the prefix of the valid token is not valid.
     */
    @Test
    public void testPrefixNotValid()
    {
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
            String redirect = URLEncoder.encode(mockDocumentUrl + "?a=b&c=d+e%2Ff", "utf-8");
            String back = URLEncoder.encode(mockDocumentUrl, "utf-8");
            String expected = resubmitUrl + "?xredirect=" + redirect + "&xback=" + back;
            Assert.assertEquals("Invalid resubmission URL", expected, url);
        } catch (UnsupportedEncodingException exception) {
            Assert.fail("Should not happen: " + exception.getMessage());
        }
    }
}

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
package com.xpn.xwiki.plugin.captcha;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.web.XWikiRequest;
import org.jmock.Mock;

/**
 * Unit tests for {@link CaptchaPlugin}
 *
 * @version $Id$
 */
public class CaptchaPluginTest extends org.jmock.cglib.MockObjectTestCase
{
    private Mock mockRequest;
    private XWikiContext context;
    private CaptchaPlugin plugin;
    private Mock mockXWiki;

    protected void setUp()
    {
        this.context = new XWikiContext();
        XWikiConfig config = new XWikiConfig();

        this.mockRequest = mock(XWikiRequest.class);

        this.mockXWiki = mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
            new Object[] {config, this.context});

        this.context.setWiki((XWiki) mockXWiki.proxy());
        this.context.setRequest((XWikiRequest) this.mockRequest.proxy());

        this.plugin = new CaptchaPlugin("captcha", "captcha", this.context);
    }

    public void testVerifyCaptchaUsingTextWithValidAnswerAndAnonymousUser() throws Exception
    {
        this.mockXWiki.stubs().method("getWebPreference").with(eq("register_anonymous"), ANYTHING)
            .will(returnValue("text"));
        this.mockRequest.stubs().method("getParameter").with(eq("sum_answer")).will(
            returnValue("2"));
        this.mockRequest.stubs().method("getParameter").with(eq("sum_result")).will(
            returnValue("2"));

        Boolean isValid = plugin.verifyCaptcha("register", this.context);

        assertTrue(isValid.booleanValue());
    }

    public void testVerifyCaptchaUsingTextWhenInvalidAnswerAndAnonymousUser() throws Exception
    {
        this.mockXWiki.stubs().method("getWebPreference").with(eq("register_anonymous"), ANYTHING)
            .will(returnValue("text"));
        this.mockRequest.stubs().method("getParameter").with(eq("sum_answer")).will(
            returnValue("2"));
        this.mockRequest.stubs().method("getParameter").with(eq("sum_result")).will(
            returnValue("1"));

        Boolean isValid = plugin.verifyCaptcha("register", this.context);

        assertFalse(isValid.booleanValue());
    }
}

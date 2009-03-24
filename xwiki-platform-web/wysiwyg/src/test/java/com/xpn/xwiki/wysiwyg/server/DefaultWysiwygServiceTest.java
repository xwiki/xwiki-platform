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
package com.xpn.xwiki.wysiwyg.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiURLFactoryServiceImpl;

public class DefaultWysiwygServiceTest extends AbstractXWikiComponentTestCase
{
    DefaultWysiwygService ws;

    private Mock mockXWiki;

    private Mock mockRequest;

    private Mock mockResponse;

    private Mock mockEngine;

    public void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING).will(
            returnValue(createDocument("Space1.Doc1", "a", "", "en", false)));
        this.mockXWiki.stubs().method("setDatabase");
        this.mockXWiki.stubs().method("isVirtual").will(returnValue(false));
        this.mockXWiki.stubs().method("Param").will(returnValue(""));
        this.mockXWiki.stubs().method("getURLFactoryService").will(
            returnValue(new XWikiURLFactoryServiceImpl((XWiki) mockXWiki.proxy())));
        this.mockXWiki.stubs().method("prepareResources");
        this.mockXWiki.stubs().method("checkAuth").will(returnValue(new XWikiUser("XWiki.User1")));

        this.mockRequest = mock(XWikiServletRequest.class, new Class[] {HttpServletRequest.class}, new Object[] {null});
        this.mockRequest.stubs().method("getRequestURL").will(returnValue(new StringBuffer("http://localhost/")));
        this.mockRequest.stubs().method("getQueryString").will(returnValue(""));
        this.mockRequest.stubs().method("getServletPath").will(returnValue(""));
        this.mockRequest.stubs().method("getContextPath").will(returnValue(""));

        this.mockResponse =
            mock(XWikiServletResponse.class, new Class[] {HttpServletResponse.class}, new Object[] {null});
        this.mockResponse.stubs().method("setLocale");

        this.mockEngine = mock(XWikiServletContext.class, new Class[] {ServletContext.class}, new Object[] {null});
        this.mockEngine.stubs().method("getAttribute").with(eq("xwiki")).will(returnValue(mockXWiki.proxy()));
        this.mockEngine.stubs().method("getAttribute").with(eq("org.xwiki.component.manager.ComponentManager")).will(
            returnValue(null));

        ws = new DefaultWysiwygService();
    }

    // non working test to test a sync session in gwt
    public void testSimple() throws XWikiGWTException
    {
        // SyncResult sync = ws.syncEditorContent(null, "Test.Test", 0);
    }

    private XWikiDocument createDocument(String name, String content, String language, String defaultLanguage,
        boolean isNew)
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(name);
        doc.setContent(content);
        doc.setLanguage(language);
        doc.setDefaultLanguage(defaultLanguage);
        doc.setNew(isNew);
        return doc;
    }

    private Object generateDummy(Class someClass)
    {
        ClassLoader loader = someClass.getClassLoader();
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        Class[] interfaces = new Class[] {someClass};
        return Proxy.newProxyInstance(loader, interfaces, handler);
    }

}

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
package com.xpn.xwiki.user.impl.xwiki;

import java.util.Collections;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl}.
 * 
 * @version $Id: $
 */
public class XWikiRightServiceImplTest extends MockObjectTestCase
{
    private XWikiRightServiceImpl rightService;

    private XWikiContext context;

    private Mock mockAuthService;

    private Mock mockXWiki;

    private static final String GLOBALGROUPNAME = "xwiki:XWiki.XWikiAllGroup";

    private static final String SHORTGROUPNAME = "XWiki.XWikiAllGroup";

    private static final String GLOBALUSERNAME = "xwiki:XWiki.User";

    private static final String LOCALUSERNAME = "XWiki.User";

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
    {
        this.rightService = new XWikiRightServiceImpl();

        this.context = new XWikiContext();
        this.context.setMainXWiki("xwiki");
        this.context.setDatabase("xwiki");

        this.mockAuthService = mock(XWikiGroupService.class, new Class[] {}, new Object[] {});
        this.mockAuthService.stubs().method("listGroupsForUser").will(
            new CustomStub("Implements XWikiGroupService.listGroupsForUser")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String member = (String) invocation.parameterValues.get(0);
                    XWikiContext context = (XWikiContext) invocation.parameterValues.get(1);

                    if (context.getDatabase().equals("xwiki") && member.equals(LOCALUSERNAME)) {
                        return Collections.singleton(SHORTGROUPNAME);
                    } else {
                        return Collections.emptyList();
                    }
                }
            });

        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {
            new XWikiConfig(), context});
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(true));
        this.mockXWiki.stubs().method("getGroupService").will(returnValue(mockAuthService.proxy()));
        
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    /**
     * Test if checkRight() take care of users's groups from other wikis.
     */
    public void testCheckRight() throws XWikiRightNotFoundException, XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setDatabase("wiki2");
        doc.setSpace("Space");
        doc.setName("Page");

        Mock mockGlobalRightObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("levels")).will(returnValue("view"));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("groups")).will(returnValue(GLOBALGROUPNAME));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("users")).will(returnValue(""));
        mockGlobalRightObj.stubs().method("getIntValue").with(eq("allow")).will(returnValue(1));
        mockGlobalRightObj.stubs().method("setNumber");

        doc.addObject("XWiki.XWikiGlobalRights", (BaseObject) mockGlobalRightObj.proxy());

        this.context.setDatabase("wiki2");

        boolean result =
            rightService.checkRight(GLOBALUSERNAME, doc, "view", true, true, true, context);

        assertTrue(GLOBALUSERNAME + "does not have global view right on wiki2", result);
    }
}

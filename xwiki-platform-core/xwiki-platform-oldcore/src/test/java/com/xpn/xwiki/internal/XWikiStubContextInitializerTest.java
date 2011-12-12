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
package com.xpn.xwiki.internal;

import org.jmock.Mock;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Validate XWikiStubContextInitializer and DefaultXWikiStubContextProvider.
 * 
 * @version $Id$
 */
public class XWikiStubContextInitializerTest extends AbstractBridgedXWikiComponentTestCase
{
    private ExecutionContextManager executionContextManager;

    private Mock mockXWiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.executionContextManager = getComponentManager().lookup(ExecutionContextManager.class);
    }

    public void testWithAndWithoutXWikiContext() throws Exception
    {
        XWikiContext xcontext = new XWikiContext();
        xcontext.put("key", "value");

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("prepareResources");

        xcontext.setWiki((XWiki) this.mockXWiki.proxy());

        ExecutionContext context = new ExecutionContext();
        context.setProperty("xwikicontext", xcontext);

        getComponentManager().lookup(XWikiStubContextProvider.class).initialize(xcontext);

        final ExecutionContext daemonContext = new ExecutionContext();

        Thread thread = new Thread(new Runnable()
        {
            public void run()
            {
                try {
                    executionContextManager.initialize(daemonContext);
                } catch (ExecutionContextException e) {
                    fail("Failed to initialize execution context: " + e.getStackTrace());
                }
            }
        });

        thread.run();
        thread.join();

        XWikiContext daemonXcontext = (XWikiContext) daemonContext.getProperty("xwikicontext");
        assertNotNull(daemonXcontext);
        assertNotSame(xcontext, daemonXcontext);
        assertEquals("value", daemonXcontext.get("key"));
        assertNotNull(daemonXcontext.getWiki());
    }
}

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
package org.xwiki.model.internal;

import java.lang.reflect.Type;
import java.net.URL;

import org.jmock.Expectations;
import org.junit.*;
import org.xwiki.cache.CacheManager;
import org.xwiki.model.*;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

public class BridgedServerTest extends AbstractBridgedComponentTestCase
{
    private Server server;

    @Before
    public void configure() throws Exception
    {
        final XWiki xwiki = getMockery().mock(XWiki.class);
        getContext().setWiki(xwiki);

        CacheManager cacheManager = getComponentManager().getInstance((Type) CacheManager.class);
        this.server = new BridgedServer(new BridgedEntityManager(cacheManager, getContext()), getContext());
    }

    @Test
    public void addWiki() throws Exception
    {
        Wiki wiki = this.server.addWiki("wiki");

        // Verify we get the exact same instance since we haven't saved yet.
        Assert.assertSame(wiki, this.server.getWiki("wiki"));
    }

    @Test
    public void hasWikiWhenWikiExists() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getServerURL("wiki", getContext());
                will(returnValue(new URL("http://whatever/not/null")));
        }});

        Assert.assertTrue(this.server.hasWiki("wiki"));
    }
}

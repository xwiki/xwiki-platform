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

import static org.mockito.Mockito.*;
import org.junit.*;
import org.xwiki.model.*;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Unit tests for {@link BridgedServer}.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class BridgedServerTest
{
    private Server server;

    private EntityManager entityManager;

    @Before
    public void configure() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        this.entityManager = mock(EntityManager.class);
        this.server = new BridgedServer(this.entityManager, xcontext);
    }

    @Test
    public void addWiki() throws Exception
    {
        Wiki expectedWiki = mock(Wiki.class);
        when(this.entityManager.addEntity(new UniqueReference(new WikiReference("wiki")))).thenReturn(expectedWiki);

        Wiki wiki = this.server.addWiki("wiki");

        Assert.assertNotNull(wiki);
    }

    @Test
    public void hasWiki() throws Exception
    {
        when(this.entityManager.hasEntity(new UniqueReference(new WikiReference("wiki")))).thenReturn(true);

        Assert.assertTrue(this.server.hasWiki("wiki"));
    }

    @Test
    public void getWiki() throws Exception
    {
        Wiki expectedWiki = mock(Wiki.class);
        when(this.entityManager.getEntity(new UniqueReference(new WikiReference("wiki")))).thenReturn(expectedWiki);

        Wiki wiki = this.server.getWiki("wiki");

        Assert.assertNotNull(wiki);
    }
}

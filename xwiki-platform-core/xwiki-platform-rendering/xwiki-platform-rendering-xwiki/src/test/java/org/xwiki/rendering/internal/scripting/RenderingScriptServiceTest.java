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
package org.xwiki.rendering.internal.scripting;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link RenderingScriptService}.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class RenderingScriptServiceTest extends AbstractComponentTestCase
{
    private RenderingScriptService rss;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.rss = (RenderingScriptService) getComponentManager().getInstance(ScriptService.class, "rendering");
    }

    @Test
    public void parseAndRender()
    {
        XDOM xdom = this.rss.parse("some [[TODO]] stuff", "plain/1.0");
        Assert.assertEquals("some ~[~[TODO]] stuff", this.rss.render(xdom, "xwiki/2.0"));
    }

    @Test
    public void resolveSyntax()
    {
        Assert.assertEquals(Syntax.XWIKI_2_1, this.rss.resolveSyntax("xwiki/2.1"));
    }
}

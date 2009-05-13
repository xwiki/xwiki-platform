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
package org.xwiki.rendering.macro.rss;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.macro.rss.RssMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A mock macro which invokes, in turn, the RSS Macro setting its feed to a test feed2.xml file.
 * 
 * @version $Id$
 */
@Component("testrssmacro2")
public class TestRssMacro2 extends RssMacro
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(RssMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        RssMacroParameters rssParameters = new RssMacroParameters();
        rssParameters.setContent(true);
        rssParameters.setCount(1);
        rssParameters.setImage(false);
        
        URL feedURL = getClass().getResource("/feed2.xml");
        try {
            rssParameters.setFeed(feedURL.toString());
        } catch (MalformedURLException e) {
            throw new MacroExecutionException(e.getMessage());
        }

        return super.execute(rssParameters, content, context);
    }

    /** 
     * {@inheritDoc}
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }
}

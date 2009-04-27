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
package org.xwiki.rendering.renderer;

import org.xwiki.rendering.internal.renderer.chaining.XWikiSyntaxChainingRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ConsecutiveNewLineStateChainingListener;
import org.xwiki.rendering.listener.chaining.GroupStateChainingListener;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.chaining.TextOnNewLineStateChainingListener;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Generates XWiki Syntax from {@link XDOM}. This is useful for example to convert other wiki syntaxes to the XWiki
 * syntax. It's also useful in our tests to verify that round-tripping from XWiki Syntax to the DOM and back to XWiki
 * Syntax generates the same content as the initial syntax.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer extends AbstractChainingPrintRenderer
{
    /**
     * @param printer the object where the XWiki Syntax output will be printed to 
     */
    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(printer, new XWikiSyntaxListenerChain());
        
        // Create a chained list of listeners that are applied before the XWiki Syntax chaining listener is executed
        // (this is the listener that outputs XWiki Syntax). This allows using several listeners that make it easier 
        // to write the XWiki Syntax chaining listener, for example for saving states (are we in a list, in a 
        // paragraph, are we starting a new line, etc).
        new LookaheadChainingListener(getListenerChain(), 2);
        new GroupStateChainingListener(getListenerChain());
        new BlockStateChainingListener(getListenerChain());
        new ConsecutiveNewLineStateChainingListener(getListenerChain());
        new TextOnNewLineStateChainingListener(getListenerChain());
        new XWikiSyntaxChainingRenderer(printer, getListenerChain());
    }
}

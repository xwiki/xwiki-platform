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

import java.util.Stack;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Common methods for all {@link org.xwiki.rendering.renderer.PrintRenderer} implementations.
 * 
 * @version $Id$
 * @since 1.6M2
 */
public abstract class AbstractPrintRenderer extends AbstractRenderer implements PrintRenderer
{
    private Stack<WikiPrinter> printers = new Stack<WikiPrinter>();

    public AbstractPrintRenderer(WikiPrinter printer, Listener stateListener)
    {
        super(stateListener);

        this.pushPrinter(printer);
    }

    /**
     * @return the main printer.
     */
    public WikiPrinter getMainPrinter()
    {
        return this.printers.firstElement();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#getPrinter()
     */
    public WikiPrinter getPrinter()
    {
        return this.printers.peek();
    }

    /**
     * Change the current {@link WikiPrinter} with the provided one.
     * 
     * @param wikiPrinter the new {@link WikiPrinter} to use
     */
    protected void pushPrinter(WikiPrinter wikiPrinter)
    {
        this.printers.push(wikiPrinter);
    }

    /**
     * Removes the current {@link WikiPrinter} and instead sets the previous printer as active.
     */
    protected void popPrinter()
    {
        this.printers.pop();
    }
}

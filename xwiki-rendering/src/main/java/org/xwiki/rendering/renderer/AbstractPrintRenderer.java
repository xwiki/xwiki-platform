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
 */package org.xwiki.rendering.renderer;

import java.util.Stack;

import org.xwiki.component.logging.AbstractLogEnabled;

/**
 * Common methods for all {@link org.xwiki.rendering.renderer.PrintRenderer} implementations.
 * 
 * @version $Id: $
 * @since 1.6M2
 */
public abstract class AbstractPrintRenderer extends AbstractLogEnabled implements PrintRenderer
{
    private Stack<WikiPrinter> printers = new Stack<WikiPrinter>();

    public AbstractPrintRenderer(WikiPrinter printer)
    {
        this.pushPrinter(printer);
    }

    protected void print(String text)
    {
        getPrinter().print(text);
    }

    protected void println(String text)
    {
        getPrinter().println(text);
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
     * Change the current {@link WikiPrinter} with provide one.
     * 
     * @param wikiPrinter the {@link WikiPrinter} to use since now.
     */
    protected void pushPrinter(WikiPrinter wikiPrinter)
    {
        this.printers.push(wikiPrinter);
    }

    /**
     * Pop the current {@link WikiPrinter}.
     */
    protected void popPrinter()
    {
        this.printers.pop();
    }

    /**
     * Switch current writer with a writer which do nothing.
     */
    protected void pushVoidPrinter()
    {
        if (getPrinter() != VoidWikiPrinter.VOIDWIKIPRINTER) {
            pushPrinter(VoidWikiPrinter.VOIDWIKIPRINTER);
        }
    }

    /**
     * Restore current printer.
     */
    protected void popVoidPrinter()
    {
        if (getPrinter() == VoidWikiPrinter.VOIDWIKIPRINTER) {
            popPrinter();
        }
    }
}

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
package org.xwiki.rendering.renderer.printer;


/**
 * A WikiPrinter implementation which does not do anything.
 * 
 * @version $Id$
 */
public class VoidWikiPrinter implements WikiPrinter
{
    /**
     * Unique instance of {@link VoidWikiPrinter}.
     */
    public static final VoidWikiPrinter VOIDWIKIPRINTER = new VoidWikiPrinter();

    /**
     * Use {@link #VOIDWIKIPRINTER}.
     */
    private VoidWikiPrinter()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.WikiPrinter#print(java.lang.String)
     */
    public void print(String text)
    {
        // Don't do anything
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.WikiPrinter#println(java.lang.String)
     */
    public void println(String text)
    {
        // Don't do anything
    }
}

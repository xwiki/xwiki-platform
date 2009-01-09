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
 * Wraps a wiki printer.
 *  
 * @version $Id$
 * @since 1.7
 */
public class WrappingWikiPrinter implements WikiPrinter
{
    private WikiPrinter printer;

    public WrappingWikiPrinter(WikiPrinter printer)
    {
        this.printer = printer;
    }

    public void print(String text)
    {
        getWrappedPrinter().print(text);
    }

    public void println(String text)
    {
        getWrappedPrinter().println(text);
    }

    public WikiPrinter getWrappedPrinter()
    {
        return this.printer;
    }
    
    @Override
    public String toString()
    {
        return getWrappedPrinter().toString();
    }
}

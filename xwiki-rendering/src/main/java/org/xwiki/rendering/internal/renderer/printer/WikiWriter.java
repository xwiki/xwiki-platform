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
package org.xwiki.rendering.internal.renderer.printer;

import java.io.IOException;
import java.io.Writer;

import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Bridge so that {@link WikiPrinter} can be used in a tools supporting {@link Writer} api.
 * 
 * @version $Id$
 */
public class WikiWriter extends Writer
{
    public WikiWriter(WikiPrinter printer)
    {
        super(printer);
    }

    public void setWikiPrinter(WikiPrinter printer)
    {
        this.lock = printer;
    }

    public WikiPrinter getWikiPrinter()
    {
        return (WikiPrinter) this.lock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException
    {
        // WikiPrinter does not support stream close
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException
    {
        // WikiPrinter does not support stream flush
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        getWikiPrinter().print(new String(cbuf, off, len));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Override it to improve speed a little. Otherwise the String is transformed in char table passed to the over
     * methods which recreate a String.
     * 
     * @see java.io.Writer#write(java.lang.String)
     */
    @Override
    public void write(String str) throws IOException
    {
        getWikiPrinter().print(str);
    }

}

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

import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * XHTMLWriter is an helper to configure XMLWriter to format a DOM4J tree as XHTML.
 * 
 * @version $Id$
 */
public class XHTMLWriter extends XMLWriter
{
    protected static final OutputFormat DEFAULT_XHTML_FORMAT;

    static {
        DEFAULT_XHTML_FORMAT = new OutputFormat();
        DEFAULT_XHTML_FORMAT.setXHTML(true);
    }

    public XHTMLWriter(Writer writer) throws UnsupportedEncodingException
    {
        super(writer, DEFAULT_XHTML_FORMAT);

        // escape all non US-ASCII to have as less encoding problems as possible
        setMaximumAllowedCharacter(127);
    }
}

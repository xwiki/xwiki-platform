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
package com.xpn.xwiki.wysiwyg.server.cleaner.internal;

import java.io.IOException;

import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.XmlSerializer;

import com.xpn.xwiki.wysiwyg.server.cleaner.XHTMLCleaner;
import com.xpn.xwiki.wysiwyg.server.cleaner.XHTMLCleanerException;

public class VnikicXHTMLCleaner implements XHTMLCleaner
{
    private HtmlCleaner cleaner;

    private XmlSerializer serializer;

    public VnikicXHTMLCleaner()
    {
        cleaner = new HtmlCleaner();

        // CleanerProperties props = cleaner.getProperties();
        // customize cleaner's behavior with property setters
        // props.setXXX(...);

        serializer = new BrowserCompactXmlSerializer(cleaner.getProperties());
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLCleaner#clean(String)
     */
    public synchronized String clean(String dirtyXHTML) throws XHTMLCleanerException
    {
        try {
            return serializer.getXmlAsString(cleaner.clean(dirtyXHTML));
        } catch (IOException e) {
            throw new XHTMLCleanerException("Exception while cleaning XHTML", e);
        }
    }
}

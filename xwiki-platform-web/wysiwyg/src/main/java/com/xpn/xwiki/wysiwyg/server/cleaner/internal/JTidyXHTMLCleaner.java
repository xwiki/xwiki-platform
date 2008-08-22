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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.tidy.Tidy;

import com.xpn.xwiki.util.TidyMessageLogger;
import com.xpn.xwiki.wysiwyg.server.cleaner.XHTMLCleaner;

public class JTidyXHTMLCleaner implements XHTMLCleaner
{
    private static final Log LOG = LogFactory.getLog(JTidyXHTMLCleaner.class);

    private static final TidyMessageLogger TIDY_LOGGER = new TidyMessageLogger(LOG);

    private Tidy tidy;

    public JTidyXHTMLCleaner()
    {
        tidy = new Tidy();

        // General configuration
        tidy.setForceOutput(true);
        tidy.setIndentAttributes(false);
        tidy.setIndentContent(false);
        tidy.setQuiet(true);
        tidy.setTrimEmptyElements(false);
        tidy.setXmlPi(false);
        tidy.setTidyMark(false);

        // XHTML configuration
        tidy.setXHTML(true);
        tidy.setDocType("strict");
        tidy.setDropProprietaryAttributes(true);
        tidy.setEncloseText(true);
        tidy.setHideComments(true);
        tidy.setLogicalEmphasis(true);
        tidy.setMakeBare(true);

        tidy.setMessageListener(TIDY_LOGGER);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLCleaner#clean(String)
     */
    public synchronized String clean(String dirtyXHTML)
    {
        StringWriter out = new StringWriter();
        tidy.parse(new ByteArrayInputStream(dirtyXHTML.getBytes()), out);
        return out.toString();
    }
}

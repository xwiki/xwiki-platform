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
 *
 */
package org.xwiki.xml.html;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.CleanerProperties;

import java.io.IOException;

/**
 * Default implementation for {@link HTMLCleaner} using the
 * <a href="HTML Cleaner framework>http://htmlcleaner.sourceforge.net/</a>.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class DefaultHTMLCleaner implements HTMLCleaner
{
    public String clean(String originalHtmlContent)
    {
        String result;

        HtmlCleaner cleaner = new HtmlCleaner();

        CleanerProperties props = cleaner.getProperties();
        props.setOmitXmlDeclaration(true);
        props.setOmitHtmlEnvelope(true);
        props.setOmitUnknownTags(true);
        
        try {
            TagNode cleanedNode = cleaner.clean(originalHtmlContent);
            result = new SimpleXmlSerializer(props).getXmlAsString(cleanedNode);
        } catch (IOException e) {
            // This shouldn't happen since we're not doing any IO... I consider this a flaw in the
            // design of HTML Cleaner.
            throw new RuntimeException("Unhandled error when cleaning HTML [" + originalHtmlContent + "]", e);
        }

        return result;
    }
}
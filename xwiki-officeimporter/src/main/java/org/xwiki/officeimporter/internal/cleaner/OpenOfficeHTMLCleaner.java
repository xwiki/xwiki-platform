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
package org.xwiki.officeimporter.internal.cleaner;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.filter.AnchorFilter;
import org.xwiki.officeimporter.filter.HTMLFilter;
import org.xwiki.officeimporter.filter.ImageFilter;
import org.xwiki.officeimporter.filter.LineBreakFilter;
import org.xwiki.officeimporter.filter.LinkFilter;
import org.xwiki.officeimporter.filter.ListFilter;
import org.xwiki.officeimporter.filter.ParagraphFilter;
import org.xwiki.officeimporter.filter.RedundancyFilter;
import org.xwiki.officeimporter.filter.StripperFilter;
import org.xwiki.officeimporter.filter.StyleFilter;
import org.xwiki.officeimporter.filter.TableFilter;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.internal.html.DefaultHTMLCleaner;

/**
 * {@link HTMLCleaner} for cleaning html generated from an openoffice server.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OpenOfficeHTMLCleaner extends AbstractLogEnabled implements HTMLCleaner
{  
    /**
     * The {@link DefaultHTMLCleaner} used internally. 
     */
    private HTMLCleaner defaultHtmlCleaner;

    /**
     * The {@link DocumentAccessBridge} used by some filters.
     */
    private DocumentAccessBridge docBridge;
    
    /**
     * {@inheritDoc}
     */
    public Document clean(Reader originalHtmlContent)
    {
        return clean(originalHtmlContent, Collections.singletonMap("filterStyles", "strict"));
    }

    /**
     * {@inheritDoc}
     */
    public Document clean(Reader originalHtmlContent, Map<String, String> params)
    {
        // Initialize filters.
        List<HTMLFilter> filterList = new ArrayList<HTMLFilter>();
        filterList.add(new StripperFilter());
        filterList.add(new StyleFilter(params.get("filterStyles")));
        filterList.add(new RedundancyFilter());
        filterList.add(new ParagraphFilter());
        filterList.add(new ImageFilter(docBridge, params.get("targetDocument")));
        filterList.add(new LinkFilter());
        filterList.add(new AnchorFilter());
        filterList.add(new ListFilter());
        filterList.add(new TableFilter());
        filterList.add(new LineBreakFilter());
        // Default cleaning.
        Document document = defaultHtmlCleaner.clean(originalHtmlContent);
        // Apply filters.
        for (HTMLFilter filter : filterList) {
            filter.filter(document);
        }
        return document;
    }
}

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
package org.xwiki.wysiwyg.server.internal.cleaner;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * Default HTML cleaner for the WYSIWYG editor's output.
 * 
 * @version $Id$
 */
public class DefaultHTMLCleaner implements HTMLCleaner
{
    /**
     * The component used to clean the HTML.
     */
    @Requirement
    private org.xwiki.xml.html.HTMLCleaner cleaner;

    /**
     * {@inheritDoc}
     * 
     * @see HTMLCleaner#clean(String)
     */
    public String clean(String dirtyHTML)
    {
        // We have to remove or replace the HTML elements that were added by the WYSIWYG editor only for internal
        // reasons, before any cleaning filter is applied. Otherwise cleaning filters might transform these
        // WYSIWYG-specific HTML elements making their removal difficult. We cannot transform the WYSIWYG output on the
        // client side because the editor is a widget that can be used independently inside or outside an HTML form and
        // thus it doesn't know when its current value is submitted.
        HTMLCleanerConfiguration config = cleaner.getDefaultConfiguration();
        List<HTMLFilter> filters = new ArrayList<HTMLFilter>();
        filters.add(new LineBreakFilter());
        filters.add(new EmptyLineFilter());
        filters.add(new StandAloneMacroFilter());
        filters.add(new EmptyAttributeFilter());
        filters.add(new NestedAnchorsFilter());
        filters.addAll(config.getFilters());
        config.setFilters(filters);

        Document document = cleaner.clean(new StringReader(dirtyHTML), config);
        return HTMLUtils.toString(document);
    }
}

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
package com.xpn.xwiki.doc;

import org.xwiki.component.annotation.Component;

/**
 * Same as {@link DefaultDocumentNameFactory} but if the page name isn't specified, returns the name
 * of the current page (located in the execution context). This is useful for parsing document name
 * references pointing to the current page being executed for example. {@link DefaultDocumentNameFactory}
 * always return "WebHome" if no page is specified.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Component("current")
public class CurrentDocumentNameFactory extends DefaultDocumentNameFactory
{
    /**
     * {@inheritDoc}
     * @see DefaultDocumentNameFactory#getDefaultPageName()
     */
    @Override
    protected String getDefaultPageName()
    {
        String page;
        XWikiDocument currentDocument = getContext().getDoc();
        if (currentDocument == null) {
            page = super.getDefaultPageName();
        } else {
            page = currentDocument.getPageName();
            if (page == null) {
                page = super.getDefaultPageName();
            }
        }
        return page;
    }
}

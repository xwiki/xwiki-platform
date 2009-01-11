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
package org.xwiki.xml.html.filter;

import org.jdom.Document;

/**
 * Clean some XML node after HTML Cleaner has been executed. This is because HTML Cleaner has some gaps and does't
 * convert HTML to XHTML so we need to perform extra cleaning.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public interface CleaningFilter
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = CleaningFilter.class.getName();
    
    /**
     * Filters the passed XML document to generate valid and clean XHTML.
     *
     * @param document the document to clean
     */
    void filter(Document document);
}

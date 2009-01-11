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
package org.xwiki.xml.internal.html.filter;

import org.jdom.DocType;
import org.jdom.Document;
import org.xwiki.xml.html.filter.CleaningFilter;

/**
 * Sets the Document DOCTYPE to XHTML 1.0 strict.
 *
 * @version $Id$
 * @since 1.6RC1
 */
public class DocTypeCleaningFilter implements CleaningFilter
{
    /**
     * {@inheritDoc}
     * @see CleaningFilter#filter(org.jdom.Document)
     */
    public void filter(Document document)
    {
        DocType docType = new DocType("html", "-//W3C//DTD XHTML 1.0 Strict//EN", 
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
        document.setDocType(docType);
    }
}

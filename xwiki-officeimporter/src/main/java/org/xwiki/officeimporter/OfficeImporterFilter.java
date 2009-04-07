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
package org.xwiki.officeimporter;

import org.w3c.dom.Document;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.rendering.block.XDOM;

/**
 * A filter interface allowing customizations to the default office importer process.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public interface OfficeImporterFilter
{
    /**
     * Allows extended filtering on the html {@link Document} generated during the import process.
     * 
     * @param document the {@link Document} containing the html content of the office document.
     */
    void filter(Document document);
    
    /**
     * Allows extended filtering on the {@link XDOM} generated during the import process.
     * 
     * @param xdom the {@link XDOM} representing the master wiki document.
     */
    void filter(XDOM xdom);
    
    /**
     * Allows extended filtering on the documents resulting from the split operation.
     * 
     * @param document The {@link WikiDocument} holding the contents of the wiki page.
     */
    void filter(WikiDocument document);
}

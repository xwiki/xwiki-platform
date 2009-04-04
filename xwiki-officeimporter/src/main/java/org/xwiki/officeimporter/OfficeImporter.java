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

import java.io.InputStream;
import java.util.Map;

import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;

/**
 * Entry point to import Office documents into wiki pages.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public interface OfficeImporter
{
    /**
     * XHTML/1.0 syntax.
     */
    Syntax XHTML_10 = new Syntax(SyntaxType.XHTML, "1.0");

    /**
     * XWiki/2.0 syntax.
     */
    Syntax XWIKI_20 = new Syntax(SyntaxType.XWIKI, "2.0");

    /**
     * Imports the specified office attachment into xhtml and returns the result. Any artifacts extracted during the
     * import operation will be attached to the wiki document. This method does not update the content of the wiki page.
     * 
     * @param documentName full name of the wiki document.
     * @param attachmentName name of the attachment.
     * @param params additional parameters for the import operation.
     * @return the xhtml code resulting from the import operation.
     * @throws OfficeImporterException if the import operation fails.
     */
    String importAttachment(String documentName, String attachmentName, Map<String, String> params)
        throws OfficeImporterException;

    /**
     * Imports the office document represented by fileStream into xwiki 2.0 syntax and stores the result in
     * targetWikiDocument.Any artifacts extracted during the import operation will be attached to the target wiki
     * document.
     * 
     * @param documentStream {@link InputStream} representing the input office document.
     * @param documentFormat string used to identify the input file format. (ppt, xls, odt etc.)
     * @param targetWikiDocument target wiki document.
     * @param params additional parameters for the import operation.
     * @throws OfficeImporterException if the import operation fails.
     */
    void importStream(InputStream documentStream, String documentFormat, String targetWikiDocument,
        Map<String, String> params) throws OfficeImporterException;
}

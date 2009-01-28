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
     * This component's role, used when code needs to look it up.
     */
    String ROLE = OfficeImporter.class.getName();
    
    /**
     * XHTML/1.0 syntax.
     */
    Syntax XHTML_10 = new Syntax(SyntaxType.XHTML, "1.0");

    /**
     * XWiki/2.0 syntax.
     */
    Syntax XWIKI_20 = new Syntax(SyntaxType.XWIKI, "2.0");

    /**
     * Imports an office document into a given syntax. Although the import operation is carried out w.r.t target
     * document, this method does not modify the target document. It is up to the caller to update the target document
     * if it wishes so. Currently xwiki/2.0 and xhtml/1.0 syntaxes are supported.
     * 
     * @param fileContent content of the office document.
     * @param fileName name of the office document (for determining document type).
     * @param targetDocument target wiki page.
     * @param options additional options for the import operation.
     * @return an {@link OfficeImporterResult} containing the results of the import operation.
     * @throws OfficeImporterException if the import operation fails.
     */
    OfficeImporterResult doImport(byte[] fileContent, String fileName, String targetDocument, Syntax targetSyntax,
        Map<String, String> options) throws OfficeImporterException;
}

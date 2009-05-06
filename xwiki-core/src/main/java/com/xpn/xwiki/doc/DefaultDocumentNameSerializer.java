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

import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;

/**
 * Generate a fully qualified document reference string (ie of the form {@code <wiki>:<space>.<page>} out of a
 * {@link DocumentName}.
 * 
 * @version $Id: $
 * @since 1.8.1
 */
public class DefaultDocumentNameSerializer implements DocumentNameSerializer
{
    /**
     * {@inheritDoc}
     * 
     * @see DocumentNameSerializer#serialize(DocumentName)
     */
    public String serialize(DocumentName documentName)
    {
        // A valid DocumentName must not have any null value and thus we don't need to check for nulls here.
        // It's the responsibility of creators of DocumentName factories to ensure it's valid.
        StringBuffer result = new StringBuffer();

        appendWikiName(result, documentName);
        appendSpaceName(result, documentName);
        appendPageName(result, documentName);

        return result.toString();
    }

    protected void appendWikiName(StringBuffer result, DocumentName documentName)
    {
        result.append(documentName.getWiki());
        result.append(':');
    }

    protected void appendSpaceName(StringBuffer result, DocumentName documentName)
    {
        result.append(documentName.getSpace());
        result.append('.');
    }

    protected void appendPageName(StringBuffer result, DocumentName documentName)
    {
        result.append(documentName.getPage());
    }
}

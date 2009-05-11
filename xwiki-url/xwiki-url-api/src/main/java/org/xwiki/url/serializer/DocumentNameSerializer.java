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
package org.xwiki.url.serializer;

import org.xwiki.bridge.DocumentName;
import org.xwiki.url.XWikiURL;

/**
 * Generate a Document name from a XWiki URL.
 *
 * @version $Id$ 
 * @since 1.6M1
 */
public class DocumentNameSerializer
{
    public String serialize(XWikiURL xwikiURL)
    {
        StringBuffer result = new StringBuffer();
        DocumentName documentName = xwikiURL.getDocumentName();
        if (documentName.getWiki() != null) {
            result.append(documentName.getWiki()).append(':');
        }
        if (documentName.getSpace() != null) {
            result.append(documentName.getSpace()).append('.');
        } else {
            // Use the default space located in the execution context if it exists.
            // Otherwise raise an exception.
            // TODO: implement it
        }
        if (documentName.getPage() != null) {
            result.append(documentName.getPage());
        } else {
            // Assume we're asking for the current page if there's one defined in the execution context.
            // Otherwise raise an exception.
            // TODO: implement it
        }

        // Add the parameters
        StringBuffer params = new StringBuffer();
        for (String parameterKey: xwikiURL.getParameters().keySet()) {
            params.append(parameterKey).append('=').append(xwikiURL.getParameterValue(parameterKey)).append('&');
        }
        if (params.length() > 0) {
            // Remove the extra "&" at the end
            params.setLength(params.length() - 1);
            result.append('?').append(params);
        }

        return result.toString();
    }
}

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
package org.xwiki.model.internal.reference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang3.CharEncoding;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Creates a {@link DocumentReference} from a string representation that has the "wiki/space/page" format, where each
 * path component is URL encoded. The current (quick) implementation doesn't support nested spaces and expects the path
 * to contain all 3 components: the wiki name, the space name and the page name (so relative references are not
 * supported either). We made this to work with {@link PathStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component("path")
public class PathStringDocumentReferenceResolver implements DocumentReferenceResolver<String>
{
    @Override
    public DocumentReference resolve(String path, Object... args)
    {
        try {
            File file = new File(path);
            String page = URLDecoder.decode(file.getName(), CharEncoding.UTF_8);
            String space = URLDecoder.decode(file.getParentFile().getName(), CharEncoding.UTF_8);
            String wiki = URLDecoder.decode(file.getParentFile().getParentFile().getName(), CharEncoding.UTF_8);
            return new DocumentReference(wiki, space, page);
        } catch (UnsupportedEncodingException e) {
            // This will never happen, UTF-8 is always available
            return null;
        }
    }
}

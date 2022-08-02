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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.CharEncoding;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Creates a {@link DocumentReference} from a string representation that has the "wiki/space1/.../spaceN/page" format,
 * where each path component is URL encoded. The current implementation only supports absolute references
 * (i.e up to the wiki part). We made this to work with {@link PathStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named("path")
@Singleton
public class PathStringDocumentReferenceResolver implements DocumentReferenceResolver<String>
{
    @Override
    public DocumentReference resolve(String path, Object... args)
    {
        try {
            File file = new File(path);
            // The last segment is the page name
            String page = URLDecoder.decode(file.getName(), CharEncoding.UTF_8);
            // All parent segments are space segment till the top level segment which is the wiki name
            File current = file;
            String segmentName = null;
            List<String> spaceNames = new ArrayList<>();
            while (current.getParentFile() != null) {
                current = current.getParentFile();
                segmentName = URLDecoder.decode(current.getName(), CharEncoding.UTF_8);
                if (current.getParentFile() != null) {
                    spaceNames.add(segmentName);
                }
            }
            Collections.reverse(spaceNames);
            String wiki = segmentName;
            return new DocumentReference(wiki, spaceNames, page);
        } catch (UnsupportedEncodingException e) {
            // This will never happen, UTF-8 is always available
            throw new RuntimeException("UTF-8 encoding is not present on the system!", e);
        }
    }
}

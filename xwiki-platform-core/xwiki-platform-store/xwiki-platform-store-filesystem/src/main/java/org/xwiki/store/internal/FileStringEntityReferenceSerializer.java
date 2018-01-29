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
package org.xwiki.store.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.internal.reference.AbstractStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;

/**
 * Generate a path representation of an entity reference (eg "Wiki/Space/Page" for a Document Reference in the "wiki"
 * Wiki, the "space" Space and the "page" Page).
 * <p>
 * Parameters:
 * <ul>
 * <li>0: a boolean, if true make the path support case insensitive filesystem (by adding a hash to each element of the
 * path). Default is false.</li>
 * <li>1: a boolean, if true the last element is a file (mostly about keeping the last '.'). Default is folder.</li>
 * </ul>
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
public class FileStringEntityReferenceSerializer extends AbstractStringEntityReferenceSerializer
{
    private boolean isCaseInsensitive(Object... parameters)
    {
        return parameters.length > 0 && parameters[0] == Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add a segment to the path. All non-URL compatible characters are escaped in the URL-escape format (%NN). Dot
     * (".") and Star ("*") characters are also encoded. If this is not the last segment in the reference, append a "/"
     * separator between reference element.
     * <p>
     * Parameters:
     * <ul>
     * <li>1: a boolean, if true make the path support case insensitive filesystem (by adding a hash to each element of
     * the path). Default is false.</li>
     * </ul>
     * <p>
     */
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        if (currentReference.getParent() != null) {
            // Note: Java will convert the file separator to the proper separator for the underlying FileSystem.
            // Note: Using "/" allows us to reuse the serialized result into URLs. Caveat: The % character might need
            // to be escaped as %25 in this case as otherwise the browser will automatically decode % encoding.
            representation.append('/');
        }

        representation.append(encodeName(currentReference.getName(), isCaseInsensitive(parameters)));
    }

    /**
     * @param isLastReference true if this portion of the reference is the last one (ie the deepest one)
     * @param caseInsensitive true if the returned element should support case insensitive filesystem
     * @param file true if the element is a file
     * @param name the reference name for this portion of the reference
     * @return the encoded reference name so that it can be used in a filesystem path.
     */
    protected String encodeName(String name, boolean caseInsensitive)
    {
        return FileSystemStoreUtils.encode(name, caseInsensitive);
    }
}

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
import java.net.URLEncoder;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

/**
 * Generate a string representation of an entity reference (eg "Wiki:Space.Page" for a
 * document reference in the "wiki" Wiki, the "space" Space and the "page" Page).
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component("path")
public class PathStringEntityReferenceSerializer extends AbstractStringEntityReferenceSerializer
{
    /**
     * {@inheritDoc}
     *
     * Add a segment to the path. All non-URL compatible characters are escaped in the URL-escape format
     * (%NN). If this is not the last segment in the reference, append a file separator at the end.
     */
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        if (currentReference.getParent() != null) {
            representation.append(File.separator);
        }

        try {
            representation.append(
                URLEncoder.encode(currentReference.getName(), "UTF-8").replace(".", "%2E"));
        } catch (UnsupportedEncodingException ex) {
            // This will never happen, UTF-8 is always available
        }
    }
}

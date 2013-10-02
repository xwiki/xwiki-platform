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
package org.xwiki.wiki.internal;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.WikiDescriptor;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Constructs a {@link org.xwiki.wiki.WikiDescriptor} instance from an XObject.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Role
public interface WikiDescriptorBuilder
{
    /**
     * Extract data from the passed XObjects to construct a wiki descriptor. The first XObject must be the descriptor
     * itself containing the full data and the other XObjects are descriptors containing just alias information.
     *
     * @param serverClassObjects the XObjects from which to extract the descriptor and its aliases
     * @param document the document containing the XObject, which we use to extract the wiki id (by removing the
     *        {@code XWikiServer} prefix from the document's page name)
     * @return the generated wiki descriptor
     */
    WikiDescriptor build(List<BaseObject> serverClassObjects, XWikiDocument document);
}

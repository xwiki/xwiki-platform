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
package org.xwiki.wiki.internal.descriptor.builder;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Component that create WikiDescriptor object from wiki document and vice versa.
 * @version $Id $
 * @since 5.3M2
 */
@Role
public interface WikiDescriptorBuilder
{
    /**
     * Build a wiki descriptor from a document and its objects.
     * @param serverClassObjects objects of that page to parse
     * @param document document that hold the wiki descriptor
     * @return a wiki descriptor
     */
    DefaultWikiDescriptor buildDescriptorObject(List<BaseObject> serverClassObjects, XWikiDocument document);

    /**
     * Save the descriptor into the wiki.
     * @param descriptor descriptor to buildDescriptorObject
     * @return the document that hold the descriptor
     * @throws WikiDescriptorBuilderException if problems occur
     */
    XWikiDocument save(WikiDescriptor descriptor) throws WikiDescriptorBuilderException;
}

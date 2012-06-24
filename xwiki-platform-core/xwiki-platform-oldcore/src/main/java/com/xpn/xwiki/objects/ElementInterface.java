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
package com.xpn.xwiki.objects;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

public interface ElementInterface
{
    /**
     * @return the reference of the element
     * @since 3.2M1
     */
    EntityReference getReference();

    @Override
    String toString();

    /**
     * @return the reference to the document in which this element is defined (for elements where this make sense, for
     *         example for an XClass or a XObject).
     * @since 2.2M2
     */
    DocumentReference getDocumentReference();

    /**
     * @return the free form name (for elements which don't point to a reference, for example for instances of
     *         {@link BaseProperty}).
     */
    String getName();

    /**
     * @since 2.2M2
     */
    void setDocumentReference(DocumentReference reference);

    void setName(String name);

    /**
     * Apply a 3 ways merge on the current element based on provided previous and new version of the element.
     * <p>
     * All 3 elements are supposed to have the same class and reference.
     * 
     * @param previousElement the previous version of the element
     * @param newElement the next version of the element
     * @param configuration the configuration of the merge Indicate how to deal with some conflicts use cases, etc.
     * @param context the XWiki context
     * @param mergeResult the merge report
     * @since 3.2M1
     */
    void merge(ElementInterface previousElement, ElementInterface newElement, MergeConfiguration configuration, XWikiContext context,
        MergeResult mergeResult);
}

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
package org.xwiki.attachment.picker;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.attachment.picker.internal.AttachmentGalleryPickerMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyName;

/**
 * Parameters for the {@link AttachmentGalleryPickerMacro} Macro.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class AttachmentGalleryPickerMacroParameters
{
    private String id;

    private List<String> filter = new ArrayList<>();

    private Integer limit = 20;

    private String targetDocumentReference;

    /**
     * @return the id of the attachment picker macro
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the id of the attachment picker macro
     */
    @PropertyName("Id")
    @PropertyDescription("The id of the attachment picker macro")
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the supported types of attachments
     */
    public List<String> getFilter()
    {
        return this.filter;
    }

    /**
     * @param filter the supported types of attachments
     */
    @PropertyName("Filter")
    @PropertyDescription("A list of mimetypes to filter the attachments by (e.g. image/*,image/jpeg)")
    public void setFilter(List<String> filter)
    {
        this.filter = filter;
    }

    /**
     * @return the maximum number of attachments to display
     */
    public Integer getLimit()
    {
        return this.limit;
    }

    /**
     * @param limit the maximum number of attachments to display
     */
    @PropertyName("Limit")
    @PropertyDescription("The maximum number of attachments to display")
    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }

    /**
     * @return the reference to the document that will be set as the current document to display the macro content
     * @since 14.10.2
     * @since 15.0RC1
     */
    public String getTarget()
    {
        return this.targetDocumentReference;
    }

    /**
     * @param targetDocumentReference refer to {@link #getTarget()}
     * @since 14.10.2
     * @since 15.0RC1
     */
    @PropertyDescription("The reference to the document serving as the current document")
    @PropertyDisplayType(DocumentReference.class)
    public void setTarget(String targetDocumentReference)
    {
        this.targetDocumentReference = targetDocumentReference;
    }
}

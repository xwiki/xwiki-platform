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
package org.xwiki.filter.instance.input;

import java.util.Collections;
import java.util.Set;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class DocumentInstanceInputProperties extends InstanceInputProperties
{
    /**
     * @see #isWithRevisions()
     */
    private boolean withRevisions = true;

    /**
     * @see #isWithJRCSRevisions()
     */
    private boolean withJRCSRevisions = true;

    /**
     * @see #isWithWikiAttachments()
     */
    private boolean withWikiAttachments = true;

    /**
     * @see #isWithWikiAttachmentsContent()
     */
    private boolean withWikiAttachmentsContent = true;

    /**
     * @see #getAttachmentsContent()
     */
    private Set<String> attachmentsContent;

    /**
     * @see #isWithWikiAttachmentsRevisions()
     */
    private boolean withWikiAttachmentsRevisions;

    /**
     * @see #isWithWikiAttachmentJRCSRevisions()
     */
    private boolean withWikiAttachmentJRCSRevisions = true;

    /**
     * @see #isWithWikiObjects()
     */
    private boolean withWikiObjects = true;

    /**
     * @see #isWithWikiClass()
     */
    private boolean withWikiClass = true;

    /**
     * @see #isWithWikiDocumentContentHTML()
     */
    private boolean withWikiDocumentContentHTML;

    /**
     * @return Indicates if events should be generated for history
     */
    @PropertyName("With revisions")
    @PropertyDescription("Indicates if events should be generated for history")
    public boolean isWithRevisions()
    {
        return this.withRevisions;
    }

    /**
     * @param withRevisions Indicates if events should be generated for history
     */
    public void setWithRevisions(boolean withRevisions)
    {
        this.withRevisions = withRevisions;
    }

    /**
     * @return Indicates if JRCS based history should be added to document locale event properties
     */
    @PropertyName("With JRCS revisions")
    @PropertyDescription("Indicates if JRCS based history should be added to document locale event properties")
    public boolean isWithJRCSRevisions()
    {
        return withJRCSRevisions;
    }

    /**
     * @param withJRCSRevisions Indicates if JRCS based history should be added to document locale event properties
     */
    public void setWithJRCSRevisions(boolean withJRCSRevisions)
    {
        this.withJRCSRevisions = withJRCSRevisions;

        // Also update the attachment JRCS status when updating document JRCS status for retro compatibility reasons
        setWithWikiAttachmentJRCSRevisions(withJRCSRevisions);
    }

    /**
     * @return Indicate if events should be generated for attachments
     */
    @PropertyName("With attachments")
    @PropertyDescription("Indicate if events should be generated for attachments")
    public boolean isWithWikiAttachments()
    {
        return this.withWikiAttachments;
    }

    /**
     * @param withWikiAttachments Indicate if events should be generated for attachments
     */
    public void setWithWikiAttachments(boolean withWikiAttachments)
    {
        this.withWikiAttachments = withWikiAttachments;
    }

    /**
     * @return Indicate if events should be generated for attachments content
     * @since 9.0RC1
     */
    @PropertyName("With attachments content")
    @PropertyDescription("Indicate if events should be generated for attachments content")
    public boolean isWithWikiAttachmentsContent()
    {
        return this.withWikiAttachmentsContent;
    }

    /**
     * @param withWikiAttachmentsContent Indicate if events should be generated for attachments content
     * @since 9.0RC1
     */
    public void setWithWikiAttachmentsContent(boolean withWikiAttachmentsContent)
    {
        this.withWikiAttachmentsContent = withWikiAttachmentsContent;
    }

    /**
     * @return the attachments for which to generate content events
     * @since 13.8RC1
     */
    @PropertyName("Content of attachments")
    @PropertyDescription("The attchments names for which to generate events")
    public Set<String> getAttachmentsContent()
    {
        return this.attachmentsContent != null ? this.attachmentsContent : Collections.emptySet();
    }

    /**
     * @param attachmentsContent the attachments for which to generate content events
     * @since 13.8RC1
     */
    public void setAttachmentsContent(Set<String> attachmentsContent)
    {
        this.attachmentsContent = attachmentsContent;
    }

    /**
     * @return Indicate if events should be generated for attachments revisions
     * @since 12.0RC1
     */
    @PropertyName("With attachments revisions")
    @PropertyDescription("Indicate if events should be generated for attachments revisions")
    public boolean isWithWikiAttachmentsRevisions()
    {
        return this.withWikiAttachmentsRevisions;
    }

    /**
     * @param withWikiAttachmentsRevisions Indicate if events should be generated for attachments revisions
     * @since 12.0RC1
     */
    public void setWithWikiAttachmentsRevisions(boolean withWikiAttachmentsRevisions)
    {
        this.withWikiAttachmentsRevisions = withWikiAttachmentsRevisions;
    }

    /**
     * @return Indicates if JRCS based history should be added to attachment event properties
     */
    @PropertyName("With attachment JRCS revisions")
    @PropertyDescription("Indicates if JRCS based history should be added to attachment event properties")
    public boolean isWithWikiAttachmentJRCSRevisions()
    {
        return withWikiAttachmentJRCSRevisions;
    }

    /**
     * @param withWikiAttachmentJRCSRevisions Indicates if JRCS based history should be added to attachment event
     *            properties
     */
    public void setWithWikiAttachmentJRCSRevisions(boolean withWikiAttachmentJRCSRevisions)
    {
        this.withWikiAttachmentJRCSRevisions = withWikiAttachmentJRCSRevisions;
    }

    /**
     * @return Indicate if events should be generated for classes
     */
    @PropertyName("With classes")
    @PropertyDescription("Indicate if events should be generated for classes")
    public boolean isWithWikiClass()
    {
        return this.withWikiClass;
    }

    /**
     * @param withWikiClass Indicate if events should be generated for classes
     */
    public void setWithWikiClass(boolean withWikiClass)
    {
        this.withWikiClass = withWikiClass;
    }

    /**
     * @return Indicate if events should be generated for objects
     */
    @PropertyName("With objects")
    @PropertyDescription("Indicate if events should be generated for objects")
    public boolean isWithWikiObjects()
    {
        return this.withWikiObjects;
    }

    /**
     * @param withWikiObjects Indicate if events should be generated for objects
     */
    public void setWithWikiObjects(boolean withWikiObjects)
    {
        this.withWikiObjects = withWikiObjects;
    }

    /**
     * @return Indicate if events should be generated for document content as HTML
     */
    @PropertyName("With content as HTML")
    @PropertyDescription("Indicate if events should be generated for document content as HTML")
    public boolean isWithWikiDocumentContentHTML()
    {
        return this.withWikiDocumentContentHTML;
    }

    /**
     * @param withWikiDocumentContentHTML Indicate if events should be generated for document content as HTML
     */
    public void setWithWikiDocumentContentHTML(boolean withWikiDocumentContentHTML)
    {
        this.withWikiDocumentContentHTML = withWikiDocumentContentHTML;
    }
}

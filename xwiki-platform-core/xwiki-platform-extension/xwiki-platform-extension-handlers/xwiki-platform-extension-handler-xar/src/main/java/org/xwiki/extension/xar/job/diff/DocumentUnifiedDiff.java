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
package org.xwiki.extension.xar.job.diff;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.ObjectReference;

/**
 * Holds the differences, in unified format, between two versions of a document.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public class DocumentUnifiedDiff extends EntityUnifiedDiff<DocumentVersionReference>
{
    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The list of attachments that have differences between the two versions of the document.
     */
    private final List<EntityUnifiedDiff<AttachmentReference>> attachmentDiffs = new ArrayList<>();

    /**
     * The list of objects that have differences between the two versions of the document.
     */
    private final List<EntityUnifiedDiff<ObjectReference>> objectDiffs = new ArrayList<>();

    /**
     * The list of class properties that have differences between the two versions of the document.
     */
    private final List<EntityUnifiedDiff<ClassPropertyReference>> classPropertyDiffs = new ArrayList<>();

    /**
     * Creates a new instance to hold the differences between the specified document versions.
     *
     * @param previousReference the reference to the previous version of the document
     * @param nextReference the reference to the next version of the document
     */
    public DocumentUnifiedDiff(DocumentVersionReference previousReference, DocumentVersionReference nextReference)
    {
        super(previousReference, nextReference);
    }

    /**
     * @return the list of class properties that have differences between the two versions of the document
     */
    public List<EntityUnifiedDiff<ClassPropertyReference>> getClassPropertyDiffs()
    {
        return classPropertyDiffs;
    }

    /**
     * @return the list of attachments that have differences between the two versions of the document
     */
    public List<EntityUnifiedDiff<AttachmentReference>> getAttachmentDiffs()
    {
        return attachmentDiffs;
    }

    /**
     * @return the list of objects that have differences between the two versions of the document
     */
    public List<EntityUnifiedDiff<ObjectReference>> getObjectDiffs()
    {
        return objectDiffs;
    }
}

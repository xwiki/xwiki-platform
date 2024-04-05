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
package org.xwiki.annotation.rights;

import org.xwiki.component.annotation.Role;

/**
 * Service to provide functions to check access rights to annotations actions (viewing, adding annotations, editing).
 *
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface AnnotationRightService
{
    /**
     * Checks if the specified user can view the annotations on the specific target.
     *
     * @param target the target of the annotations to view
     * @param userName the name of the user to view the annotations
     * @return {@code true} if the user can view the annotations on the passed target, {@code false} otherwise
     */
    boolean canViewAnnotations(String target, String userName);

    /**
     * Checks if the specified user can view the passed target annotated (which implies view on the target itself and
     * the ability to view the annotations).
     *
     * @param target the target to view annotated
     * @param userName the name of the user to view the annotated target
     * @return {@code true} if the user can see the target annotated, {@code false} otherwise.
     */
    boolean canViewAnnotatedTarget(String target, String userName);

    /**
     * Checks if the user can add annotations on the specified target.
     *
     * @param target the target on which to add annotations
     * @param userName the name of the user to add annotations
     * @return {@code true} if the user can annotate the target, {@code false} otherwise
     */
    boolean canAddAnnotation(String target, String userName);

    /**
     * Checks if the user can edit the specified annotation.
     *
     * @param target the target of the annotation to edit
     * @param userName the name of the user to edit the annotation
     * @param annotationId the id of the annotation to be edited
     * @return {@code true} if the user can edit the annotation, {@code false} otherwise
     */
    boolean canEditAnnotation(String annotationId, String target, String userName);

    /**
     * Checks if the user can upload attachment to the page as part of adding or editing an annotation.
     * @param target the target of the page where to upload the attachment
     * @param userName the name of the user performing the upload
     * @return {@code true} if the user can upload the attachment, {@code false} otherwise
     * @since 14.10
     */
    default boolean canUploadAttachment(String target, String userName)
    {
        return false;
    }
}

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
package org.xwiki.eventstream.store.internal;

/**
 * The type of request. This list is extensible.
 *
 * @version $Id$
 */
public interface EventType
{
    /**
     * Document creation.
     */
    String CREATE = "create";

    /**
     * Document modification.
     */
    String UPDATE = "update";

    /**
     * Document deletion.
     */
    String DELETE = "delete";

    /**
     * Comment add.
     */
    String ADD_COMMENT = "addComment";

    /**
     * Comment modification.
     */
    String UPDATE_COMMENT = "updateComment";

    /**
     * Comment deletion.
     */
    String DELETE_COMMENT = "deleteComment";

    /**
     * Attachment add.
     */
    String ADD_ATTACHMENT = "addAttachment";

    /**
     * Attachment modification.
     */
    String UPDATE_ATTACHMENT = "updateAttachment";

    /**
     * Attachment deletion.
     */
    String DELETE_ATTACHMENT = "deleteAttachment";

    /**
     * Annotation add.
     */
    String ADD_ANNOTATION = "addAnnotation";

    /**
     * Annotation modification.
     */
    String UPDATE_ANNOTATION = "updateAnnotation";

    /**
     * Annotation deletion.
     */
    String DELETE_ANNOTATION = "deleteAnnotation";
}

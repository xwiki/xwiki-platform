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
package org.xwiki.annotation.maintainer;

import org.xwiki.component.annotation.Role;

/**
 * Interface defining the annotation maintainer service, which should be able to update the annotations on a specified
 * target, wrt to the previous and the current versions of the content.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface AnnotationMaintainer
{
    /**
     * Updates all the annotations found on the passed target. Should use the IO services to get the annotations and
     * information about the target, such as syntax.
     * 
     * @param target is serialized reference to the content concerned by the annotation
     * @param previousContent the previous content of the document (before the update)
     * @param currentContent the current content of the document (after the update)
     * @throws MaintainerServiceException in case something goes wrong handling the annotation updates on the passed
     *             content
     */
    void updateAnnotations(String target, String previousContent, String currentContent)
        throws MaintainerServiceException;
}

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
package org.xwiki.annotation;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * The configuration of the Annotations Application for the current wiki.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface AnnotationConfiguration
{
    /**
     * The configuration page's space name.
     */
    String CONFIGURATION_PAGE_SPACE_NAME = "AnnotationCode";

    /**
     * The configuration page's name.
     */
    String CONFIGURATION_PAGE_NAME = "AnnotationConfig";

    /**
     * @return true if the annotations application is installed.
     */
    boolean isInstalled();
    
    /**
     * @return true if the annotations UI is activated, false otherwise.
     */
    boolean isActivated();

    /**
     * @return the list of spaces where the annotations UI should not be displayed.
     */
    List<SpaceReference> getExceptionSpaces();

    /**
     * @return true if annotations are displayed by default in the UI.
     */
    boolean isDisplayedByDefault();

    /**
     * @return true if the annotations should be highlighted by default in the UI.
     */
    boolean isDisplayedHighlightedByDefault();

    /**
     * @return the reference of the XWiki class defining an annotation's structure.
     */
    DocumentReference getAnnotationClassReference();
}

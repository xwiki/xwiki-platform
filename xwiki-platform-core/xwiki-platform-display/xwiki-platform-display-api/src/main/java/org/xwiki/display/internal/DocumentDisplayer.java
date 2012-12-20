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
package org.xwiki.display.internal;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;

import org.xwiki.rendering.block.XDOM;

/**
 * Component used to display documents.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Role
public interface DocumentDisplayer
    extends Displayer<DocumentModelBridge, DocumentDisplayerParameters>
{
    /**
     * Displays the given data with a security item set, to enable privilege delegation.
     *
     * This method is necessary because the document the displayer is abused in XWikiDocument for displaying text
     * strings that are not actual documents, and because the default translation version of the document will allways
     * be passed as 'data', even when the actually displayed document is a translation.  (In this latter case, the
     * translated document will be passed as security item.)
     *
     * @param data the data to be displayed
     * @param parameters display parameters
     * @param securityItem the security item
     * @return the result of displaying the given data
     * @since 4.4M1
     */
    XDOM display(DocumentModelBridge data, DocumentDisplayerParameters parameters, DocumentModelBridge securityItem);
}

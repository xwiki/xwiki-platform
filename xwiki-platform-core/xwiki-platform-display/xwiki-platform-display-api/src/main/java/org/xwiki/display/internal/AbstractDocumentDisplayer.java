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

import javax.inject.Inject;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContentAuthorController;

/**
 * Abstract class for performing common preparations before actually displaying.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractDocumentDisplayer implements DocumentDisplayer
{

    /** This is used for setting the content author in the authorization context. */
    @Inject
    private ContentAuthorController contentAuthorController;

    @Override
    public final XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        contentAuthorController.pushContentDocument(parameters.getContentDocument());
        try {
            return doDisplay(document, parameters);
        } finally {
            contentAuthorController.popContentDocument();
        }
    }

    /**
     * @param document the document to be displayed
     * @param parameters display parameters
     * @return the result of displaying the given data
     */
    protected abstract XDOM doDisplay(DocumentModelBridge document, DocumentDisplayerParameters parameters);
}

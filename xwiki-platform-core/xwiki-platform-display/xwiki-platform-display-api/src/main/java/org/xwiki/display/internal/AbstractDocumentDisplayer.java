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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.phase.Initializable;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContentDocumentController;
import org.xwiki.security.authorization.PrivilegedModeController;

/**
 * Abstract class for performing common preparations before actually displaying.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractDocumentDisplayer implements DocumentDisplayer, Initializable
{

    /** Used to push the document to the security stack. */
    @Inject
    private ContentDocumentController contentDocumentController;

    /** Provide a privileged mode controller. */
    @Inject
    private Provider<PrivilegedModeController> privilegedModeControllerProvider;
    
    /** Provide a logger. */
    @Inject
    private Logger logger;

    /** This is used for disabling privileged mode, if the restricted parameter is set. */
    private PrivilegedModeController privilegedModeController;

    @Override
    public void initialize()
    {
        privilegedModeController = privilegedModeControllerProvider.get();
        privilegedModeControllerProvider = null;
    }

    @Override
    public final XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        return display(document, parameters, null);
    }

    @Override
    public final XDOM display(DocumentModelBridge document,
                              DocumentDisplayerParameters parameters,
                              DocumentModelBridge securityDocument)
    {
        final boolean restricted = parameters.isTransformationContextRestricted();

        if (restricted) {
            privilegedModeController.disablePrivilegedMode();
        }

        contentDocumentController.pushContentDocument(securityDocument);

        try {
            return doDisplay(document, parameters);
        } finally {
            contentDocumentController.popContentDocument();

            if (restricted) {
                privilegedModeController.restorePrivilegedMode();
            }
        }
    }

    /**
     * @param document the document to be displayed
     * @param parameters display parameters
     * @return the result of displaying the given data
     */
    protected abstract XDOM doDisplay(DocumentModelBridge document, DocumentDisplayerParameters parameters);
}

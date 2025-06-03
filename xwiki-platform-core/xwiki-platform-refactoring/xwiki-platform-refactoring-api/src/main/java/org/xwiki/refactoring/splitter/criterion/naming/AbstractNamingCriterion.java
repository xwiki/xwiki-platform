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
package org.xwiki.refactoring.splitter.criterion.naming;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Base class for {@link NamingCriterion} implementations.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
public abstract class AbstractNamingCriterion implements NamingCriterion
{
    private static final String DEFAULT_DOCUMENT_NAME = "WebHome";

    @Inject
    protected Logger logger;

    /**
     * {@link DocumentAccessBridge} used to lookup for existing wiki pages and avoid name clashes.
     */
    @Inject
    protected DocumentAccessBridge docBridge;

    private final NamingCriterionParameters parameters = new NamingCriterionParameters();

    @Override
    public NamingCriterionParameters getParameters()
    {
        return this.parameters;
    }

    protected boolean exists(DocumentReference documentReference)
    {
        try {
            return this.docBridge.exists(documentReference);
        } catch (Exception e) {
            this.logger.warn("Failed to check the existence of the document with reference [{}]. Root cause is [{}].",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
        }

        return false;
    }

    protected String getBasePageName()
    {
        return getPageName(this.parameters.getBaseDocumentReference());
    }

    protected String getPageName(DocumentReference documentReference)
    {
        return DEFAULT_DOCUMENT_NAME.equals(documentReference.getName())
            ? documentReference.getLastSpaceReference().getName() : documentReference.getName();
    }

    protected DocumentReference newDocumentReference(String pageName)
    {
        DocumentReference baseDocumentReference = this.parameters.getBaseDocumentReference();
        if (this.parameters.isUseTerminalPages()) {
            return new DocumentReference(pageName, baseDocumentReference.getLastSpaceReference());
        } else {
            return new DocumentReference(DEFAULT_DOCUMENT_NAME,
                new SpaceReference(pageName, baseDocumentReference.getLastSpaceReference()));
        }
    }
}

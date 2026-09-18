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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default {@link DocumentDisplayer} implementation.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Singleton
public class DefaultDocumentDisplayer implements DocumentDisplayer
{
    /**
     * The component used to display the document title.
     */
    @Inject
    @Named("title")
    private DocumentDisplayer titleDisplayer;

    /**
     * The component used to display the document content.
     */
    @Inject
    @Named("content")
    private DocumentDisplayer contentDisplayer;

    /**
     * The component used to lookup a syntax specific implementation for content and title displayer.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The component used to access the translation of the displayed document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        DocumentDisplayer displayer;
        if (parameters.isTitleDisplayed()) {
            displayer = this.titleDisplayer;

            // The title is taken from the passed document, so the syntax of the passed document is the right one.
            String titleHint = "title/" + document.getSyntax().toIdString();
            if (this.componentManager.hasComponent(DocumentDisplayer.class, titleHint)) {
                try {
                    displayer = this.componentManager.getInstance(DocumentDisplayer.class, titleHint);
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to load title document displayer", e);
                }
            }
        } else {
            displayer = this.contentDisplayer;

            // The displayed content is the one of the translation, which can have a syntax of its own.
            String contentHint = "content/" + getContentSyntax(document, parameters).toIdString();
            if (this.componentManager.hasComponent(DocumentDisplayer.class, contentHint)) {
                try {
                    displayer = this.componentManager.getInstance(DocumentDisplayer.class, contentHint);
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to load content document displayer", e);
                }
            }
        }

        return displayer.display(document, parameters);
    }

    /**
     * @param document the document to display
     * @param parameters the display parameters
     * @return the syntax of the content that is going to be displayed, which is the syntax of the translation when a
     *         translated content is displayed
     */
    private Syntax getContentSyntax(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        if (parameters.isContentTranslated()) {
            try {
                return this.documentAccessBridge.getTranslatedDocumentInstance(document).getSyntax();
            } catch (Exception e) {
                this.logger.warn("Failed to load the translation of document [{}]. Falling back on the syntax of the "
                    + "default translation. Root cause is [{}]", document.getDocumentReference(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return document.getSyntax();
    }
}

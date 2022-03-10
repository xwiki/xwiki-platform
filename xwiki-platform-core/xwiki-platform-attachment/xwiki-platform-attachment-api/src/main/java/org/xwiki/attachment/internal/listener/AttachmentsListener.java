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
package org.xwiki.attachment.internal.listener;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.HINT;

/**
 * Listen for attachments creation and if the created attachment name matches a redirection, remove the corresponding
 * XObject.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named(HINT)
public class AttachmentsListener implements EventListener
{
    @Inject
    private AttachmentsManager attachmentsManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public List<Event> getEvents()
    {
        return singletonList(new AttachmentAddedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        AttachmentAddedEvent attachmentAddedEvent = (AttachmentAddedEvent) event;
        XWikiDocument doc = (XWikiDocument) source;
        if (this.attachmentsManager.removeExistingRedirection(attachmentAddedEvent.getName(), doc)) {
            // The document is only saved if a redirection has actually been removed.
            try {
                String message = this.contextualLocalizationManager.getTranslationPlain(
                    "attachment.listener.attachmentAdded.removeRedirection", attachmentAddedEvent.getName());
                this.xcontextProvider.get().getWiki().saveDocument(doc,
                    message, true,
                    this.xcontextProvider.get());
            } catch (XWikiException e) {
                this.logger.warn(
                    "Unable to remove deprecated attachment redirection object from [{}] for attachment [{}]. "
                        + "Cause: [{}].", doc, attachmentAddedEvent.getName(), getRootCauseMessage(e));
            }
        }
    }
}

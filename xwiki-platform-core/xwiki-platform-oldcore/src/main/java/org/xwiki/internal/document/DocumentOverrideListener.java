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
package org.xwiki.internal.document;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;

/**
 * Cancel any save that tries to save a new document if the document already exists.
 *
 * <p>
 *     This can happen when the context document is set to an empty document because the user doesn't have view right,
 *     but the user still has, e.g., edit right. In this situation, the user would still be able to to save but it is
 *     hard to imagine a scenario where this would make sense. This listener therefore cancels the save in this
 *     situation.
 * </p>
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.6
 */
@Component
@Singleton
@Named(DocumentOverrideListener.NAME)
public class DocumentOverrideListener extends AbstractEventListener
{
    /**
     * The unique identifier of the listener.
     */
    public static final String NAME = "org.xwiki.internal.document.DocumentOverrideListener";

    /**
     * The default constructor.
     */
    public DocumentOverrideListener()
    {
        super(NAME, new UserUpdatingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiDocument originalDocument = document.getOriginalDocument();

        if (document.isNew() && originalDocument != null && !originalDocument.isNew()) {
            ((CancelableEvent) event).cancel(
                "The document already exists but the document to be saved is marked as new.");
        }
    }
}

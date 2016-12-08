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
package org.xwiki.platform.blog.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.blog.BlogVisibilityUpdater;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Synchronize the visibility of blog pages with the "published" and "hidden" fields of the BlogPostClass object.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Component
@Singleton
@Named(BlogDocumentSavingListener.NAME)
public class BlogDocumentSavingListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "Blog Document Saving Listener";

    @Inject
    private BlogVisibilityUpdater blogVisibilityUpdater;

    /**
     * Construct a BlogDocumentSavingListener.
     */
    public BlogDocumentSavingListener()
    {
        super(NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        blogVisibilityUpdater.synchronizeHiddenMetadata((XWikiDocument) source);
    }
}

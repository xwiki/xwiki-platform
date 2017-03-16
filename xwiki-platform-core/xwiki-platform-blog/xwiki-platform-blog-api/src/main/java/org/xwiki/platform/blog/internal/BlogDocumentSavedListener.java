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

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.blog.events.BlogPostPublishedEvent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Send the {@link BlogPostPublishedEvent} when a blog post is published.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named(BlogDocumentSavedListener.NAME)
public class BlogDocumentSavedListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "Blog Document Saved Listener";

    @Inject
    private ObservationManager observationManager;

    /**
     * Construct a BlogDocumentSavedListener.
     */
    public BlogDocumentSavedListener()
    {
        super(NAME, new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        // Send a BlogPostPublishedEvent if the blog post is published but was not before
        final DocumentReference blogPostClass = new DocumentReference("BlogPostClass", new SpaceReference("Blog",
                document.getDocumentReference().getWikiReference()));

        BaseObject blogPost         = document.getXObject(blogPostClass);
        BaseObject previousBlogPost = document.getOriginalDocument().getXObject(blogPostClass);

        if (isPublished(blogPost) && !isPublished(previousBlogPost)) {
            observationManager.notify(new BlogPostPublishedEvent(), "org.xwiki.platform:xwiki-platform-blog-api",
                    document);
        }
    }

    private boolean isPublished(BaseObject blogPost)
    {
        return blogPost != null && blogPost.getIntValue("published") == 1 && blogPost.getIntValue("hidden") == 0;
    }
}

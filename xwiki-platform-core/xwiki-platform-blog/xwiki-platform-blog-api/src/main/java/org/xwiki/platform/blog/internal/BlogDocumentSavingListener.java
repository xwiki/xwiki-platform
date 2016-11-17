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

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Synchronize the visibility of blog pages with the "published" and "hidden" fields of the BlogPostClass object.
 *
 * The Blog Application offers the ability to hide from the visitors some articles that are being written. When a blog
 * post is not "published" or "hidden", it is not listed on the blog pages. However, the visitor can still find the
 * article using the Search Engine or the breadcrumb. This is usually not what the writer expects.
 *
 * So we have decided to synchronize the "hidden" field of the document with the values of the object.
 *
 * Note: according to the blog workflow, a post cannot be "unpublished" and "hidden" in the same time. The workflow is:
 * "unpublished" (draft) -> "published" -> "hidden".
 *
 * For some reason, we cannot go back from "published" to "unpublished", and that is why the "hidden" field has been
 * introduced in the BlogPostClass.
 *
 * In the future we could also remove the "published" and the "hidden" boolean to replace them with a "status" field,
 * which would be more accurate to represent the blog workflow.
 *
 * TODO: decide if we should change the page's rights as well, ie. giving 'view' right only to the blog author or to a
 * "blog authors" group.
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.1
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

    /**
     * Construct a BlogDocumentSavingListener.
     */
    public BlogDocumentSavingListener()
    {
        super(NAME, Arrays.asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        final DocumentReference blogPostClass = new DocumentReference("BlogPostClass", new SpaceReference("Blog",
                document.getDocumentReference().getWikiReference()));

        BaseObject blogPost = document.getXObject(blogPostClass);
        if (blogPost != null) {
            // Set the document visibility according to the values of the blog object.
            // The change will be saved after because the event is sent before the actual saving.
            document.setHidden(blogPost.getIntValue("published") == 0 || blogPost.getIntValue("hidden") == 1);
        }
    }
}

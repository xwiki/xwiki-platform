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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.platform.blog.BlogVisibilityUpdater;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link BlogVisibilityUpdater}.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.2
 * @since 7.4.6
 */
@Component
@Singleton
public class DefaultBlogVisibilityUpdater implements BlogVisibilityUpdater
{
    @Override
    public void synchronizeHiddenMetadata(XWikiDocument document)
    {
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

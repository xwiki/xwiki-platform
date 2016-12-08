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
package org.xwiki.platform.blog;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Component that set the "hidden" flag of a document according to its blog post values.
 *
 * The Blog Application offers the ability to hide from the visitors some articles that are being written. When a blog
 * post is not "published" or "hidden", it is not listed on the blog pages. However, the visitor can still find the
 * article using the Search Engine or the breadcrumb. This is usually not what the writer expects.
 *
 * So we have decided to synchronize the "hidden" field of the document with the values of the object.
 *
 * Note: according to the blog workflow, a post cannot be "unpublished" and "hidden" in the same time. The workflow is:
 * "unpublished" (draft) -&gt; "published" -&gt; "hidden".
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
 *
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Role
@Unstable
public interface BlogVisibilityUpdater
{
    /**
     * Set the "hidden" flag of the document (without saving it) according to the values of the blog post.
     * @param document the document to modify
     */
    void synchronizeHiddenMetadata(XWikiDocument document);
}

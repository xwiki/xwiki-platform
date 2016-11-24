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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.blog.BlogVisibilityMigration;
import org.xwiki.platform.blog.BlogVisibilityUpdater;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link BlogVisibilityMigration}.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.2
 * @since 7.4.6
 */
@Component
@Singleton
public class DefaultBlogVisibilityMigration implements BlogVisibilityMigration
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private BlogVisibilityUpdater blogVisibilityUpdater;

    @Inject
    private Logger logger;

    @Override
    public void execute(WikiReference wikiReference) throws Exception
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();

            // Detect the blog posts where the visibility is not what it should be
            final String xwql =
                    "from doc.object(Blog.BlogPostClass) obj where "
                            + "((obj.published = 0 or obj.hidden = 1) and doc.hidden <> 1)"
                            + " or "
                            + "(obj.published = 1 and obj.hidden = 0 and doc.hidden <> 0)";

            Query query = queryManager.createQuery(xwql, Query.XWQL).setWiki(wikiReference.getName());
            for (String docName : query.<String>execute()) {
                DocumentReference documentReference = referenceResolver.resolve(docName, wikiReference);
                XWikiDocument document = xwiki.getDocument(documentReference, context);
                // The following line is not necessary since the BlogDocumentSavingListener will do the same
                // but I think it's more clean to not rely on this and to do it anyway.
                blogVisibilityUpdater.synchronizeHiddenMetadata(document);
                // The saving is the necessary thing here.
                xwiki.saveDocument(document, "Change the page's visibility according to the blog post.", context);
            }

            logger.info("Migration of blog posts' visibility has been successfully executed on the wiki [{}].",
                    wikiReference.getName());
        } catch (Exception e) {
            throw new Exception("Failed to migrate the blog posts' visibility.", e);
        }
    }
}

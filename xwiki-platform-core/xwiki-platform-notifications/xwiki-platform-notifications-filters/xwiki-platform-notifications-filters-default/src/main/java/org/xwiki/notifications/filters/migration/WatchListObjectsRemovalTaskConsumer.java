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
package org.xwiki.notifications.filters.migration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Task for removing WatchListClass xobjects from page. This task should only be triggered when
 * {@link R160000000XWIKI17243DataMigration} has been done.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@Component
@Named(WatchListObjectsRemovalTaskConsumer.TASK_NAME)
@Singleton
public class WatchListObjectsRemovalTaskConsumer implements TaskConsumer
{
    static final String TASK_NAME = "watchlist-xobject-removal";

    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "WatchListClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        XWikiContext context = this.contextProvider.get();
        EntityReference classReference = CLASS_REFERENCE.appendParent(documentReference.getWikiReference());
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            if (document.removeXObjects(classReference)) {
                context.getWiki().saveDocument(document, "Migration of watchlist preferences", context);
            }
        } catch (XWikiException e) {
            throw new RuntimeException(e);
        }
    }
}

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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Task for removing WatchListClass xobjects from page. This task should only be triggered when
 * {@link R160000000XWIKI17243DataMigration} has been done.
 * Note that this task might also create an autowatch xobject to get the new property if it's not defined, as we use
 * to fallback on the autowatch property defined in the WatchListClass xobject.
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

    private static final String XWIKI_SPACE = "XWiki";

    private static final List<String> NOTIFICATIONS_CODE_SPACE = Arrays.asList(XWIKI_SPACE, "Notifications", "Code");

    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(XWIKI_SPACE, "WatchListClass");

    private static final LocalDocumentReference AUTOMATIC_WATCH_CLASS_REFERENCE =
        new LocalDocumentReference(NOTIFICATIONS_CODE_SPACE, "AutomaticWatchModeClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        XWikiContext context = this.contextProvider.get();
        EntityReference classReference = CLASS_REFERENCE.appendParent(documentReference.getWikiReference());
        EntityReference autoWatchClassReference =
            AUTOMATIC_WATCH_CLASS_REFERENCE.appendParent(documentReference.getWikiReference());
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);

            // We use to fallback on the old WatchClass xobject automaticwatch property when it was defined, so we
            // also take back this value and create the new autowatch xobject if needed.
            BaseObject watchXObject = document.getXObject(classReference);

            // watchlist xobject exists, and the doc is about a user profile and there's no autowatch xobject yet
            if (watchXObject != null
                && document.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE) != null
                && document.getXObject(autoWatchClassReference) == null) {
                String automaticwatch = watchXObject.getStringValue("automaticwatch");
                if (!StringUtils.isBlank(automaticwatch) && !"default".equals(automaticwatch)) {
                    BaseObject autowatchXObject = document.newXObject(autoWatchClassReference, context);
                    autowatchXObject.setStringValue("automaticWatchMode", automaticwatch);
                }
            }

            if (document.removeXObjects(classReference)) {
                context.getWiki().saveDocument(document, "Migration of watchlist preferences", context);
            }
        } catch (XWikiException e) {
            throw new IndexException(
                String.format("Error when trying to clean up watchlist object preferences in [%s]",
                    documentReference), e);
        }
    }
}

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
package org.xwiki.watchlist.internal.documents;

import java.util.Collection;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.watchlist.internal.WatchListNotificationCache;
import org.xwiki.watchlist.internal.api.AutomaticWatchMode;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Document initializer for {@value #DOCUMENT_FULL_NAME}.
 * 
 * @version $Id$
 */
@Component
@Named(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME)
@Singleton
@Priority(10000)
public class WatchListClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Class document name.
     */
    public static final String DOCUMENT_NAME = "WatchListClass";

    /**
     * Class document full name.
     */
    public static final String DOCUMENT_FULL_NAME = XWiki.SYSTEM_SPACE + "." + DOCUMENT_NAME;

    /**
     * Class document reference.
     */
    public static final EntityReference DOCUMENT_REFERENCE = new EntityReference(DOCUMENT_NAME, EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * Property of the watchlist class used to store the notification interval preference.
     */
    public static final String INTERVAL_PROPERTY = "interval";

    /**
     * Property of the watchlist class used to store the list of wikis to watch.
     */
    public static final String WIKIS_PROPERTY = "wikis";

    /**
     * Property of the watchlist class used to store the list of spaces to watch.
     */
    public static final String SPACES_PROPERTY = "spaces";

    /**
     * Property of the watchlist class used to store the list of documents to watch.
     */
    public static final String DOCUMENTS_PROPERTY = "documents";

    /**
     * Property of the watchlist class used to store the list of users to watch.
     */
    public static final String USERS_PROPERTY = "users";

    /**
     * Property of the watchlist class used to indicate what should be automatically watched.
     */
    public static final String AUTOMATICWATCH_PROPERTY = "automaticwatch";

    /**
     * Used to get the main wiki id.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Used to get the list of possible values for {@value #INTERVAL_PROPERTY}.
     */
    @Inject
    private Provider<WatchListNotificationCache> notificationCacheProvider;

    /**
     * Default constructor.
     */
    public WatchListClassDocumentInitializer()
    {
        super(DOCUMENT_REFERENCE);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;
        BaseClass bclass = document.getXClass();

        // Interval property
        needsUpdate |= bclass.addStaticListField(INTERVAL_PROPERTY, "Email notifications interval", "");

        // Check that the interval property contains all the available jobs
        StaticListClass intervalClass = (StaticListClass) bclass.get(INTERVAL_PROPERTY);
        List<String> intervalValues = ListClass.getListFromString(intervalClass.getValues());

        // Look for missing or outdated jobs in the interval list
        Collection<String> jobDocumentNames = notificationCacheProvider.get().getJobDocumentNames();
        if (!CollectionUtils.disjunction(jobDocumentNames, intervalValues).isEmpty()) {
            needsUpdate = true;
            // TODO: Sort them by cron expression?
            intervalClass.setValues(StringUtils.join(jobDocumentNames, ListClass.DEFAULT_SEPARATOR));
        }

        // Watched elements properties
        needsUpdate |= this.addWatchedElementField(bclass, WIKIS_PROPERTY, "Wiki list");
        needsUpdate |= this.addWatchedElementField(bclass, SPACES_PROPERTY, "Space list");
        needsUpdate |= this.addWatchedElementField(bclass, DOCUMENTS_PROPERTY, "Document list");
        needsUpdate |= this.addWatchedElementField(bclass, USERS_PROPERTY, "User list");

        // Automatic watching property
        needsUpdate |=
            bclass.addStaticListField(AUTOMATICWATCH_PROPERTY, "Automatic watching",
                "default|" + StringUtils.join(AutomaticWatchMode.values(), "|"));

        // Handle the fields and the sheet of the document containing the class.
        needsUpdate |= setClassDocumentFields(document, "XWiki WatchList Notification Rules Class");

        return needsUpdate;
    }

    /**
     * @param bclass the class to add to
     * @param name the name of the property to add
     * @param prettyName the pretty name of the property to add
     * @return true if the property was added; false otherwise
     */
    private boolean addWatchedElementField(BaseClass bclass, String name, String prettyName)
    {
        boolean needsUpdate = false;

        needsUpdate = bclass.addDBListField(name, prettyName, 80, true, null);
        if (needsUpdate) {
            // Set the input display type in order to easily debug from the object editor.
            DBListClass justAddedProperty = (DBListClass) bclass.get(name);
            justAddedProperty.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
        }

        return needsUpdate;
    }
}

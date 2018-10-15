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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.watchlist.internal.WatchListNotificationCache;
import org.xwiki.watchlist.internal.api.AutomaticWatchMode;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Document initializer for {@value #DOCUMENT_FULL_NAME}.
 * 
 * @version $Id$
 */
@Component
@Named(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME)
@Singleton
@Priority(10000)
public class WatchListClassDocumentInitializer extends AbstractMandatoryClassInitializer
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
     * Value for the {@value #AUTOMATICWATCH_PROPERTY} property to use the default behavior.
     */
    public static final String AUTOMATICWATCH_DEFAULT_VALUE = "default";

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
        super(DOCUMENT_REFERENCE, "XWiki WatchList Notification Rules Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        // Interval property
        Collection<String> availableIntervals = this.notificationCacheProvider.get().getIntervals();
        String values = StringUtils.join(availableIntervals, ListClass.DEFAULT_SEPARATOR);
        xclass.addStaticListField(INTERVAL_PROPERTY, "Email Notifications Interval", values);

        // Watched elements properties
        addWatchedElementField(xclass, WIKIS_PROPERTY, "Wiki List");
        addWatchedElementField(xclass, SPACES_PROPERTY, "Space List");
        xclass.addPageField(DOCUMENTS_PROPERTY, "Document List", 80, true);
        xclass.addUsersField(USERS_PROPERTY, "User List", 80);

        // Automatic watching property
        String automaticWatchValues = String.format("%s%s%s", AUTOMATICWATCH_DEFAULT_VALUE, ListClass.DEFAULT_SEPARATOR,
            StringUtils.join(AutomaticWatchMode.values(), ListClass.DEFAULT_SEPARATOR));
        xclass.addStaticListField(AUTOMATICWATCH_PROPERTY, "Automatic Watching", automaticWatchValues);
    }

    /**
     * @param xclass the class to add to
     * @param name the name of the property to add
     * @param prettyName the pretty name of the property to add
     */
    private void addWatchedElementField(BaseClass xclass, String name, String prettyName)
    {
        xclass.addDBListField(name, prettyName, 80, true, null);
        // Set the input display type in order to easily debug from the object editor.
        DBListClass justAddedProperty = (DBListClass) xclass.get(name);
        justAddedProperty.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
    }
}

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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

@Component
@Named("R160700000XWIKIXXX")
@Singleton
public class R160700000XWIKIXXXDataMigration extends AbstractHibernateDataMigration
{
    private static final String OLD_MAPPING = """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD//EN" "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
<hibernate-mapping>
  <class name="org.xwiki.notifications.filters.migration.R160700000XWIKIXXXDataMigration.OldNotificationFilterPreference" table="notification_filter_prefs">
    <id name="internalId" type="long">
      <column name="nfp_id" not-null="true" />
      <generator class="native"/>
    </id>
    <property name="owner" type="string" column="nfp_owner" length="768" index="NFP_OWNER"/>
    <property name="filterName" type="string" column="nfp_filter_name" length="768" index="NFP_FILTER_NAME" />
    <property name="enabled" type="boolean" column="nfp_enabled" />
    <property name="active" type="boolean" column="nfp_active" />
    <property name="filterType" column="nfp_filter_type">
      <type name="org.hibernate.type.EnumType">
        <param name="enumClass">org.xwiki.notifications.filters.NotificationFilterType</param>
      </type>
    </property>
    <property name="startingDate" type="timestamp" column="nfp_date" />
    <property name="allEventTypes" type="text" column="nfp_event_types" />
    <property name="user" type="string" column="nfp_user" length="768"/>
    <property name="pageOnly" type="string" column="nfp_page_only" length="768" />
    <property name="page" type="string" column="nfp_page" length="768" />
    <property name="wiki" type="string" column="nfp_wiki" length="255" />
    <property name="alertEnabled" type="boolean" column="nfp_alert" />
    <property name="emailEnabled" type="boolean" column="nfp_email" />
  </class>
</hibernate-mapping>
    """;
    private static final String SELECT_QUERY = "select nfp from "
        + "OldNotificationFilterPreference nfp order by "
        + "nfp.internalId";
    private static final int BATCH_SIZE = 1000;

    public class OldNotificationFilterPreference
    {
        private static final String LIST_SEPARATOR = ",";
        private String id;
        private long internalId;
        private String owner;
        private String filterName;
        private boolean enabled;
        private NotificationFilterType filterType;
        private Set<NotificationFormat> notificationFormats = new HashSet<>();
        private Date startingDate;
        private Set<String> eventTypes = new HashSet<>();
        private String user;
        private String pageOnly;
        private String page;
        private String wiki;

        public OldNotificationFilterPreference()
        {
        }

        /**
         * @param id the unique identifier to set
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * @return the internal id used to store the preference
         */
        public long getInternalId()
        {
            return internalId;
        }

        /**
         * @param internalId the internal id used to store the preference
         */
        public void setInternalId(long internalId)
        {
            this.internalId = internalId;
            this.id = String.format("%s%d", "NFP_", internalId);
        }

        /**
         * @return the owner of the preference
         */
        public String getOwner()
        {
            return owner;
        }

        /**
         * @param owner the owner of the preference
         */
        public void setOwner(String owner)
        {
            this.owner = owner;
        }

        /**
         * @param filterName the name of the filter concerned by the preference
         */
        public void setFilterName(String filterName)
        {
            this.filterName = filterName;
        }

        /**
         * @param enabled if the preference is enabled or not
         */
        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        /**
         * @param active if the preference is active or not
         */
        public void setActive(boolean active)
        {
        }

        /**
         * @param filterType the type of the filter described by this preference.
         */
        public void setFilterType(NotificationFilterType filterType)
        {
            this.filterType = filterType;
        }

        /**
         * @param filterFormats a set of {@link NotificationFormat} for which the filter should be applied.
         */
        public void setNotificationFormats(Set<NotificationFormat> filterFormats)
        {
            this.notificationFormats = filterFormats;
        }

        /**
         * @param startingDate the date from which the filter preference is enabled.
         */
        public void setStartingDate(Date startingDate)
        {
            this.startingDate = startingDate;
        }

        /**
         * @param eventTypes the event types concerned by the preference
         */
        public void setEventTypes(Set<String> eventTypes)
        {
            this.eventTypes = eventTypes;
        }

        /**
         * @param user the user concerned by the preference
         */
        public void setUser(String user)
        {
            this.user = user;
        }

        /**
         * @param pageOnly the page concerned by the preference
         */
        public void setPageOnly(String pageOnly)
        {
            this.pageOnly = pageOnly;
        }

        /**
         * @param page the page (and its children) concerned by the preference
         */
        public void setPage(String page)
        {
            this.page = page;
        }

        /**
         * @param wiki the wiki concerned by the preference
         */
        public void setWiki(String wiki)
        {
            this.wiki = wiki;
        }

        public String getId()
        {
            return id;
        }

        public String getFilterName()
        {
            return filterName;
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public NotificationFilterType getFilterType()
        {
            return filterType;
        }

        public Set<NotificationFormat> getNotificationFormats()
        {
            return notificationFormats;
        }

        public Date getStartingDate()
        {
            return startingDate;
        }

        public Set<String> getEventTypes()
        {
            return eventTypes;
        }

        public String getUser()
        {
            return user;
        }

        public String getPageOnly()
        {
            return pageOnly;
        }

        public String getPage()
        {
            return page;
        }

        public String getWiki()
        {
            return wiki;
        }

        /**
         * @return if the alert format is enabled (for storage use)
         */
        public boolean isAlertEnabled()
        {
            return this.notificationFormats.contains(NotificationFormat.ALERT);
        }

        /**
         * @param alertEnabled if the alert format is enabled (for storage use)
         */
        public void setAlertEnabled(boolean alertEnabled)
        {
            if (alertEnabled) {
                this.notificationFormats.add(NotificationFormat.ALERT);
            } else {
                this.notificationFormats.remove(NotificationFormat.ALERT);
            }
        }

        /**
         * @return if the email format is enabled (for storage used)
         */
        public boolean isEmailEnabled()
        {
            return this.notificationFormats.contains(NotificationFormat.EMAIL);
        }

        /**
         * @param emailEnabled if the email format is enabled (for storage used)
         */
        public void setEmailEnabled(boolean emailEnabled)
        {
            if (emailEnabled) {
                this.notificationFormats.add(NotificationFormat.EMAIL);
            } else {
                this.notificationFormats.remove(NotificationFormat.EMAIL);
            }
        }

        /**
         * To store a list in hibernate without the need to create a new table, we create this accessor that simply
         * join the values together, separated by commas.
         *
         * @return a unique string containing all event types, separated by commas
         */
        public String getAllEventTypes()
        {
            if (eventTypes.isEmpty()) {
                return "";
            }
            // We add a separator (",") at the beginning and at the end so we can make query like
            // "event.eventTypes LIKE '%,someEventType,%'" without of matching an other event type
            return LIST_SEPARATOR + StringUtils.join(eventTypes, LIST_SEPARATOR) + LIST_SEPARATOR;
        }

        /**
         * Allow to load a list stored in hibernate as a commas-separated list of values.
         *
         * @param eventTypes unique string containing all event types, separated by commas
         */
        public void setAllEventTypes(String eventTypes)
        {
            this.eventTypes.clear();

            if (eventTypes != null) {
                String[] types = eventTypes.split(LIST_SEPARATOR);
                for (int i = 0; i < types.length; ++i) {
                    if (StringUtils.isNotBlank(types[i])) {
                        this.eventTypes.add(types[i]);
                    }
                }
            }
        }
    }

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Override
    public String getDescription()
    {
        return "Migrate all notification filter preferences to a new table with a new schema.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160700000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String wikiId = getXWikiContext().getWikiId();
        MetadataSources builder = new MetadataSources();

        builder.addInputStream(
            new ByteArrayInputStream(OLD_MAPPING.getBytes(StandardCharsets.UTF_8)));

        MetadataBuilder metadataBuilder = builder.getMetadataBuilder();


        if (wikiId != null) {
            this.hibernateStore.setWiki(metadataBuilder, wikiId);
        }

        this.hibernateStore.updateDatabase(metadataBuilder.build());

        int offset = 0;
        try {
            List<OldNotificationFilterPreference> filterPreferenceList = this.queryManager
                .createQuery(SELECT_QUERY, Query.HQL)
                .setLimit(BATCH_SIZE)
                .setOffset(offset)
                .execute();
            for (OldNotificationFilterPreference oldNotificationFilterPreference : filterPreferenceList) {
                this.logger.info("Found filter pref [{}]", oldNotificationFilterPreference.getId());
            }
        } catch (QueryException e) {
            throw new RuntimeException(e);
        }
    }
}

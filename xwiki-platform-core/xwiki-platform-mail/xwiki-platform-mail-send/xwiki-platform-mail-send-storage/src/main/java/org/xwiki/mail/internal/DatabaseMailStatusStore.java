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
package org.xwiki.mail.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Stores mail results in the database using Hibernate.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("database")
@Singleton
public class DatabaseMailStatusStore implements MailStatusStore
{
    private static final String ID_PARAMETER_NAME = "id";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("hibernate")
    private XWikiStoreInterface hibernateStore;

    @Override
    public void save(final MailStatus status, final Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        try {
            // Delete any previous state of the message
            delete(status.getMessageId(), parameters);

            store.executeWrite(xwikiContext, session -> {
                session.save(status);
                return null;
            });

            // Log the save for debugging purpose
            this.logger.debug("Saved mail status [{}]", status);
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to save mail status [%s] to the database.", status), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public MailStatus load(String uniqueMessageId) throws MailStoreException
    {
        List<MailStatus> statuses =
            load(Collections.singletonMap(ID_PARAMETER_NAME, uniqueMessageId), 0, 0, null, false);
        if (statuses.isEmpty()) {
            return null;
        }
        return statuses.get(0);
    }

    @Override
    public List<MailStatus> load(final Map<String, Object> filterMap, final int offset, final int count,
        String sortField, boolean sortAscending) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Load from the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Compute the Query string based on the passed filter map
        final String queryString = computeSelectQueryString(filterMap, sortField, sortAscending);

        // Log query and parameters
        logQuery(queryString, filterMap);

        try {
            List<MailStatus> mailStatuses =
                store.executeRead(xwikiContext, session -> {
                    Query<MailStatus> query = session.createQuery(queryString, MailStatus.class);
                    if (offset > 0) {
                        query.setFirstResult(offset);
                    }
                    if (count > 0) {
                        query.setMaxResults(count);
                    }
                    query.setProperties(filterMap);

                    return query.list();
                });

            // Log loaded statuses
            if (this.logger.isDebugEnabled()) {
                for (MailStatus mailStatus : mailStatuses) {
                    this.logger.debug("Loaded mail status [{}]", mailStatus);
                }
            }

            return mailStatuses;

        } catch (Exception e) {
            throw new MailStoreException(
                String.format("Failed to load mail statuses matching the filter [%s] from the database.", filterMap),
                e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public long count(final Map<String, Object> filterMap) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Count in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Compute the Query string based on the passed filter map
        final String queryString = computeCountQueryString(filterMap);

        try {
            return store.executeRead(xwikiContext, session -> {
                Query<Long> query = session.createQuery(queryString, Long.class);
                query.setProperties(filterMap);
                return query.uniqueResult();
            });
        } catch (Exception e) {
            throw new MailStoreException(
                String.format("Failed to count mail statuses matching the filter [%s] from the database.", filterMap),
                e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public void delete(final String uniqueMessageId, Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        XWikiContext xwikiContext = this.contextProvider.get();
        // Delete from the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        try {
            store.executeWrite(xwikiContext, session -> {
                // Delete the message
                String queryString = String.format("delete from %s where mail_id=:id", MailStatus.class.getName());
                session.createQuery(queryString).setParameter(ID_PARAMETER_NAME, uniqueMessageId).executeUpdate();
                return null;
            });
        } catch (Exception e) {
            throw new MailStoreException(String
                .format("Failed to delete mail status (message id [%s]) " + "from the database.", uniqueMessageId), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    protected String computeQueryString(String prefix, Map<String, Object> filterMap, String sortField,
        boolean sortAscending)
    {
        StringBuilder queryBuilder = new StringBuilder(prefix);
        if (!filterMap.isEmpty()) {
            queryBuilder.append(" where");
            Iterator<String> iterator = filterMap.keySet().iterator();
            while (iterator.hasNext()) {
                String filterKey = iterator.next();
                queryBuilder.append(" mail_").append(filterKey).append(" like ").append(':').append(filterKey);
                if (iterator.hasNext()) {
                    queryBuilder.append(" and");
                }
            }
        }
        if (sortField != null) {
            queryBuilder.append(" order by ");
            queryBuilder.append(sortField);
            if (!sortAscending) {
                queryBuilder.append(" desc");
            }
        }
        return queryBuilder.toString();
    }

    protected String computeCountQueryString(Map<String, Object> filterMap)
    {
        return computeQueryString(String.format("select count(*) from %s", MailStatus.class.getName()), filterMap, null,
            false);
    }

    protected String computeSelectQueryString(Map<String, Object> filterMap, String sortField, boolean sortAscending)
    {
        return computeQueryString(String.format("from %s", MailStatus.class.getName()), filterMap, sortField,
            sortAscending);
    }

    private void logQuery(String queryString, Map<String, Object> filterMap)
    {
        if (this.logger.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            Iterator<Map.Entry<String, Object>> entryIterator = filterMap.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Object> entry = entryIterator.next();
                addEntryTolog(builder, entry);
                if (entryIterator.hasNext()) {
                    builder.append(',').append((' '));
                }
            }
            this.logger.debug("Find mail statuses for query [{}] and parameters [{}]", queryString, builder.toString());
        }
    }

    private void addEntryTolog(StringBuilder builder, Map.Entry<String, Object> entry)
    {
        addValueTolog(builder, entry.getKey());
        builder.append(" = ");
        addValueTolog(builder, entry.getValue());
    }

    private void addValueTolog(StringBuilder builder, Object value)
    {
        builder.append('[').append(value).append(']');
    }
}

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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

            store.executeWrite(xwikiContext, new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    session.save(status);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to save mail status [%s] to the database.", status), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public List<MailStatus> load(final Map<String, Object> filterMap, final int offset, final int count)
        throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Load from the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Compute the Query string based on the passed filter map
        final String queryString = computeSelectQueryString(filterMap);

        try {
            return store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
                {
                    @Override
                    public List<MailStatus> doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        if (offset > 0) {
                            query.setFirstResult(offset);
                        }
                        if (count > 0) {
                            query.setMaxResults(count);
                        }
                        query.setProperties(filterMap);
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to load mail statuses matching the filter [%s] from the database.", filterMap), e);
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
            Long count = store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<Long>()
                {
                    @Override
                    public Long doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        query.setProperties(filterMap);
                        return (Long) query.uniqueResult();
                    }
                });
            return count;
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to count mail statuses matching the filter [%s] from the database.", filterMap), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public void delete(final String messageId, Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        XWikiContext xwikiContext = this.contextProvider.get();
        // Delete from the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        try {
            store.executeWrite(xwikiContext, new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    // Delete the message
                    String queryString = String.format("delete from %s where mail_id=:id", MailStatus.class.getName());
                    session.createQuery(queryString).setParameter(ID_PARAMETER_NAME, messageId).executeUpdate();
                    return null;
                }
            });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to delete mail status (message id [%s]) "
                + "from the database.", messageId), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    protected String computeQueryString(String prefix, Map<String, Object> filterMap)
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
        return queryBuilder.toString();
    }

    protected String computeCountQueryString(Map<String, Object> filterMap)
    {
        return computeQueryString(String.format("select count(*) from %s", MailStatus.class.getName()), filterMap);
    }

    protected String computeSelectQueryString(Map<String, Object> filterMap)
    {
        return computeQueryString(String.format("from %s", MailStatus.class.getName()), filterMap);
    }
}

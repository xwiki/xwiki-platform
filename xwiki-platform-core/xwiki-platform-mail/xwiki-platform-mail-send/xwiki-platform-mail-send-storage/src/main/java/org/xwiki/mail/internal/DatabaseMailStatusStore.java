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
import org.xwiki.mail.MailState;
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
    private static final String BATCHID_PARAMETER_NAME = "batchid";

    private static final String ID_PARAMETER_NAME = "id";

    private static final String WIKI_PARAMETER_NAME = "wiki";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("hibernate")
    private XWikiStoreInterface hibernateStore;

    @Override
    public void save(final MailStatus status, Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        try {
            store.executeWrite(xwikiContext, new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    // Delete previous state of the message
                    String queryString = String.format("delete from %s where mail_id=:id", MailStatus.class.getName());
                    session.createQuery(queryString)
                        .setParameter(ID_PARAMETER_NAME, status.getMessageId()).executeUpdate();
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
    public MailStatus loadFromMessageId(final String messageId, final Map<String, Object> parameters)
        throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Only display the message statuses for the current wiki except if we're on the main wiki.
        final String queryString;
        if (xwikiContext.isMainWiki()) {
            queryString = String.format("from %s where mail_id=:id", MailStatus.class.getName());
        } else {
            queryString = String.format("from %s where mail_id=:id and mail_wiki=:wiki",
                MailStatus.class.getName());
        }

        try {
            return store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<MailStatus>()
                {
                    @Override
                    public MailStatus doInHibernate(Session session) throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        query.setParameter(ID_PARAMETER_NAME, messageId);
                        if (!xwikiContext.isMainWiki()) {
                            query.setParameter(WIKI_PARAMETER_NAME, xwikiContext.getWikiId());
                        }
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        if (!queryResult.isEmpty()) {
                            return queryResult.get(0);
                        }
                        return null;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail status for message [%s] from the database.",
                messageId), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public List<MailStatus> loadFromBatchId(final String batchId, final MailState state,
        final Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Only display the message statuses for the current wiki except if we're on the main wiki.
        final String queryString;
        if (xwikiContext.isMainWiki()) {
            queryString = String.format("from %s where mail_batchid=:batchid and mail_state=:state",
                MailStatus.class.getName());
        } else {
            queryString = String.format("from %s where mail_batchid=:batchid and mail_state=:state "
                + "and mail_wiki=:wiki", MailStatus.class.getName());
        }

        try {
            return store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
                {
                    @Override
                    public List<MailStatus> doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId).setParameter("state", state.toString());
                        if (!xwikiContext.isMainWiki()) {
                            query.setParameter(WIKI_PARAMETER_NAME, xwikiContext.getWikiId());
                        }
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail statuses "
                + "(for batch id [%s] and state [%s]) from the database.", batchId, state), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public List<MailStatus> loadFromBatchId(final String batchId, final Map<String, Object> parameters)
        throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Only display the message statuses for the current wiki except if we're on the main wiki.
        final String queryString;
        if (xwikiContext.isMainWiki()) {
            queryString = String.format("from %s where mail_batchid=:batchid",
                MailStatus.class.getName());
        } else {
            queryString = String.format("from %s where mail_batchid=:batchid and mail_wiki=:wiki",
                MailStatus.class.getName());
        }

        try {
            return store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
                {
                    @Override
                    public List<MailStatus> doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId);
                        if (!xwikiContext.isMainWiki()) {
                            query.setParameter(WIKI_PARAMETER_NAME, xwikiContext.getWikiId());
                        }
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail statuses from the database "
                + "for batch id [%s].", batchId), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }

    @Override
    public long count(final String batchId, final Map<String, Object> parameters) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

        final XWikiContext xwikiContext = this.contextProvider.get();
        // Save in the main wiki
        String currentWiki = xwikiContext.getWikiId();
        xwikiContext.setWikiId(xwikiContext.getMainXWiki());

        // Only count the message statuses for the current wiki except if we're on the main wiki.
        final String queryString;
        if (xwikiContext.isMainWiki()) {
            queryString = String.format("select count(*) from %s where mail_batchid=:batchid",
                MailStatus.class.getName());
        } else {
            queryString = String.format("select count(*) from %s where mail_batchid=:batchid and "
                + "mail_wiki=:wiki", MailStatus.class.getName());
        }

        try {
            Long count = store.executeRead(xwikiContext,
                new XWikiHibernateBaseStore.HibernateCallback<Long>()
                {
                    @Override
                    public Long doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(queryString);
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId);
                        if (!xwikiContext.isMainWiki()) {
                            query.setParameter(WIKI_PARAMETER_NAME, xwikiContext.getWikiId());
                        }
                        return (Long) query.uniqueResult();
                    }
                });
            return count;
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to get count of mail statuses with batch id "
                + "[%s] from the database .", batchId), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }
}

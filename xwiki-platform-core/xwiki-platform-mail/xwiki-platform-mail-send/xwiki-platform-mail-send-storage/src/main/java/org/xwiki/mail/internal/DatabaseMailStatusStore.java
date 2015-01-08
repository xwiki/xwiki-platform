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
    private static final String FROM_QUERY = "from " + MailStatus.class.getName();

    private static final String BATCHID_PARAMETER_NAME = "batchid";

    private static final String WHERE_QUERY_BATCH_ID = " where mail_batchid=:batchid";

    private static final String WHERE_QUERY_MAIL_ID = " where mail_id=:id";

    private static final String ID_PARAMETER_NAME = "id";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("hibernate")
    private XWikiStoreInterface hibernateStore;

    @Override
    public void save(final MailStatus status) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            store.executeWrite(this.contextProvider.get(), new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    //Delete previous state of the message
                    session.createQuery("delete " + FROM_QUERY + WHERE_QUERY_MAIL_ID)
                        .setParameter(ID_PARAMETER_NAME, status.getMessageId()).executeUpdate();
                    session.save(status);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to save mail status [%s] to the database.", status), e);
        }
    }

    @Override
    public MailStatus loadFromMessageId(final String messageId) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            return store.executeRead(this.contextProvider.get(),
                new XWikiHibernateBaseStore.HibernateCallback<MailStatus>()
                {
                    @Override public MailStatus doInHibernate(Session session) throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(FROM_QUERY + WHERE_QUERY_MAIL_ID);
                        query.setParameter(ID_PARAMETER_NAME, messageId);
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
        }
    }

    @Override
    public List<MailStatus> loadFromBatchId(final String batchId, final MailState state) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            return store.executeRead(this.contextProvider.get(),
                new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
                {
                    @Override public List<MailStatus> doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query =
                            session.createQuery(FROM_QUERY + " where mail_batchid=:batchid and mail_state=:state");
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId).setParameter("state", state.toString());
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail statuses "
                + "(for batch id [%s] and state [%s]) from the database.", batchId, state), e);
        }
    }

    @Override
    public List<MailStatus> loadFromBatchId(final String batchId) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            return store.executeRead(this.contextProvider.get(),
                new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
                {
                    @Override public List<MailStatus> doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query =
                            session.createQuery(FROM_QUERY + WHERE_QUERY_BATCH_ID);
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId);
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail statuses from the database "
                + "for batch id [%s].", batchId), e);
        }
    }

    @Override
    public long count(final String batchId) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            Long count = store.executeRead(this.contextProvider.get(),
                new XWikiHibernateBaseStore.HibernateCallback<Long>()
                {
                    @Override public Long doInHibernate(Session session)
                        throws HibernateException, XWikiException
                    {
                        Query query =
                            session.createQuery("select count(*) " + FROM_QUERY + WHERE_QUERY_BATCH_ID);
                        query.setParameter(BATCHID_PARAMETER_NAME, batchId);
                        return (Long) query.uniqueResult();
                    }
                });
            return count;
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to get count of mail statuses with batch id "
                + "[%s] from the database .", batchId), e);
        }
    }
}

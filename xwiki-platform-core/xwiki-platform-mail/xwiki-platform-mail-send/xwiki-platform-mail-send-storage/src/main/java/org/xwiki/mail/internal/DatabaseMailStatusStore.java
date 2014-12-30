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
                    session.save(status);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to save mail status [%s] to the database.", status), e);
        }
    }

    @Override
    public MailStatus load(final String messageID) throws MailStoreException
    {
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
        try {
            return store.executeRead(this.contextProvider.get(),
                new XWikiHibernateBaseStore.HibernateCallback<MailStatus>()
                {
                    @Override public MailStatus doInHibernate(Session session) throws HibernateException, XWikiException
                    {
                        Query query = session.createQuery(FROM_QUERY + " where mail_id=:id");
                        query.setParameter("id", messageID);
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        if (!queryResult.isEmpty()) {
                            return queryResult.get(0);
                        }
                        return null;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail status for message [%s] from the database.",
                messageID), e);
        }
    }

    @Override
    public List<MailStatus> load(final String batchID, final MailState state) throws MailStoreException
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
                            session.createQuery(FROM_QUERY + " where mail_batchid=:batchid an mail_status=:state");
                        query.setParameter("batchid", batchID).setParameter("state", state);
                        List<MailStatus> queryResult = (List<MailStatus>) query.list();
                        return queryResult;
                    }
                });
        } catch (Exception e) {
            throw new MailStoreException(String.format("Failed to load mail statuses "
                + "(for batch id [%s] and state [%s]) from the database.", batchID, state), e);
        }
    }
}

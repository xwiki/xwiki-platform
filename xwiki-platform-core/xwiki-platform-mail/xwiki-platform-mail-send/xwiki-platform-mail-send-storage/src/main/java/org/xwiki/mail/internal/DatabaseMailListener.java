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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.AbstractMailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStore;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Singleton
@Named("database")
public class DatabaseMailListener extends AbstractMailListener
{
    /**
     * Used to get the XWiki Context.
     */
    @Inject
    private Execution execution;

    @Inject
    @Named("hibernate")
    private XWikiStoreInterface hibernateStore;

    /**
     * Log in case of problem.
     */
    @Inject
    private Logger logger;

    @Inject
    private MailStore mailStore;

    private XWikiContext context = getXWikiContext();

    private XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;

    private String batchID;

    private String fromQuery = "from " + MailStatus.class.getName();

    @Override
    public void onPrepare(MimeMessage message)
    {
        this.batchID = getMessageBatchID(message);
        final MailStatus result = new MailStatus(getMessageID(message));
        result.setBatchID(getMessageBatchID(message));
        result.setStatus(MailState.READY);
        saveResult(result);
    }

    @Override
    public void onSuccess(MimeMessage message)
    {
        String messageID = getMessageID(message);
        MailStatus result = getMailResult(messageID);
        if (result != null) {
            result.setStatus(MailState.SENT);
            saveResult(result);
        }
    }

    @Override
    public void onError(MimeMessage message, Exception e)
    {
        String messageID = getMessageID(message);
        MailStatus result = getMailResult(messageID);
        if (result != null) {
            mailStore.save(message);
            result.setReference(messageID);
            result.setStatus(MailState.FAILED);
            result.setException(ExceptionUtils.getMessage(e));
            saveResult(result);
        }
    }

    @Override
    public Iterator<MailStatus> getErrors()
    {
        return getMailResults(this.batchID, MailState.FAILED).iterator();
    }

    @Override
    public int getErrorsNumber()
    {
        return getMailResults(this.batchID, MailState.FAILED).size();
    }

    private String getMessageID(MimeMessage message)
    {
        String messageID = null;
        try {
            messageID = message.getMessageID();
        } catch (MessagingException e) {
            this.logger.warn("Failed to retrieve Message ID from message. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return messageID;
    }

    private String getMessageBatchID(MimeMessage message)
    {
        String xbatchID = null;
        try {
            xbatchID = message.getHeader("X-BatchID")[0];
        } catch (MessagingException e) {
            this.logger.warn("Failed to retrieve Batch ID from message. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return xbatchID;
    }

    private MailStatus getMailResult(final String messageID)
    {
        return this.store
            .failSafeExecuteRead(this.context, new XWikiHibernateBaseStore.HibernateCallback<MailStatus>()
            {
                @Override public MailStatus doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    Query query = session.createQuery(fromQuery + " where mail_id=:id");
                    query.setParameter("id", messageID);
                    List<MailStatus> queryResult = (List<MailStatus>) query.list();
                    if (!queryResult.isEmpty()) {
                        return queryResult.get(0);
                    }
                    return null;
                }
            });
    }

    private List<MailStatus> getMailResults(final String batchID, final MailState state)
    {
        return this.store
            .failSafeExecuteRead(this.context, new XWikiHibernateBaseStore.HibernateCallback<List<MailStatus>>()
            {
                @Override public List<MailStatus> doInHibernate(Session session)
                    throws HibernateException, XWikiException
                {
                    Query query =
                        session.createQuery(fromQuery + " where mail_batchid=:batchid an mail_status=:state");
                    query.setParameter("batchid", batchID).setParameter("state", state);
                    List<MailStatus> queryResult = (List<MailStatus>) query.list();
                    return queryResult;
                }
            });
    }

    private void saveResult(final MailStatus result)
    {
        try {
            this.store.executeWrite(this.context, new XWikiHibernateBaseStore.HibernateCallback<Object>()
            {
                @Override public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    session.save(result);
                    return null;
                }
            });
        } catch (XWikiException e) {
            this.logger.warn("Failed to save status id to database. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * @return XWikiContext
     */
    private XWikiContext getXWikiContext()
    {
        ExecutionContext executionContext = this.execution.getContext();
        return (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}

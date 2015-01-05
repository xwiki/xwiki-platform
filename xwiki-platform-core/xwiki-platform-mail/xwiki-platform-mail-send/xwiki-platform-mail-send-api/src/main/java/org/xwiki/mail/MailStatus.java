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
package org.xwiki.mail;

import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Contains information about a mail (when it was sent, its status, etc).
 *
 * @version $Id$
 * @since 6.4M3
 */
public class MailStatus
{
    /**
     * @see #getMessageId()
     */
    private String messageId;

    /*
     * @see #getState()
     */
    private String state;

    /*
     * @see #getBatchID()
     */
    private String batchId;

    /*
     * @see #getDate()
     */
    private Date date;

    /*
     * @see #getRecipients()
     */
    private String recipients;

    /*
     * @see #getType()
     */
    private String type;

    /*
     * @see #getError()
     */
    private String error;

    /**
     * Default constructor to avoid org.Hibernate.InstantiationException.
     */
    public MailStatus()
    {
        //
    }

    /**
     * Constructor initializing the MailStatus with the MimeMessage ID.
     *
     * @param messageId the id of Message
     */
    public MailStatus(String messageId)
    {
        this.messageId = messageId;
        setDate(new Date());
    }

    /**
     * Constructor initializing the MailStatus with the MimeMessage ID and root cause message of exception.
     *
     * @param messageId see {@link #getMessageId()}
     * @param error see {@link #getError()}
     */
    public MailStatus(String messageId, String error)
    {
        this.messageId = messageId;
        setError(error);
        setDate(new Date());
    }

    /**
     * Constructor initializing the MailStatus with the MimeMessage ID the exception.
     *
     * @param messageId the id of Message
     * @param exception the exception that was encountered during sending mail
     */
    public MailStatus(String messageId, Exception exception)
    {
        this.messageId = messageId;
        setError(exception);
        setDate(new Date());
    }

    /**
     * @param message the message for which to construct a status
     * @param error the error encountered when sending the mail (if it failed), can be null
     * @throws MessagingException when the passed message has invalid recipients
     */
    public MailStatus(MimeMessage message, String error) throws MessagingException
    {
        this(message.getHeader("X-MailID", null), error);
        setRecipients(InternetAddress.toString(message.getAllRecipients()));
    }

    /**
     * @return the MimeMessage ID
     */
    public String getMessageId()
    {
        return this.messageId;
    }

    /**
     * @param messageId see {@link #getMessageId()}
     */
    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    /**
     * @return the state of the mail: ready to be sent, sent successfully, failed to be sent
     * @see MailState
     */
    public String getState()
    {
        return this.state;
    }

    /**
     * @param state see {@link #getState()}
     */
    public void setState(MailState state)
    {
        this.state = state.toString();
    }

    /**
     * @param state see {@link #getState()}
     */
    public void setState(String state)
    {
        this.state = state;
    }


    /**
     * @return the ID of the batch mail sender
     */
    public String getBatchId()
    {
        return this.batchId;
    }

    /**
     * @param batchId the ID of the batch mail sender
     */
    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    /**
     * @return the date of status of mail
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param date of status of mail
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * @return the comma-separated list of email addresses to which the mail was addressed to
     */
    public String getRecipients()
    {
        return this.recipients;
    }

    /**
     * @param recipients see {@link #getRecipients()}
     */
    public void setRecipients(String recipients)
    {
        this.recipients = recipients;
    }

    /**
     * @return the type of batch mail (Watchlist, news ...)
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type of batch mail (Watchlist, news ...)
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return the error message when the mail has failed to be sent
     */
    public String getError()
    {
        return this.error;
    }

    /**
     * @param error see {@link #getError()}
     */
    public void setError(String error)
    {
        this.error = error;
    }

    /**
     * @param exception the exception that was encountered during sending mail
     */
    public void setError(Exception exception)
    {
        this.error = ExceptionUtils.getRootCauseMessage(exception);
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("messageId", getMessageId());
        builder.append("batchId", getBatchId());
        builder.append("state", getState());
        builder.append("date", getDate());
        builder.append("recipients", getRecipients());
        builder.append("type", getType());
        builder.append("error", getError());
        return builder.toString();
    }
}

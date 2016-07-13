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
    private String uniqueMessageId;

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
     * @see #getErrorSummary()
     */
    private String errorSummary;

    /*
     * @see #getErrorDescription()
     */
    private String errorDescription;

    /*
     * @see #getWiki()
     */
    private String wiki;

    /**
     * Default constructor used by Hibernate to load an instance from the Database.
     */
    public MailStatus()
    {
        // Empty voluntarily (used by Hibernate)
    }

    /**
     * Constructor initializing the MailStatus with mandatory fields (message id, batch id and recipients are extracted
     * from the passed message, the date is set as now and the state is passed).
     * Also sets the Type if set in the passed message.
     *
     * @param batchId the identifier of the batch sending the message
     * @param message the message for which to construct a status
     * @param state the state of the referenced mail (ready, failed to send, success)
     * @since 7.4.1
     */
    public MailStatus(String batchId, ExtendedMimeMessage message, MailState state)
    {
        try {
            setMessageId(message.getUniqueMessageId());
            setBatchId(batchId);
            setType(message.getType());
            setRecipients(InternetAddress.toString(message.getAllRecipients()));
            setState(state);
            setDate(new Date());
        } catch (MessagingException e) {
            // This should never happen since the implementation for getHeader() never throws an exception (even
            // though the interface specifies it can) and similarly getAllRecipients() will also never throw an
            // exception since the only reason would be if an address is malformed but there's a check when setting
            // it already in the MimeMessage and thus in practice it cannot happen.
            throw new RuntimeException(String.format(
                "Unexpected exception constructing the Mail Status for state [%s]", state), e);
        }
    }

    /**
     * @return the unique message ID used for identifying the mime message matching this status. Between XWiki 7.1rc1
     * and 7.4, this identifier is equivalent to the message-id header, but you should not rely on this fact. Since
     * XWiki 7.4.1, this value is equivalent to {@link ExtendedMimeMessage#getUniqueMessageId()}.
     */
    public String getMessageId()
    {
        return this.uniqueMessageId;
    }

    /**
     * @param messageId see {@link #getMessageId()}
     */
    public void setMessageId(String messageId)
    {
        this.uniqueMessageId = messageId;
    }

    /**
     * Note: the returned value is a String and not {@link MailState} to allow Hibernate to save that property to the
     * database.
     *
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
     * Used by Hibernate to load that property from the database.
     *
     * @param state see {@link #getState()}
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the ID of the batch mail sender
     */
    public String getBatchId()
    {
        return this.batchId;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param batchId the ID of the batch mail sender
     */
    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the date of status of mail
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param date of status of mail
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Used by Hibernate to save that property to the database (hence the String).
     *
     * @return the comma-separated list of email addresses to which the mail was addressed to
     */
    public String getRecipients()
    {
        return this.recipients;
    }

    /**
     * Used by Hibernate to load that property from the database (hence the reason why it's a String).
     *
     * @param recipients see {@link #getRecipients()}
     */
    public void setRecipients(String recipients)
    {
        this.recipients = recipients;
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the type of batch mail (Watchlist, news ...)
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param type of batch mail (Watchlist, news ...)
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the error message summary when the mail has failed to be sent
     */
    public String getErrorSummary()
    {
        return this.errorSummary;
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the error message description (the full stack trace for example) when the mail has failed to be sent
     */
    public String getErrorDescription()
    {
        return this.errorDescription;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param errorSummary see {@link #getErrorSummary()}
     */
    public void setErrorSummary(String errorSummary)
    {
        this.errorSummary = errorSummary;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param errorDescription see {@link #getErrorDescription()}
     */
    public void setErrorDescription(String errorDescription)
    {
        this.errorDescription = errorDescription;
    }

    /**
     * @param exception the exception that was encountered during sending mail
     */
    public void setError(Exception exception)
    {
        this.errorSummary = ExceptionUtils.getRootCauseMessage(exception);
        this.errorDescription  = ExceptionUtils.getStackTrace(exception);
    }

    /**
     * Used by Hibernate to save that property to the database.
     *
     * @return the wiki in which the message is trying to be sent (can be null, in which case it means the main wiki)
     */
    public String getWiki()
    {
        return this.wiki;
    }

    /**
     * Used by Hibernate to load that property from the database.
     *
     * @param wiki see {@link #getWiki()}
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
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
        if (getType() != null) {
            builder.append("type", getType());
        }
        if (getErrorSummary() != null) {
            builder.append("errorSummary", getErrorSummary());
        }
        if (getErrorDescription() != null) {
            builder.append("errorDescription", getErrorDescription());
        }
        if (getWiki() != null) {
            builder.append("wiki", getWiki());
        }
        return builder.toString();
    }
}

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

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Contains information about mail to sent.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class MailStatus
{
    /**
     * ID.
     */
    private String messageID;

    /*
     * Mail Status.
     */
    private String status;

    /*
     * Mail Batch ID.
     */
    private String batchID;

    /*
     * Mail Date.
     */
    private Date date;

    /*
     * Mail To email address.
     */
    private String recipient;

    /*
     * Mail Type.
     */
    private String type;

    /*
     * Mail Reference to emails stored in the permanent directory.
     */
    private String reference;

    /*
     * Mail Exception
     */
    private String exception;



    /**
     * Constructor initializing the MailStatus with the MimeMessage ID.
     *
     * @param messageID the id of Message
     */
    public MailStatus(String messageID)
    {
        this.messageID = messageID;
        setDate(new Date());
    }

    /**
     * Constructor initializing the MailStatus with the MimeMessage ID and root cause message of exception.
     *
     * @param messageID the id of Message
     * @param exception the root cause message of exception that was encountered during sending mail
     */
    public MailStatus(String messageID, String exception)
    {
        this.messageID = messageID;
        setException(exception);
        setDate(new Date());
    }

    /**
     * Constructor initializing the MailStatus with the MimeMessage ID the exception.
     *
     * @param messageID the id of Message
     * @param exception the exception that was encountered during sending mail
     */
    public MailStatus(String messageID, Exception exception)
    {
        this.messageID = messageID;
        setException(exception);
        setDate(new Date());
    }

    /**
     * @return the MimeMessage ID
     */
    public String getMessageID()
    {
        return this.messageID;
    }

    /**
     * @return the status of the mail
     */
    public String getStatus()
    {
        return this.status;
    }

    /**
     * @param status of the mail
     */
    public void setStatus(MailState status)
    {
        this.status = status.toString();
    }

    /**
     * @return the ID of the batch mail sender
     */
    public String getBatchID()
    {
        return this.batchID;
    }

    /**
     * @param batchID the ID of the batch mail sender
     */
    public void setBatchID(String batchID)
    {
        this.batchID = batchID;
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
     * @return the mail To email address
     */
    public String getRecipient()
    {
        return this.recipient;
    }

    /**
     * @param recipient the mail To email address
     */
    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
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
     * @return the reference of serialized MimeMessage in permanent directory
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @param reference of serialized MimeMessage in permanent directory
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the root cause message of exception
     */
    public String getException()
    {
        return this.exception;
    }

    /**
     * @param exception the root cause message of exception that was encountered during sending mail
     */
    public void setException(String exception)
    {
        this.exception = exception;
    }

    /**
     * @param exception the exception that was encountered during sending mail
     */
    public void setException(Exception exception)
    {
        this.exception = ExceptionUtils.getRootCauseMessage(exception);
    }
}

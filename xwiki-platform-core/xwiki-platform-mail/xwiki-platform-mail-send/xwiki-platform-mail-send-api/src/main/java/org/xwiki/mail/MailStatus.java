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

import javax.mail.internet.MimeMessage;

/**
 * Contains information about mail to sent.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class MailStatus
{
    private MimeMessage message;

    private Exception exception;

    private Date date;

    /**
     *
     * @param message to send
     * @param exception when an error occurs when sending the mail
     */
    public MailStatus(MimeMessage message, Exception exception)
    {
        this.message = message;
        this.exception = exception;
        this.date = new Date();
    }

    /**
     * @return the exception when an error occurs when sending the mail
     */
    public Exception getException()
    {
        return exception;
    }

    /**
     * @return the message to send
     */
    public MimeMessage getMessage()
    {
        return message;
    }

    /**
     * @return the date of status
     */
    public Date getDate() {
        return this.date;
    }
}

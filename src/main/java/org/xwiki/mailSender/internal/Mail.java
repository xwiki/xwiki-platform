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

package org.xwiki.mailSender.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.api.Attachment;

/**
 * The variables to, cc, bcc can contain several email addresses, separated by commas.
 * 
 * @version $Id$
 */
public class Mail
{
    /** Sender adress. */
    private String from;

    /** Recipient adresses. */
    private String to;

    /** ReplyTo adress. */
    private String replyTo;

    /** Carbon copy recipients adresses. */
    private String cc;

    /** Hidden Carbon Copy recipients adresses. */
    private String bcc;

    /** Mail Subject. */
    private String subject;

    /** List of the mail parts. */
    private List<String[]> contents;

    /** Mail attachments. */
    private List<Attachment> attachments;

    /** Mail headers. */
    private Map<String, String> headers;
    
    /**
     * Creates a new Mail object.
     */
    public Mail()
    {
        this.headers = new TreeMap<String, String>();
        this.contents = new LinkedList<String[]>();
        this.attachments = new LinkedList<Attachment>();
    }

    /**
     * Creates a new Mail object.
     * 
     * @param from Sender
     * @param to Recipients
     * @param cc Carbon copy recipients
     * @param bcc Hidden carbon copy recipients
     * @param subject Mail subject
     */
    public Mail(String from, String to, String cc, String bcc, String subject)
    {
        this();
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
    }

    /**
     * Retrieves all the attachments of this mail.
     * 
     * @return List of the attachments
     */
    public List<Attachment> getAttachments()
    {
        return this.attachments;
    }

    /**
     * Add an attachment.
     * 
     * @param attachments List of attachments to add
     */
    public void setAttachments(List<Attachment> attachments)
    {
        this.attachments = attachments;
    }

    /**
     * Add a single attachment.
     * 
     * @param attachment File to attach
     */
    public void addFile(Attachment attachment)
    {
        this.attachments.add(attachment);
    }

    /**
     * Retrieves mail sender.
     * 
     * @return the address of the sender
     */
    public String getFrom()
    {
        return this.from;
    }

    /**
     * Set the sender.
     * 
     * @param from Address of the sender
     */
    public void setFrom(String from)
    {
        this.from = from;
    }

    /**
     * Retrieves the addresses of the recipients.
     * 
     * @return addresses of the recipients
     */
    public String getTo()
    {
        return this.to;
    }

    /**
     * Set the recipients.
     * 
     * @param to List of the recipients addresses (coma separated)
     */
    public void setTo(String to)
    {
        this.to = to;
    }

    /**
     * Retrieves the replyTo field.
     * 
     * @return the replyTo field
     */
    public String getReplyTo()
    {
        return this.replyTo;
    }

    /**
     * Set the replyTo field.
     * 
     * @param replyTo Address to reply
     */
    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    /**
     * Retrieves carbon copy recipients.
     * 
     * @return addresses of the carbon copy recipients
     */
    public String getCc()
    {
        return this.cc;
    }

    /**
     * Set carbon copy recipients.
     * 
     * @param cc List of the carbon copy recipients (coma separated addresses list)
     */
    public void setCc(String cc)
    {
        this.cc = cc;
    }

    /**
     * Retrieves hidden carbon copy recipients.
     * 
     * @return List of hidden carbon copy recipients
     */
    public String getBcc()
    {
        return this.bcc;
    }

    /**
     * Set hidden carbon copy recipients.
     * 
     * @param bcc List of the hidden carbon copy recipients (coma separated addresses list)
     */
    public void setBcc(String bcc)
    {
        this.bcc = bcc;
    }

    /**
     * Retrieves mail subject.
     * 
     * @return mail subject
     */
    public String getSubject()
    {
        return this.subject;
    }

    /**
     * Set mail subject.
     * 
     * @param subject Mail subject
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        if (getFrom() != null) {
            buffer.append("From [" + getFrom() + "]");
        }

        if (getTo() != null) {
            buffer.append(", To [" + getTo() + "]");
        }

        if (getCc() != null) {
            buffer.append(", Cc [" + getCc() + "]");
        }

        if (getBcc() != null) {
            buffer.append(", Bcc [" + getBcc() + "]");
        }

        if (getSubject() != null) {
            buffer.append(", Subject [" + getSubject() + "]");
        }

        if (getContents() != null) {
            buffer.append(", Contents [" + getContentsAsString() + "]");
        }

        if (!getHeaders().isEmpty()) {
            buffer.append(", Headers [" + toStringHeaders() + "]");
        }

        return buffer.toString();
    }

    /**
     * Retrieves the mail headers and convert them into a string.
     * 
     * @return a string representing the mail headers
     */
    private String toStringHeaders()
    {
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> header : getHeaders().entrySet()) {
            buffer.append('[');
            buffer.append(header.getKey());
            buffer.append(']');
            buffer.append(' ');
            buffer.append('=');
            buffer.append(' ');
            buffer.append('[');
            buffer.append(header.getValue());
            buffer.append(']');
        }
        return buffer.toString();
    }

    /**
     * Add a header to the mail headers.
     * 
     * @param header Key
     * @param value Value 
     */
    public void setHeader(String header, String value)
    {
        this.headers.put(header, value);
    }

    /**
     * Retrieves a specific mail header.
     * 
     * @param header Name of the header to retrieve
     * @return value of the header
     */
    public String getHeader(String header)
    {
        return this.headers.get(header);
    }

    /**
     * Set the mail headers.
     * 
     * @param headers Headers of the mail
     */
    public void setHeaders(Map<String, String> headers)
    {
        this.headers = headers;
    }
    

    /**
     * Retrieves the mail headers.
     * 
     * @return all the mail headers
     */
    public Map<String, String> getHeaders()
    {
        return this.headers;
    }

    /**
     * Retrieves the different parts of the mail.
     * 
     * @return List of the part of the mail (MIME type and content of the part)
     */
    public List<String[]> getContents()
    {
        return this.contents;
    }

    /**
     * Retrieves the mail contents as a string.
     * 
     * @return a string representing the mail contents
     */
    public String getContentsAsString()
    {
        String result = "";
        for (String[] part : this.contents) {
            result += part[0] + ":" + part[1] + " \n ";
        }
        return result;
    }

    /**
     * Add a new part in the email multipart.
     * 
     * @param contentType MimeType of the part to add
     * @param content Content of the part to add
     */
    public void addContent(String contentType, String content)
    {
        String[] newContent = {contentType, content};
        this.contents.add(newContent);
    }

    /**
     * Add a html part to the mail content.
     * 
     * @param html Content of the html part
     */
    public void addHtmlContent(String html)
    {
        String[] newContent = {"text/html", html};
        this.contents.add(newContent);
    }
    
    /**
     * Check a mail validity : A mail should have at least one recipient and one Mime Part.
     * 
     * @return true is the mail has at least a recipient and a Mime Part
     */
    public boolean isValid()
    {
        boolean hasTo = (this.getTo() != null && !StringUtils.isEmpty(this.getTo()));
        boolean hasCc = (this.getCc() != null && !StringUtils.isEmpty(this.getCc()));
        boolean hasBcc = (this.getBcc() != null && !StringUtils.isEmpty(this.getBcc()));
        boolean hasRecipient = (hasTo || hasCc || hasBcc);
        if (!hasRecipient) {
            return false;
        }
        if (this.getContents().size() == 0) {
            return false;
        }
        return true;
    }

}

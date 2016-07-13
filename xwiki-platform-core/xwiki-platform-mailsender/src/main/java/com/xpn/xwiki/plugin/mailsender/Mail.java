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
package com.xpn.xwiki.plugin.mailsender;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.xpn.xwiki.api.Attachment;

/**
 * The variables to, cc, bcc can contain several email addresses, separated by commas.
 */
public class Mail
{
    private String from;

    private String to;

    private String cc;

    private String bcc;

    private String subject;

    private String textPart;

    private String htmlPart;

    private List<Attachment> attachments;

    private Map<String, String> headers;

    public Mail()
    {
        this.headers = new TreeMap<String, String>();
    }

    public Mail(String from, String to, String cc, String bcc, String subject, String textPart, String htmlPart)
    {
        this();

        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.textPart = textPart;
        this.htmlPart = htmlPart;
    }

    public List<Attachment> getAttachments()
    {
        return this.attachments;
    }

    public void setAttachments(List<Attachment> attachments)
    {
        this.attachments = attachments;
    }

    public String getFrom()
    {
        return this.from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return this.to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }

    public String getCc()
    {
        return this.cc;
    }

    public void setCc(String cc)
    {
        this.cc = cc;
    }

    public String getBcc()
    {
        return this.bcc;
    }

    public void setBcc(String bcc)
    {
        this.bcc = bcc;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getTextPart()
    {
        return this.textPart;
    }

    public void setTextPart(String message)
    {
        this.textPart = message;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

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

        if (getTextPart() != null) {
            buffer.append(", Text [" + getTextPart() + "]");
        }

        if (getHtmlPart() != null) {
            buffer.append(", HTML [" + getHtmlPart() + "]");
        }

        if (!getHeaders().isEmpty()) {
            buffer.append(", Headers [" + toStringHeaders() + "]");
        }

        return buffer.toString();
    }

    private String toStringHeaders()
    {
        StringBuilder buffer = new StringBuilder();
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

    public String getHtmlPart()
    {
        return this.htmlPart;
    }

    public void setHtmlPart(String htmlPart)
    {
        this.htmlPart = htmlPart;
    }

    public void setHeader(String header, String value)
    {
        this.headers.put(header, value);
    }

    public String getHeader(String header)
    {
        return this.headers.get(header);
    }

    public void setHeaders(Map<String, String> headers)
    {
        this.headers = headers;
    }

    public Map<String, String> getHeaders()
    {
        return this.headers;
    }
}

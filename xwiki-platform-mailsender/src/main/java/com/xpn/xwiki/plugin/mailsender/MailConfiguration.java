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

import com.xpn.xwiki.api.XWiki;

/**
 * Represents a Mail Server configuration.
 */
public class MailConfiguration
{
    private int port;

    private String host;

    private String from;

    public MailConfiguration()
    {
        setPort(25);
        setHost("localhost");
    }

    public MailConfiguration(XWiki xwiki)
    {
        this();

        String smtpServer = xwiki.getXWikiPreference("smtp_server");
        if (smtpServer.length() > 0) {
            setHost(smtpServer);
        }

        String from = xwiki.getXWikiPreference("smtp_from");
        if (from.length() > 0) {
            setFrom(from);
        }
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getHost()
    {
        return this.host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return this.port;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getFrom()
    {
        return this.from;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        if (getHost() != null) {
            buffer.append("Host [" + getHost() + "]");
        }

        if (getFrom() != null) {
            buffer.append(", From [" + getFrom() + "]");
        }

        buffer.append(", Port [" + getPort() + "]");

        return buffer.toString();
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.api.XWiki;

/**
 * Represents a Mail Server configuration.
 * 
 * @version $Id$
 */
public class MailConfiguration
{
    private int port;

    private String host;

    private String from;

    private String smtpUsername;

    private String smtpPassword;

    private Properties extraProperties;

    public MailConfiguration()
    {
        setPort(25);
        setHost("localhost");
    }

    public MailConfiguration(XWiki xwiki)
    {
        this();

        String smtpServer = xwiki.getXWikiPreference("smtp_server");
        if (!StringUtils.isBlank(smtpServer)) {
            setHost(smtpServer);
        }

        int port = xwiki.getXWikiPreferenceAsInt("smtp_port", 25);
        setPort(port);

        String from = xwiki.getXWikiPreference("smtp_from");
        if (!StringUtils.isBlank(from)) {
            setFrom(from);
        }

        String smtpServerUsername = xwiki.getXWikiPreference("smtp_server_username");
        String smtpServerPassword = xwiki.getXWikiPreference("smtp_server_password");
        if (!StringUtils.isEmpty(smtpServerUsername) && !StringUtils.isEmpty(smtpServerPassword)) {
            setSmtpUsername(smtpServerUsername);
            setSmtpPassword(smtpServerPassword);
        }

        String javaMailExtraProps = xwiki.getXWikiPreference("javamail_extra_props");
        if (!StringUtils.isEmpty(javaMailExtraProps)) {
            setExtraProperties(javaMailExtraProps);
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

    public void setSmtpUsername(String smtpUsername)
    {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpUsername()
    {
        return this.smtpUsername;
    }

    public void setSmtpPassword(String smtpPassword)
    {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpPassword()
    {
        return this.smtpPassword;
    }

    public boolean usesAuthentication()
    {
        return !StringUtils.isEmpty(getSmtpUsername()) && !StringUtils.isEmpty(getSmtpPassword());
    }

    public void setExtraProperties(String extraPropertiesString)
    {
        if (StringUtils.isEmpty(extraPropertiesString)) {
            this.extraProperties = null;
        } else {
            InputStream is = new ByteArrayInputStream(extraPropertiesString.getBytes());
            this.extraProperties = new Properties();
            try {
                this.extraProperties.load(is);
            } catch (IOException e) {
                // Shouldn't ever occur...
                throw new RuntimeException("Error configuring mail connection.", e);
            }
        }
    }

    /**
     * Add extraProperties to an external Properties object
     * 
     * @param externalProperties
     * @param overwrite
     */
    public void appendExtraPropertiesTo(Properties externalProperties, boolean overwrite)
    {
        // sanity check
        if (externalProperties == null) {
            throw new IllegalArgumentException("externalProperties can't be null");
        }

        if (this.extraProperties != null && this.extraProperties.size() > 0) {
            for (Entry<Object, Object> e : this.extraProperties.entrySet()) {
                String propName = (String) e.getKey();
                String propValue = (String) e.getValue();
                if (overwrite || externalProperties.getProperty(propName) == null) {
                    externalProperties.setProperty(propName, propValue);
                }
            }
        }
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

        if (usesAuthentication()) {
            buffer.append(", Username [" + getSmtpUsername() + "]");
            buffer.append(", Password [*****]");
        }

        return buffer.toString();
    }
}

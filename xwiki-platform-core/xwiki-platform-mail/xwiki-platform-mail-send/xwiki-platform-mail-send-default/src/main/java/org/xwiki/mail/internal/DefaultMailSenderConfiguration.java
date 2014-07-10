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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.MailSenderConfiguration;

/**
 * Gets the Mail Sending configuration by first looking it up in document sources (space and wiki preferences) and if
 * not found in the xwiki properties file.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultMailSenderConfiguration implements MailSenderConfiguration
{
    /**
     * Java Mail SMTP property for the protocol.
     */
    public static final String JAVAMAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    /**
     * Java Mail SMTP property for the host.
     */
    public static final String JAVAMAIL_SMTP_HOST = "mail.smtp.host";

    /**
     * Java Mail SMTP property for the server port.
     */
    public static final String JAVAMAIL_SMTP_PORT = "mail.smtp.port";

    /**
     * Java Mail SMTP property for the username.
     */
    public static final String JAVAMAIL_SMTP_USERNAME = "mail.smtp.user";

    /**
     * Java Mail SMTP property for the from email address.
     */
    public static final String JAVAMAIL_FROM = "mail.smtp.from";

    /**
     * Prefix for configuration keys for the Mail Sending module.
     */
    private static final String PREFIX = "mail.sender.";

    private static final int DEFAULT_PORT = 25;

    private static final String DEFAULT_FROM_EMAIL = "no-reply@xwiki.org";

    @Inject
    private Logger logger;

    @Inject
    @Named("documents")
    private ConfigurationSource documentsSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public String getHost()
    {
        return this.documentsSource.getProperty("smtp_server",
                this.xwikiPropertiesSource.getProperty(PREFIX + "host", "localhost"));
    }

    @Override
    public int getPort()
    {
        // TODO: Improve this code by fixing the Document Configuration Source.
        int port;
        String portAsString = this.documentsSource.getProperty("smtp_port");
        if (!StringUtils.isEmpty(portAsString)) {
            try {
                port = Integer.parseInt(portAsString);
            } catch (NumberFormatException e) {
                port = DEFAULT_PORT;
            }
        } else {
            port = this.xwikiPropertiesSource.getProperty(PREFIX + "port", DEFAULT_PORT);
        }
        return port;
    }

    @Override
    public String getUsername()
    {
        return this.documentsSource.getProperty("smtp_server_username",
                this.xwikiPropertiesSource.getProperty(PREFIX + "username", String.class));
    }

    @Override
    public String getPassword()
    {
        return this.documentsSource.getProperty("smtp_server_password",
                this.xwikiPropertiesSource.getProperty(PREFIX + "password", String.class));
    }

    @Override
    public String getFromAddress()
    {
        return this.documentsSource.getProperty("admin_email",
            this.xwikiPropertiesSource.getProperty(PREFIX + "from", String.class));
    }

    @Override
    public Properties getAdditionalProperties()
    {
        Properties properties;

        // The "javamail_extra_props" property is stored in a text area and thus we need to convert it to a Map.
        String extraPropertiesAsString = this.documentsSource.getProperty("javamail_extra_props");
        if (StringUtils.isEmpty(extraPropertiesAsString)) {
            properties = this.xwikiPropertiesSource.getProperty(PREFIX + "properties", Properties.class);
        } else {
            // Convert the String to Map
            InputStream is = new ByteArrayInputStream(extraPropertiesAsString.getBytes());
            properties = new Properties();
            try {
                properties.load(is);
            } catch (Exception e) {
                // Will happen if the user has not used the right format, in which case we log a warning but discard
                // the user values.
                this.logger.warn("Error while parsing mail properties [{}]. Root cause [{}]. Ignoring configuration...",
                        extraPropertiesAsString, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return properties;
    }

    @Override
    public Properties getAllProperties()
    {
        Properties properties = new Properties();
        addProperty(properties, JAVAMAIL_TRANSPORT_PROTOCOL, "smtp");
        addProperty(properties, JAVAMAIL_SMTP_HOST, getHost());
        addProperty(properties, JAVAMAIL_SMTP_USERNAME, getUsername());
        addProperty(properties, JAVAMAIL_FROM, getFromAddress());
        addProperty(properties, JAVAMAIL_SMTP_PORT, Integer.toString(getPort()));
        properties.putAll(getAdditionalProperties());
        return properties;
    }

    private void addProperty(Properties properties, String key, String value)
    {
        if (value != null) {
            properties.setProperty(key, value);
        }
    }

    @Override
    public boolean usesAuthentication()
    {
        return !StringUtils.isEmpty(getUsername()) && !StringUtils.isEmpty(getPassword());
    }
}

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
package org.xwiki.mail.internal.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.MailSenderConfiguration;

/**
 * Gets the Mail Sending configuration. The configuration is checked in the following order:
 * <ul>
 *   <li>Look in Mail.MailConfig in the current wiki</li>
 *   <li>[Backward compatibility] Look in (current space).XWikiPreferences in the current wiki</li>
 *   <li>[Backward compatibility] Look in XWiki.XWikiPreferences in the current wiki</li>
 *   <li>Look in the xwiki properties file</li>
 * </ul>
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
    public static final String JAVAMAIL_SMTP_FROM = "mail.smtp.from";

    /**
     * Java Mail SMTP property for specifying that we are authenticating.
     */
    public static final String JAVAMAIL_SMTP_AUTH = "mail.smtp.auth";

    /**
     * Prefix for configuration keys for the Mail Sending module.
     */
    private static final String PREFIX = "mail.sender.";

    private static final int DEFAULT_PORT = 25;

    /**
     * By default we wait 8 seconds between each mail in order to throttle the mail sending and not be considered as
     * a spammer by mail servers.
     */
    private static final long DEFAULT_SEND_WAIT_TIME = 8 * 1000L;

    private static final String FROM_PROPERTY = "from";
    private static final String BCC_PROPERTY = "bcc";
    private static final String HOST_PROPERTY = "host";
    private static final String PORT_PROPERTY = "port";
    private static final String USERNAME_PROPERTY = "username";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String PROPERTIES_PROPERTY = "properties";
    private static final String SEND_WAIT_TIME = "sendWaitTime";

    @Inject
    private Logger logger;

    @Inject
    @Named("mailsend")
    private ConfigurationSource mailConfigSource;

    @Inject
    @Named("documents")
    private ConfigurationSource documentsSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public String getHost()
    {
        String host;

        // First, look in the document sources
        host = this.mailConfigSource.getProperty(HOST_PROPERTY,
            this.documentsSource.getProperty("smtp_server", String.class));

        // If not found, look in the xwiki properties source
        if (host == null) {
            host = this.xwikiPropertiesSource.getProperty(PREFIX + HOST_PROPERTY, "localhost");
        }

        return host;
    }

    @Override
    public int getPort()
    {
        Integer port;

        // First, look in the document sources
        String portAsString = this.documentsSource.getProperty("smtp_port");
        if (!StringUtils.isEmpty(portAsString)) {
            try {
                port = this.mailConfigSource.getProperty(PORT_PROPERTY, Integer.parseInt(portAsString));
            } catch (NumberFormatException e) {
                port = DEFAULT_PORT;
            }
        } else {
            port = this.mailConfigSource.getProperty(PORT_PROPERTY, Integer.class);
        }

        // If not found, look in the xwiki properties source
        if (port == null) {
            port = this.xwikiPropertiesSource.getProperty(PREFIX + PORT_PROPERTY, DEFAULT_PORT);
        }

        return port;
    }

    @Override
    public String getUsername()
    {
        String username;

        // First, look in the document sources
        username = this.mailConfigSource.getProperty(USERNAME_PROPERTY,
            this.documentsSource.getProperty("smtp_server_username", String.class));

        // If not found, look in the xwiki properties source
        if (username == null) {
            username = this.xwikiPropertiesSource.getProperty(PREFIX + USERNAME_PROPERTY, String.class);
        }

        return username;
    }

    @Override
    public String getPassword()
    {
        String password;

        // First, look in the document sources
        password = this.mailConfigSource.getProperty(PASSWORD_PROPERTY,
            this.documentsSource.getProperty("smtp_server_password", String.class));

        // If not found, look in the xwiki properties source
        if (password == null) {
            password = this.xwikiPropertiesSource.getProperty(PREFIX + PASSWORD_PROPERTY, String.class);
        }

        return password;
    }

    @Override
    public List<String> getBCCAddresses()
    {
        List<String> bccAddresses = new ArrayList<>();

        // First, look in the document source
        String bccAsString = this.mailConfigSource.getProperty(BCC_PROPERTY, String.class);

        // If not found, look in the xwiki properties source
        if (bccAsString == null) {
            bccAsString = this.xwikiPropertiesSource.getProperty(PREFIX + BCC_PROPERTY, String.class);
        }

        // Convert into a list (if property is found and not null)
        if (bccAsString != null) {
            for (String address : StringUtils.split(bccAsString, ',')) {
                bccAddresses.add(StringUtils.trim(address));
            }
        }

        return bccAddresses;
    }

    @Override
    public String getFromAddress()
    {
        String from;

        // First, look in the document sources
        from = this.mailConfigSource.getProperty(FROM_PROPERTY,
            this.documentsSource.getProperty("admin_email", String.class));

        // If not found, look in the xwiki properties source
        if (from == null) {
            from = this.xwikiPropertiesSource.getProperty(PREFIX + FROM_PROPERTY, String.class);
        }

        return from;
    }

    @Override
    public Properties getAdditionalProperties()
    {
        Properties properties;

        // First, look in the document sources
        String extraPropertiesAsString = this.mailConfigSource.getProperty(PROPERTIES_PROPERTY,
            this.documentsSource.getProperty("javamail_extra_props", String.class));

        // If not found, look in the xwiki properties source
        if (extraPropertiesAsString == null) {
            properties = this.xwikiPropertiesSource.getProperty(PREFIX + PROPERTIES_PROPERTY, Properties.class);
        } else {
            // The "javamail_extra_props" property is stored in a text area and thus we need to convert it to a Map.
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
        addProperty(properties, JAVAMAIL_SMTP_FROM, getFromAddress());
        addProperty(properties, JAVAMAIL_SMTP_PORT, Integer.toString(getPort()));

        // If a username and a password have been provided consider we're authenticating against the SMTP server
        if (usesAuthentication()) {
            properties.put(JAVAMAIL_SMTP_AUTH, "true");
        }

        // Add user-specified mail properties.
        // Note: We're only supporting SMTP (and not SMTPS) at the moment, which means that for sending emails to a
        // SMTP server requiring TLS the user will need to pass the "mail.smtp.starttls.enable=true" property and use
        // the proper port for TLS (587 for Gmail for example, while port 465 is used for SMTPS/SSL).
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

    @Override
    public String getScriptServicePermissionCheckerHint()
    {
        return this.xwikiPropertiesSource.getProperty(PREFIX + "scriptServiceCheckerHint", "programmingrights");
    }

    @Override
    public long getSendWaitTime()
    {
        Long waitTime = this.mailConfigSource.getProperty(SEND_WAIT_TIME);

        if (waitTime == null) {
            waitTime = this.xwikiPropertiesSource.getProperty(PREFIX + SEND_WAIT_TIME, DEFAULT_SEND_WAIT_TIME);
        }

        return waitTime;
    }
}

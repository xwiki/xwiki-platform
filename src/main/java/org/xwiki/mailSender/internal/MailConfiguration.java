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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;

/**
 * To set the SMTP properties for the mail sender.
 * 
 * @version $Id$
 *
 */
public class MailConfiguration
{
    /** Where to get the SMTP server configuration. */
    private static final String PREFERENCES = "XWiki.XWikiPreferences";

    /** SMTP authentification. */
    private static final String SMTP_AUTH = "mail.smtp.auth";

    /** Mail SMTP Server Username. */
    private static final String MAIL_SMTP_SERVER_USERNAME = "mail.smtp.server.username";

    /** Mail SMTP Server Password. */
    private static final String MAIL_SMTP_SERVER_PASSWORD = "mail.smtp.server.password";

    /** Localhost. */
    private static final String LOCALHOST = "localhost";
    
    /** True string. */
    private static final String TRUE = "true";

    /** False string. */
    private static final String FALSE = "false";
    
    /** Properties of the mail Configuration. */
    private Properties properties;
    
    /**
     * Fetch the SMTP configuration from the XWikiPreferences.
     * 
     * @param documentAccessBridge To access XWiki Preferences where the SMTP properties are stored.
     */
    public MailConfiguration(DocumentAccessBridge documentAccessBridge)
    {
        this.properties = new Properties();

        // Note: The full list of available properties that we can set is defined here:
        // http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html

        String host = documentAccessBridge.getProperty(PREFERENCES, "smtp_server").toString();
        String port = documentAccessBridge.getProperty(PREFERENCES, "smtp_port").toString();
        String serverUserName = documentAccessBridge.getProperty(PREFERENCES, "smtp_server_username").toString();
        String serverPassword = documentAccessBridge.getProperty(PREFERENCES, "smtp_server_password").toString();
        String extraPropertiesString = documentAccessBridge.getProperty(PREFERENCES, "javamail_extra_props").toString();
        properties.put("mail.smtp.port", Integer.parseInt(port));
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.localhost", LOCALHOST);
        properties.put("mail.host", LOCALHOST);
        properties.put("mail.debug", FALSE);
        // properties.put("mail.smtp.from", from);
        if (!StringUtils.isEmpty(serverPassword) && !StringUtils.isEmpty(serverUserName)) {
            properties.put(SMTP_AUTH, TRUE);
            properties.put(MAIL_SMTP_SERVER_USERNAME, serverUserName);
            properties.put(MAIL_SMTP_SERVER_PASSWORD, serverPassword);
        } else {
            properties.put(SMTP_AUTH, FALSE);
        }
        if (extraPropertiesString != null && !StringUtils.isEmpty(extraPropertiesString)) {
            InputStream is = new ByteArrayInputStream(extraPropertiesString.getBytes());
            Properties extraProperties = new Properties();
            try {
                extraProperties.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Error configuring mail connection.", e);
            }
            for (Entry<Object, Object> e : extraProperties.entrySet()) {
                String propName = (String) e.getKey();
                String propValue = (String) e.getValue();
                if (properties.getProperty(propName) == null) {
                    properties.setProperty(propName, propValue);
                }
            }
        }
    }
    
    /**
     * Retrieves the SMTP configuration.
     * 
     * @return the set of properties defining the SMTP configuration
     */
    public Properties getProperties()
    {
        return properties;
    }
    
}

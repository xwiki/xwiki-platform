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

import org.xwiki.mail.SessionFactory;
import org.xwiki.mail.MailSenderConfiguration;

import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Map;
import java.util.Properties;

/**
 * @version $Id$
 * @since 6.1M2
 */
public class DefaultSessionFactory implements SessionFactory {

    /** Mail protocol */
    public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    /** Mail SMTP Server host */
    public static final String MAIL_SMTP_HOST = "mail.smtp.host";

    /** Mail SMTP Server Username */
    public static final String MAIL_SMTP_USERNAME = "mail.smtp.user";

    /** Mail SMTP Server Password */
    public static final String MAIL_SMTP_PASSWORD = "mail.smtp.user";

    /** Mail SMTP Server Host */
    public static final String MAIL_FROM = "mail.from";

    /** Mail SMTP Server Port */
    public static final String MAIL_SMTP_PORT = "mail.smtp.port";

    private Properties properties;

    @Inject
    MailSenderConfiguration senderConfiguration;

    /**
     * @return the configured {@link javax.mail.Session} object
     */
    @Override
    public Session create() {
        return Session.getInstance(this.getProperties(), this.getAuthenticator());
    }

    /**
     * @return the configured {@link javax.mail.Session} object
     */
    @Override
    public Session getDefaultSession() {
        return Session.getDefaultInstance(this.getProperties(), this.getAuthenticator());
    }

    /**
     * Retrive the SMTP configutation from MailSenderConfiguration
     *
     * @return the properties defining the SMTP configuration
     */
    private Properties getProperties(){
        properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtp");
        properties.setProperty(MAIL_SMTP_HOST, senderConfiguration.getHost());
        properties.setProperty(MAIL_SMTP_USERNAME, senderConfiguration.getUsername());
        properties.setProperty(MAIL_FROM, senderConfiguration.getFromAddress());
        properties.setProperty(MAIL_SMTP_PORT, Integer.toString(senderConfiguration.getPort()));
        Properties extraProperties = senderConfiguration.getProperties();
        for (Map.Entry<Object, Object> extra :extraProperties.entrySet()){
            String propertyName = (String) extra.getKey();
            String propertyValue = (String) extra.getValue();
            if (properties.getProperty(propertyName) == null){
                properties.setProperty(propertyName, propertyValue);
            }
        }
        return properties;
    }

    /**
     * @return the Authenticator object
     */
    private Authenticator getAuthenticator(){
        if (senderConfiguration.usesAuthentication()){
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderConfiguration.getUsername(), senderConfiguration.getPassword());
                }
            };
        }
        return null;
    }
}
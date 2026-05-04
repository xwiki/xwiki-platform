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
package com.xpn.xwiki.objects.classes;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.Utils;

/**
 * Email Field Class allows to create a field for email values. This will allow using a custom displayer assigned to
 * that field by default. The field also includes a default regexp for validation.
 *
 * @version $Id$
 * @since 4.2M2
 */
public class EmailClass extends StringClass
{
    /**
     * The type used as a hint to find the class.
     * @since 18.2.0RC1
     */
    @Unstable
    public static final String PROPERTY_TYPE = "Email";

    /**
     * Constant defining the field name.
     **/
    protected static final String XCLASSNAME = "email";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailClass.class);

    private static final long serialVersionUID = 1L;

    /**
     * Default value for the email validation regular expression.
     */
    private static final String DEFAULT_EMAIL_VALIDATION_REGEXP =
        "/^(([^@\\s]+)@((?:[-a-zA-Z0-9]+\\.)+[a-zA-Z]{2,}))?$/";

    /**
     * Constructor for Email Class.
     *
     * @param wclass Meta Class
     **/
    public EmailClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, PROPERTY_TYPE, wclass);
    }

    /**
     * Constructor for Email Class.
     **/
    public EmailClass()
    {
        setValidationRegExp(DEFAULT_EMAIL_VALIDATION_REGEXP);
    }

    /**
     * {@inheritDoc}
     * @return {@code true} if the {@link GeneralMailConfiguration} requires to obfuscate.
     */
    @Override
    public boolean isSensitive(XWikiContext context)
    {
        GeneralMailConfiguration generalMailConfiguration = Utils.getComponent(GeneralMailConfiguration.class);
        return generalMailConfiguration.shouldObfuscate();
    }

    @Override
    public String getPropertyType()
    {
        return PROPERTY_TYPE;
    }

    /**
     * Use {@link EmailAddressObfuscator} to obfuscate email addresses.
     * If the value cannot be properly parsed, then this returns {@code null}.
     *
     * @param value the value to be obfuscated
     * @return the output of {@link EmailAddressObfuscator#obfuscate(InternetAddress)} or {@code null} if the given
     * value cannot be parsed to an email address.
     */
    @Override
    public Object getObfuscatedValue(Object value)
    {
        EmailAddressObfuscator emailAddressObfuscator = Utils.getComponent(EmailAddressObfuscator.class);
        try {
            InternetAddress address = InternetAddress.parse(String.valueOf(value))[0];
            return emailAddressObfuscator.obfuscate(address);
        } catch (AddressException e) {
            LOGGER.debug("Invalid email address value when trying to obfuscate [{}] falling back on null.", value);
            return null;
        }
    }
}

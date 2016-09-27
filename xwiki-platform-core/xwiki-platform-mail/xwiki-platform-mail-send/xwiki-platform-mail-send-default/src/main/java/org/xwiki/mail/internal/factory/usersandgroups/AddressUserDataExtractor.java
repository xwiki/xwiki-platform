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
package org.xwiki.mail.internal.factory.usersandgroups;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.plugin.rightsmanager.UserDataExtractor;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extracts email addresses from User profiles.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class AddressUserDataExtractor implements UserDataExtractor<Address>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressUserDataExtractor.class);

    @Override
    public Address extractFromSuperadmin(DocumentReference reference)
    {
        return null;
    }

    @Override
    public Address extractFromGuest(DocumentReference reference)
    {
        return null;
    }

    @Override
    public Address extract(DocumentReference reference, XWikiDocument document, BaseObject userObject)
    {
        Address address = null;

        // Extract the email
        String email = userObject.getStringValue("email");
        if (!StringUtils.isBlank(email)) {
            // Convert to an address
            try {
                address = InternetAddress.parse(email)[0];
            } catch (AddressException e) {
                // Invalid address, skip it, but log a warning!
                LOGGER.warn("Found invalid email address [{}] for user [{}]. Email will not been sent to that user.",
                    email, reference);
            }
        } else {
            LOGGER.warn("User [{}] has no email defined. Email will not been sent to that user.", reference);
        }

        return address;
    }
}

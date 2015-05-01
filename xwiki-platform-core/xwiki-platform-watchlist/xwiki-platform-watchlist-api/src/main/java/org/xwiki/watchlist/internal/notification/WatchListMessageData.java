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
package org.xwiki.watchlist.internal.notification;

import java.util.List;

import javax.mail.Address;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.watchlist.internal.api.WatchListEvent;

/**
 * Simple transfer class to store the information extracted from each subscriber that will then be used to send the
 * actual email message (and to render the message template used).
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WatchListMessageData
{
    private DocumentReference userReference;

    private DocumentReference templateReference;

    private String firstName;

    private String lastName;

    private Address address;

    private List<WatchListEvent> events;

    /**
     * @param userReference the subscriber's profile document reference
     * @param templateReference the email template's document reference
     * @param firstName the subscriber's first name
     * @param lastName the subscriber's last name
     * @param address the subscriber's email address
     * @param events the list of events to notify the subscriber of
     */
    public WatchListMessageData(DocumentReference userReference, DocumentReference templateReference, String firstName,
        String lastName, Address address, List<WatchListEvent> events)
    {
        this.userReference = userReference;
        this.templateReference = templateReference;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.events = events;
    }

    /**
     * @return the subscriber's profile document reference
     */
    public DocumentReference getUserReference()
    {
        return userReference;
    }

    /**
     * @return the email template's document reference
     */
    public DocumentReference getTemplateReference()
    {
        return templateReference;
    }

    /**
     * @return the subscriber's first name
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * @return the subscriber's last name
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * @return the subscriber's email address
     */
    public Address getAddress()
    {
        return address;
    }

    /**
     * @return the list of events to notify the subscriber of
     */
    public List<WatchListEvent> getEvents()
    {
        return events;
    }

}

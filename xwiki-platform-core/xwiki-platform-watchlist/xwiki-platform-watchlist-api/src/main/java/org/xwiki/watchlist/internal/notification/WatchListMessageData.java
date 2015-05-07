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

    private boolean showDiff;

    /**
     * @return the subscriber's profile document reference
     */
    public DocumentReference getUserReference()
    {
        return userReference;
    }

    /**
     * @param userReference see {@link #getUserReference()}
     */
    public void setUserReference(DocumentReference userReference)
    {
        this.userReference = userReference;
    }

    /**
     * @return the email template's document reference
     */
    public DocumentReference getTemplateReference()
    {
        return templateReference;
    }

    /**
     * @param templateReference see {@link #getTemplateReference()}
     */
    public void setTemplateReference(DocumentReference templateReference)
    {
        this.templateReference = templateReference;
    }

    /**
     * @return the subscriber's first name
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * @param firstName see {@link #getFirstName()}
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * @return the subscriber's last name
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * @param lastName see {@link #getLastName()}
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * @return the subscriber's email address
     */
    public Address getAddress()
    {
        return address;
    }

    /**
     * @param address {@link #getAddress()}
     */
    public void setAddress(Address address)
    {
        this.address = address;
    }

    /**
     * @return the list of events to notify the subscriber of
     */
    public List<WatchListEvent> getEvents()
    {
        return events;
    }

    /**
     * @param events see {@link #getEvents()}
     */
    public void setEvents(List<WatchListEvent> events)
    {
        this.events = events;
    }

    /**
     * @return true if the diff for each document will be shown in the WatchList message; false otherwise
     */
    public boolean isShowDiff()
    {
        return showDiff;
    }

    /**
     * @param showDiff see {@link #isShowDiff()}
     */
    public void setShowDiff(boolean showDiff)
    {
        this.showDiff = showDiff;
    }

}

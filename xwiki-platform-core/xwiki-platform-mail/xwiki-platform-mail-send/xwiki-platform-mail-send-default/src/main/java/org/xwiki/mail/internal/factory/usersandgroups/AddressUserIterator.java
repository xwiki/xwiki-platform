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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.mail.Address;

import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.internal.plugin.rightsmanager.UserIterator;

/**
 * Iterates over passed user and group references and return user's email addresses, but also handles an extra list
 * of email addresses to iterate over. Handles email duplication and an email exclusion list can be passed.
 *
 * @version $Id$
 * @since 6.4.2, 7.0M2
 */
public class AddressUserIterator extends UserIterator<Address>
{
    private UsersAndGroupsSource usersAndGroupsSource;

    private Iterator<Address> addressIterator;

    private List<Address> excludedAddresses;

    private List<Address> processedAddresses = new ArrayList<>();

    private Address lookaheadAddress;

    /**
     * @param usersAndGroupsSource the list of group and user references to iterate over along with a list of email
     *        addresses, with optional exclusion lists
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link org.xwiki.model.reference.DocumentReference}
     * @param execution the component used to access the {@link com.xpn.xwiki.XWikiContext} we use to call oldcore APIs
     */
    public AddressUserIterator(UsersAndGroupsSource usersAndGroupsSource,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        super(usersAndGroupsSource.getIncludedUserAndGroupReferences(),
            usersAndGroupsSource.getExcludedUserAndGroupReferences(), new AddressUserDataExtractor(),
            explicitDocumentReferenceResolver, execution);
        this.addressIterator = usersAndGroupsSource.getIncludedAddresses().iterator();
        this.excludedAddresses = usersAndGroupsSource.getExcludedAddresses();
        this.usersAndGroupsSource = usersAndGroupsSource;
    }

    @Override
    public boolean hasNext()
    {
        boolean hasNext = false;
        if (this.lookaheadAddress == null) {
            Address address = getNext();
            if (address != null) {
                this.lookaheadAddress = address;
                hasNext = true;
            }
        }
        return hasNext;
    }

    @Override
    public Address next()
    {
        Address address = this.lookaheadAddress;
        if (address != null) {
            this.lookaheadAddress = null;
        } else {
            address = getNext();
            if (address == null) {
                throw new NoSuchElementException(String.format("No more addresses to extract from [%s]",
                    this.usersAndGroupsSource));
            }
        }
        return address;
    }

    private Address getNext()
    {
        Address address;

        // Are there still group and user references to process?
        // If not, are there still email addresses to process?
        if (super.hasNext()) {
            address = super.next();
        } else if (this.addressIterator.hasNext()) {
            address = this.addressIterator.next();
        } else {
            return null;
        }

        // Handle excludes
        if (this.excludedAddresses.contains(address)) {
            return getNext();
        }

        // Handle duplicates:
        // - If the address has already been returned, skip it!
        // - Otherwise add it to the list of processed addresses.
        if (this.processedAddresses.contains(address)) {
            address = getNext();
        } else {
            this.processedAddresses.add(address);
        }

        return address;
    }
}

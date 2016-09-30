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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents the inputs of the {@link UsersAndGroupsMimeMessageFactory}: a list of user and group references
 * + a list of email addresses, and optional exclusion lists.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class UsersAndGroupsSource
{
    private List<DocumentReference> includedUserAndGroupReferences;

    private List<DocumentReference> excludedUserAndGroupReferences;

    private List<Address> includedAddresses;

    private List<Address> excludedAdresses;

    /**
     * @param includedUserAndGroupReferences see {@link #getIncludedUserAndGroupReferences()}
     * @param excludedUserAndGroupReferences see {@link #getExcludedUserAndGroupReferences()}
     * @param includedAddresses see {@link #getIncludedAddresses()}
     * @param excludedAddresses see {@link #getExcludedAddresses()}
     */
    public UsersAndGroupsSource(List<DocumentReference> includedUserAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences, List<Address> includedAddresses,
        List<Address> excludedAddresses)
    {
        this.includedUserAndGroupReferences = includedUserAndGroupReferences;
        this.excludedUserAndGroupReferences = excludedUserAndGroupReferences;
        this.includedAddresses = includedAddresses;
        this.excludedAdresses = excludedAddresses;
    }

    /**
     * @return the list of user and group references to iterate over
     */
    public List<DocumentReference> getIncludedUserAndGroupReferences()
    {
        return this.includedUserAndGroupReferences;
    }

    /**
     * @return the list of user and group references to exclude
     */
    public List<DocumentReference> getExcludedUserAndGroupReferences()
    {
        return this.excludedUserAndGroupReferences;
    }

    /**
     * @return the list of email addresses to iterate over
     */
    public List<Address> getIncludedAddresses()
    {
        return this.includedAddresses;
    }

    /**
     * @return the list of email addresses to exclude
     */
    public List<Address> getExcludedAddresses()
    {
        return this.excludedAdresses;
    }

    /**
     * @param sourceMap a Map containing the list of user + group references + a list of email addresses to iterate over
     *        (with optional excludes). The supported map keys are {@code users}, {@code groups}, {@code emails}
     *        {@code excludedUsers}, {@code excludedGroups} and {@code excludedEmails}
     * @return the typed instance representing the inputs passed
     * @throws MessagingException if one the passed email addresses is invalid (note that we're not parsing emails in
     *         strict mode and thus it's unlikely any exception will be raised in practice)
     */
    public static UsersAndGroupsSource parse(Map<String, Object> sourceMap) throws MessagingException
    {
        List<DocumentReference> includedUserAndGroupReferences =
            extractUserAndGroupReferences("users", "groups", sourceMap);
        List<DocumentReference> excludedUserAndGroupReferences =
            extractUserAndGroupReferences("excludedUsers", "excludedGroups", sourceMap);

        List<Address> includedAddresses = extractAddresses("emails", sourceMap);
        List<Address> excludedAddresses = extractAddresses("excludedEmails", sourceMap);

        return new UsersAndGroupsSource(includedUserAndGroupReferences, excludedUserAndGroupReferences,
            includedAddresses, excludedAddresses);
    }

    private static List<Address> extractAddresses(String key, Map<String, Object> sourceMap) throws MessagingException
    {
        List<Address> addresses;
        List<String> emails = (List<String>) sourceMap.get(key);
        if (emails != null) {
            addresses = new ArrayList<>();
            for (String email : emails) {
                addresses.add(InternetAddress.parse(email)[0]);
            }
        } else {
            addresses = Collections.emptyList();
        }
        return addresses;
    }

    private static List<DocumentReference> extractUserAndGroupReferences(String userKey, String groupKey,
        Map<String, Object> sourceMap)
    {
        List<DocumentReference> userAndGroupReferences = new ArrayList<>();
        List<DocumentReference> userReferences = (List<DocumentReference>) sourceMap.get(userKey);
        if (userReferences != null) {
            userAndGroupReferences.addAll(userReferences);
        }
        List<DocumentReference> groupReferences = (List<DocumentReference>) sourceMap.get(groupKey);
        if (groupReferences != null) {
            userAndGroupReferences.addAll(groupReferences);
        }
        return userAndGroupReferences;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("includedUserAndGroupsReferences", getIncludedUserAndGroupReferences());
        builder.append("excludedUserAndGroupsReferences", getExcludedUserAndGroupReferences());
        builder.append("includedAddresses", getIncludedAddresses());
        builder.append("excludedAddresses", getExcludedAddresses());
        return builder.toString();
    }
}

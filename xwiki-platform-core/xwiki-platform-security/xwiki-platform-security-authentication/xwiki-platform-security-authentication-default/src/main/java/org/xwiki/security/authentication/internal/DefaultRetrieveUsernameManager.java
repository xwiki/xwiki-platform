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
package org.xwiki.security.authentication.internal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authentication.RetrieveUsernameException;
import org.xwiki.security.authentication.RetrieveUsernameManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link RetrieveUsernameManager}.
 *
 * @version $Id$
 * @since 14.9
 * @since 14.4.6
 * @since 13.10.10
 */
@Component
@Singleton
public class DefaultRetrieveUsernameManager implements RetrieveUsernameManager
{
    private static final String HQL_QUERY = ", BaseObject obj, StringProperty prop where obj.name = doc.fullName and "
        + "obj.className = 'XWiki.XWikiUsers' and prop.id.id = obj.id and prop.id.name = 'email' "
        + "and LOWER(prop.value) = :email";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<AuthenticationMailSender> authenticationMailSenderProvider;

    @Override
    public Set<UserReference> findUsers(String requestEmail) throws RetrieveUsernameException
    {
        Set<UserReference> result = this.findUsers(requestEmail, false);
        if (result.isEmpty()) {
            result = this.findUsers(requestEmail, true);
        }
        return result;
    }

    private Set<UserReference> findUsers(String requestEmail, boolean mainWiki) throws RetrieveUsernameException
    {
        try {
            Query query = this.queryManager.createQuery(HQL_QUERY, Query.HQL)
                .bindValue("email", StringUtils.toRootLowerCase(requestEmail));

            if (mainWiki) {
                query = query.setWiki(this.wikiDescriptorManagerProvider.get().getMainWikiId());
            }

            List<String> userSerializedReferences = query.execute();
            return userSerializedReferences.stream()
                .map(this.userReferenceResolver::resolve)
                .collect(Collectors.toSet());
        } catch (QueryException e) {
            throw new RetrieveUsernameException(
                String.format("Error when performing the query to retrieve user from email [%s]", requestEmail), e);
        }
    }

    @Override
    public void sendRetrieveUsernameEmail(String requestEmail, Set<UserReference> userReferences)
        throws RetrieveUsernameException
    {
        if (userReferences.isEmpty()) {
            throw new RetrieveUsernameException("The list of user is empty.");
        }
        try {
            InternetAddress email = new InternetAddress(requestEmail);
            this.authenticationMailSenderProvider.get().sendRetrieveUsernameEmail(email, userReferences);
        } catch (AddressException e) {
            throw new RetrieveUsernameException(
                String.format("Error with the given email adresse: [%s]", requestEmail), e);
        }
    }
}

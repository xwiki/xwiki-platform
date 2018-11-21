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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.notifiers.email.NotificationEmailInterval;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Iterator that retrieve all users of the current wiki interested in the notifications emails at the specified
 * interval.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = NotificationUserIterator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NotificationUserIterator implements Iterator<DocumentReference>
{
    private static final int BATCH_SIZE = 50;

    /**
     * The query to perform to get all users having a not-empty email address.
     *
     * Here, we re using <code>length(objUser.email) > 0</code> instead of <code>objUser.email <> ''</code> because
     * ORACLE stores NULL instead of empty strings.
     *
     * But if we do
     * <code>objUser.email <> NULL AND objUser.email <> ''</code>, then we have wrong results with MySQL.
     *
     * This <code>length()</code> trick allows us to use the same query on every database we support, but a better
     * solution would be to write a different query for ORACLE than for the others, because this length() may be bad for
     * performances.
     */
    // TODO: try with the solution suggested by Sergiu Dumitriu there: https://jira.xwiki.org/browse/XWIKI-14914
    private static final String XWQL_QUERY = "select distinct doc.fullName from Document doc, "
            + "doc.object(XWiki.XWikiUsers) objUser where length(objUser.email) > 0 order by doc.fullName";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    private Queue<String> users = new ArrayDeque<>();

    private NotificationEmailInterval interval;

    private int offset;

    private DocumentReference nextUser;

    /**
     * Initialize the user iterator.
     * @param interval the interval that users must have configured
     */
    public void initialize(NotificationEmailInterval interval)
    {
        this.interval = interval;
        getNext();
    }

    private void getNext()
    {
        DocumentReference classReference = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                Arrays.asList("XWiki", "Notifications", "Code"), "NotificationEmailPreferenceClass");

        try {
            nextUser = null;
            while (!hasNext()) {
                if (users.isEmpty()) {
                    doQuery();
                    if (users.isEmpty()) {
                        return;
                    }
                }
                while (!hasNext() && !users.isEmpty()) {
                    DocumentReference user = resolver.resolve(users.poll(),
                            new WikiReference(wikiDescriptorManager.getCurrentWikiId()));
                    Object userInterval
                            = documentAccessBridge.getProperty(user, classReference, "interval");
                    if (isDefaultInterval(userInterval) || isSameInterval(userInterval)) {
                        nextUser = user;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fail to get next user.", e);
        }
    }

    private void doQuery() throws QueryException
    {
        Query query = queryManager.createQuery(XWQL_QUERY, Query.XWQL);
        query.setLimit(BATCH_SIZE);
        query.setOffset(offset);
        users.addAll(query.execute());
        offset += BATCH_SIZE;
    }

    private boolean isDefaultInterval(Object interval)
    {
        return (interval == null || StringUtils.isBlank((String) interval))
                && this.interval == NotificationEmailInterval.DAILY;
    }

    private boolean isSameInterval(Object interval)
    {
        if (interval == null || !(interval instanceof String)) {
            return false;
        }

        String stringInterval = (String) interval;
        return StringUtils.isNotBlank(stringInterval)
                && this.interval.equals(NotificationEmailInterval.valueOf(StringUtils.upperCase(stringInterval)));
    }

    @Override
    public boolean hasNext()
    {
        return nextUser != null;
    }

    @Override
    public DocumentReference next()
    {
        DocumentReference userReference = this.nextUser;
        getNext();
        return userReference;
    }
}

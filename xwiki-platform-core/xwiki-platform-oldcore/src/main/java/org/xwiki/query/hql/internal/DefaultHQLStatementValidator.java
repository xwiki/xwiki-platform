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
package org.xwiki.query.hql.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.internal.DefaultQuery;

import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;

/**
 * The main entry point to validate a query.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 * @since 15.10.16
 * @since 16.4.6
 */
@Component
@Singleton
public class DefaultHQLStatementValidator implements HQLStatementValidator
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("standard")
    private HQLCompleteStatementValidator standardValidator;

    @Override
    public boolean isSafe(String statement) throws QueryException
    {
        // An empty statement is safe
        if (StringUtils.isEmpty(statement)) {
            return true;
        }

        // Validators are expected to work with complete statements statements
        String completeStatement = HqlQueryUtils.toCompleteStatement(statement);

        // Parse and validate the statement
        ComponentManager componentManager = this.componentManagerProvider.get();
        try {
            for (HQLCompleteStatementValidator validator : componentManager
                .<HQLCompleteStatementValidator>getInstanceList(HQLCompleteStatementValidator.class)) {
                Optional<Boolean> result = validator.isSafe(completeStatement);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        } catch (ComponentLookupException e) {
            throw new QueryException("Failed to get query statement validators",
                new DefaultQuery(completeStatement, Query.HQL, null), e);
        }

        // Fallback on the standard validator (it's already supposed to be part of the found validators above, but
        // something might cause it to be unregistered)
        Optional<Boolean> result = this.standardValidator.isSafe(completeStatement);
        if (result.isPresent()) {
            return result.get();
        }

        // If we really could not find any way to validate the statement, consider it unsafe
        return false;
    }
}

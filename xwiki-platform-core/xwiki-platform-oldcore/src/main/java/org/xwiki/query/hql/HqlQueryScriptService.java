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
package org.xwiki.query.hql;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.query.hql.internal.HQLStatementValidator;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Provider various HQL related scripting APIs.
 * 
 * @version $Id$
 * @since 17.3.0RC1
 * @since 16.10.6
 */
@Component
@Named("query.hql")
@Singleton
@Unstable
public class HqlQueryScriptService implements ScriptService
{
    @Inject
    private HQLStatementValidator hqlStatementValidator;

    /**
     * Validate the given order by value according to the given list of allowed prefixes.
     * 
     * @param allowedPrefixes the prefix allowed for each order by parameter
     * @param orderByValue the order by value to check
     * @throws QueryException if the action should be denied, which may also happen when an error occurs
     */
    public void checkOrderBySafe(List<String> allowedPrefixes, String orderByValue) throws QueryException
    {
        this.hqlStatementValidator.checkOrderBySafe(allowedPrefixes, orderByValue);
    }
}

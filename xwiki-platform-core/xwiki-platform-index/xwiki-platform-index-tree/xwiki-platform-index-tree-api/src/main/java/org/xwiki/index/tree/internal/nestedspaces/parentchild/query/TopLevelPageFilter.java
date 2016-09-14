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
package org.xwiki.index.tree.internal.nestedspaces.parentchild.query;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.query.AbstractNestedPageFilter;

/**
 * Include only the documents that either don't have a parent document or that have a parent document in a different
 * space.
 * 
 * @version $Id$
 * @since 8.3RC1, 7.4.5
 */
@Component
@Named("topLevelPage/parentChildOnNestedSpaces")
@Singleton
public class TopLevelPageFilter extends AbstractNestedPageFilter
{
    @Override
    protected String filterTerminalPagesStatement(String statement)
    {
        // Note that in Oracle the empty string is stored as null.
        String hasNoParent = "(doc.XWD_PARENT = '' or doc.XWD_PARENT is null)";
        String hasParentOutsideSpace = "(doc.XWD_PARENT like '%.%' and doc.XWD_PARENT not like :absoluteRef "
            + "and doc.XWD_PARENT not like :localRef)";
        return statement + String.format(" and (%s or %s)", hasNoParent, hasParentOutsideSpace);
    }
}

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
package org.xwiki.rest.internal.resources.search;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("hql")
@Singleton
public class HQLSearchSource extends AbstractDatabaseSearchSource
{
    /**
     * Default constructor.
     */
    public HQLSearchSource()
    {
        super("hql");
    }

    @Override
    protected String resolveQuery(boolean distinct, String query)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("select ");

        if (distinct) {
            builder.append("distinct ");
        }

        builder.append("doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc ");

        if (query.toLowerCase().startsWith("from")) {
            builder.append(',');
            builder.append(query.substring(4));
        } else {
            builder.append(query);
        }

        return builder.toString();
    }
}

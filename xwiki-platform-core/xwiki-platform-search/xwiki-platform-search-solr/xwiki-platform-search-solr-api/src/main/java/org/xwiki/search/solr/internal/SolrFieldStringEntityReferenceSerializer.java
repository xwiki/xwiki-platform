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
package org.xwiki.search.solr.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Serializes an {@link EntityReference} to a {@link String} that can be included in a Solr field name. Examples:
 * <ul>
 * <li>Class reference 'Blog.BlogPostClass' produces 'Blog.BlogPostClass'</li>
 * <li>Class reference 'Bl\.og.BlogPos\.tClass' produces 'Bl..og.BlogPos..tClass'</li>
 * <li>Class property reference 'Blog.BlogPostClass^title' produces 'Blog.BlogPostClass.title'</li>
 * <li>Class property reference 'B\.log.BlogPost\.Class^titl\.e' produces 'B..log.BlogPost..Class.titl..e'</li>
 * </ul>
 * Note that we need this special serialization syntax because both '^' and '\' are special characters in the Solr query
 * syntax so it's not possible to use them in the field name. We chose '.' (dot) as the separator because it was already
 * used for separating the space and page name in the default {@link EntityReferenceSerializer} and because it was one
 * of the few non-alphanumeric characters allowed in the field name. Other options would have been '_' (but it appears
 * more often in the space/page/property name than dot), '$' (already used by the Solr field name encoder) and '-'
 * (which doesn't look natural).
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Named("solr")
@Singleton
public class SolrFieldStringEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    /**
     * The string version of the {@link SolrFieldStringEntityReferenceResolver#SEPARATOR}.
     */
    static final String SEPARATOR = String.valueOf(SolrFieldStringEntityReferenceResolver.SEPARATOR);

    /**
     * The escaped separator.
     */
    static final String ESCAPED_SEPARATOR = SEPARATOR + SEPARATOR;

    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        StringBuilder output = new StringBuilder();
        for (EntityReference parent : reference.getReversedReferenceChain()) {
            output.append(SEPARATOR).append(parent.getName().replace(SEPARATOR, ".."));
        }
        return output.substring(1);
    }
}

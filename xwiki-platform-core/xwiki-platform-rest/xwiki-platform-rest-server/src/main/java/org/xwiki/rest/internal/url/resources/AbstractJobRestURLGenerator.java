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
package org.xwiki.rest.internal.url.resources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.rest.internal.url.AbstractParametrizedRestURLGenerator;

/**
 * Jobs related Abstract class for ParametrizedRestURLGenerator.
 * 
 * @param <T> the type of the resource for which the URL are created for. Must inherit from
 *            {@link org.xwiki.model.reference.EntityReference}.
 * @version $Id$
 * @since 8.0
 */
@Singleton
public abstract class AbstractJobRestURLGenerator<T> extends AbstractParametrizedRestURLGenerator<T>
{
    protected String getIdStringElement(List<String> id)
    {
        StringBuilder builder = new StringBuilder();

        for (String idElement : id) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            try {
                builder.append(URLEncoder.encode(idElement, "UTF8"));
            } catch (UnsupportedEncodingException e) {
                // Should never happen
            }
        }

        return builder.toString();
    }
}

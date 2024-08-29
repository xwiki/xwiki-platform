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
package org.xwiki.template;

import java.time.Instant;

/**
 * A template.
 *
 * @version $Id$
 * @since 7.0M1
 */
public interface Template
{
    /**
     * @return the identifier of the template
     */
    String getId();

    /**
     * @return the path of the template
     */
    String getPath();

    /**
     * @return the instant the cached content of that template was last modified or null if it's unknown
     * @since 15.8RC1
     */
    default Instant getInstant()
    {
        return null;
    }

    /**
     * Parse and return the template content.
     * 
     * @return the content of the template
     * @throws Exception when failing to parse the template
     */
    TemplateContent getContent() throws Exception;
}

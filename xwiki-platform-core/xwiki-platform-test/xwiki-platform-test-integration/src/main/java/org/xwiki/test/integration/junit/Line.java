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
package org.xwiki.test.integration.junit;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Defines an expected or excluded line (can be a regex or not).
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class Line
{
    private String content;

    private boolean isRegex;

    /**
     * @param content the log line that represents either an expectation or an exclude
     */
    public Line(String content)
    {
        this.content = content;
    }

    /**
     * @param content the log line that represents either an expectation or an exclude
     * @param isRegex if true then the content is specified as a regex
     */
    public Line(String content, boolean isRegex)
    {
        this(content);
        this.isRegex = isRegex;
    }

    /**
     * @return the log line that represents either an expectation or an exclude
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * @return true if the content is specified as a regex
     */
    public boolean isRegex()
    {
        return this.isRegex;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(7, 5)
            .append(getContent())
            .append(isRegex())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        Line rhs = (Line) object;
        return new EqualsBuilder()
            .append(getContent(), rhs.getContent())
            .append(isRegex(), rhs.isRegex())
            .isEquals();
    }
}

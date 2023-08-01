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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import org.xwiki.text.XWikiToStringBuilder;

/**
 * See the <a href="https://ossf.github.io/osv-schema/#severity-field">Open Source Vulnerability format API
 * documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class SeverityObject
{
    private String type;

    private String score;

    /**
     * @return the type field
     * @see <a href="https://ossf.github.io/osv-schema/#severitytype-field">type field documentation</a>
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type field
     * @see <a href="https://ossf.github.io/osv-schema/#severitytype-field">type field documentation</a>
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return the score field
     * @see <a href="https://ossf.github.io/osv-schema/#severityscore-field">score field documentation</a>
     */
    public String getScore()
    {
        return this.score;
    }

    /**
     * @param score the score field
     * @see <a href="https://ossf.github.io/osv-schema/#severityscore-field">score field documentation</a>
     */
    public void setScore(String score)
    {
        this.score = score;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("type", getType())
            .append("score", getScore())
            .toString();
    }
}

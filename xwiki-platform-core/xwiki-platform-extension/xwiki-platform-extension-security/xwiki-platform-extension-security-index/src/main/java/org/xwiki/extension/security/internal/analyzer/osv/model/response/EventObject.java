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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See the <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability format API
 * documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class EventObject
{
    private String introduced;

    private String fixed;

    @JsonProperty("last_affected")
    private String lastAffected;

    /**
     * @return the introduced field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     *     format</a>
     */
    public String getIntroduced()
    {
        return this.introduced;
    }

    /**
     * @param introduced the introduced field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     format</a>
     */
    public void setIntroduced(String introduced)
    {
        this.introduced = introduced;
    }

    /**
     * @return the fixed field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     format</a>
     */
    public String getFixed()
    {
        return this.fixed;
    }

    /**
     * @param fixed the fixed field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     format</a>
     */
    public void setFixed(String fixed)
    {
        this.fixed = fixed;
    }

    /**
     * @return the last_affected field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     format</a>
     */
    public String getLastAffected()
    {
        return this.lastAffected;
    }

    /**
     * @param lastAffected the last_affected field
     * @see <a href="https://ossf.github.io/osv-schema/#affectedrangesevents-fields">Open Source Vulnerability
     *     format</a>
     */
    public void setLastAffected(String lastAffected)
    {
        this.lastAffected = lastAffected;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("introduced", getIntroduced())
            .append("fixed", getFixed())
            .append("lastAffected", getLastAffected())
            .toString();
    }
}

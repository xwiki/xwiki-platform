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

import java.util.List;

import org.xwiki.text.XWikiToStringBuilder;

/**
 * POJO for the <a href="https://ossf.github.io/osv-schema/">Open Source Vulnerability format API response</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class OsvResponse
{
    private List<VulnObject> vulns;

    /**
     * @return the list of vulnerabilities
     * @see <a href="https://ossf.github.io/osv-schema/">the osv vulnerability format</a>
     */
    public List<VulnObject> getVulns()
    {
        return this.vulns;
    }

    /**
     * @param vulns the list of vulnerabilities
     * @see <a href="https://ossf.github.io/osv-schema/">the osv vulnerability format</a>
     */
    public void setVulns(List<VulnObject> vulns)
    {
        this.vulns = vulns;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("vulns", getVulns())
            .toString();
    }
}

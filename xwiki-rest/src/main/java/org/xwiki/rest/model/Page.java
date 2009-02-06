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
package org.xwiki.rest.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @version $Id$
 */
@XStreamAlias("page")
public class Page extends PageSummary
{
    private String language;

    private String version;

    private Integer majorVersion;

    private Integer minorVersion;

    private String xwikiUrl;

    private Long created;

    private String creator;

    private Long modified;

    private String modifier;

    private String content;

    public Page()
    {
        super();
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public Integer getMinorVersion()
    {
        return minorVersion;
    }

    public void setMinorVersion(Integer minorVersion)
    {
        this.minorVersion = minorVersion;
    }

    public String getXwikiUrl()
    {
        return xwikiUrl;
    }

    public void setXWikiUrl(String xwikiUrl)
    {
        this.xwikiUrl = xwikiUrl;
    }

    public Long getCreated()
    {
        return created;
    }

    public void setCreated(Long created)
    {
        this.created = created;
    }

    public String getCreator()
    {
        return creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public Long getModified()
    {
        return modified;
    }

    public void setModified(Long modified)
    {
        this.modified = modified;
    }

    public String getModifier()
    {
        return modifier;
    }

    public void setModifier(String modifier)
    {
        this.modifier = modifier;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getMajorVersion()
    {
        return majorVersion;
    }

    public void setMajorVersion(Integer majorVersion)
    {
        this.majorVersion = majorVersion;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}

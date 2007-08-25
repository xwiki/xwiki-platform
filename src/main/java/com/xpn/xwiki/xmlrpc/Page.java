/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.xmlrpc.Convert.ConversionException;

import java.util.Date;
import java.util.Map;

import org.suigeneris.jrcs.rcs.Version;

/**
 * Represents a Page as described in the <a href="Confluence specification">
 * http://confluence.atlassian.com/display/DOC/Remote+API+Specification</a>.
 * 
 * @todo right now there's no validation done on any parameter and this class accepts null values
 *       for all parameters. In the future we need a validation strategy defined which corresponds
 *       to how this class is used: for creating a page, for udpating it, etc. The validation needs
 *       are different across the use cases so it might even be best to have different validation
 *       classes used where this class is used in the code.
 * @version $Id: $
 */
public class Page extends PageSummary
{
    private int version;

    private String content;

    private Date created;

    private String creator;

    private Date modified;

    private String modifier;

    private boolean homepage;

    private String comment;

    // This constructor is really ridiculous
    public Page(String id, String space, String parentId, String title, String url, int version,
        String content, Date created, String creator, Date modified, String modifier,
        boolean homepage, String comment, int locks)
    {
        super(id, space, parentId, title, url, locks);
        setVersion(version);
        setContent(content);
        setCreated(created);
        setCreator(creator);
        setModified(modified);
        setModifier(modifier);
        setHomepage(homepage);
        setComment(comment);
    }

    public Page(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        super(doc, context);
        setVersion(constructVersion(doc.getRCSVersion()));
        setContent(doc.getContent());
        setCreated(doc.getCreationDate());
        setCreator(doc.getAuthor());
        setModified(doc.getDate());
        setModifier(doc.getAuthor());
        setComment(doc.getComment());
        setHomepage((doc.getName().equals("WebHome")));
    }

    public Page(Map map) throws ConversionException
    {
        super(map);

        if (map.containsKey("version")) {
            setVersion(Convert.str2int((String) map.get("version")));
        }
        setContent((String) map.get("content"));
        if (map.containsKey("created")) {
            setCreated(Convert.str2date((String) map.get("created")));
        }
        setCreator((String) map.get("creator"));
        if (map.containsKey("modified")) {
            setModified(Convert.str2date((String) map.get("modified")));
        }
        setModifier((String) map.get("modifier"));
        setComment((String) map.get("comment"));
        if (map.containsKey("homepage")) {
            setHomepage(Convert.str2bool((String) map.get("homepage")));
        }
    }

    public Map toMap()
    {
        Map map = super.toMap();
        map.put("version", Convert.int2str(getVersion()));
        map.put("content", getContent());
        map.put("created", Convert.date2str(getCreated()));
        map.put("creator", getCreator());
        map.put("modified", Convert.date2str(getModified()));
        map.put("modifier", getModifier());
        map.put("comment", getComment());
        map.put("homepage", Convert.bool2str(isHomepage()));
        return map;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public static int constructVersion(Version ver)
    {
        return ((ver.at(0)-1) << 16) + ver.at(1);
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date created)
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

    public Date getModified()
    {
        return modified;
    }

    public void setModified(Date modified)
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

    public boolean isHomepage()
    {
        return homepage;
    }

    public void setHomepage(boolean homepage)
    {
        this.homepage = homepage;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}

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
package com.xpn.xwiki.doc.rcs;

import java.io.Serializable;

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.util.AbstractSimpleClass;

/**
 * Composite ID component used in {@link XWikiRCSNodeInfo} & {@link XWikiRCSNodeContent}. Immutable.
 *
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiRCSNodeId extends AbstractSimpleClass implements Serializable, Cloneable
{
    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#getId()
     */
    private long docId;

    /**
     * Version of document.
     */
    private Version version = new Version(1, 1);

    /**
     * Default constructor used in Hibernate to load this class.
     */
    public XWikiRCSNodeId()
    {
    }

    /**
     * @param docId = {@link com.xpn.xwiki.doc.XWikiDocument#getId()}
     * @param version - version of document
     */
    public XWikiRCSNodeId(long docId, Version version)
    {
        super();
        this.docId = docId;
        this.version = version;
    }

    /**
     * @return {@link com.xpn.xwiki.doc.XWikiDocument#getId()}
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docId = {@link com.xpn.xwiki.doc.XWikiDocument#getId()}
     */
    protected void setDocId(long docId)
    {
        this.docId = docId;
    }

    /**
     * @return version of document
     */
    public Version getVersion()
    {
        return this.version;
    }

    /**
     * @param ver - version of document
     */
    protected void setVersion(Version ver)
    {
        this.version = ver;
    }

    /**
     * @return 1st number in version used in Hibernate to store this class
     */
    protected int getVersion1()
    {
        return this.version.at(0);
    }

    /**
     * @return 2nd number in version used in Hibernate to store this class
     */
    protected int getVersion2()
    {
        return this.version.at(1);
    }

    /**
     * @param v1 = 1st number in version used in Hibernate to load this class
     */
    protected void setVersion1(int v1)
    {
        this.version = new Version(v1, this.version.at(1));
    }

    /**
     * @param v2 = 2nd number in version used in Hibernate to load this class
     */
    protected void setVersion2(int v2)
    {
        this.version = new Version(this.version.at(0), v2);
    }

    @Override
    public Object clone()
    {
        try {
            // there are no mutable fields, so simple do super.clone()
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // clone is supported. exception is nonsense.
            throw new RuntimeException(e);
        }
    }
}

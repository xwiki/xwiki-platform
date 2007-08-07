/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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
 *
 */
package com.xpn.xwiki.doc.rcs;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Composite ID component used in {@link XWikiRCSNodeInfo} & {@link XWikiRCSNodeContent}.
 * Mutable.
 * @version $Id: $
 */
public class XWikiRCSNodeId implements Serializable, Cloneable
{
    // composite-key
    /**
     * = {@link XWikiDocument#getId()}.
     * part of composite key
     */
    private long    docId;
    /**
     * version of document.
     * part of composite key
     */
    private Version version         = new Version(1, 1);
    
    /**
     * default constructor used in Hibernate to load this class.
     */
    public XWikiRCSNodeId() { }
    
    /**
     * @param docId = {@link XWikiDocument#getId()}
     * @param version - version of document
     */
    public XWikiRCSNodeId(long docId, Version version)
    {
        super();
        this.docId = docId;
        this.version = version;
    }

    /*/**
     * Clone-constructor.
     * @param node - clone from what node
     *
    public XWikiRCSNodeId(XWikiRCSNodeId node)
    {
        this(node.getDocId(), node.getVersion());
    }*/

    /**
     * @return {@link XWikiDocument#getId()}
     */
    public long getDocId()
    {
        return docId;
    }
    
    /**
     * @param docId = {@link XWikiDocument#getId()}
     */
    public void setDocId(long docId)
    {
        this.docId = docId;
    }
    
    /**
     * @return version of document
     */
    public Version getVersion()
    {
        return version;
    }
    /**
     * @return 1st number in version
     * used in Hibernate to store this class
     */
    protected int getVersion1()
    {
        return version.at(0);
    }
    /**
     * @return 2nd number in version
     * used in Hibernate to store this class
     */
    protected int getVersion2()
    {
        return version.at(1);
    }
    /**
     * @param ver - version of document
     */
    public void setVersion(Version ver)
    {
        this.version = ver;
    }
    /**
     * @param v1 = 1st number in version
     * used in Hibernate to load this class
     */
    protected void setVersion1(int v1)
    {
        this.version = new Version(v1, version.at(1));
    }
    /**
     * @param v2 = 2nd number in version
     * used in Hibernate to load this class
     */
    protected void setVersion2(int v2)
    {
        this.version = new Version(version.at(0), v2);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);        
    }
    
    /**
     * {@inheritDoc}
     * @throws CloneNotSupportedException 
     */
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
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}

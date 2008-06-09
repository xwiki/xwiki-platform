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
package org.xwiki.rendering.listener;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class Link
{
    private String label;

    private String interWikiAlias;

    private String target;

    private String documentNameOrUri;

    private boolean isDocumentName;
    
    private String queryString;

    private String anchor;

    public void setDocumentNameOrUri(String documentNameOrUri)
    {
        this.documentNameOrUri = documentNameOrUri;
    }

    public String getDocumentNameOrUri()
    {
        return this.documentNameOrUri;
    }

    public boolean isDocumentName()
    {
        return this.isDocumentName;
    }

    public void setContainsDocumentName(boolean isDocumentName)
    {
        this.isDocumentName = isDocumentName;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getTarget()
    {
        return this.target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public String getInterWikiAlias()
    {
        return this.interWikiAlias;
    }

    public void setInterWikiAlias(String interWikiAlias)
    {
        this.interWikiAlias = interWikiAlias;
    }

    public String getQueryString()
    {
        return this.queryString;
    }

    public void setQueryString(String queryString)
    {
        this.queryString = queryString;
    }

    public String getAnchor()
    {
        return this.anchor;
    }

    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
    }

    /**
     * The output is syntax independent since this class is used for all syntaxes. Specific
     * syntaxes should extend this class and override this method to perform syntax-dependent
     * formatting.
     */
    public String toString()
    {
        boolean shouldAddSpace = false;
        StringBuffer sb = new StringBuffer();
        if (getLabel() != null) {
            sb.append("Label = [").append(getLabel()).append("]");
            shouldAddSpace = true;
        }
        if (getDocumentNameOrUri() != null) {
            sb.append(shouldAddSpace ? " ": "");
            sb.append("DocumentNameOrUri = [").append(getDocumentNameOrUri()).append("]");
            shouldAddSpace = true;
        }
        if (getQueryString() != null) {
            sb.append(shouldAddSpace ? " ": "");
            sb.append("QueryString = [").append(getQueryString()).append("]");
            shouldAddSpace = true;
        }
        if (getAnchor() != null) {
            sb.append(shouldAddSpace ? " ": "");
            sb.append("Anchor = [").append(getAnchor()).append("]");
            shouldAddSpace = true;
        }
        if (getInterWikiAlias() != null) {
            sb.append(shouldAddSpace ? " ": "");
            sb.append("InterWikiAlias = [").append(getInterWikiAlias()).append("]");
            shouldAddSpace = true;
        }
        if (getTarget() != null) {
            sb.append(shouldAddSpace ? " ": "");
            sb.append("Target = [").append(getTarget()).append("]");
        }

        return sb.toString();
    }
}

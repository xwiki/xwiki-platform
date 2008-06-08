package org.xwiki.rendering.listener;

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

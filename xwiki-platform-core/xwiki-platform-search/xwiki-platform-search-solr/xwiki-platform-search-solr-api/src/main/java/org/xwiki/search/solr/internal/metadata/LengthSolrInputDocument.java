package org.xwiki.search.solr.internal.metadata;

import org.apache.solr.common.SolrInputDocument;

/**
 * Extended SolrInputDocument with calculated size.
 * 
 * @version $Id$
 * @since 5.1M2
 */
public class LengthSolrInputDocument extends SolrInputDocument
{
    /**
     * @see #getLength()
     */
    private int length;

    /**
     * @return the length (generally the number of characters). It's not the exact byte length, it's more a scale value.
     */
    public int getLength()
    {
        return this.length;
    }

    @Override
    public void addField(String name, Object value, float boost)
    {
        super.addField(name, value, boost);

        if (value instanceof String) {
            this.length += ((String) value).length();
        } else if (value instanceof byte[]) {
            this.length += ((byte[]) value).length;
        }

        // TODO: support more type ?
    }
}

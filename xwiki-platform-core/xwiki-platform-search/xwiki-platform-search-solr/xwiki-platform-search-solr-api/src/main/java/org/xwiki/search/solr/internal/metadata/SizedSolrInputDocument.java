package org.xwiki.search.solr.internal.metadata;

import org.apache.solr.common.SolrInputDocument;

/**
 * Extended SolrInputDocument with calculated size.
 * 
 * @version $Id$
 * @since 5.1M2
 */
public class SizedSolrInputDocument extends SolrInputDocument
{
    /**
     * @see #getSize()
     */
    private int size;

    /**
     * @return the size (generally the number of characters). It's not the exact byte size, it's more a scale value.
     */
    public int getSize()
    {
        return this.size;
    }

    @Override
    public void addField(String name, Object value, float boost)
    {
        super.addField(name, value, boost);

        if (value instanceof String) {
            this.size += ((String) value).length();
        } else if (value instanceof byte[]) {
            this.size += ((byte[]) value).length;
        }

        // TODO: support more type ?
    }
}

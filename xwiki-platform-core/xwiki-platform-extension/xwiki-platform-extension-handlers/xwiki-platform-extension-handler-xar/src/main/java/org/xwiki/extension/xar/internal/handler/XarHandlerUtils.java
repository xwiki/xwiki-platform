package org.xwiki.extension.xar.internal.handler;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarHandlerUtils
{
    protected static final String WIKI_NAMESPACEPREFIX = "wiki:";

    public static String getWikiFromNamespace(String namespace) throws UnsupportedNamespaceException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new UnsupportedNamespaceException("Unsupported namespace [" + namespace
                    + "], only wiki:wikiid format is supported");
            }
        }

        return wiki;
    }
}

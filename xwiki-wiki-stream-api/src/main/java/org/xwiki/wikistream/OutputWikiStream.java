package org.xwiki.wikistream;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.wikistream.listener.Listener;

/**
 * @param <P> The parameter bean class
 * @version $Id$
 */
@ComponentRole
public interface OutputWikiStream<P> extends WikiStream<P>
{
    /**
     * @param parameters
     * @return {@link Listener}
     * @throws OutputWikiStreamException
     */
    Listener createListener(P parameters) throws WikiStreamException;

}

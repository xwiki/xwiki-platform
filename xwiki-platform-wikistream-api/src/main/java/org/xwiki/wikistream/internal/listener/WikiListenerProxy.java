package org.xwiki.wikistream.internal.listener;

import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.descriptor.ListenerDescriptor;
import org.xwiki.wikistream.listener.WikiListener;

public class WikiListenerProxy implements WikiListener
{
    private Object listener;

    public WikiListenerProxy(Object listener, ListenerDescriptor descriptor)
    {
        this.listener = listener;
    }

    protected boolean isWikiListener()
    {
        return this.listener instanceof WikiListener;
    }

    protected WikiListener getWikiListener()
    {
        return (WikiListener) this.listener;
    }

    // WikiListener

    @Override
    public void beginWiki(String name, MetaData metadata)
    {
        if (isWikiListener()) {
            getWikiListener().beginWiki(name, metadata);
        }
    }

    @Override
    public void endWiki(String name, MetaData metadata)
    {
        if (isWikiListener()) {
            getWikiListener().endWiki(name, metadata);
        }
    }
}

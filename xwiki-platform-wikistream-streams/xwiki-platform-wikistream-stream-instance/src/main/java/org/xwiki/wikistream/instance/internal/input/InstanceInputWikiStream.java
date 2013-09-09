package org.xwiki.wikistream.instance.internal.input;

import javax.inject.Provider;

import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;

import com.xpn.xwiki.XWikiContext;

public class InstanceInputWikiStream implements InputWikiStream
{
    private Provider<XWikiContext> xcontextProvider;

    private InstanceInputProperties properties;

    public InstanceInputWikiStream(Provider<XWikiContext> xcontextProvider, InstanceInputProperties properties)
    {
        this.xcontextProvider = xcontextProvider;
        this.properties = properties;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        
        
    }
}

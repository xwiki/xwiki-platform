package org.xwiki.wikistream.databaseold.internal.input;

import javax.inject.Provider;

import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;

import com.xpn.xwiki.XWikiContext;

public class DatabaseInputWikiStream implements InputWikiStream
{
    private Provider<XWikiContext> xcontextProvider;

    private DatabaseInputProperties properties;

    public DatabaseInputWikiStream(Provider<XWikiContext> xcontextProvider, DatabaseInputProperties properties)
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

package org.xwiki.wikistream.xml.internal.input;

import org.xml.sax.ContentHandler;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;

public abstract class AbstractXMLBeanInputWikiStreamFactory<P extends XMLInputParameters> extends
    AbstractBeanInputWikiStreamFactory<P>
{
    public AbstractXMLBeanInputWikiStreamFactory(WikiStreamType type)
    {
        super(type);
    }

    @Override
    protected InputWikiStream createInputWikiStream(P parameters) throws WikiStreamException
    {
        return new DefaultXMLInputWikiStream<P>(this, parameters);
    }

    protected abstract ContentHandler createContentHandler(Object listener, P parameters);
}

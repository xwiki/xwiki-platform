package org.xwiki.wikistream.xml.internal.input;

import org.xml.sax.ContentHandler;

public class DefaultXMLInputWikiStream<P extends XMLInputParameters> extends AbstractXMLInputWikiStream<P>
{
    private final AbstractXMLBeanInputWikiStreamFactory<P> factory;

    public DefaultXMLInputWikiStream(AbstractXMLBeanInputWikiStreamFactory<P> factory, P parameters)
    {
        super(parameters);

        this.factory = factory;
    }

    @Override
    protected ContentHandler createContentHandler(Object listener)
    {
        return this.factory.createContentHandler(listener);
    }
}

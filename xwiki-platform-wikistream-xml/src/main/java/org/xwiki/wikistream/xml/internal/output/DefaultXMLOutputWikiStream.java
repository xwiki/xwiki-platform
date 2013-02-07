package org.xwiki.wikistream.xml.internal.output;

import org.xwiki.wikistream.WikiStreamException;

public class DefaultXMLOutputWikiStream<P extends XMLOuputParameters> extends AbstractXMLOutputWikiStream<P>
{
    private final AbstractXMLBeanOutputWikiStreamFactory<P> factory;

    public DefaultXMLOutputWikiStream(AbstractXMLBeanOutputWikiStreamFactory<P> factory, P parameters)
        throws WikiStreamException
    {
        super(parameters);

        this.factory = factory;
    }

    @Override
    protected Object createListener()
    {
        return this.factory.createListener(this.contentHandler);
    }
}

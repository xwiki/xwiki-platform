package org.xwiki.wikistream.input;

import org.xwiki.wikistream.WikiStreamException;

public interface InputWikiStream
{
    void read(Object listener) throws WikiStreamException;
}

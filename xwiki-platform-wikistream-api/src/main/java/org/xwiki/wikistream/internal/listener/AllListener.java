package org.xwiki.wikistream.internal.listener;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.wikistream.listener.DocumentListener;
import org.xwiki.wikistream.listener.SpaceListener;
import org.xwiki.wikistream.listener.WikiListener;

public interface AllListener extends WikiListener, SpaceListener, DocumentListener, Listener
{

}

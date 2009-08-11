package com.xpn.xwiki.util;

import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.XWikiContext;

/**
 * Tool to make easier to generate stub XWikiContext. It's supposed to be initialized once with the first request and it
 * can be called to get a stub context generated from this initial XWikiContext.
 * <p>
 * The reason to initialize it based on first request is to get some informations we could not know otherwise like a
 * default scheme/host/port.
 * 
 * @version $Id$
 */
@ComponentRole
public interface XWikiStubContextProvider
{
    /**
     * Initialize a stub context from a real context.
     * <p>
     * We create initial stub context from a real XWikiContext to have a stub as complete as possible. Like getting the
     * proper host/port/scheme, the engine context etc.
     * 
     * @param context a real XWikiContext
     */
    void initialize(XWikiContext context);

    /**
     * @return a usable XWikiContext
     */
    XWikiContext createStubContext();
}

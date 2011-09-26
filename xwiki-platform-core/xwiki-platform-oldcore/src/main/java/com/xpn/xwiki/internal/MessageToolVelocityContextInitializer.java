package com.xpn.xwiki.internal;

import groovy.lang.Singleton;

import javax.inject.Named;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.velocity.VelocityContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Puts the {@code $msg} variable in the context.
 * 
 * @version $Id$
 */
@Component
@Named("messagetool")
@Singleton
public class MessageToolVelocityContextInitializer implements VelocityContextInitializer
{
    /** The key under which the message tool should be found. */
    private static final String CONTEXT_KEY = "msg";

    @Override
    public void initialize(VelocityContext context)
    {
        XWikiContext xcontext = Utils.getContext();
        if (xcontext == null || xcontext.getWiki() == null) {
            // Nothing we can do yet, incomplete context
            return;
        }
        if (xcontext.get(CONTEXT_KEY) == null) {
            xcontext.getWiki().prepareResources(xcontext);
        }
        context.put(CONTEXT_KEY, xcontext.get(CONTEXT_KEY));
    }
}

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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

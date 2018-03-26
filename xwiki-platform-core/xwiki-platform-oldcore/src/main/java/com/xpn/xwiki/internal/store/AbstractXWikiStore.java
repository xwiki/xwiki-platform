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
package com.xpn.xwiki.internal.store;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;

/**
 * A base class to help old stores to deal with unsynchronized XWikiContext (passed and {@link ExecutionContext}).
 * 
 * @version $Id$
 * @since 10.3RC1
 */
public abstract class AbstractXWikiStore
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXWikiStore.class);

    private static final String PREVIOUS_WIKI = AbstractXWikiStore.class.getName() + "#previouswiki";

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readonlyxcontextProvider;

    /**
     * @param inputxcontext the XWikiContext given as input to the API
     * @return the {@link XWikiContext} located in the {@link ExecutionContext} if any
     */
    protected XWikiContext getExecutionXContext(XWikiContext inputxcontext, boolean savewiki)
    {
        // If not a component return input context
        if (this.readonlyxcontextProvider == null) {
            return inputxcontext;
        }

        XWikiContext xcontext = this.readonlyxcontextProvider.get();

        // If no context can be found return input context
        if (xcontext == null) {
            return inputxcontext;
        }

        if (inputxcontext != null && xcontext != inputxcontext) {
            LOGGER.warn("ExecutionContext and passed XWikiContext argument mismatched, for data safety,"
                + " the XWikiContext from the ExecutionContext has been used.", new Exception("Stack trace"));

            // Make sure to use the wiki expected by the called for the API
            if (savewiki && !Objects.equals(inputxcontext.getWikiReference(), xcontext.getWikiReference())) {
                xcontext.put(PREVIOUS_WIKI, xcontext.getWikiReference());
                xcontext.setWikiReference(inputxcontext.getWikiReference());
            }
        }

        return xcontext;
    }

    protected void restoreExecutionXContext()
    {
        if (this.readonlyxcontextProvider != null) {
            XWikiContext xcontext = this.readonlyxcontextProvider.get();

            if (xcontext != null) {
                WikiReference wikiReference = (WikiReference) xcontext.remove(PREVIOUS_WIKI);

                if (wikiReference != null) {
                    xcontext.setWikiReference(wikiReference);
                }
            }
        }
    }
}

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
package org.xwiki.ircbot.internal.wiki;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link WikiIRCModel}.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultWikiIRCModel implements WikiIRCModel, WikiIRCBotConstants
{
    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceSerializer defaultEntityReferenceSerializer;

    @Override
    public XWikiDocument getDocument(DocumentReference reference) throws IRCBotException
    {
        XWikiDocument doc;
        XWikiContext xwikiContext = getXWikiContext();
        try {
            doc = xwikiContext.getWiki().getDocument(reference, xwikiContext);
        } catch (XWikiException e) {
            throw new IRCBotException(String.format("Unable to load document [%s]",
                this.defaultEntityReferenceSerializer.serialize(reference), e));
        }
        return doc;
    }

    @Override
    public XWikiDocument getConfigurationDocument() throws IRCBotException
    {
        return getDocument(new DocumentReference(getXWikiContext().getDatabase(), SPACE, CONFIGURATION_PAGE));
    }

    /**
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     * @throws IRCBotException if the XWiki Context is not available in the Execution Context
     */
    @Override
    public XWikiContext getXWikiContext() throws IRCBotException
    {
        XWikiContext xwikiContext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        if (xwikiContext == null) {
            throw new IRCBotException("The XWiki Context is not available in the Execution Contexte");
        }
        return xwikiContext;
    }
}

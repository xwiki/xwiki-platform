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
package org.xwiki.display.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;

/**
 * This is just a wrapper for the document displayer specified in the configuration. If there is no
 * {@link DocumentDisplayer} component registered with the {@link DisplayConfiguration#getDocumentDisplayerHint()} hint
 * then the default document displayer is used instead.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("configured")
@Singleton
public class ConfiguredDocumentDisplayer implements DocumentDisplayer
{
    /**
     * The object used for logging.
     */
    @Inject
    private Logger logger;

    /**
     * The display configuration.
     */
    @Inject
    private DisplayConfiguration displayConfiguration;

    /**
     * The component manager.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public XDOM display(DocumentModelBridge data, DocumentDisplayerParameters parameters)
    {
        return getDocumentDisplayer().display(data, parameters);
    }

    /**
     * Tries to lookup the document displayer with the configured hint, falling back on the default document displayer.
     * <p>
     * Note: There are two reasons why we don't do this in the component initialization phase:
     * <ul>
     * <li>it would trigger an infinite recursion if XWikiDocument class initializes the document displayer in the
     * constructor (or field initialization) because accessing the configuration triggers the load of the
     * XWiki.XWikiPreferences document which would trigger displayer initialization which would trigger the load of the
     * XWiki.XWikiPreferences document and so on..</li>
     * <li>we want to support the live change of the configured document displayer</li>
     * </ul>
     * 
     * @return the configured document displayer
     */
    private DocumentDisplayer getDocumentDisplayer()
    {
        String documentDisplayerHint = displayConfiguration.getDocumentDisplayerHint();
        try {
            return componentManager.getInstance(DocumentDisplayer.class, documentDisplayerHint);
        } catch (ComponentLookupException e) {
            logger.warn("Failed to lookup document displayer with hint [{}]. Using default document displayer.",
                documentDisplayerHint);
            try {
                return componentManager.getInstance(DocumentDisplayer.class);
            } catch (ComponentLookupException ex) {
                throw new RuntimeException("Failed to lookup default document displayer.", ex);
            }
        }
    }
}

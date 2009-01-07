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
package org.xwiki.officeimporter.internal.transformer;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterContext;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Abstract class for all html to xwiki transformers.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public abstract class AbstractHtmlToXWikiTransformer extends AbstractLogEnabled implements DocumentTransformer
{

    /**
     * {@link ComponentManager} used to lookup for other components.
     */
    protected ComponentManager componentManager;

    /**
     * {@inheritDoc}
     */
    public AbstractHtmlToXWikiTransformer(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * Cleans the html content provided.
     * 
     * @param html The html content.
     * @param context {@link OfficeImporterContext}
     * @return Filtered xhtml content.
     * @throws OfficeImporterException If it's not possible to lookup for appropriate cleaners.
     */
    protected String filterHTML(String html, OfficeImporterContext context)
        throws OfficeImporterException
    {
        // Load the html cleaner.
        HTMLCleaner htmlCleaner = null;
        try {
            htmlCleaner = (HTMLCleaner) componentManager.lookup(HTMLCleaner.ROLE, "openoffice");
        } catch (ComponentLookupException ex) {
            getLogger().error("Error while looking up for openoffice html cleaner component.", ex);
            throw new OfficeImporterException(ex);
        }
        // Perform cleaning & Filtering.
        Document document = htmlCleaner.clean(new StringReader(html), context.getOptions());
        // Finally strip the html envelop.
        XMLUtils.stripHTMLEnvelope(document);
        return XMLUtils.toString(document);
    }
}

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
package org.xwiki.image.style.internal.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.image.style.ImageStyleConfiguration;
import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.internal.configuration.source.ImageStyleConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Configuration of the Image Style application.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultImageStyleConfiguration implements ImageStyleConfiguration
{
    @Inject
    @Named(ImageStyleConfigurationSource.HINT)
    private ConfigurationSource configurationSource;

    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getDefaultStyle(String wikiName, String documentReference) throws ImageStyleException
    {
        return getConfiguration(wikiName, documentReference, "defaultStyle");
    }

    @Override
    public boolean getForceDefaultStyle(String wikiName, String documentReference) throws ImageStyleException
    {
        Integer forceDefaultStyle = getConfiguration(wikiName, documentReference, "forceDefaultStyle");
        return forceDefaultStyle != null && forceDefaultStyle == 1;
    }

    private <T> T getConfiguration(String wikiName, String documentReference, String forceDefaultStyle)
        throws ImageStyleException
    {
        try {
            this.contextManager.pushContext(new ExecutionContext(), false);
            XWikiContext context = this.xcontextProvider.get();
            context.setWikiId(wikiName);
            if (documentReference != null) {
                DocumentReference resolvedDocumentReference =
                    this.documentReferenceResolver.resolve(documentReference, context);
                XWikiDocument doc = context.getWiki().getDocument(resolvedDocumentReference, context);
                context.setDoc(doc);
            }
            return this.configurationSource.getProperty(forceDefaultStyle);
        } catch (ExecutionContextException e) {
            throw new ImageStyleException("Failed to initialize the execution context", e);
        } catch (XWikiException e) {
            throw new ImageStyleException(String.format("Failed to resolved document [%s]", documentReference), e);
        } finally {
            this.contextManager.popContext();
        }
    }
}

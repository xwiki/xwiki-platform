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
package org.xwiki.lesscss.internal.compiler;

import java.io.StringWriter;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.LESSConfiguration;
import org.xwiki.lesscss.internal.cache.CachedCompilerInterface;
import org.xwiki.lesscss.internal.compiler.less4j.Less4jCompiler;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.WikiLESSResourceReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Compile a LESS resource in a particular context (@see org.xwiki.lesscss.compiler.IntegratedLESSCompiler}. To be used
 * with AbstractCachedCompiler.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CachedLESSCompiler.class)
@Singleton
public class CachedLESSCompiler implements CachedCompilerInterface<String>, Initializable
{
    /**
     * The name of the file holding the main skin style (on which Velocity is always executed).
     */
    public static final String MAIN_SKIN_STYLE_FILENAME = "style.less.vm";

    private static final String SKIN_CONTEXT_KEY = "skin";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Less4jCompiler less4JCompiler;

    @Inject
    private LESSConfiguration lessConfiguration;

    @Inject
    private TemplateManager templateManager;

    private Semaphore semaphore;

    @Override
    public void initialize() throws InitializationException
    {
        this.semaphore = new Semaphore(lessConfiguration.getMaximumSimultaneousCompilations(), true);
    }

    @Override
    public String compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean useLESS, String skin) throws LESSCompilerException
    {
        StringWriter source = new StringWriter();

        try {
            semaphore.acquire();

            // Resolve the LESS resource only once so that the evaluated content and its author always match.
            TemplateContent templateContent = getTemplateContent(lessResourceReference, includeSkinStyle, skin);

            if (includeSkinStyle) {
                // Add the import line to the LESS resource.
                // We import this file to be able to use variables and mix-ins defined in it.
                // But we don't want it in the output.
                source.write(String.format("@import (reference) \"%s\";%s", MAIN_SKIN_STYLE_FILENAME,
                    System.lineSeparator()));
            }

            // Get the content of the LESS resource
            source.write(templateContent.getContent());

            // Parse the LESS content with Velocity
            String lessCode = source.toString();
            if (useVelocity) {
                lessCode = evaluate(lessResourceReference.toString(), lessCode, skin,
                    templateContent.getAuthorReference(), templateContent.getDocumentReference());
            }

            // Compile the LESS code
            if (useLESS) {
                return less4JCompiler.compile(lessCode, skin, lessConfiguration.isGenerateInlineSourceMaps());
            }

            // Otherwise return the raw LESS code
            return lessCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new LESSCompilerException("Current thread has been interrupted", e);
        } catch (Exception e) {
            throw new LESSCompilerException(
                String.format("Failed to compile the resource [%s] with LESS.", lessResourceReference), e);
        } finally {
            semaphore.release();
        }
    }

    private TemplateContent getTemplateContent(LESSResourceReference lessResourceReference, boolean includeSkinStyle,
        String skin) throws Exception
    {
        if (lessResourceReference instanceof LESSSkinFileResourceReference skinFileResourceReference) {
            // The resolved template carries its own author and document: for a wiki skin, the file is an attachment
            // or an object property of the skin document, while filesystem skin templates have their own author.
            return skinFileResourceReference.getTemplateContent(skin);
        }

        // Resources other than skin files contribute their content only when the main skin style is included.
        String content = includeSkinStyle ? lessResourceReference.getContent(skin) : "";

        // When the origin of the code is unknown, it has no author and is thus evaluated with the guest user rights.
        DocumentReference authorReference = null;
        DocumentReference documentReference = null;
        if (lessResourceReference instanceof WikiLESSResourceReference wikiLESSResourceReference) {
            authorReference = wikiLESSResourceReference.getAuthorReference();
            documentReference = wikiLESSResourceReference.getDocumentReference();
        }

        return this.templateManager
            .createStringTemplate(lessResourceReference.toString(), content, authorReference, documentReference)
            .getContent();
    }

    private String evaluate(String id, String source, String skin, DocumentReference authorReference,
        DocumentReference documentReference) throws Exception
    {
        // Get the XWiki object
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        String currentSkin = xwiki.getSkin(xcontext);

        try {
            // Trick: change the current skin in order to compile the LESS file as if the specified skin
            // was the current skin
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, skin);
            }

            Template template =
                this.templateManager.createStringTemplate(id, source, authorReference, documentReference);

            StringWriter result = new StringWriter();
            this.templateManager.renderNoException(template, result);
            return result.toString();
        } finally {
            // Reset the current skin to the old value
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, currentSkin);
            }
        }
    }
}

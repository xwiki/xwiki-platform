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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation for {@link org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory}.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultColorThemeReferenceFactory implements ColorThemeReferenceFactory
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public ColorThemeReference createReference(String colorThemeName) throws LESSCompilerException
    {
        // Get the XWiki Object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        DocumentReference colorThemeDocRef =
            documentReferenceResolver.resolve(colorThemeName, new WikiReference(currentWikiId));

        try {
            XWikiDocument colorThemeDoc = xwiki.getDocument(colorThemeDocRef, xcontext);

            if (!colorThemeDoc.isNew()) {
                // Is there any color theme?
                DocumentReference colorThemeClassRef = new DocumentReference(
                    colorThemeDocRef.getWikiReference().getName(), "ColorThemes", "ColorThemeClass");
                if (colorThemeDoc.getXObjectSize(colorThemeClassRef) > 0) {
                    return createReference(colorThemeDocRef);
                }

                // Is there any flamingo theme?
                DocumentReference flamingoThemeClassRef = new DocumentReference(
                    colorThemeDocRef.getWikiReference().getName(), "FlamingoThemesCode", "ThemeClass");
                if (colorThemeDoc.getXObjectSize(flamingoThemeClassRef) > 0) {
                    return createReference(colorThemeDocRef);
                }
            }
        } catch (XWikiException e) {
            throw new LESSCompilerException(String.format("Unable to read document [%s]", colorThemeDocRef));
        }

        // Not an XWiki page so probably a file system color theme
        return new NamedColorThemeReference(colorThemeName);
    }

    @Override
    public ColorThemeReference createReference(DocumentReference documentReference)
    {
        return new DocumentColorThemeReference(documentReference, entityReferenceSerializer);
    }
}

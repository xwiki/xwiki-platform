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
package org.xwiki.lesscss.internal.skin;

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
 * Default implementation for {@link org.xwiki.lesscss.internal.skin.SkinReferenceFactory}.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultSkinReferenceFactory implements SkinReferenceFactory
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
    public SkinReference createReference(String skinName) throws LESSCompilerException
    {
        // Get the XWIki Object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        DocumentReference skinDocRef = documentReferenceResolver.resolve(skinName, new WikiReference(currentWikiId));

        XWikiDocument skinDoc;
        try {
            skinDoc = xwiki.getDocument(skinDocRef, xcontext);
        } catch (XWikiException e) {
            throw new LESSCompilerException(String.format("Unable to read document [%s]", skinDocRef), e);
        }

        if (!skinDoc.isNew()) {
            DocumentReference skinClassDocRef =
                new DocumentReference(skinDocRef.getWikiReference().getName(), "XWiki", "XWikiSkins");

            if (skinDoc.getXObjectSize(skinClassDocRef) > 0) {
                return createReference(skinDocRef);
            }
        }

        return new FSSkinReference(skinName);
    }

    @Override
    public SkinReference createReference(DocumentReference documentReference)
    {
        return new DocumentSkinReference(documentReference, entityReferenceSerializer);
    }
}

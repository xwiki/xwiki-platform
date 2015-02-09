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
package org.xwiki.lesscss.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Component to get the directory of the base skin of any skin.
 *
 * @since 6.2.6
 * @version $Id$
 */
@Component(roles = SkinDirectoryGetter.class)
@Singleton
public class SkinDirectoryGetter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Returns the parent filesystem skin (if any) of the specified skin.
     *  
     * @param skin name of the skin
     * @return the name of the filesystem skin
     * * 
     * @throws LESSCompilerException if problem occurs
     */
    public String getFileSystemSkin(String skin) throws LESSCompilerException
    {
        return getSkinDirectory(skin, new ArrayList<String>());    
    }

    /**
     * @param skin name of the skin
     * @return the directory of the base skin of the specified skin. The directory is relative to the webapp one.
     * @throws LESSCompilerException if problem occurs
     */
    public String getSkinDirectory(String skin) throws LESSCompilerException
    {
        // Is the skin a Wiki Document?
        return "/skins/" + getFileSystemSkin(skin);
    }

    private String getSkinDirectory(String skin, List<String> alreadyVisitedSkins) throws LESSCompilerException
    {
        // Avoid infinite loop
        if (alreadyVisitedSkins.contains(skin)) {
            throw new LESSCompilerException(String.format("Infinite loop of 'baseskin' dependencies [%s].",
                    alreadyVisitedSkins.toString()), null);
        }
        alreadyVisitedSkins.add(skin);

        // Get the xwiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Is the skin a Wiki Document?
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();
        DocumentReference skinDocRef = documentReferenceResolver.resolve(skin, new WikiReference(currentWikiId));
        if (skinDocRef != null && xwiki.exists(skinDocRef, xcontext)) {
            // Skin class
            DocumentReference skinClass = new DocumentReference(skinDocRef.getWikiReference().getName(),
                    "XWiki", "XWikiSkins");
            // Verify that the document is a skin by checking if a skin object is attached or not
            try {
                XWikiDocument skinDoc = xwiki.getDocument(skinDocRef, xcontext);
                BaseObject skinObj = skinDoc.getXObject(skinClass);
                if (skinObj != null) {
                    // Get the "baseskin" property of the skin
                    String baseSkin = skinObj.getStringValue("baseskin");
                    if (StringUtils.isBlank(baseSkin)) {
                        throw new LESSCompilerException(
                                String.format("Failed to get the base skin of the skin [%s].", skin),
                                null);
                    }
                    // Recursively get the skin directory from the baseskin
                    return getSkinDirectory(baseSkin, alreadyVisitedSkins);
                }
            } catch (XWikiException e) {
                throw new LESSCompilerException(String.format("Failed to get the document [%s].", skinDocRef), e);
            }
        }
        // If not, we assume it is a skin on the filesystem
        return skin;
    }
}

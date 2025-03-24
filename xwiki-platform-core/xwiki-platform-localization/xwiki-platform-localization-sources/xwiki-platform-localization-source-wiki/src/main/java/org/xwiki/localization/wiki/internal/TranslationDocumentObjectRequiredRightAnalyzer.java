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
package org.xwiki.localization.wiki.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Required right analyzer for instances of XWiki.TranslationDocumentClass.
 *
 * @version $Id$
 * @since 15.10
 */
@Component
@Singleton
@Named("XWiki.TranslationDocumentClass")
public class TranslationDocumentObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> xObjectDisplayerProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WikiTranslationConfiguration configuration;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object)
    {
        String scope = object.getStringValue("scope");

        RequiredRight requiredRight;
        String translationKey;
        switch (scope) {
            case "GLOBAL":
                requiredRight = RequiredRight.PROGRAM;
                translationKey = "localization.requiredrights.translationDocument.global";
                break;

            case "WIKI":
                requiredRight = RequiredRight.WIKI_ADMIN;
                translationKey = "localization.requiredrights.translationDocument.wiki";
                break;

            case "USER":
                if (this.configuration.isRestrictUserTranslations()) {
                    requiredRight = RequiredRight.SCRIPT;
                    translationKey = "localization.requiredrights.translationDocument.user";
                } else {
                    return List.of();
                }
                break;

            default:
                return List.of();
        }

        XWikiDocument document = this.contextProvider.get().getDoc();
        return List.of(
            new RequiredRightAnalysisResult(
                object.getReference(),
                this.translationMessageSupplierProvider.get(translationKey),
                this.xObjectDisplayerProvider.get(object),
                List.of(requiredRight)),
            new RequiredRightAnalysisResult(
                document.getDocumentReference(),
                this.translationMessageSupplierProvider.get(translationKey + ".content"),
                this.stringCodeBlockSupplierProvider.get(document.getContent()),
                List.of(requiredRight))
            );
    }
}

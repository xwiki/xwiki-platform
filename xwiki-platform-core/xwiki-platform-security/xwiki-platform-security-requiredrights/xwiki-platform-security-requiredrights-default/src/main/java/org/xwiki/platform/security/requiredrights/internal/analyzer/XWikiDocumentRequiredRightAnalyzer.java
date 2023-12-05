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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class XWikiDocumentRequiredRightAnalyzer implements RequiredRightAnalyzer<XWikiDocument>
{
    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private RequiredRightAnalyzer<BaseObject> objectRequiredRightAnalyzer;

    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(XWikiDocument document) throws RequiredRightsException
    {
        // Analyze the content
        try {
            // Push the document into the context such that we, e.g., get the correct context wiki with the correct
            // wiki macros etc.
            return this.documentContextExecutor.call(() ->
            {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();

                // Analyze the title
                if (this.velocityDetector.containsVelocityScript(document.getTitle())) {
                    result.add(new RequiredRightAnalysisResult(
                        document.getDocumentReferenceWithLocale(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title"),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title.description",
                            document.getTitle()),
                        List.of(RequiredRight.MAYBE_SCRIPT, RequiredRight.MAYBE_PROGRAM)
                    ));
                }

                // Analyze the content
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(document.getXDOM()));

                // Analyze XObjects on the Root locale version of the document
                XWikiDocument rootLocaleDocument = document;
                if (document.getLocale() != null && !document.getLocale().equals(Locale.ROOT)) {
                    XWikiContext context = this.contextProvider.get();
                    rootLocaleDocument = context.getWiki().getDocument(document.getDocumentReference(), context);
                }

                for (List<BaseObject> baseObjects : rootLocaleDocument.getXObjects().values()) {
                    for (BaseObject object : baseObjects) {
                        result.addAll(this.objectRequiredRightAnalyzer.analyze(object));
                    }
                }

                return result;
            }, document);
        } catch (Exception e) {
            throw new RequiredRightsException("Error...", e);
        }
    }
}

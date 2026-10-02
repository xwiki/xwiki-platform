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
package org.xwiki.edit.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Analyzes the rights required by the editors defined in wiki pages through {@code XWiki.EditorClass} objects.
 *
 * @version $Id$
 * @since 16.10.20
 * @since 17.10.15
 * @since 18.4.8
 * @since 18.9.0
 */
@Component
@Named(EditorRequiredRightsAnalyzer.EDITOR_CLASS)
@Singleton
public class EditorRequiredRightsAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    /**
     * The serialized reference of the class used to define editors in wiki pages.
     */
    public static final String EDITOR_CLASS = "XWiki.EditorClass";

    private static final String TRANSLATION_PREFIX = "platform.edit.editor.requiredrights.";

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringBlockSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @Inject
    private ContentParser parser;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result = new ArrayList<>();

        BaseObjectReference reference = object.getReference();

        // Registering an editor for more than the current user requires the corresponding rights.
        switch (WikiComponentScope.fromString(object.getStringValue("scope"))) {
            case GLOBAL -> result.add(new RequiredRightAnalysisResult(reference,
                this.translationBlockSupplierProvider.get(TRANSLATION_PREFIX + "global"),
                this.objectBlockSupplierProvider.get(object), List.of(RequiredRight.PROGRAM)));
            case WIKI -> result.add(new RequiredRightAnalysisResult(reference,
                this.translationBlockSupplierProvider.get(TRANSLATION_PREFIX + "wiki"),
                this.objectBlockSupplierProvider.get(object), List.of(RequiredRight.WIKI_ADMIN)));
            default -> {
                // Editors registered for the current user don't require any specific right.
            }
        }

        // The editor code is executed with the rights of the author of the editor document.
        String code = object.getStringValue("code");
        if (StringUtils.isNotBlank(code)) {
            try {
                XDOM xdom = this.parser.parse(code, object.getOwnerDocument().getSyntax(),
                    object.getDocumentReference());
                xdom.getMetaData().addMetaData("entityReference", reference);
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(xdom));
            } catch (Exception e) {
                result.add(new RequiredRightAnalysisResult(reference,
                    this.translationBlockSupplierProvider.get(TRANSLATION_PREFIX + "codeError",
                        ExceptionUtils.getRootCauseMessage(e)),
                    this.stringBlockSupplierProvider.get(code), List.of(RequiredRight.MAYBE_PROGRAM)));
            }
        }

        return result;
    }
}

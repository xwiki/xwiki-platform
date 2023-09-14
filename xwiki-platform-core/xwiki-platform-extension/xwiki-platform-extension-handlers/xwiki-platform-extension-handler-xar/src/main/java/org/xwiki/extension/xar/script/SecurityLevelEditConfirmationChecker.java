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
package org.xwiki.extension.xar.script;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.xar.internal.security.XarSecurityTool;
import org.xwiki.extension.xar.security.ProtectionLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Checks if the configured security level allows for the edition of the current document. This can lead to a strict
 * block to the edition of the document, or to a warning, according to the documentation.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named("securityLevel")
public class SecurityLevelEditConfirmationChecker implements EditConfirmationChecker
{
    @Inject
    private XarSecurityTool securityTool;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Override
    public Optional<EditConfirmationCheckerResult> check(boolean editForced)
    {
        DocumentReference userReference = this.xcontextProvider.get().getUserReference();
        XWikiDocument tdoc = (XWikiDocument) this.xcontextProvider.get().get("tdoc");
        DocumentReference documentReference = tdoc.getDocumentReferenceWithLocale();
        ProtectionLevel protectionLevel =
            this.securityTool.getProtectionLevel(Right.EDIT, userReference, documentReference);

        Optional<EditConfirmationCheckerResult> result;
        if (protectionLevel == ProtectionLevel.DENY) {
            result = Optional.of(new EditConfirmationCheckerResult(renderMessage(), true));
        } else if (protectionLevel != ProtectionLevel.NONE && !editForced) {
            result = Optional.of(new EditConfirmationCheckerResult(renderMessage(), false));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    private Block renderMessage()
    {
        return this.templateManager.executeNoException("extension/handler/xar/securityLevelEditConfirmationChecker.vm");
    }
}

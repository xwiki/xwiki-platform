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
package org.xwiki.internal.migration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Apply {@link InvitationInternalDocumentParameterEscapingFixer} on the documents queued by
 * {@link R150000000XWIKI20285DataMigration} if applicable, and log the skipped ones. This translation key is initially
 * introduced by the invitation application but the fix is localed in oldcore so that the fix is applied on pages even
 * if the invitation application has been uninstalled.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component
@Singleton
@Named(InvitationInternalDocumentParameterEscapingTaskConsumer.HINT)
public class InvitationInternalDocumentParameterEscapingTaskConsumer implements TaskConsumer
{
    /**
     * This task consumer hint.
     */
    public static final String HINT = "internal-document-parameter-escaping";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private InvitationInternalDocumentParameterEscapingFixer invitationInternalDocumentParameterEscapingFixer;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        try {
            XWikiContext context = this.contextProvider.get();
            task(context.getWiki().getDocument(documentReference, context));
        } catch (XWikiException e) {
            throw new IndexException(String.format("Failed to resolve document [%s]", documentReference), e);
        }
    }

    private void task(XWikiDocument document)
    {
        Syntax syntax = document.getSyntax();
        if (List.of(Syntax.XWIKI_2_1, Syntax.XWIKI_2_0).contains(syntax)) {
            try {
                this.invitationInternalDocumentParameterEscapingFixer.fix(document.getContent(), document.getSyntax())
                    .ifPresent(content -> {
                        document.setContent(content);
                        try {
                            XWikiContext context = this.contextProvider.get();
                            context.getWiki().saveDocument(document, "Automatic bad escaping fix.", true, context);
                            this.logger.info("[{}] successfully fixed.", document);
                        } catch (XWikiException e) {
                            this.logger.error("Failed to save document [{}]", document, e);
                        }
                    });
            } catch (Exception e) {
                this.logger.error("Unexpected error while fixing [{}]", document, e);
            }
        } else {
            this.logger.warn(
                "[{}] skipped because escaping for syntax [{}] is not supported. It is advised to review this file.",
                document, syntax);
        }
    }
}

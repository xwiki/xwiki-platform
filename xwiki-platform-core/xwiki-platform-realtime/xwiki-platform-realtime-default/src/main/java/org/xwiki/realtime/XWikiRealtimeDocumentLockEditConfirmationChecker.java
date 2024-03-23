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
package org.xwiki.realtime;

import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.model.validation.edit.XWikiDocumentLockEditConfirmationChecker;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Checks if a a realtime session is running, if not: check if the document is locked.
 *
 * @version $Id$
 * @since 16.2.0
 * @since 16.3.0RC1
 */
@Singleton
@Named("documentLock")
@Priority(3000)
public class XWikiRealtimeDocumentLockEditConfirmationChecker extends XWikiDocumentLockEditConfirmationChecker
{
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private RealtimeEditorManager realtimeEditorManager;

    @Override
    public Optional<EditConfirmationCheckerResult> check()
    {
        XWikiContext context = this.xwikiContextProvider.get();
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        if (realtimeEditorManager.sessionIsActive(tdoc.getDocumentReference(), tdoc.getRealLocale().toLanguageTag(),
            realtimeEditorManager.getSelectedEditor())) {
            return Optional.empty();
        }
        return super.check();
    }
}

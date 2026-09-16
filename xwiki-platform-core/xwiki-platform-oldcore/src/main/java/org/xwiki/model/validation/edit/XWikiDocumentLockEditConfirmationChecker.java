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
package org.xwiki.model.validation.edit;

import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.UnlockRule;
import org.xwiki.edit.EditorManager;
import org.xwiki.internal.document.lock.DefaultLockContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.edit.EditModeResolver;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Checks if a document is locked by another user before editing, unless an {@link UnlockRule} allows taking over the
 * lock without asking the user to confirm.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named("documentLock")
@Priority(2000)
public class XWikiDocumentLockEditConfirmationChecker implements EditConfirmationChecker
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private EditModeResolver editModeResolver;

    @Inject
    private EditorManager editManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public Optional<EditConfirmationCheckerResult> check()
    {
        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        Optional<EditConfirmationCheckerResult> result;
        if (new Document(tdoc, context).getLocked() && !canUnlock(tdoc)) {
            result = Optional.of(new EditConfirmationCheckerResult(renderMessage(), false));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    private boolean canUnlock(XWikiDocument tdoc)
    {
        LockContext lockContext = new DefaultLockContext(tdoc, this.editModeResolver, this.editManager);
        return getUnlockRules().stream().anyMatch(unlockRule -> unlockRule.canUnlock(lockContext));
    }

    private List<UnlockRule> getUnlockRules()
    {
        // We look up the unlock rules for each check because they can be provided by extensions that are installed
        // after this component is instantiated, and by wiki components.
        try {
            return this.componentManagerProvider.get().getInstanceList(UnlockRule.class);
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to look up the unlock rules. Root cause is [{}].", getRootCauseMessage(e));
            return List.of();
        }
    }

    private Block renderMessage()
    {
        return this.templateManager.executeNoException("edit_confirmation/lock.vm");
    }
}

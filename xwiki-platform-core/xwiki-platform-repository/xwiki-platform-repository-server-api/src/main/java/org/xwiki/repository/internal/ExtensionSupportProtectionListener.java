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
package org.xwiki.repository.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserCreatingDocumentEvent;
import com.xpn.xwiki.internal.event.UserEvent;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Make sure only allowed people can modify the support plan of the extensions.
 * 
 * @version $Id$
 * @since 16.8.0RC1
 */
@Component
@Named(ExtensionSupportProtectionListener.NAME)
@Singleton
public class ExtensionSupportProtectionListener extends AbstractEventListener implements EventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "ExtensionSupportProtectionListener";

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private AuthorizationManager authorization;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public ExtensionSupportProtectionListener()
    {
        super(NAME, new UserCreatingDocumentEvent(), new UserUpdatingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument nextDocument = (XWikiDocument) source;

        XWikiDocument previousDocument = nextDocument.getOriginalDocument();

        BaseObject previousExtensionObject = previousDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
        BaseObject nextExtensionObject = nextDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        // If the extension object was removed we cannot really filter anything
        if (nextExtensionObject != null) {
            // Get the new support plans
            List<String> nextSupportPlans =
                nextExtensionObject.getListValue(XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS);

            // Get the previous support plans
            List<String> previousSupportPlans;
            if (previousExtensionObject != null) {
                previousSupportPlans =
                    previousExtensionObject.getListValue(XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS);
            } else {
                previousSupportPlans = List.of();
            }

            DocumentReference extensionReference = nextDocument.getDocumentReference();
            DocumentReference userReference = ((UserEvent) event).getUserReference();

            // Make sure the list is modifiable
            nextSupportPlans = new ArrayList<>(nextSupportPlans);
            boolean updated = false;

            // Remove all the plans the current user is not allowed to add
            List<String> addedSupportPlans = ListUtils.subtract(nextSupportPlans, previousSupportPlans);
            for (String supportPlan : addedSupportPlans) {
                if (!isAllowed(userReference, supportPlan, extensionReference)) {
                    this.logger.warn("The user [{}] tried to add the support plan [{}] to extension [{}]. Reverted.",
                        userReference, supportPlan, extensionReference);

                    // Remove the support plan
                    nextSupportPlans.remove(supportPlan);
                    updated = true;
                }
            }

            // Restore all the plans the current user is not allowed to remove
            List<String> removedSupportPlans = ListUtils.subtract(previousSupportPlans, nextSupportPlans);
            for (String supportPlan : removedSupportPlans) {
                if (!isAllowed(userReference, supportPlan, extensionReference)) {
                    this.logger.warn(
                        "The user [{}] tried to remove the support plan [{}] from extension [{}]. Reverted.",
                        userReference, supportPlan, extensionReference);

                    // Add back the support plan
                    nextSupportPlans.add(supportPlan);
                    updated = true;
                }
            }

            if (updated) {
                nextExtensionObject.setStringListValue(XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS,
                    nextSupportPlans);
            }
        }
    }

    private boolean isAllowed(DocumentReference userReference, String supportPlan, DocumentReference extensionReference)
    {
        return this.authorization.hasAccess(Right.EDIT, userReference,
            this.resolver.resolve(supportPlan, extensionReference));
    }
}

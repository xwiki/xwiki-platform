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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserCreatingDocumentEvent;
import com.xpn.xwiki.internal.event.UserEvent;
import com.xpn.xwiki.internal.mandatory.XWikiGlobalRightsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Make sure new supporters spaces are protected by default.
 * 
 * @version $Id$
 * @since 16.8.0RC1
 */
@Component
@Named(ExtensionSupporterProtectionListener.NAME)
@Singleton
public class ExtensionSupporterProtectionListener extends AbstractEventListener implements EventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "ExtensionSupporterProtectionListener";

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactwikiSerializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public ExtensionSupporterProtectionListener()
    {
        super(NAME, new UserCreatingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        DocumentReference documentReference = document.getDocumentReference();

        // Check if it's a new supporter space
        if (event instanceof UserCreatingDocumentEvent
            && documentReference.getName().equals(XWiki.DEFAULT_SPACE_HOMEPAGE)
            && document.getXObject(XWikiRepositoryModel.EXTENSIONSUPPORTER_CLASSREFERENCE) != null) {
            DocumentReference userReference = ((UserEvent) event).getUserReference();
            DocumentReference preferenceReference =
                new DocumentReference("WebPreferences", documentReference.getLastSpaceReference());

            XWikiContext xcontext = (XWikiContext) data;

            try {
                XWikiDocument preferenceDocument = xcontext.getWiki().getDocument(preferenceReference, xcontext);

                // Make sure only the user who created the space is allowed to edit it by default
                BaseObject preferenceRightObject =
                    preferenceDocument.newXObject(XWikiGlobalRightsDocumentInitializer.CLASS_REFERENCE, xcontext);
                preferenceRightObject.set("levels", "edit", xcontext);
                preferenceRightObject.set("users",
                    this.compactwikiSerializer.serialize(userReference, documentReference), xcontext);
                preferenceRightObject.set("allow", 1, xcontext);

                xcontext.getWiki().saveDocument(preferenceDocument, xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to load the preference document [{}]", preferenceReference, e);
            }
        }
    }
}

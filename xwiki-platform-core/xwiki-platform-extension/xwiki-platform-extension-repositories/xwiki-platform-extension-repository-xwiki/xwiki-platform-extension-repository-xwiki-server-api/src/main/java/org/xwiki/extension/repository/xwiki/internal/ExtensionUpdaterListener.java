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
package org.xwiki.extension.repository.xwiki.internal;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;

@Component("ExtensionUpdaterListener")
@Singleton
public class ExtensionUpdaterListener implements EventListener
{
    /**
     * The reference to match class extension version class on whatever wiki.
     */
    private static final RegexEntityReference EXTENSIONVERSION_REFERENCE =
        new RegexEntityReference(Pattern.compile(XWikiRepositoryModel.PROP_VERSION_VERSION),
            EntityType.OBJECT_PROPERTY, new RegexEntityReference(Pattern.compile(".*:"
                + XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME + "\\[\\d*\\]"), EntityType.OBJECT));

    /**
     * Listened events.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new XObjectPropertyAddedEvent(
        EXTENSIONVERSION_REFERENCE), new XObjectPropertyUpdatedEvent(EXTENSIONVERSION_REFERENCE),
        new XObjectPropertyDeletedEvent(EXTENSIONVERSION_REFERENCE));

    /**
     * Used to find last version.
     */
    @Inject
    private VersionManager versionManager;

    /**
     * Get the reference of the class in the current wiki.
     */
    @Inject
    @Named("default/reference")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "ExtensionUpdaterListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        String lastVersion = findLastVersion(document);

        DocumentReference extensionClassReference =
            getClassReference(document, XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        BaseObject extensionObject = document.getXObject(extensionClassReference);

        if (!StringUtils.equals(lastVersion,
            extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION))) {
            try {
                // FIXME: We can't save directly the provided document coming from the event
                document = context.getWiki().getDocument(document, context);
                extensionObject = document.getXObject(extensionObject.getReference());

                extensionObject.setStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion);

                context.getWiki().saveDocument(document, "Update extension last version", context);
            } catch (XWikiException e) {
                this.logger.error("Failed to update extension last version", e);
            }
        }
    }

    private DocumentReference getClassReference(XWikiDocument document, EntityReference localReference)
    {
        return this.referenceResolver.resolve(localReference, document.getDocumentReference().getWikiReference());
    }

    /**
     * Compare all version located in a document to find the last one.
     * 
     * @param document the extension document
     * @return the last version
     */
    private String findLastVersion(XWikiDocument document)
    {
        DocumentReference versionClassReference =
            getClassReference(document, XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);

        List<BaseObject> versionObjects = document.getXObjects(versionClassReference);

        String lastVersion = null;
        for (BaseObject versionObject : versionObjects) {
            String version = versionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION);
            if (version != null) {
                if (lastVersion == null || this.versionManager.compareVersions(version, lastVersion) > 0) {
                    lastVersion = version;
                }
            }
        }

        return lastVersion;
    }
}

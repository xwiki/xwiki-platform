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
package org.xwiki.uiextension.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Initializes the XClasses required by {@link WikiUIExtensionComponentBuilder}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("wikiUIExtensionComponentBuilderEventListener")
@Singleton
public class WikiUIExtensionComponentBuilderEventListener implements EventListener, WikiUIExtensionConstants
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Our execution. Needed to access the XWiki context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to serialize wiki pages reference in the log.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    /**
     * Document reference resolver used to retrieve the UI Extension class from the current wiki.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new ApplicationReadyEvent(), new WikiReadyEvent());
    }

    @Override
    public String getName()
    {
        return "wikiUIExtensionComponentBuilderEventListener";
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        try {
            this.installOrUpgradeUIExtensionClasses();
        } catch (XWikiException e) {
            this.logger.error("Failed to install or update UI Extension XClasses", e);
        }
    }

    /**
     * Sets metadata in the given class document.
     *
     * @param doc the document holding the class
     * @param title the title to set in the class document
     * @return true if the given document has been modified and needs to be saved
     */
    private boolean setClassesDocumentFields(XWikiDocument doc, String title)
    {
        boolean needsUpdate = false;

        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator(XWikiRightService.SUPERADMIN_USER);
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthorReference(doc.getCreatorReference());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle(title);
        }
        if (StringUtils.isBlank(doc.getContent())
            || !Syntax.XWIKI_2_0.equals(doc.getSyntax())) {
            needsUpdate = true;
            doc.setContent("{{include reference=\"XWiki.ClassSheet\" /}}");
            doc.setSyntax(Syntax.XWIKI_2_0);
        }
        if (!doc.isHidden()) {
            needsUpdate = true;
            doc.setHidden(true);
        }

        return needsUpdate;
    }

    /**
     * Creates or upgrade the XClass allowing to define UI extensions in the wiki.
     *
     * @throws XWikiException when the creation or upgrade fails, for example because of an exception in the storage.
     */
    public void installOrUpgradeUIExtensionClasses() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        // Install or Upgrade XWiki.UIExtensionClass
        XWikiDocument doc = xcontext.getWiki().getDocument(documentReferenceResolver.resolve(UI_EXTENSION_CLASS),
            xcontext);
        BaseClass bclass = doc.getXClass();

        boolean needsUpdate = false;

        needsUpdate |= setClassesDocumentFields(doc, "UI Extension Class");
        needsUpdate |= bclass.addTextField(EXTENSION_POINT_ID_PROPERTY, "Extension Point ID", 30);
        needsUpdate |= bclass.addTextField(ID_PROPERTY, "Extension ID", 30);
        needsUpdate |= bclass.addTextAreaField(CONTENT_PROPERTY, "Extension Content", 40, 10);
        needsUpdate |= bclass.addTextAreaField(PARAMETERS_PROPERTY, "Extension Parameters", 40, 10);
        needsUpdate |= bclass.addStaticListField(SCOPE_PROPERTY, "Extension Scope", 1, false,
                    "wiki=Current Wiki|user=Current User|global=Global", "select");
        if (needsUpdate) {
            if (doc.isNew()) {
                doc.setParent("XWiki.WebHome");
            }
            xcontext.getWiki().saveDocument(doc, xcontext);
        }
    }

    /**
     * @return the XWikiContext extracted from the execution.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}

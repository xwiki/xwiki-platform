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
package org.xwiki.component.wiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initializes the XClasses required by {@link DefaultWikiComponentBuilder}.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("defaultWikiComponentBuilderEventListener")
@Singleton
public class DefaultWikiComponentBuilderEventListener implements EventListener, WikiComponentConstants
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

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new ApplicationReadyEvent());
    }

    @Override
    public String getName()
    {
        return "defaultWikiComponentBuilderEventListener";
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        try {
            this.installOrUpdateComponentXClass();
            this.installOrUpdateComponentRequirementXClass();
            this.installOrUpdateComponentMethodXClass();
            this.installOrUpdateComponentInterfaceXClass();
        } catch (XWikiException e) {
            this.logger.error("Failed to install or update wiki component XClasses", e);
        }
    }

    /**
     * Verify that the {@link #INTERFACE_CLASS} exists and is up-to-date (act if not).
     *
     * @throws com.xpn.xwiki.XWikiException on failure
     */
    private void installOrUpdateComponentInterfaceXClass() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(INTERFACE_CLASS, xcontext);

        BaseClass bclass = doc.getXClass();
        bclass.setName(INTERFACE_CLASS);

        boolean needsUpdate = false;

        needsUpdate |= this.initializeXClassDocumentMetadata(doc, "Wiki Component Implements Interface XWiki Class");
        needsUpdate |= bclass.addTextField(INTERFACE_NAME_FIELD, "Interface Qualified Name", 30);

        if (needsUpdate) {
            this.update(doc);
        }
    }

    /**
     * Verify that the {@link #COMPONENT_CLASS} exists and is up-to-date (act if not).
     *
     * @throws XWikiException on failure
     */
    private void installOrUpdateComponentXClass() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(COMPONENT_CLASS, xcontext);

        BaseClass bclass = doc.getXClass();
        bclass.setName(COMPONENT_CLASS);

        boolean needsUpdate = false;

        needsUpdate |= initializeXClassDocumentMetadata(doc, "Wiki Component XWiki Class");
        needsUpdate |= bclass.addTextField(COMPONENT_ROLE_TYPE_FIELD, "Component Role Type", 30);
        needsUpdate |= bclass.addTextField(COMPONENT_ROLE_HINT_FIELD, "Component Role Hint", 30);

        if (needsUpdate) {
            this.update(doc);
        }
    }

    /**
     * Verify that the {@link #DEPENDENCY_CLASS} exists and is up-to-date (act if not).
     *
     * @throws XWikiException on failure
     */
    private void installOrUpdateComponentRequirementXClass() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(DEPENDENCY_CLASS, xcontext);

        BaseClass bclass = doc.getXClass();
        bclass.setName(DEPENDENCY_CLASS);

        boolean needsUpdate = false;

        needsUpdate |= this.initializeXClassDocumentMetadata(doc, "Wiki Component Dependency XWiki Class");
        needsUpdate |= bclass.addTextField(COMPONENT_ROLE_TYPE_FIELD, "Dependency Role Type", 30);
        needsUpdate |= bclass.addTextField(COMPONENT_ROLE_HINT_FIELD, "Dependency Role Hint", 30);
        needsUpdate |= bclass.addTextField("bindingName", "Binding name", 30);
        needsUpdate |= bclass.addStaticListField("type", "Requirement type", "single=Single|list=List|map=Map");

        if (needsUpdate) {
            this.update(doc);
        }
    }

    /**
     * Verify that the {@link #METHOD_CLASS} exists and is up-to-date (act if not).
     *
     * @throws XWikiException on failure
     */
    private void installOrUpdateComponentMethodXClass() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(METHOD_CLASS, xcontext);

        BaseClass bclass = doc.getXClass();
        bclass.setName(METHOD_CLASS);

        boolean needsUpdate = false;

        needsUpdate |= this.initializeXClassDocumentMetadata(doc, "Wiki Component Method XWiki Class");
        needsUpdate |= bclass.addTextField(METHOD_NAME_FIELD, "Method name", 30);
        needsUpdate |= bclass.addTextAreaField(METHOD_CODE_FIELD, "Method body code", 40, 20);

        if (needsUpdate) {
            this.update(doc);
        }
    }

    /**
     * Utility method for updating a wiki macro class definition document.
     *
     * @param doc xwiki document containing the wiki macro class.
     * @throws XWikiException if an error occurs while saving the document.
     */
    private void update(XWikiDocument doc) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        xcontext.getWiki().saveDocument(doc, xcontext);
    }

    /**
     * Helper method to prepare a document that will hold an XClass definition, setting its initial metadata, if needed
     * (author, title, parent, content, etc.).
     *
     * @param doc the document to prepare
     * @param title the title to set
     * @return true if the doc has been modified and needs saving, false otherwise
     */
    private boolean initializeXClassDocumentMetadata(XWikiDocument doc, String title)
    {
        boolean needsUpdate = false;

        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator(CLASS_AUTHOR);
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle(title);
        }
        if (StringUtils.isBlank(doc.getContent()) || !Syntax.XWIKI_2_0.equals(doc.getSyntax())) {
            needsUpdate = true;
            doc.setContent("{{include document=\"XWiki.ClassSheet\" /}}");
            doc.setSyntax(Syntax.XWIKI_2_0);
        }
        if (!doc.isHidden()) {
            needsUpdate = true;
            doc.setHidden(true);
        }

        return needsUpdate;
    }

    /**
     * @return the XWikiContext extracted from the execution.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}

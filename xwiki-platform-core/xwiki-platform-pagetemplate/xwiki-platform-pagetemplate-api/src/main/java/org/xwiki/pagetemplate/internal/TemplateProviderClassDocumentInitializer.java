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
package org.xwiki.pagetemplate.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PageClass;

/**
 * Initializes the {@code XWiki.TemplateProviderClass} document.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(TemplateProviderClassDocumentInitializer.CLASS_REFERENCE_STRING)
@Singleton
public class TemplateProviderClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The local reference of the template provider class as a String.
     */
    public static final String CLASS_REFERENCE_STRING = "XWiki.TemplateProviderClass";

    /**
     * The local reference of the template provider class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "TemplateProviderClass");

    /**
     * The name of the property holding the name of the template provider.
     */
    public static final String NAME = "name";

    /**
     * The name of the property holding the reference of the template.
     */
    public static final String TEMPLATE = "template";

    /**
     * The name of the property holding the action to perform after creating a page.
     */
    public static final String ACTION = "action";

    /**
     * The name of the property holding the description of the template provider.
     */
    public static final String DESCRIPTION = "description";

    /**
     * The name of the property indicating whether the created pages are terminal.
     */
    public static final String TERMINAL = "terminal";

    /**
     * The name of the property holding the icon of the template provider.
     */
    public static final String ICON = "icon";

    /**
     * The name of the legacy property holding both the visibility and creation restrictions.
     */
    public static final String SPACES = "spaces";

    /**
     * The name of the property holding the spaces where the template provider is offered.
     */
    public static final String VISIBILITY_RESTRICTIONS = "visibilityRestrictions";

    /**
     * The name of the property holding the spaces where pages can be created with the template provider.
     */
    public static final String CREATION_RESTRICTIONS = "creationRestrictions";

    /**
     * The name of the property indicating whether the creation restrictions are only suggestions.
     */
    public static final String CREATION_RESTRICTIONS_ARE_SUGGESTIONS = "creationRestrictionsAreSuggestions";

    private static final String SPACES_QUERY = "select distinct doc.fullName as unfilterableRightCheck, doc.web "
        + "from XWikiDocument doc where doc.name = 'WebHome' order by doc.web";

    private static final String RESTRICTIONS_SEPARATOR = ",";

    private static final String RESTRICTIONS_DISPLAY_SEPARATOR = " ,";

    private static final int SIZE = 30;

    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public TemplateProviderClassDocumentInitializer()
    {
        super(CLASS_REFERENCE, "Template Provider Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(NAME, NAME, SIZE);
        addTemplateField(xclass);
        xclass.addStaticListField(ACTION, "Action on create", 1, false, "edit|saveandedit|saveandview",
            ListClass.DISPLAYTYPE_SELECT, " ,|");
        xclass.addTextField(DESCRIPTION, DESCRIPTION, SIZE);
        xclass.addBooleanField(TERMINAL, TERMINAL);
        xclass.addTextField(ICON, ICON, SIZE);
        addSpacesField(xclass, SPACES, SPACES, SIZE, false, RESTRICTIONS_SEPARATOR);
        addSpacesField(xclass, VISIBILITY_RESTRICTIONS, "Visibility Restrictions", 1, true,
            RESTRICTIONS_DISPLAY_SEPARATOR);
        addSpacesField(xclass, CREATION_RESTRICTIONS, "Creation Restrictions", 1, true,
            RESTRICTIONS_DISPLAY_SEPARATOR);
        xclass.addBooleanField(CREATION_RESTRICTIONS_ARE_SUGGESTIONS, "Creation Restrictions Are Suggestions",
            BooleanClass.DISPLAY_CHECKBOX, null, null);
    }

    private void addTemplateField(BaseClass xclass)
    {
        PageClass templateClass = new PageClass();
        templateClass.setName(TEMPLATE);
        templateClass.setPrettyName(TEMPLATE);
        templateClass.setSize(SIZE);
        templateClass.setMultiSelect(false);
        templateClass.setRelationalStorage(false);
        templateClass.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
        templateClass.setPicker(true);
        templateClass.setSeparator(" ");
        templateClass.setObject(xclass);
        xclass.put(TEMPLATE, templateClass);
    }

    private void addSpacesField(BaseClass xclass, String name, String prettyName, int size,
        boolean relationalStorage, String separator)
    {
        xclass.addDBListField(name, prettyName, size, true, relationalStorage, SPACES_QUERY);
        DBListClass spacesClass = (DBListClass) xclass.getField(name);
        spacesClass.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
        spacesClass.setPicker(true);
        spacesClass.setSeparators(RESTRICTIONS_SEPARATOR);
        spacesClass.setSeparator(separator);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Use TemplateProviderSheet to display template providers if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "TemplateProviderSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}

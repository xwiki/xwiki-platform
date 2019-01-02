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
package org.xwiki.wiki.template.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the WikiManagerCode.WikiTemplateClass document with all required information.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named("WikiManagerCode.WikiTemplateClass")
@Singleton
public class WikiTemplateClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "WikiTemplateClass";

    /**
     * The space of the mandatory document.
     */
    public static final String DOCUMENT_SPACE = "WikiManager";

    /**
     * Reference to the server class.
     */
    public static final LocalDocumentReference SERVER_CLASS = new LocalDocumentReference(DOCUMENT_SPACE, DOCUMENT_NAME);

    /**
     * Name of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELD_ISWIKITEMPLATE = "iswikitemplate";

    /**
     * Pretty name of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELDPN_ISWIKITEMPLATE = "Template";

    /**
     * Form type of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     * 
     * @since 10.7RC1
     */
    public static final String FIELDFT_ISWIKITEMPLATE = "checkbox";

    /**
     * Display type of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELDDT_ISWIKITEMPLATE = "";

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final Boolean DEFAULT_ISWIKITEMPLATE = Boolean.FALSE;

    /**
     * Constructor.
     */
    public WikiTemplateClassDocumentInitializer()
    {
        // Since we can`t get the main wiki here, this is just to be able to use the Abstract class.
        // getDocumentReference() returns the actual main wiki document reference.
        super(SERVER_CLASS);
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        // The class is used inside wiki descriptors which are located on main wiki
        return true;
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addBooleanField(FIELD_ISWIKITEMPLATE, FIELDPN_ISWIKITEMPLATE, FIELDFT_ISWIKITEMPLATE,
            FIELDDT_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);
    }
}

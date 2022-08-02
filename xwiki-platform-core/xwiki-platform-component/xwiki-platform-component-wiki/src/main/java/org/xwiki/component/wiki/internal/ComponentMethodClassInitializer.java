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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Initialize wiki component method class.
 * 
 * @version $Id$
 * @since 11.4
 */
@Component
@Named(WikiComponentConstants.METHOD_CLASS)
@Singleton
public class ComponentMethodClassInitializer extends AbstractMandatoryClassInitializer implements WikiComponentConstants
{
    /**
     * The default constructor.
     */
    public ComponentMethodClassInitializer()
    {
        super(METHOD_CLASS_REFERENCE, "Wiki Component Method XWiki Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(METHOD_NAME_FIELD, "Method name", 30);

        // This field contains wiki syntax (usually some scripting macro) but it's technical content that should be
        // edited with a Text editor.
        xclass.addTextAreaField(METHOD_CODE_FIELD, "Method body code", 40, 20, TextAreaClass.EditorType.TEXT,
            TextAreaClass.ContentType.WIKI_TEXT);
    }
}

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
package org.xwiki.wikistream.instance.internal.input;

import java.util.Iterator;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiClassFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.BaseClassProperties;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class BaseClassInputWikiStream implements InputWikiStream
{
    private BaseClass xclass;

    private BaseClassProperties properties;

    public BaseClassInputWikiStream(BaseClass xclass, BaseClassProperties properties)
    {
        this.xclass = xclass;
        this.properties = properties;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiDocumentFilter documentFilter = (XWikiDocumentFilter) filter;

        // WikiClass

        FilterEventParameters classParameters = new FilterEventParameters();

        classParameters.put(WikiClassFilter.PARAMETER_CUSTOMCLASS, this.xclass.getCustomClass());
        classParameters.put(WikiClassFilter.PARAMETER_CUSTOMMAPPING, this.xclass.getCustomMapping());
        classParameters.put(WikiClassFilter.PARAMETER_DEFAULTSPACE, this.xclass.getDefaultWeb());
        classParameters.put(WikiClassFilter.PARAMETER_NAMEFIELD, this.xclass.getNameField());
        classParameters.put(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT, this.xclass.getDefaultEditSheet());
        classParameters.put(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW, this.xclass.getDefaultViewSheet());
        classParameters.put(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT, this.xclass.getValidationScript());

        documentFilter.beginWikiClass(classParameters);

        // Properties

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<PropertyClass> it = this.xclass.getSortedIterator();
        while (it.hasNext()) {
            PropertyClass xclassProperty = it.next();

            PropertyClassInputWikiStream propertyClassStream = new PropertyClassInputWikiStream(xclassProperty, this.properties);
            propertyClassStream.read(documentFilter);
        }

        // /WikiClass

        documentFilter.endWikiClass(classParameters);
    }
}

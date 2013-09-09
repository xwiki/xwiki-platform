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
import org.xwiki.wikistream.filter.WikiObjectFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.BaseObjectFilter;
import org.xwiki.wikistream.instance.internal.BaseObjectProperties;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class BaseObjectInputWikiStream implements InputWikiStream
{
    private BaseObject xobject;

    private BaseObjectProperties properties;

    private XWikiContext xcontext;

    public BaseObjectInputWikiStream(BaseObject xobject, XWikiContext xcontext, BaseObjectProperties properties)
    {
        this.xobject = xobject;
        this.properties = properties;
        this.xcontext = xcontext;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        BaseObjectFilter objectFilter = (BaseObjectFilter) filter;

        // > WikiObject

        FilterEventParameters objectParameters = new FilterEventParameters();

        objectParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, this.xobject.getClassName());
        objectParameters.put(WikiObjectFilter.PARAMETER_GUID, this.xobject.getGuid());
        objectParameters.put(WikiObjectFilter.PARAMETER_NUMBER, this.xobject.getNumber());

        objectFilter.beginWikiObject(this.xobject.getReference().getName(), objectParameters);

        // Properties

        // Iterate over values/properties sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<BaseProperty< ? >> it = this.xobject.getSortedIterator();
        while (it.hasNext()) {
            BaseProperty< ? > xproperty = it.next();

            String pname = xproperty.getName();
            if (pname != null && !pname.trim().equals("")) {
                BasePropertyInputWikiStream propertyStream =
                    new BasePropertyInputWikiStream(xproperty, this.properties);
                propertyStream.read(objectFilter);
            }
        }

        // Object class

        BaseClass xclass = this.xobject.getXClass(this.xcontext);
        BaseClassInputWikiStream classStream = new BaseClassInputWikiStream(xclass, this.properties);
        classStream.read(objectFilter);

        // < WikiObject

        objectFilter.endWikiObject(this.xobject.getReference().getName(), objectParameters);
    }
}

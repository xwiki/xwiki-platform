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

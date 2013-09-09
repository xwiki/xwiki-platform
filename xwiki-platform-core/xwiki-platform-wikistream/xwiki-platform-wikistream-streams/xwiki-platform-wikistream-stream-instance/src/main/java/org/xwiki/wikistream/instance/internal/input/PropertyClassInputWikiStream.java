package org.xwiki.wikistream.instance.internal.input;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiClassPropertyFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.PropertyClassProperties;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class PropertyClassInputWikiStream implements InputWikiStream
{
    private PropertyClass xclassProperty;

    private PropertyClassProperties properties;

    public PropertyClassInputWikiStream(PropertyClass xclassProperty, PropertyClassProperties properties)
    {
        this.xclassProperty = xclassProperty;
        this.properties = properties;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiDocumentFilter documentFilter = (XWikiDocumentFilter) filter;

        // > WikiClassProperty

        FilterEventParameters propertyParameters = new FilterEventParameters();

        Map<String, String> fields = new LinkedHashMap<String, String>();

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<BaseProperty< ? >> it = this.xclassProperty.getSortedIterator();
        while (it.hasNext()) {
            BaseProperty< ? > bprop = it.next();
            fields.put(bprop.getName(), bprop.toText());
        }

        propertyParameters.put(WikiClassPropertyFilter.PARAMETER_FIELDS, fields);

        String classType = this.xclassProperty.getClassType();
        if (this.xclassProperty.getClass().getSimpleName().equals(classType + "Class")) {
            // Keep exporting the full Java class name for old/default property types to avoid breaking the XAR format
            // (to allow XClasses created with the current version of XWiki to be imported in an older version).
            classType = getClass().getName();
        }

        documentFilter.beginWikiClassProperty(this.xclassProperty.getName(), classType, propertyParameters);

        // < WikiClassProperty

        documentFilter.endWikiClassProperty(this.xclassProperty.getName(), classType, propertyParameters);
    }
}

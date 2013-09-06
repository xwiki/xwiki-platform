package org.xwiki.wikistream.databaseold.internal.input;

import java.util.Iterator;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.databaseold.internal.BaseClassProperties;
import org.xwiki.wikistream.databaseold.internal.XWikiDocumentFilter;
import org.xwiki.wikistream.filter.WikiClassFilter;
import org.xwiki.wikistream.input.InputWikiStream;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

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

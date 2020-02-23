/*
 */

package com.xpn.xwiki.plugin.tag;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.HiddenDocumentFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TagQueryUtilsTest
{
    @Test
    void getTagCountForQuery() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("className", "XWiki.XWikiUsers");
        parameters.put("classTemplate1", "XWiki.XWikiUsersTemplate");
        parameters.put("classTemplate2", "XWiki.XWikiUsers");
        parameters.put("prop_first_name_name", "first_name");

        String fromHQL = ", BaseObject as obj  , BaseObject as tobject, DBStringListProperty as tagprop, "
            + "StringProperty prop_first_name ";
        String expectedHQL = "select elements(prop.list) from XWikiDocument as doc, BaseObject as tagobject, "
            + "DBStringListProperty as prop, BaseObject as obj  , BaseObject as tobject, DBStringListProperty as "
            + "tagprop, StringProperty prop_first_name  where tagobject.name=doc.fullName and "
            + "tagobject.className='XWiki.TagClass' and tagobject.id=prop.id.id and prop.id.name='tags' and "
            + "doc.translation=0 and obj.name=doc.fullName and obj.className = :className and doc.fullName not in "
            + "(:classTemplate1, :classTemplate2)  and tobject.className='XWiki.TagClass' and "
            + "tobject.name=doc.fullName and tobject.id=tagprop.id.id and tagprop.id.name='tags' and "
            + "( lower(:wikitag1) in (select lower(tag) from tagprop.list tag)) and obj.id=prop_first_name.id.id and "
            + "prop_first_name.name = :prop_first_name_name ";

        XWikiContext context = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiStoreInterface storeInterface = mock(XWikiStoreInterface.class);
        QueryManager queryManager = mock(QueryManager.class);
        Query query = mock(Query.class);
        when(queryManager.createQuery(expectedHQL, Query.HQL)).thenReturn(query);
        when(storeInterface.getQueryManager()).thenReturn(queryManager);
        when(xwiki.getStore()).thenReturn(storeInterface);
        when(context.getWiki()).thenReturn(xwiki);

        ComponentManager componentManager = mock(ComponentManager.class);
        QueryFilter queryFilter = mock(QueryFilter.class);
        when(componentManager.getInstance(QueryFilter.class, HiddenDocumentFilter.HINT)).thenReturn(queryFilter);
        when(componentManager.getInstance(ComponentManager.class, "context")).thenReturn(componentManager);
        Utils.setComponentManager(componentManager);

        TagQueryUtils.getTagCountForQuery(fromHQL, "obj.name=doc.fullName and "
            + "obj.className = :className and doc.fullName not in (:classTemplate1, :classTemplate2)  and "
            + "tobject.className='XWiki.TagClass' and tobject.name=doc.fullName and tobject.id=tagprop.id.id and "
            + "tagprop.id.name='tags' and ( lower(:wikitag1) in (select lower(tag) from tagprop.list tag)) and "
            + "obj.id=prop_first_name.id.id and prop_first_name.name = :prop_first_name_name ", parameters, context);
    }
}

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
package com.xpn.xwiki.objects.classes;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link StringClass}.
 *
 * @version $Id$
 */
@OldcoreTest
@XWikiDocumentFilterUtilsComponentList
class StringClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @Mock
    private XWikiURLFactory urlFactory;

    @Test
    void displayEdit() throws Exception
    {
        // Setup
        this.oldCore.getMocker().registerMockComponent(ContextualLocalizationManager.class);
        
        XWikiContext xWikiContext = this.oldCore.getXWikiContext();
        xWikiContext.setURLFactory(this.urlFactory);
        when(this.oldCore.getSpyXWiki()
            .getURL(new LocalDocumentReference("Main", "WebHome"), "view", xWikiContext)).thenReturn("/a/b");
        
        String fieldName = "test";
        String spaceName = "\" + alert(1) + \"";
        String pageName = "WebHome";
        StringClass stringClass = new StringClass();
        stringClass.setPicker(true);
        BaseClass baseClass = new BaseClass();
        stringClass.setObject(baseClass);
        baseClass.setDocumentReference(
            new DocumentReference(this.oldCore.getXWikiContext().getWikiId(), spaceName, pageName));
        stringClass.setName(fieldName);
        StringBuffer stringBuffer = new StringBuffer();
        stringClass.displayEdit(stringBuffer, fieldName, spaceName + "." + pageName + "_0_", baseClass,
            xWikiContext);
        assertEquals("<input aria-label='core.model.xclass.editClassProperty.textAlternative' "
            + "onfocus='new ajaxSuggest(this, &#123;script:&#34;\\/a\\/b?xpage=suggest&#38;"
            + "classname=%22%20%2B%20alert%281%29%20%2B%20%22.WebHome&#38;fieldname=test&#38;firCol=-&#38;"
            + "secCol=-&#38;&#34;, varname:&#34;input&#34;} )' "
            + "class='suggested' "
            + "id='&#34; + alert(1) + &#34;.WebHome_0_test' "
            + "name='&#34; + alert(1) + &#34;.WebHome_0_test' "
            + "maxlength='255' "
            + "size='30' "
            + "type='text'/>", stringBuffer.toString());
    }

    @Test
    void displayEditReadsStoreLimitOncePerRequest() throws Exception
    {
        this.oldCore.getMocker().registerMockComponent(ContextualLocalizationManager.class);
        XWikiContext xWikiContext = this.oldCore.getXWikiContext();
        when(this.oldCore.getMockStore().getLimitSize(xWikiContext, StringProperty.class, "value")).thenReturn(768);

        StringClass stringClass = new StringClass();
        stringClass.setName("test");
        BaseObject object = new BaseObject();

        StringBuffer firstBuffer = new StringBuffer();
        stringClass.displayEdit(firstBuffer, "test", "Space.Page_0_", object, xWikiContext);
        assertTrue(firstBuffer.toString().contains(" maxlength='768' "), firstBuffer.toString());

        // Another string property rendered during the same request reuses the limit.
        StringClass otherStringClass = new StringClass();
        otherStringClass.setName("test");
        StringBuffer secondBuffer = new StringBuffer();
        otherStringClass.displayEdit(secondBuffer, "test", "Space.Page_0_", object, xWikiContext);
        assertEquals(firstBuffer.toString(), secondBuffer.toString());

        // The limit requires a database metadata query so it must be read from the store only once per request.
        verify(this.oldCore.getMockStore(), times(1)).getLimitSize(xWikiContext, StringProperty.class, "value");
    }

    @Test
    void displayEditWithoutStoreLimit() throws Exception
    {
        this.oldCore.getMocker().registerMockComponent(ContextualLocalizationManager.class);
        XWikiContext xWikiContext = this.oldCore.getXWikiContext();
        when(this.oldCore.getMockStore().getLimitSize(xWikiContext, StringProperty.class, "value")).thenReturn(0);

        StringClass stringClass = new StringClass();
        stringClass.setName("test");
        StringBuffer buffer = new StringBuffer();
        stringClass.displayEdit(buffer, "test", "Space.Page_0_", new BaseObject(), xWikiContext);

        assertFalse(buffer.toString().contains("maxlength"), buffer.toString());
    }
}

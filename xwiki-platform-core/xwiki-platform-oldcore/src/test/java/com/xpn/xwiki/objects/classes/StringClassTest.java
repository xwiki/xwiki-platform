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
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        oldCore.getMocker().registerMockComponent(ContextualLocalizationManager.class);
        
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
            + "size='30' "
            + "type='text'/>", stringBuffer.toString());
    }
}

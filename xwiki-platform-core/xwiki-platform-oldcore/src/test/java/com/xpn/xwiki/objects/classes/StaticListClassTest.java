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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link StaticListClass}.
 *
 * @version $Id$
 */
@ReferenceComponentList
public class StaticListClassTest
{
    /**
     * Static list values that contain HTML special characters that need to be escaped in the HTML display.
     */
    private static final List<String> VALUES_WITH_HTML_SPECIAL_CHARS = Arrays.asList("a<b>c", "1\"2'3", "x{y&z");

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /** Tests that {@link StaticListClass#getList} returns values sorted according to the property's sort option. */
    @Test
    public void testGetListIsSorted()
    {
        StaticListClass listClass = new StaticListClass();
        listClass.setValues("a=A|c=D|d=C|b");

        assertEquals("Default order was not preserved.", "[a, c, d, b]",
            listClass.getList(this.oldcore.getXWikiContext()).toString());
        listClass.setSort("none");
        assertEquals("Default order was not preserved.", "[a, c, d, b]",
            listClass.getList(this.oldcore.getXWikiContext()).toString());
        listClass.setSort("id");
        assertEquals("Items were not ordered by ID.", "[a, b, c, d]",
            listClass.getList(this.oldcore.getXWikiContext()).toString());
        listClass.setSort("value");
        assertEquals("Items were not ordered by value.", "[a, b, d, c]",
            listClass.getList(this.oldcore.getXWikiContext()).toString());
    }

    /**
     * Tests that {@link StaticListClass#getMap} properly supports the values definition syntax:
     * <ul>
     * <li>values are separated by {@code |}</li>
     * <li>simple {@code key} entries are allowed, and the key is also used as the label</li>
     * <li>{@code key=a pretty label} is allowed</li>
     * <li>{@code key=label with = in it} is allowed</li>
     * <li>{@code key=label with \| in it} is allowed</li>
     * </ul>
     */
    @Test
    public void testValuesSyntax()
    {
        StaticListClass listClass = new StaticListClass();
        listClass.setValues("a|b=B and B|c=<=C=>|d=d\\|D|e");

        Map<String, ListItem> result = listClass.getMap(this.oldcore.getXWikiContext());
        assertEquals("Proper splitting not supported", 5, result.size());
        assertEquals("key syntax not supported", "a", result.get("a").getValue());
        assertEquals("key=label syntax not supported", "B and B", result.get("b").getValue());
        assertEquals("= in labels not supported", "<=C=>", result.get("c").getValue());
        assertEquals("Escaped \\| in labels not supported", "d|D", result.get("d").getValue());
    }

    /**
     * Tests that the list values are joined using the specified separator without being XML-encoded.
     *
     * @see "XWIKI-9680: Apostrophes in static list value are encoded on .display()"
     */
    @Test
    public void testDisplayView()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(VALUES_WITH_HTML_SPECIAL_CHARS);

        String propertyName = "foo";
        BaseObject object = new BaseObject();
        object.addField(propertyName, listProperty);

        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setSeparator(" * ");
        staticListClass.setValues(VALUES_WITH_HTML_SPECIAL_CHARS.get(0) + '|' + VALUES_WITH_HTML_SPECIAL_CHARS.get(1)
            + '=' + StringUtils.reverse(VALUES_WITH_HTML_SPECIAL_CHARS.get(1)) + '|'
            + VALUES_WITH_HTML_SPECIAL_CHARS.get(2));
        assertEquals("a<b>c * 3'2\"1 * x{y&z", staticListClass.displayView(propertyName, "", object, null));
    }

    /**
     * Tests the HTML output produced in edit mode.
     *
     * @param displayType the display type (input, radio, select, etc.)
     * @param selectedValues the selected values
     * @param expectedHTML the expected HTML output
     */
    private void testDisplayEdit(String displayType, List<String> selectedValues, String expectedHTML)
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(selectedValues);

        // Use special XML characters, even if they are not valid inside an XML name, just to test the XML escaping.
        String propertyName = "b&a<r";
        String prefix = "w>v";
        BaseObject object = new BaseObject();
        object.addField(propertyName, listProperty);

        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setSize(7);
        StringBuilder values = new StringBuilder();
        for (String value : VALUES_WITH_HTML_SPECIAL_CHARS) {
            if (values.length() > 0) {
                values.append('|');
            }
            values.append(value).append('=').append(StringUtils.reverse(value));
        }
        staticListClass.setValues(values.toString());

        staticListClass.setDisplayType(displayType);
        assertEquals(expectedHTML, staticListClass.displayEdit(propertyName, prefix, object, null));
    }

    /**
     * Tests the 'input' display type.
     */
    @Test
    public void testDisplayEditInput()
    {
        testDisplayEdit("input", VALUES_WITH_HTML_SPECIAL_CHARS, "<input size='7' id='w&#62;vb&#38;a&#60;r' "
            + "value='a&#60;b&#62;c|1&#34;2&#39;3|x&#123;y&#38;z' name='w&#62;vb&#38;a&#60;r' type='text'/>");
    }

    /**
     * Tests the 'select' display type.
     */
    @Test
    public void testDisplayEditSelect()
    {
        String expectedHTML = "<select id='w&#62;vb&#38;a&#60;r' name='w&#62;vb&#38;a&#60;r' size='7'>"
            + "<option value='a&#60;b&#62;c' label='c&#62;b&#60;a'>c&#62;b&#60;a</option>"
            + "<option selected='selected' value='1&#34;2&#39;3' label='3&#39;2&#34;1'>3&#39;2&#34;1</option>"
            + "<option value='x&#123;y&#38;z' label='z&#38;y&#123;x'>z&#38;y&#123;x</option>"
            + "</select><input name='w&#62;vb&#38;a&#60;r' type='hidden' value=''/>";
        testDisplayEdit("select", Arrays.asList(VALUES_WITH_HTML_SPECIAL_CHARS.get(1)), expectedHTML);
    }

    /**
     * Tests the 'radio' display type.
     */
    @Test
    public void testDisplayEditRadio()
    {
        StringBuilder expectedHTML = new StringBuilder();
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-0\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-0' value='a&#60;b&#62;c'");
        expectedHTML.append(" name='w&#62;vb&#38;a&#60;r' type='radio'/>c&#62;b&#60;a</label>");
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-1\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-1' checked='checked' value='1&#34;2&#39;3' ");
        expectedHTML.append("name='w&#62;vb&#38;a&#60;r' type='radio'/>3&#39;2&#34;1</label>");
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-2\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-2' value='x&#123;y&#38;z'");
        expectedHTML.append(" name='w&#62;vb&#38;a&#60;r' type='radio'/>z&#38;y&#123;x</label>");
        expectedHTML.append("<input name='w&#62;vb&#38;a&#60;r' type='hidden' value=''/>");
        testDisplayEdit("radio", Arrays.asList(VALUES_WITH_HTML_SPECIAL_CHARS.get(1)), expectedHTML.toString());
    }

    /**
     * Tests the 'checkbox' display type.
     */
    @Test
    public void testDisplayEditCheckbox()
    {
        StringBuilder expectedHTML = new StringBuilder();
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-0\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-0' value='a&#60;b&#62;c'");
        expectedHTML.append(" name='w&#62;vb&#38;a&#60;r' type='checkbox'/>c&#62;b&#60;a</label>");
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-1\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-1' checked='checked' value='1&#34;2&#39;3' ");
        expectedHTML.append("name='w&#62;vb&#38;a&#60;r' type='checkbox'/>3&#39;2&#34;1</label>");
        expectedHTML.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-b&#38;a&#60;r-0-2\">");
        expectedHTML.append("<input id='xwiki-form-b&#38;a&#60;r-0-2' value='x&#123;y&#38;z'");
        expectedHTML.append(" name='w&#62;vb&#38;a&#60;r' type='checkbox'/>z&#38;y&#123;x</label>");
        expectedHTML.append("<input name='w&#62;vb&#38;a&#60;r' type='hidden' value=''/>");
        testDisplayEdit("checkbox", Arrays.asList(VALUES_WITH_HTML_SPECIAL_CHARS.get(1)), expectedHTML.toString());
    }

    /**
     * Tests the hidden display type.
     */
    @Test
    public void testDisplayHidden()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(VALUES_WITH_HTML_SPECIAL_CHARS);

        // Use special XML characters, even if they are not valid inside an XML name, just to test the XML escaping.
        String propertyName = "f<o&o";
        BaseObject object = new BaseObject();
        object.addField(propertyName, listProperty);

        assertEquals(
            "<input id='f&#60;o&#38;o' value='a&#60;b&#62;c|1&#34;2&#39;3|x&#123;y&#38;z' "
                + "name='f&#60;o&#38;o' type='hidden'/>",
            new StaticListClass().displayHidden(propertyName, "", object, null));
    }

    /**
     * Tests the suggest code generated when "use suggest" is set.
     */
    @Test
    public void testDisplayEditWithSuggest() throws Exception
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(VALUES_WITH_HTML_SPECIAL_CHARS);

        // Use special XML characters, even if they are not valid inside an XML name, just to test the XML escaping.
        String propertyName = "b&a<r";
        String prefix = "w>v";
        BaseObject object = new BaseObject();
        object.addField(propertyName, listProperty);

        StaticListClass staticListClass = new StaticListClass();
        BaseClass ownerClass = new BaseClass();
        ownerClass.setDocumentReference(new DocumentReference("xwiki", "ClassSpace", "ClassName"));
        staticListClass.setName(propertyName);
        staticListClass.setObject(ownerClass);
        staticListClass.setSize(7);
        StringBuilder values = new StringBuilder();
        for (String value : VALUES_WITH_HTML_SPECIAL_CHARS) {
            if (values.length() > 0) {
                values.append('|');
            }
            values.append(value).append('=').append(StringUtils.reverse(value));
        }
        staticListClass.setValues(values.toString());

        staticListClass.setDisplayType("input");
        staticListClass.setPicker(true);
        doReturn("/xwiki/bin/view/Main/WebHome").when(this.oldcore.getSpyXWiki()).getURL("Main.WebHome", "view",
            this.oldcore.getXWikiContext());
        String output = staticListClass.displayEdit(propertyName, prefix, object, this.oldcore.getXWikiContext());
        System.err.println(output);
        assertTrue(
            output.contains("new ajaxSuggest(this, &#123;script:&#34;/xwiki/bin/view/Main/WebHome?xpage=suggest&#38;"
                + "classname=ClassSpace.ClassName&#38;fieldname=b&#38;a&#60;r&#38;firCol=-&#38;"
                + "secCol=-&#38;&#34;, varname:&#34;input&#34;} )"));
    }
}

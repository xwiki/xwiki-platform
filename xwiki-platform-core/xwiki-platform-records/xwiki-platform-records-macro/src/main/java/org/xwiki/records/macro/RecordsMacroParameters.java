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
package org.xwiki.records.macro;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyGroup;
import org.xwiki.properties.annotation.PropertyId;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.properties.annotation.PropertyOrder;
import org.xwiki.stability.Unstable;

/**
 * Parameters of the {@code records} macro.
 * <p>
 * The annotations are what the WYSIWYG macro dialog is built from, and the layout they produce is not a free choice.
 * {@code MacroDescriptorUIFactory} renders the mandatory nodes flat, above the tab strip, and turns the optional
 * groups into the tabs. {@code class} is mandatory and carries no group, so it is that flat field; the
 * {@code columns}, {@code filterSort}, {@code display} and {@code advanced} groups become the tabs, in that order.
 * <p>
 * Leaving {@code class} ungrouped is a choice rather than a requirement: a group holding a mandatory parameter is
 * itself mandatory and would be rendered in the same place, as a titled panel. It is ungrouped because it is
 * currently alone there, and a panel headed <em>Data</em> wrapping a single field labelled <em>Data type</em> is
 * redundant. It belongs in a group again once {@code location} joins it, since the two parameters that decide which
 * rows the table has do belong together.
 * <p>
 * Every <em>optional</em> parameter, by contrast, must carry an explicit group. An ungrouped optional parameter is
 * added to the framework's own {@code defaultOptionalGroup}, and because {@code MacroDescriptorUIFactory} pins that
 * group at order {@code 0} while {@code DefaultBeanDescriptor} keeps an {@link PropertyOrder} value only when it is
 * strictly positive, no Java macro can move it: it would show up as the <em>first</em> tab, ahead of
 * {@code Columns}. Leaving it childless is the only way to get the intended order. A mandatory parameter never
 * reaches that branch, which is why {@code class} is exempt.
 * <p>
 * Three parameters the design calls for are deliberately absent from this first increment. {@code limit}, which
 * decides how many entries a page holds, is left out until there is a reason to override Live Data's own default of
 * fifteen: the reader can already change the page size from the pagination controls, so the parameter would only
 * pick the starting point. {@code location}, which scopes the table to one part of the page tree, needs an exact
 * prefix predicate that the {@code liveTable} source cannot express, so it waits on the results page this module
 * will ship. {@code editable} cannot be honoured
 * without harm: it maps onto the per-property {@code editable} flag of the Live Data <em>configuration</em> rather
 * than onto a macro parameter, and {@code LiveDataRenderer} treats a configuration as trusted only when it is blank
 * or the author holds script right. Since the {@code liveTable} property types already allow editing, suppressing
 * it is the default path, so honouring the parameter would silently downgrade the content trust of every table
 * authored by someone without script right and get the link displayers sanitized away.
 * <p>
 * The display types are what select the parameter widgets. {@link RecordsDataType} names the XClass picker of the
 * object and class editors, {@link RecordsColumns} names this module's own field picker, {@link RecordsSort} its
 * sort picker and {@link RecordsLayouts} its layout radio group; all four resolve to a template under
 * {@code templates/html_displayer}. Everything else relies on the displayer the Java type already has: a
 * {@link String} renders a text input.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public class RecordsMacroParameters
{
    private DocumentReference dataType;

    private String properties;

    private String filters;

    private String sort;

    private String layouts = "table";

    private String description;

    private String id;

    /**
     * @return the data type whose entries are listed
     */
    public DocumentReference getDataType()
    {
        return this.dataType;
    }

    /**
     * @param dataType the data type whose entries are listed
     */
    @PropertyId("class")
    @PropertyDisplayType(RecordsDataType.class)
    @PropertyName("Data type")
    @PropertyDescription("The data type whose entries are listed.")
    @PropertyMandatory
    @PropertyOrder(1)
    public void setDataType(DocumentReference dataType)
    {
        this.dataType = dataType;
    }

    /**
     * @return the columns to display, in the order given
     */
    public String getProperties()
    {
        return this.properties;
    }

    /**
     * @param properties the columns to display, in the order given
     */
    @PropertyDisplayType(RecordsColumns.class)
    @PropertyName("Columns")
    @PropertyDescription("The columns to display, separated by commas, in the order given. Entry metadata such as "
        + "doc.title and the fields of the data type can be mixed. Leave empty to display the entry title followed "
        + "by every field.")
    @PropertyGroup("columns")
    @PropertyOrder(2)
    public void setProperties(String properties)
    {
        this.properties = properties;
    }

    /**
     * @return the filters applied before readers see the table
     */
    public String getFilters()
    {
        return this.filters;
    }

    /**
     * @param filters the filters applied before readers see the table
     */
    @PropertyName("Filters")
    @PropertyDescription("Filters applied before readers see the table, as a query string, for example "
        + "status=Active&client=Acme. The operator is the one the field's type defines.")
    @PropertyGroup("filterSort")
    @PropertyOrder(3)
    public void setFilters(String filters)
    {
        this.filters = filters;
    }

    /**
     * @return the columns the table is sorted on when the page opens
     */
    public String getSort()
    {
        return this.sort;
    }

    /**
     * @param sort the columns the table is sorted on when the page opens
     */
    @PropertyDisplayType(RecordsSort.class)
    @PropertyName("Sort")
    @PropertyDescription("The columns the table is sorted on when the page opens, in the order they apply.")
    @PropertyGroup("filterSort")
    @PropertyOrder(4)
    public void setSort(String sort)
    {
        this.sort = sort;
    }

    /**
     * @return the layout the entries are displayed in
     */
    public String getLayouts()
    {
        return this.layouts;
    }

    /**
     * @param layouts the layout the entries are displayed in
     */
    @PropertyDisplayType(RecordsLayouts.class)
    @PropertyName("Layout")
    @PropertyDescription("How the entries are laid out.")
    @PropertyGroup("display")
    @PropertyOrder(5)
    public void setLayouts(String layouts)
    {
        this.layouts = layouts;
    }

    /**
     * @return the description shown above the table
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description shown above the table, which is also its accessible description
     */
    @PropertyName("Description")
    @PropertyDescription("Describes what the table lists. Shown above the table, and used as its accessible "
        + "description.")
    @PropertyGroup("display")
    @PropertyOrder(6)
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the identifier of this table
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier of this table
     */
    @PropertyName("Table identifier")
    @PropertyDescription("Identifier for this table, needed only when one page holds more than one.")
    @PropertyGroup("advanced")
    @PropertyAdvanced
    @PropertyOrder(7)
    public void setId(String id)
    {
        this.id = id;
    }
}

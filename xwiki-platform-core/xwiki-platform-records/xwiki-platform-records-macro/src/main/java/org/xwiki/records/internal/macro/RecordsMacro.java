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
package org.xwiki.records.internal.macro;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.records.macro.RecordsMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Lists the entries of a data type as a table readers can sort and filter.
 * <p>
 * The macro owns no rendering of its own: it maps its parameters onto a Live Data backed by the {@code liveTable}
 * source and hands them to {@link LiveDataRenderer}, which is the same component the {@code liveData} macro uses. So
 * the table, its layouts, its filter row, its pagination and its view-rights enforcement are Live Data's, and this
 * class is only the translation layer.
 * <p>
 * The one thing worth knowing about that translation is what it deliberately does <em>not</em> do. The renderer also
 * accepts an advanced configuration, which is the only way to reach the parts of a Live Data that are not macro
 * parameters. It is not used here, and cannot be while it stays the trust switch it is today: the renderer marks the
 * content trusted only when the advanced configuration is blank or the author holds script right, so passing one
 * would downgrade every table authored by someone without that right and get its HTML displayers sanitized. The
 * mapping below therefore sticks to what the parameters can express.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Component
@Named(RecordsMacro.ID)
@Singleton
public class RecordsMacro extends AbstractMacro<RecordsMacroParameters>
{
    /**
     * The identifier of this macro.
     */
    public static final String ID = "records";

    /**
     * The Live Data source listing the XObjects of an XClass.
     */
    static final String SOURCE = "liveTable";

    /**
     * Source parameter naming the XClass whose objects are listed.
     */
    static final String CLASS_NAME_PARAMETER = "className";

    /**
     * Source parameter prefixing the translation keys of the column headers.
     */
    static final String TRANSLATION_PREFIX_PARAMETER = "translationPrefix";

    /**
     * The prefix the {@code doc.*} column headers are translated under.
     * <p>
     * Set by the macro rather than exposed as a parameter, exactly as the {@code documents} macro does. It only ever
     * resolves the metadata columns: a data type's own fields have no key under this prefix, so they keep the
     * translated pretty name the property store already put on their descriptor.
     */
    static final String DOC_TRANSLATION_PREFIX = "platform.index.";

    /**
     * The identifier prefix of the entry metadata properties, which the default column list leaves out.
     */
    private static final String METADATA_PREFIX = "doc.";

    /**
     * The identifier prefix of the Live Data pseudo-columns, which are rendering affordances rather than data.
     */
    private static final String INTERNAL_PREFIX = "_";

    /**
     * The column identifying the entry, which the default column list opens with.
     */
    private static final String TITLE_PROPERTY = "doc.title";

    /**
     * The prefix of a generated table identifier. Has to be alphabetical: {@link IdGenerator} builds HTML ids, which
     * may not start with a digit.
     */
    private static final String ID_PREFIX = ID;

    private static final String DESCRIPTION =
        "Displays a collection of entries of the same data type, as a table readers can sort and filter.";

    @Inject
    private LiveDataRenderer liveDataRenderer;

    /**
     * Reads the fields of the chosen data type, to fill in the default column list.
     */
    @Inject
    private LiveDataSourceManager liveDataSourceManager;

    /**
     * Serializes the picked data type for the source. The compact form keeps the wiki only when it is not the
     * current one, which is what a class reference looks like in a Live Data source parameter.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Default constructor.
     */
    public RecordsMacro()
    {
        super("Records", DESCRIPTION, null, RecordsMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(RecordsMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (parameters.getDataType() == null) {
            throw new MacroExecutionException("The [class] parameter is mandatory.");
        }

        try {
            boolean restricted = context.getTransformationContext().isRestricted();
            // The advanced configuration is left blank on purpose, see the class javadoc.
            return List.of(
                this.liveDataRenderer.execute(toLiveDataParameters(parameters, context), (String) null, restricted));
        } catch (LiveDataException e) {
            throw new MacroExecutionException("Failed to render the Records macro.", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public boolean isExecutionIsolated(RecordsMacroParameters parameters, String content)
    {
        return true;
    }

    /**
     * Maps the macro parameters onto the Live Data ones.
     * <p>
     * Only {@code class} needs real work, because it is the only parameter that is not already in the shape Live Data
     * expects: it becomes a source parameter of the {@code liveTable} source. The rest are passed through, since the
     * macro deliberately reuses Live Data's own encodings — a comma-separated properties list, a query-string filter
     * and a {@code name:asc} sort list — so that a value an author writes here means the same thing it would mean in
     * a {@code liveData} macro.
     *
     * @param parameters the macro parameters
     * @param context the transformation context, which carries the document's identifier generator
     * @return the equivalent Live Data renderer parameters
     */
    private LiveDataRendererParameters toLiveDataParameters(RecordsMacroParameters parameters,
        MacroTransformationContext context) throws MacroExecutionException
    {
        LiveDataRendererParameters liveDataParameters = new LiveDataRendererParameters();
        liveDataParameters.setId(getId(parameters, context));
        liveDataParameters.setSource(SOURCE);
        liveDataParameters.setSourceParameters(getSourceParameters(parameters));
        liveDataParameters.setProperties(getProperties(parameters));
        liveDataParameters.setFilters(parameters.getFilters());
        liveDataParameters.setSort(parameters.getSort());
        liveDataParameters.setLimit(parameters.getLimit());
        liveDataParameters.setLayouts(parameters.getLayouts());
        liveDataParameters.setDescription(parameters.getDescription());
        return liveDataParameters;
    }

    /**
     * Returns the identifier of this table, which is always set.
     *
     * Live Data needs one whether or not the author supplied it. Its layout builds the element id of the table's
     * description as {@code <id>-description} and points at it with {@code aria-describedby}, so a table without an
     * id gets the literal id {@code undefined-description}: harmless alone, but two such tables on one page share
     * that element id and the second is then described by the first one's text.
     *
     * The document's {@link IdGenerator} is used rather than a counter of our own, so a Records table takes its
     * place among the ids the rest of the page generates — headings included — and cannot collide with them. It
     * appends {@code -1}, {@code -2} and so on, which is what makes two tables on a page distinct whether the author
     * named them the same thing or named neither.
     *
     * @param parameters the macro parameters
     * @param context the transformation context, which carries the generator
     * @return the authored identifier, made unique, or a generated one when the author supplied none
     */
    private String getId(RecordsMacroParameters parameters, MacroTransformationContext context)
    {
        IdGenerator idGenerator = context.getXDOM() == null ? null : context.getXDOM().getIdGenerator();
        String authored = parameters.getId();
        if (idGenerator == null) {
            // Nothing to be unique against, which happens when the macro is executed outside a document.
            return StringUtils.defaultIfBlank(authored, ID_PREFIX);
        }
        if (StringUtils.isBlank(authored)) {
            return idGenerator.generateUniqueId(ID_PREFIX, "");
        }
        if (Character.isLetter(authored.charAt(0))) {
            return idGenerator.adaptId(authored);
        }
        // adaptId takes the first character of the id as the prefix and rejects one that is not a letter, so an
        // identifier the author started with anything else is prefixed rather than refused.
        return idGenerator.generateUniqueId(ID_PREFIX, authored);
    }

    /**
     * Returns the columns to display.
     *
     * The default has to be resolved here rather than left to Live Data: the {@code liveTable} source needs an
     * explicit column list, and an absent one yields a table with no columns at all rather than one with every
     * column. The {@code documents} macro assembles its list for the same reason.
     *
     * It opens with the entry's title. A table of nothing but field values gives a reader no way to tell one entry
     * from another, and no way to reach the page an entry lives in; the title column is what makes the rest of the
     * row mean something. An author who wants the fields alone can say so, since naming any column replaces this
     * list entirely.
     *
     * @param parameters the macro parameters
     * @return the columns the author chose, or the entry title followed by every field of the data type
     * @throws MacroExecutionException when the data type's fields cannot be read
     */
    private String getProperties(RecordsMacroParameters parameters) throws MacroExecutionException
    {
        if (StringUtils.isNotBlank(parameters.getProperties())) {
            return parameters.getProperties();
        }
        String dataType = this.entityReferenceSerializer.serialize(parameters.getDataType());
        return Stream.concat(Stream.of(TITLE_PROPERTY), getFields(dataType).stream())
            .collect(Collectors.joining(","));
    }

    /**
     * Reads the field identifiers of a data type, in the order the data type declares them.
     *
     * The entry metadata is left out here: the property store reports it alongside the fields, and the only piece
     * of it the default wants is the title, which {@link #getProperties} adds itself. The Live Data pseudo-columns
     * are left out because they are affordances rather than data.
     *
     * @param dataType the serialized reference of the data type
     * @return its field identifiers
     * @throws MacroExecutionException when the source cannot be reached or its properties cannot be read
     */
    private List<String> getFields(String dataType) throws MacroExecutionException
    {
        Source source = new Source(SOURCE);
        source.setParameter(CLASS_NAME_PARAMETER, dataType);
        try {
            return this.liveDataSourceManager.get(source)
                .orElseThrow(() -> new MacroExecutionException(
                    String.format("The [%s] Live Data source is not available.", SOURCE)))
                .getProperties().get().stream()
                .map(LiveDataPropertyDescriptor::getId)
                .filter(id -> id != null && !id.startsWith(METADATA_PREFIX) && !id.startsWith(INTERNAL_PREFIX))
                .toList();
        } catch (LiveDataException e) {
            throw new MacroExecutionException(String.format("Failed to read the fields of [%s].", dataType), e);
        }
    }

    /**
     * @param parameters the macro parameters
     * @return the source parameters, as the query string {@link LiveDataRendererParameters} expects
     */
    private String getSourceParameters(RecordsMacroParameters parameters)
    {
        Map<String, String> sourceParameters = new LinkedHashMap<>();
        sourceParameters.put(CLASS_NAME_PARAMETER,
            this.entityReferenceSerializer.serialize(parameters.getDataType()));
        sourceParameters.put(TRANSLATION_PREFIX_PARAMETER, DOC_TRANSLATION_PREFIX);

        return sourceParameters.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + '=' + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    }

    /**
     * @param value the value to encode
     * @return the value, URL-encoded, since the receiving end URL-decodes each source parameter
     */
    private String encode(String value)
    {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}

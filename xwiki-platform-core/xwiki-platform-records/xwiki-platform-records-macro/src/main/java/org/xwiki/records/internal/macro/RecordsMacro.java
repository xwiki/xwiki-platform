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

import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.records.macro.RecordsMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

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
 * @since 18.9.0RC1
 */
@Component
@Named(RecordsMacro.ID)
// The macro is the one place that maps its parameters onto Live Data and builds the messages, which is why it
// depends on that many types.
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
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
     * The identifier prefix of the page metadata properties, which the default column list leaves out.
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

    /**
     * The prefix of the translation keys of the messages this macro displays.
     */
    private static final String MESSAGE_PREFIX = "rendering.macro.records.";

    private static final String RENDER_FAILED = "error.renderFailed";

    private static final String FIELDS_UNREADABLE = "error.fieldsUnreadable";

    /**
     * What separates the constraints of a filters value.
     */
    private static final String FILTER_SEPARATOR = "&";

    /**
     * What separates the field of a filter constraint from its value.
     */
    private static final String FILTER_VALUE_SEPARATOR = "=";

    /**
     * What separates the items of a columns or sort value.
     */
    private static final String LIST_SEPARATOR = ",";

    /**
     * The sort direction suffixes Live Data understands, after the property identifier.
     */
    private static final List<String> SORT_DIRECTIONS = List.of(":asc", ":desc");

    /**
     * The filter the {@code liveTable} source matches as a number, which it cannot do with a value that is not one.
     */
    private static final String NUMBER_FILTER = "number";

    /**
     * The filter the {@code liveTable} source matches against its two parameters below.
     */
    private static final String BOOLEAN_FILTER = "boolean";

    private static final List<String> BOOLEAN_VALUES = List.of("trueValue", "falseValue");

    /**
     * Builds the warning for a field the data type no longer has. Not a {@link Function} because building a message
     * can fail.
     */
    @FunctionalInterface
    private interface WarningBuilder
    {
        Block build(String field) throws MacroExecutionException;
    }

    /**
     * What the table can do with a field of the data type.
     *
     * @param sortable whether the table can be sorted on the field
     * @param filterable whether the table can be filtered on the field
     * @param values whether a filter value fits the field's type
     */
    private record Field(boolean sortable, boolean filterable, Predicate<String> values)
    {
    }

    private static final String DESCRIPTION =
        "Displays a collection of entries of the same object type, as a table readers can sort and filter.";

    @Inject
    private LiveDataRenderer liveDataRenderer;

    /**
     * Translates the messages shown to authors and readers.
     */
    @Inject
    private ContextualLocalizationManager localization;

    /**
     * Tells whether the chosen data type still exists.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Reads the fields of the chosen data type, to fill in the default column list.
     */
    @Inject
    private LiveDataSourceManager liveDataSourceManager;

    /**
     * The defaults of the {@code liveTable} source, which say whether a field can be sorted and filtered, and with
     * which filter, whenever the field's own descriptor leaves that to its type.
     */
    @Inject
    @Named(SOURCE)
    private Provider<LiveDataConfiguration> sourceDefaults;

    /**
     * Tells whether the reader can view the data type, without which its fields cannot be checked.
     */
    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * Serializes the picked data type for the source. The compact form keeps the wiki only when it is not the
     * current one, which is what a class reference looks like in a Live Data source parameter.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Parses a translated message as plain text, so that nothing in it, such as a field name taken from the wiki, is
     * read as wiki syntax.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    /**
     * Renders the plain text blocks back to XWiki syntax, which escapes whatever the message macros would otherwise
     * interpret when they parse their content.
     */
    @Inject
    @Named("xwiki/2.1")
    private BlockRenderer xwikiRenderer;

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
        DocumentReference dataType = parameters.getDataType();
        if (dataType == null) {
            throw new MacroExecutionException(translate("error.noDataType"));
        }
        String serializedDataType = this.entityReferenceSerializer.serialize(dataType);
        if (!exists(dataType)) {
            // The macro call is left untouched, so that restoring the data type restores the table.
            return List.of(message("error", "error.dataTypeMissing", serializedDataType));
        }

        List<Block> blocks = new ArrayList<>();
        try {
            boolean restricted = context.getTransformationContext().isRestricted();
            // The advanced configuration is left blank on purpose, see the class javadoc.
            LiveDataRendererParameters liveDataParameters =
                toLiveDataParameters(parameters, context, dataType, serializedDataType, blocks);
            blocks.add(this.liveDataRenderer.execute(liveDataParameters, (String) null, restricted));
        } catch (LiveDataException e) {
            throw new MacroExecutionException(translate(RENDER_FAILED), e);
        }
        return blocks;
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
     * @param dataTypeReference the reference of the data type
     * @param dataType the serialized reference of the data type
     * @param warnings receives a warning for every field the data type no longer has, and for every filter and sort
     *     criterion the field's type does not allow
     * @return the equivalent Live Data renderer parameters
     */
    private LiveDataRendererParameters toLiveDataParameters(RecordsMacroParameters parameters,
        MacroTransformationContext context, DocumentReference dataTypeReference, String dataType,
        List<Block> warnings) throws MacroExecutionException
    {
        Map<String, Field> fields = getFields(dataType);
        Predicate<String> known = getKnownFields(dataTypeReference, fields);
        WarningBuilder unknownColumn = field -> warning("warning.columnSkipped", field, dataType);
        WarningBuilder unknownSort = field -> warning("warning.sortSkipped", field, dataType);
        WarningBuilder unknownFilter = field -> warning("warning.filterSkipped", field, dataType);

        LiveDataRendererParameters liveDataParameters = new LiveDataRendererParameters();
        liveDataParameters.setId(getId(parameters, context));
        liveDataParameters.setSource(SOURCE);
        liveDataParameters.setSourceParameters(getSourceParameters(parameters, dataType));
        String properties = getProperties(parameters, fields, known, unknownColumn, warnings);
        liveDataParameters.setProperties(properties);
        String filters = keepKnown(keepReadable(parameters.getFilters(), warnings), FILTER_SEPARATOR,
            this::getFilterField, known, unknownFilter, warnings);
        liveDataParameters.setFilters(keepApplicable(filters, fields, warnings));
        String sort =
            keepKnown(parameters.getSort(), LIST_SEPARATOR, this::getSortField, known, unknownSort, warnings);
        liveDataParameters.setSort(keepSortable(sort, properties, fields, warnings));
        liveDataParameters.setLayouts(parameters.getLayouts());
        liveDataParameters.setDescription(parameters.getDescription());
        return liveDataParameters;
    }

    /**
     * Tells which fields the data type has, for the purpose of dropping the authored ones it no longer has.
     * <p>
     * The source only reports the fields of a data type to a reader who can view it, and reports none to anyone
     * else. For such a reader the field list says nothing about the data type, so every field is taken as known and
     * the authored columns, filters and sort are passed through unchanged. Dropping them would show that reader
     * every entry the author filtered out, and warn them about fields that do exist, in a message they cannot act on.
     * The default column list is still built from the fields they are offered, so with no column authored they see
     * the entry title alone, which is also all that Live Data would describe to them.
     *
     * @param dataType the reference of the data type
     * @param fields the fields the data type's source offers to the current user
     * @return whether a field counts as one the data type has
     */
    private Predicate<String> getKnownFields(DocumentReference dataType, Map<String, Field> fields)
    {
        if (this.authorization.hasAccess(Right.VIEW, dataType)) {
            return fields::containsKey;
        }
        return field -> true;
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
     * The columns the author named that the data type no longer has are skipped, with a warning. When none of them
     * is left the default applies, since a table with no column at all would say nothing.
     *
     * @param parameters the macro parameters
     * @param fields everything the data type's source offers, by identifier
     * @param known whether a field counts as one the data type has
     * @param unknown builds the warning for a column that is gone
     * @param warnings receives the warnings
     * @return the columns the author chose, or the entry title followed by every field of the data type
     */
    private String getProperties(RecordsMacroParameters parameters, Map<String, Field> fields,
        Predicate<String> known, WarningBuilder unknown, List<Block> warnings) throws MacroExecutionException
    {
        String kept = keepKnown(parameters.getProperties(), LIST_SEPARATOR, String::trim, known, unknown, warnings);
        if (kept != null) {
            return kept;
        }
        return Stream.concat(Stream.of(TITLE_PROPERTY),
            fields.keySet().stream().filter(id -> !id.startsWith(METADATA_PREFIX) && !id.startsWith(INTERNAL_PREFIX)))
            .collect(Collectors.joining(LIST_SEPARATOR));
    }

    /**
     * Keeps the items of a Live Data list parameter whose field the data type still has.
     *
     * @param value the parameter value, which may be blank
     * @param separator what separates the items
     * @param fieldOf extracts the field an item is about
     * @param known whether a field counts as one the data type has
     * @param unknown builds the warning for a field that is gone
     * @param warnings receives the warnings
     * @return the items kept, in their original text, or {@code null} when there is none
     */
    private String keepKnown(String value, String separator, Function<String, String> fieldOf,
        Predicate<String> known, WarningBuilder unknown, List<Block> warnings)
        throws MacroExecutionException
    {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String item : value.split(Pattern.quote(separator))) {
            String field = fieldOf.apply(item);
            if (field.isEmpty()) {
                continue;
            }
            if (field.startsWith(INTERNAL_PREFIX) || known.test(field)) {
                kept.add(item);
            } else {
                warnings.add(unknown.build(field));
            }
        }
        return kept.isEmpty() ? null : String.join(separator, kept);
    }

    /**
     * Keeps the constraints of a filters value that the renderer can decode.
     * <p>
     * The renderer reads the value as form data, and fails on a {@code %} that is not followed by two hexadecimal
     * digits. It fails as a whole, so one such constraint, which only a hand-written value can hold since the dialog
     * encodes what it saves, would otherwise replace the table with an error. The constraint is dropped with a warning
     * instead, and the table shows the others.
     *
     * @param filters the filters value, which may be blank
     * @param warnings receives a warning for every constraint that cannot be decoded
     * @return the constraints kept, possibly none
     */
    private String keepReadable(String filters, List<Block> warnings) throws MacroExecutionException
    {
        if (StringUtils.isBlank(filters)) {
            return filters;
        }
        List<String> kept = new ArrayList<>();
        for (String constraint : filters.split(Pattern.quote(FILTER_SEPARATOR))) {
            try {
                URLDecoder.decode(constraint, StandardCharsets.UTF_8);
                kept.add(constraint);
            } catch (IllegalArgumentException e) {
                warnings.add(warning("warning.filterUnreadable", constraint));
            }
        }
        return String.join(FILTER_SEPARATOR, kept);
    }

    /**
     * Keeps the constraints of a filters value that the type of their field still allows.
     * <p>
     * A constraint the type does not allow does not make Live Data ignore it: the {@code liveTable} source matches no
     * entry at all for a number field filtered on {@code cheap}, which is what is left of a filter authored before
     * the field became a number. The constraint is dropped with a warning instead, and the table shows the others.
     * The fields the reader is not told about are left alone, since nothing is known of their type.
     *
     * @param filters the filters value, which may be blank
     * @param fields the fields of the data type, by identifier
     * @param warnings receives a warning for every constraint the type of its field does not allow
     * @return the constraints kept, or {@code null} when there is none
     */
    private String keepApplicable(String filters, Map<String, Field> fields, List<Block> warnings)
        throws MacroExecutionException
    {
        if (StringUtils.isBlank(filters)) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String constraint : filters.split(Pattern.quote(FILTER_SEPARATOR))) {
            String fieldId = getFilterField(constraint);
            Field field = fields.get(fieldId);
            if (field == null) {
                kept.add(constraint);
            } else if (!field.filterable()) {
                warnings.add(warning("warning.filterUnsupported", fieldId));
            } else if (!field.values().test(getFilterValue(constraint))) {
                warnings.add(warning("warning.filterTypeChanged", fieldId));
            } else {
                kept.add(constraint);
            }
        }
        return kept.isEmpty() ? null : String.join(FILTER_SEPARATOR, kept);
    }

    /**
     * Keeps the criteria of a sort value whose field can be sorted on.
     * <p>
     * The warning names what the table is sorted on instead: the first criterion left, or else the column Live Data
     * falls back to when given no sort, which is the first one when it can be sorted on.
     *
     * @param sort the sort value, which may be {@code null}
     * @param properties the columns displayed
     * @param fields the fields of the data type, by identifier
     * @param warnings receives a warning for every criterion whose field cannot be sorted on
     * @return the criteria kept, or {@code null} when there is none
     */
    private String keepSortable(String sort, String properties, Map<String, Field> fields, List<Block> warnings)
        throws MacroExecutionException
    {
        if (sort == null) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (String criterion : sort.split(Pattern.quote(LIST_SEPARATOR))) {
            String fieldId = getSortField(criterion);
            Field field = fields.get(fieldId);
            if (field == null || field.sortable()) {
                kept.add(criterion);
            } else {
                dropped.add(fieldId);
            }
        }
        String fallback = kept.isEmpty() ? getDefaultSort(properties, fields) : getSortField(kept.get(0));
        for (String fieldId : dropped) {
            warnings.add(fallback == null ? warning("warning.sortUnsupportedUnsorted", fieldId)
                : warning("warning.sortUnsupported", fieldId, fallback));
        }
        return kept.isEmpty() ? null : String.join(LIST_SEPARATOR, kept);
    }

    /**
     * @param properties the columns displayed
     * @param fields the fields of the data type, by identifier
     * @return the column Live Data sorts on when given no sort, or {@code null} when it leaves the table unsorted
     */
    private String getDefaultSort(String properties, Map<String, Field> fields)
    {
        return Stream.of(properties.split(LIST_SEPARATOR)).map(String::trim)
            .filter(property -> !property.startsWith(INTERNAL_PREFIX)).findFirst()
            .filter(property -> fields.containsKey(property) && fields.get(property).sortable())
            .orElse(null);
    }

    private boolean exists(DocumentReference dataType) throws MacroExecutionException
    {
        try {
            return this.documentAccessBridge.exists(dataType);
        } catch (Exception e) {
            throw new MacroExecutionException(
                translate(FIELDS_UNREADABLE, this.entityReferenceSerializer.serialize(dataType)), e);
        }
    }

    private String getSortField(String item)
    {
        String trimmed = item.trim();
        return SORT_DIRECTIONS.stream().filter(trimmed::endsWith).findFirst()
            .map(direction -> trimmed.substring(0, trimmed.length() - direction.length())).orElse(trimmed);
    }

    private String getFilterField(String item)
    {
        return URLDecoder.decode(StringUtils.substringBefore(item, FILTER_VALUE_SEPARATOR), StandardCharsets.UTF_8)
            .trim();
    }

    private String getFilterValue(String item)
    {
        return URLDecoder.decode(StringUtils.substringAfter(item, FILTER_VALUE_SEPARATOR), StandardCharsets.UTF_8)
            .trim();
    }

    /**
     * Reads the fields the data type's source offers: the page metadata, the data type's fields and the Live Data
     * pseudo-columns, in the order the source reports them.
     *
     * @param dataType the serialized reference of the data type
     * @return the fields, by identifier
     * @throws MacroExecutionException when the source cannot be reached or its properties cannot be read
     */
    private Map<String, Field> getFields(String dataType) throws MacroExecutionException
    {
        Source source = new Source(SOURCE);
        source.setParameter(CLASS_NAME_PARAMETER, dataType);
        Collection<LiveDataPropertyDescriptor> descriptors;
        try {
            descriptors = this.liveDataSourceManager.get(source)
                .orElseThrow(() -> new MacroExecutionException(translate(RENDER_FAILED)))
                .getProperties().get();
        } catch (LiveDataException e) {
            throw new MacroExecutionException(translate(FIELDS_UNREADABLE, dataType), e);
        }
        Map<String, LiveDataPropertyDescriptor> types = new HashMap<>();
        this.sourceDefaults.get().getMeta().getPropertyTypes().forEach(type -> types.putIfAbsent(type.getId(), type));
        Map<String, Field> fields = new LinkedHashMap<>();
        for (LiveDataPropertyDescriptor descriptor : descriptors) {
            if (descriptor.getId() != null) {
                fields.putIfAbsent(descriptor.getId(), getField(descriptor, types.get(descriptor.getType())));
            }
        }
        return fields;
    }

    /**
     * Resolves what the table can do with a field the way Live Data does: from the field's own descriptor, and from
     * the defaults of its type for whatever the descriptor leaves unset. A field of a type with no defaults can be
     * neither sorted nor filtered on.
     *
     * @param descriptor the descriptor of the field
     * @param type the defaults of the field's type, {@code null} when there are none
     * @return what the table can do with the field
     */
    private Field getField(LiveDataPropertyDescriptor descriptor, LiveDataPropertyDescriptor type)
    {
        boolean sortable = isEnabled(descriptor.isSortable(), type == null ? null : type.isSortable());
        boolean filterable = isEnabled(descriptor.isFilterable(), type == null ? null : type.isFilterable());
        FilterDescriptor filter = descriptor.getFilter() != null || type == null ? descriptor.getFilter()
            : type.getFilter();
        return new Field(sortable, filterable, getAcceptedValues(filter));
    }

    private static boolean isEnabled(Boolean own, Boolean typeDefault)
    {
        return own != null ? own : Boolean.TRUE.equals(typeDefault);
    }

    /**
     * Tells which values the {@code liveTable} source can match a field with. It matches a number filter by parsing
     * the value, and a boolean one by comparing it with the filter's own true and false values, so anything else
     * matches no entry. An empty value is always accepted, since it filters nothing.
     *
     * @param filter the filter of the field, {@code null} when it has none
     * @return whether a decoded filter value fits the field
     */
    private static Predicate<String> getAcceptedValues(FilterDescriptor filter)
    {
        String filterId = filter == null ? null : filter.getId();
        if (NUMBER_FILTER.equals(filterId)) {
            return value -> value.isEmpty() || NumberUtils.isCreatable(value);
        } else if (BOOLEAN_FILTER.equals(filterId)) {
            List<String> accepted = BOOLEAN_VALUES.stream().map(filter.getParameters()::get).filter(Objects::nonNull)
                .map(String::valueOf).toList();
            return value -> value.isEmpty() || accepted.contains(value);
        }
        return value -> true;
    }

    /**
     * @param parameters the macro parameters
     * @param dataType the serialized reference of the data type
     * @return the source parameters, as the query string {@link LiveDataRendererParameters} expects
     */
    private String getSourceParameters(RecordsMacroParameters parameters, String dataType)
    {
        Map<String, String> sourceParameters = new LinkedHashMap<>();
        sourceParameters.put(CLASS_NAME_PARAMETER, dataType);
        sourceParameters.put(TRANSLATION_PREFIX_PARAMETER, DOC_TRANSLATION_PREFIX);

        return sourceParameters.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + '=' + encode(entry.getValue()))
            .collect(Collectors.joining(FILTER_SEPARATOR));
    }

    private String translate(String key, Object... arguments)
    {
        return this.localization.getTranslationPlain(MESSAGE_PREFIX + key, arguments);
    }

    private Block warning(String key, Object... arguments) throws MacroExecutionException
    {
        return message("warning", key, arguments);
    }

    /**
     * Builds a call to a message macro, so that the message looks and is announced like a {@code {{warning}}} or an
     * {@code {{error}}}, icon and accessible name included. The call is left for the macro transformation to execute,
     * which it does for the blocks a macro returns.
     *
     * @param macroId the identifier of the message macro, {@code warning} or {@code error}
     * @param key the translation key of the message
     * @param arguments the arguments of the message
     * @return the macro call
     * @throws MacroExecutionException when the message cannot be escaped
     */
    private Block message(String macroId, String key, Object... arguments) throws MacroExecutionException
    {
        return new MacroBlock(macroId, Map.of(), escape(translate(key, arguments)), false);
    }

    /**
     * The message macros parse their content as wiki syntax, while the messages embed names that come from the wiki,
     * so the text goes through a plain text parser and back out as XWiki syntax, which escapes it.
     *
     * @param text the plain text to escape
     * @return the text as XWiki syntax content that renders as the text itself
     * @throws MacroExecutionException when the text cannot be parsed
     */
    private String escape(String text) throws MacroExecutionException
    {
        try {
            WikiPrinter printer = new DefaultWikiPrinter();
            this.xwikiRenderer.render(this.plainParser.parse(new StringReader(text)), printer);
            return printer.toString();
        } catch (ParseException e) {
            throw new MacroExecutionException(translate(RENDER_FAILED), e);
        }
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

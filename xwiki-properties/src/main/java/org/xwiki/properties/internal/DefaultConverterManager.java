package org.xwiki.properties.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;

/**
 * Default implementation for {@link ConverterManager}.
 * <p>
 * It try to find a {@link Converter} for the provided target type. If it can't find:
 * <ul>
 * <li>if the type is an {@link Enum}, it use the {@link Converter} with component hint "enum"</li>
 * <li>then it use the default {@link Converter} (which is based on {@link org.apache.commons.beanutils.ConvertUtils} by
 * default)</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultConverterManager extends AbstractLogEnabled implements ConverterManager
{
    /**
     * Use to find the proper {@link Converter} component for provided target type.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used when no direct {@link Converter} can be found for provided target type and the target type is an
     * {@link Enum}.
     */
    @Requirement("enum")
    private Converter enumConverter;

    /**
     * Used when no direct {@link Converter} can be found for provided target type.
     */
    @Requirement
    private Converter defaultConverter;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.ConverterManager#convert(java.lang.reflect.Type, java.lang.Object)
     */
    public <T> T convert(Type targetType, Object value)
    {
        // Convert
        Converter converter = lookupConverter(targetType);

        if (converter != null) {
            return (T) converter.convert(targetType, value);
        } else {
            throw new ConversionException("Can't find converter to convert value [" + value + "] to type ["
                + targetType + "] ");
        }
    }

    /**
     * Find the right {@link Converter} for the provided {@link Class}.
     * 
     * @param targetType the type to convert to
     * @return the {@link Converter} corresponding to the class
     */
    private Converter lookupConverter(Type targetType)
    {
        Converter converter = null;

        String typeGenericName = getTypeGenericName(targetType);
        try {
            converter = this.componentManager.lookup(Converter.class, getTypeGenericName(targetType));
        } catch (ComponentLookupException e) {
            getLogger().debug("Failed to find a proper Converter for type [" + typeGenericName + "]", e);

            if (targetType instanceof ParameterizedType) {
                String typeName = getTypeName(targetType);
                try {
                    converter = this.componentManager.lookup(Converter.class, typeName);
                } catch (ComponentLookupException e2) {
                    getLogger().debug("Failed to find a proper Converter for class [" + typeName + "]", e);
                }
            }
        }

        if (converter == null) {
            if (targetType instanceof Class && Enum.class.isAssignableFrom((Class< ? >) targetType)) {
                converter = this.enumConverter;
            } else {
                getLogger().debug("Trying default Converter for target type [" + typeGenericName + "]");

                converter = this.defaultConverter;
            }
        }

        return converter;
    }

    /**
     * Get class name without generics.
     * 
     * @param type the type
     * @return type name without generics
     */
    private String getTypeName(Type type)
    {
        String name;
        if (type instanceof Class) {
            name = ((Class) type).getName();
        } else if (type instanceof ParameterizedType) {
            name = ((Class) ((ParameterizedType) type).getRawType()).getName();
        } else {
            name = type.toString();
        }

        return name;
    }

    /**
     * Get type name.
     * 
     * @param type the type
     * @return type name
     */
    private String getTypeGenericName(Type type)
    {
        StringBuilder sb = new StringBuilder(getTypeName(type));

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type[] generics = parameterizedType.getActualTypeArguments();
            if (generics.length > 0) {
                sb.append('<');
                for (int i = 0; i < generics.length; ++i) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(getTypeGenericName(generics[i]));
                }
                sb.append('>');
            }
        }

        return sb.toString();
    }
}

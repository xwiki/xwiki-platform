package org.xwiki.rendering.macro.parameter;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

public class EnumConverter implements Converter
{
    public Object convert(Class type, Object value)
    {
        Object[] enumValues = type.getEnumConstants();

        for (Object enumValue : enumValues) {
            if (enumValue.toString().equalsIgnoreCase(value.toString())) {
                return enumValue;
            }
        }

        throw new ConversionException(generateInvalidErrorMessage(enumValues, value));
    }

    private String generateInvalidErrorMessage(Object[] enumValues, Object value)
    {
        StringBuffer errorMessage = new StringBuffer("Can't find coversionn for value [" + value + "]");

        errorMessage.append(" Alowed values are (case insensitive) ");

        StringBuffer valueList = new StringBuffer();

        int index = 1;
        for (Object enumValue : enumValues) {
            if (valueList.length() > 0) {
                if (++index == enumValues.length) {
                    valueList.append(" or ");
                } else {
                    valueList.append(", ");
                }
            }

            valueList.append('"');
            valueList.append(enumValue);
            valueList.append('"');
        }

        errorMessage.append(valueList);
        errorMessage.append('.');

        return errorMessage.toString();
    }
}

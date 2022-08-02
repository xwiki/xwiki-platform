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
package org.xwiki.notifications.sources.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.ConcatNode;
import org.xwiki.notifications.filters.expression.EndsWith;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.InSubQueryNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StartsWith;
import org.xwiki.notifications.filters.expression.generics.AbstractBinaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractUnaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.notifications.filters.internal.status.ForUserNode;
import org.xwiki.notifications.sources.internal.OrderByNode.Order;

/**
 * Converter used to transform {@link ExpressionNode} based abstract syntax trees to {@link EventQuery}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component(roles = ExpressionNodeToEventQueryConverter.class)
@Singleton
public class ExpressionNodeToEventQueryConverter
{
    private static final String FORMAT_UNSUPPORTED_OPERATOR = "Unsupported operator [%s]";

    private static final EnumMap<EventProperty, String> PROPERTY_MAPPING = new EnumMap<>(EventProperty.class);

    static {
        PROPERTY_MAPPING.put(EventProperty.ID, Event.FIELD_ID);
        PROPERTY_MAPPING.put(EventProperty.GROUP_ID, Event.FIELD_GROUPID);
        PROPERTY_MAPPING.put(EventProperty.STREAM, Event.FIELD_STREAM);
        PROPERTY_MAPPING.put(EventProperty.DATE, Event.FIELD_DATE);
        PROPERTY_MAPPING.put(EventProperty.REMOTE_OBSERVATION_ID, Event.FIELD_REMOTE_OBSERVATION_ID);
        PROPERTY_MAPPING.put(EventProperty.IMPORTANCE, Event.FIELD_IMPORTANCE);
        PROPERTY_MAPPING.put(EventProperty.TYPE, Event.FIELD_TYPE);
        PROPERTY_MAPPING.put(EventProperty.APPLICATION, Event.FIELD_APPLICATION);
        PROPERTY_MAPPING.put(EventProperty.USER, Event.FIELD_USER);
        PROPERTY_MAPPING.put(EventProperty.WIKI, Event.FIELD_WIKI);
        PROPERTY_MAPPING.put(EventProperty.SPACE, Event.FIELD_SPACE);
        PROPERTY_MAPPING.put(EventProperty.PAGE, Event.FIELD_DOCUMENT);
        PROPERTY_MAPPING.put(EventProperty.HIDDEN, Event.FIELD_HIDDEN);
        PROPERTY_MAPPING.put(EventProperty.URL, Event.FIELD_URL);
        PROPERTY_MAPPING.put(EventProperty.TITLE, Event.FIELD_TITLE);
        PROPERTY_MAPPING.put(EventProperty.BODY, Event.FIELD_BODY);
        PROPERTY_MAPPING.put(EventProperty.DOCUMENT_VERSION, Event.FIELD_DOCUMENTVERSION);
    }

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Convert an ExpressionNode to an HQLQuery.
     * 
     * @param node the node to convert
     * @return the generated HQL query
     * @throws EventStreamException when failing to convert the expression
     */
    public SimpleEventQuery parse(ExpressionNode node) throws EventStreamException
    {
        SimpleEventQuery result = new SimpleEventQuery();

        parseBlock(node, result, false);

        return result;
    }

    private void parseBlock(ExpressionNode node, SimpleEventQuery result, boolean ingroup) throws EventStreamException
    {
        if (node instanceof AbstractUnaryOperatorNode) {
            parseUnaryOperator((AbstractUnaryOperatorNode) node, result, ingroup);
        } else if (node instanceof AbstractBinaryOperatorNode) {
            parseBinaryOperator((AbstractBinaryOperatorNode) node, result, ingroup);
        } else if (node instanceof AbstractOperatorNode) {
            parseOtherOperation((AbstractOperatorNode) node, result, ingroup);
        } else {
            // Unsupported
            throw new EventStreamException(String.format("Unsupported block node [%s]", node));
        }
    }

    private String getProperty(AbstractBinaryOperatorNode operator) throws EventStreamException
    {
        if (operator.getLeftOperand() instanceof PropertyValueNode) {
            return getProperty((PropertyValueNode) operator.getLeftOperand());
        } else if (operator.getRightOperand() instanceof PropertyValueNode) {
            return getProperty((PropertyValueNode) operator.getRightOperand());
        } else {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        }
    }

    private Object getValue(AbstractBinaryOperatorNode operator) throws EventStreamException
    {
        if (operator.getLeftOperand() instanceof PropertyValueNode) {
            return getValue((AbstractValueNode) operator.getRightOperand());
        } else if (operator.getRightOperand() instanceof PropertyValueNode) {
            return getValue((AbstractValueNode) operator.getLeftOperand());
        } else {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        }
    }

    private String getProperty(PropertyValueNode propertyNode) throws EventStreamException
    {
        String property = PROPERTY_MAPPING.get(propertyNode.getContent());

        if (property == null) {
            // TODO: Unsupported
            throw new EventStreamException(String.format("Unsupported property node [%s]", propertyNode));
        }

        return property;
    }

    private Object getValue(AbstractValueNode value) throws EventStreamException
    {
        if (value instanceof ConcatNode) {
            // TODO: Unsupported
            throw new EventStreamException(String.format("Unsupported value node [%s]", value));
        }

        return value.getContent();
    }

    private SimpleEventQuery parseUnaryOperator(AbstractUnaryOperatorNode operator, SimpleEventQuery result,
        boolean ingroup) throws EventStreamException
    {
        if (operator instanceof NotNode) {
            result.not();

            parseBlock(operator.getOperand(), result, true);
        }

        return result;
    }

    private void parseEqualsNode(AbstractBinaryOperatorNode operator, SimpleEventQuery result)
        throws EventStreamException
    {
        if (operator.getLeftOperand() instanceof PropertyValueNode) {
            result.eq(getProperty((PropertyValueNode) operator.getLeftOperand()),
                getValue((AbstractValueNode) operator.getRightOperand()));
        } else if (operator.getRightOperand() instanceof PropertyValueNode) {
            result.eq(getProperty((PropertyValueNode) operator.getRightOperand()),
                getValue((AbstractValueNode) operator.getLeftOperand()));
        } else if (!operator.getLeftOperand().equals(operator.getRightOperand())) {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        }
    }

    private void parseBinaryOperator(AbstractBinaryOperatorNode operator, SimpleEventQuery result, boolean ingroup)
        throws EventStreamException
    {
        if (operator instanceof AndNode) {
            if (ingroup) {
                result.open();
            }

            parseBlock(operator.getLeftOperand(), result, ingroup);
            parseBlock(operator.getRightOperand(), result, ingroup);

            if (ingroup) {
                result.close();
            }
        } else if (operator instanceof OrNode) {
            result.open();

            parseBlock(operator.getLeftOperand(), result, true);
            result.or();
            parseBlock(operator.getRightOperand(), result, true);

            result.close();
        } else if (operator instanceof EqualsNode) {
            parseEqualsNode(operator, result);
        } else if (operator instanceof NotEqualsNode) {
            result.not();

            parseEqualsNode(operator, result);
        } else if (operator instanceof StartsWith) {
            result.startsWith(getProperty(operator), getValue(operator));
        } else if (operator instanceof EndsWith) {
            result.endsWith(getProperty(operator), getValue(operator));
        } else if (operator instanceof GreaterThanNode) {
            GreaterThanNode greater = (GreaterThanNode) operator;

            if (greater.isOrEquals()) {
                result.greaterOrEq(getProperty(operator), getValue(operator));
            } else {
                result.greater(getProperty(operator), getValue(operator));
            }
        } else if (operator instanceof LesserThanNode) {
            LesserThanNode lesser = (LesserThanNode) operator;

            if (lesser.isOrEquals()) {
                result.lessOrEq(getProperty(operator), getValue(operator));
            } else {
                result.less(getProperty(operator), getValue(operator));
            }
        } else {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        }
    }

    private void parseOtherOperation(AbstractOperatorNode operator, SimpleEventQuery result, boolean ingroup)
        throws EventStreamException
    {
        if (operator instanceof InNode) {
            InNode inNode = (InNode) operator;
            if (inNode.getLeftOperand() instanceof PropertyValueNode) {
                List<Object> values = new ArrayList<>(inNode.getValues().size());
                for (AbstractValueNode valueNode : inNode.getValues()) {
                    values.add(getValue(valueNode));
                }

                result.in(getProperty((PropertyValueNode) inNode.getLeftOperand()), values);
            } else {
                // TODO: Unsupported
                throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
            }
        } else if (operator instanceof InSubQueryNode) {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        } else if (operator instanceof OrderByNode) {
            OrderByNode order = (OrderByNode) operator;

            parseBlock(order.getQuery(), result, ingroup);

            result.addSort(getProperty(order.getProperty()),
                order.getOrder() == Order.ASC ? org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order.ASC
                    : org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order.DESC);
        } else if (operator instanceof ForUserNode) {
            ForUserNode forUser = (ForUserNode) operator;

            if (forUser.getFormat() == NotificationFormat.ALERT) {
                if (forUser.getUser() != null && forUser.isRead() != null) {
                    result.withStatus(this.serializer.serialize(forUser.getUser()), forUser.isRead());
                } else if (forUser.getUser() != null) {
                    result.withStatus(this.serializer.serialize(forUser.getUser()));
                } else if (forUser.isRead() != null) {
                    result.withStatus(forUser.isRead());
                }
            } else if (forUser.getFormat() == NotificationFormat.EMAIL) {
                if (forUser.getUser() != null) {
                    result.open();
                    String entity = this.serializer.serialize(forUser.getUser());
                    result.withMail(entity);
                    if (forUser.isRead() != null) {
                        result.withStatus(entity, forUser.isRead());
                    }
                    result.close();
                } else if (forUser.isRead() != null) {
                    result.withStatus(forUser.isRead());
                }
            }
        } else {
            // TODO: Unsupported
            throw new EventStreamException(String.format(FORMAT_UNSUPPORTED_OPERATOR, operator));
        }
    }
}

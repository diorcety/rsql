/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.jirutka.rsql.visitor.hibernate;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EntityType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringRepresentableType;
import org.hibernate.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.jirutka.rsql.ComparisonOperator;
import cz.jirutka.rsql.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.RSQLOperators;
import cz.jirutka.rsql.StringUtils;
import cz.jirutka.rsql.ast.AndNode;
import cz.jirutka.rsql.ast.ComparisonNode;
import cz.jirutka.rsql.ast.Node;
import cz.jirutka.rsql.ast.OrNode;

public class HibernateCriterionVisitor extends NoArgRSQLVisitorAdapter<Criterion> {

    private static final String ALIAS_PREFIX = "__alias";
    private static final PropertyResolver DEFAULT_PROPERTY_RESOLVER = new PropertyResolver () {

        @Override
        public String getPropertyName(Class<?> type, String name) {
            return name;
        }
    };

    private final Session session;
    private final PropertyResolver propertyResolver;
    private final String queryClass;

    private ClassMetadata root;

    private Map<String, String> aliases = new LinkedHashMap<String, String>();
    private int aliasNum = 0;

    public HibernateCriterionVisitor(Session session, Class<?> queryClass) {
        this(session, queryClass.getName(), null);
    }

    public HibernateCriterionVisitor(Session session, Class<?> queryClass, PropertyResolver propertyResolver) {
        this(session, queryClass.getName(), propertyResolver);
    }

    public HibernateCriterionVisitor(Session session, String queryClass) {
        this(session, queryClass, null);
    }

    public HibernateCriterionVisitor(Session session, String queryClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver != null ? propertyResolver : DEFAULT_PROPERTY_RESOLVER;
        this.session = session;
        this.queryClass = queryClass;
        this.root = getSessionFactory().getClassMetadata(queryClass);
    }

    public Criteria createCriteria(Criterion criterion, Criteria criteria) {
        if (criteria == null) {
            criteria = session.createCriteria(queryClass);
        }
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            criteria.createAlias(entry.getKey(), entry.getValue());
        }
        criteria.add(criterion);
        return criteria;
    }

    private SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) session.getSessionFactory();
    }

    /*
     *
     * Path functions
     *
     */

    private Tuple<Type, String>[] getPath(ClassMetadata element, String name) {
        return getPath(element, name.split("\\."));
    }

    private Tuple<Type, String>[] getPath(ClassMetadata classMetadata, String[] names) {
        Tuple<Type, String>[] path = new Tuple[names.length];
        Object element = classMetadata;
        for (int i = 0; i < names.length; ++i) {
            String name = names[i];
            Type propertyType = getPropertyType(element, name);
            path[i] = new Tuple<Type, String>(propertyType, name);
            if (propertyType.isCollectionType()) {
                CollectionType collectionType = (CollectionType) propertyType;
                String associatedEntityName = collectionType.getRole();
                CollectionPersister collectionPersister = getSessionFactory().
                        getCollectionPersister(associatedEntityName);
                element = collectionPersister.getElementType();
            } else if (propertyType.isAssociationType()) {
                AssociationType associationType = (AssociationType) propertyType;
                String associatedEntityName = associationType.getAssociatedEntityName(getSessionFactory());
                element = getSessionFactory().getClassMetadata(associatedEntityName);
            } else if (propertyType.isComponentType()) {
                element = propertyType;
            }
        }
        return path;
    }

    private Type getPropertyType(Object element, String property) {
        if (element instanceof ClassMetadata) {
            return getPropertyType((ClassMetadata) element, property);
        } else if (element instanceof ComponentType) {
            return getPropertyType((ComponentType) element, property);
        } else if (element instanceof EntityType) {
            return getPropertyType((EntityType) element, property);
        } else if (element instanceof BasicType) {
            return getPropertyType((BasicType) element, property);
        }
        throw new IllegalArgumentException("Not handled: " + element.getClass());
    }

    private Type getPropertyType(ClassMetadata classMetadata, String property) {
        property = propertyResolver.getPropertyName(classMetadata.getMappedClass(), property);
        return classMetadata.getPropertyType(property);
    }

    private Type getPropertyType(EntityType entityType, String property) {
        String associatedEntityName = entityType.getAssociatedEntityName(getSessionFactory());
        ClassMetadata classMetadata = getSessionFactory().getClassMetadata(associatedEntityName);
        property = propertyResolver.getPropertyName(classMetadata.getMappedClass(), property);
        return getPropertyType(classMetadata, property);
    }

    private Type getPropertyType(ComponentType componentType, String property) {
        property = propertyResolver.getPropertyName(componentType.getReturnedClass(), property);
        int propertyIndex = componentType.getPropertyIndex(property);
        return componentType.getSubtypes()[propertyIndex];
    }

    private Type getPropertyType(BasicType basicType, String property) {
        if (CollectionPropertyNames.COLLECTION_ELEMENTS.equals(property)) {
            return basicType;
        } else if (CollectionPropertyNames.COLLECTION_INDEX.equals(property)) {
            return IntegerType.INSTANCE;
        }
        throw new IllegalArgumentException("Not handled: " + basicType + " with property: " + property);
    }

    /*
     *
     * Create Hibernate expression (using alias) functions
     *
     */

    private synchronized String createAlias() {
        String s = ALIAS_PREFIX + aliasNum;
        aliasNum++;
        return s;
    }

    private String getAlias(String prop) {
        String alias = aliases.get(prop);
        if (alias == null) {
            alias = createAlias();
            aliases.put(prop, alias);
        }
        return alias;
    }

    private Type getType(Tuple<Type, String>[] path) {
        return path[path.length - 1].first;
    }

    private String getExpression(Tuple<Type, String>[] path, boolean collection) {
        List<String> finalName = new LinkedList<String>();
        for (int i = 0; i < path.length; ++i) {
            Tuple<Type, String> p = path[i];
            Type propertyType = p.getFirst();
            finalName.add(p.getSecond());
            if (propertyType instanceof AssociationType) {
                /*
                 * 1 - Don't alias the last the last property
                 * 2 - Alias the last if the expression is not for a collection (see next part)
                 */
                if ((i < path.length - 1) || !collection) {
                    String pp = StringUtils.join(finalName, ".");
                    String alias = getAlias(pp);
                    finalName.clear();
                    finalName.add(alias);
                }
            }
        }
        return StringUtils.join(finalName, ".");
    }

    @Override
    public Criterion visit(AndNode node) {
        List<Node> children = node.getChildren();
        assert children.size() >= 2;
        Criterion previous = children.get(0).accept(this);
        for (int i = 1; i < children.size(); ++i) {
            previous = Restrictions.and(previous, children.get(i).accept(this));
        }
        return previous;
    }

    @Override
    public Criterion visit(OrNode node) {
        List<Node> children = node.getChildren();
        assert children.size() >= 2;
        Criterion previous = children.get(0).accept(this);
        for (int i = 1; i < children.size(); ++i) {
            previous = Restrictions.or(previous, children.get(i).accept(this));
        }
        return previous;
    }

    @Override
    public Criterion visit(ComparisonNode node) {
        String name = node.getSelector();
        /*
         Get the path and property names
         */
        Tuple<Type, String>[] path = getPath(root, name);
        Type type = getType(path);
        boolean isPrimitive = type instanceof StringRepresentableType;
        boolean isCustom = type instanceof CustomType;
        boolean isCollection = type instanceof CollectionType;

        String exp = getExpression(path, isCollection);
        List<Object> arguments = new ArrayList<Object>(node.getArguments().size());
        for (String argument : node.getArguments()) {
            Object value = argument;
            if (isPrimitive) {
                value = ((StringRepresentableType) type).fromStringValue((String) value);
            } else if (isCustom) {
                value = ((CustomType) type).stringToObject((String) value);
            } else if (isCollection) {
                value = IntegerType.INSTANCE.fromString((String) value);
            }
            if (value instanceof String) {
                value = toSqlWildcardString((String) value);
            }
            arguments.add(value);
        }
        ComparisonOperator operator = node.getOperator();

        assert arguments.size() >= 1;

        Object firstArgument = arguments.get(0);
        if (operator.equals(RSQLOperators.EQUAL)) {
            if (!isCollection) {
                if (String.class.isInstance(firstArgument) && ((String) firstArgument).contains("%")) {
                    return Restrictions.like(exp, firstArgument);
                } else {
                    return Restrictions.eq(exp, firstArgument);
                }
            } else {
                return Restrictions.sizeEq(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.NOT_EQUAL)) {
            if (!isCollection) {
                if (String.class.isInstance(firstArgument) && ((String) firstArgument).contains("%")) {
                    return Restrictions.not(Restrictions.like(exp, firstArgument));
                } else {
                    return Restrictions.ne(exp, firstArgument);
                }
            } else {
                return Restrictions.sizeNe(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.GREATER_THAN)) {
            if (!isCollection) {
                return Restrictions.gt(exp, firstArgument);
            } else {
                return Restrictions.sizeGt(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.GREATER_THAN_OR_EQUAL)) {
            if (!isCollection) {
                return Restrictions.ge(exp, firstArgument);
            } else {
                return Restrictions.sizeGe(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.LESS_THAN)) {
            if (!isCollection) {
                return Restrictions.lt(exp, firstArgument);
            } else {
                return Restrictions.sizeLt(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.LESS_THAN_OR_EQUAL)) {
            if (!isCollection) {
                return Restrictions.le(exp, firstArgument);
            } else {
                return Restrictions.sizeLe(exp, (Integer) firstArgument);
            }
        } else if (operator.equals(RSQLOperators.IN)) {
            if (!isCollection) {
                return Restrictions.in(exp, arguments);
            }
        } else if (operator.equals(RSQLOperators.NOT_IN)) {
            if (!isCollection) {
                return Restrictions.not(Restrictions.in(exp, arguments));
            }
        }
        throw new IllegalArgumentException("Unknown operation " + operator.toString() + " for property" + name);
    }

    public static String toSqlWildcardString(String value) {
        if (value.startsWith("*")) {
            value = "%" + value.substring(1);
        }
        if (value.endsWith("*")) {
            value = value.substring(0, value.length() - 1) + "%";
        }
        return value;
    }

    public class Tuple<X, Y> {

        private final X first;
        private final Y second;

        public Tuple(X first, Y second) {
            this.first = first;
            this.second = second;
        }

        public X getFirst() {
            return first;
        }

        public Y getSecond() {
            return second;
        }
    }

    public interface PropertyResolver {

        String getPropertyName(Class<?> type, String name);

    }
}

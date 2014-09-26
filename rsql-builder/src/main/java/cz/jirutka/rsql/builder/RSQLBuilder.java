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
package cz.jirutka.rsql.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cz.jirutka.rsql.ComparisonOperator;
import cz.jirutka.rsql.RSQLOperators;
import cz.jirutka.rsql.ast.AndNode;
import cz.jirutka.rsql.ast.ComparisonNode;
import cz.jirutka.rsql.ast.LogicalNode;
import cz.jirutka.rsql.ast.Node;
import cz.jirutka.rsql.ast.OrNode;

public class RSQLBuilder {

    private static final Printer DEFAULT_PRINTER = new Printer() {
        @Override
        public String toString(Object value) {
            return value.toString();
        }
    };

    private final Printer printer;

    private class PartialConditionImpl implements PartialCondition {

        private LogicalNode previous;

        public PartialConditionImpl(LogicalNode previous) {
            this.previous = previous;
        }

        @Override
        public Property is(String property) {
            return new PropertyImpl(previous, property);
        }

        private List<Node> getNodes(List<CompleteCondition> conditions) {
            ArrayList<Node> arrayList = new ArrayList<Node>(conditions.size());
            for (CompleteCondition condition : conditions) {
                arrayList.add(((CompleteConditionImpl) condition).getNode());
            }
            return arrayList;
        }

        @Override
        public CompleteCondition and(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            List<CompleteCondition> arrayList = new ArrayList<CompleteCondition>(cn.length + 2);
            arrayList.add(c1);
            arrayList.add(c2);
            Collections.addAll(arrayList, cn);
            return and(arrayList);
        }

        @Override
        public CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            List<CompleteCondition> arrayList = new ArrayList<CompleteCondition>(cn.length + 2);
            arrayList.add(c1);
            arrayList.add(c2);
            Collections.addAll(arrayList, cn);
            return or(arrayList);
        }

        @Override
        public CompleteCondition and(List<CompleteCondition> conditions) {
            return new CompleteConditionImpl(append(previous, new AndNode(getNodes(conditions))));
        }

        @Override
        public CompleteCondition or(List<CompleteCondition> conditions) {
            return new CompleteConditionImpl(append(previous, new OrNode(getNodes(conditions))));
        }
    }

    private class PropertyImpl implements Property {

        private final String name;
        private final LogicalNode previous;

        public PropertyImpl(LogicalNode previous, String name) {
            this.previous = previous;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public CompleteCondition comparesTo(ComparisonOperator operator, Object value) {
            return condition(operator, value);
        }

        public CompleteCondition equalTo(Object value) {
            return condition(RSQLOperators.EQUAL, value);
        }

        public CompleteCondition notEqualTo(Object value) {
            return condition(RSQLOperators.NOT_EQUAL, value);
        }

        public CompleteCondition greaterOrEqualTo(Object value) {
            return condition(RSQLOperators.GREATER_THAN_OR_EQUAL, value);
        }

        public CompleteCondition greaterThan(Object value) {
            return condition(RSQLOperators.GREATER_THAN, value);
        }

        public CompleteCondition lessOrEqualTo(Object value) {
            return condition(RSQLOperators.LESS_THAN_OR_EQUAL, value);
        }

        public CompleteCondition lessThan(Object value) {
            return condition(RSQLOperators.LESS_THAN, value);
        }

        protected CompleteCondition condition(ComparisonOperator operator, Object... values) {
            ArrayList<String> arrayList = new ArrayList<String>(values.length);
            for (Object value : values) {
                arrayList.add(RSQLBuilder.this.printer.toString(value));
            }
            Node node = new ComparisonNode(operator, name, arrayList);
            return new CompleteConditionImpl(append(previous, node));
        }
    }

    private class CompleteConditionImpl implements CompleteCondition {

        private final Node node;

        public CompleteConditionImpl(Node node) {
            this.node = node;
        }

        public Node getNode() {
            return node;
        }

        @Override
        public PartialCondition and() {
            return new PartialConditionImpl(new AndNode(Arrays.asList(node)));
        }

        @Override
        public Property and(String name) {
            return and().is(name);
        }

        @Override
        public PartialCondition or() {
            return new PartialConditionImpl(new OrNode(Arrays.asList(node)));
        }

        @Override
        public Property or(String name) {
            return or().is(name);
        }

        @Override
        public String query() {
            return node.toString();
        }
    }

    private RSQLBuilder(Printer printer) {
        this.printer = printer;
    }

    public static PartialCondition create() {
        return new RSQLBuilder(DEFAULT_PRINTER).getPartialCondition();
    }

    public static PartialCondition create(Printer printer) {
        return new RSQLBuilder(printer).getPartialCondition();
    }

    private PartialCondition getPartialCondition() {
        return new PartialConditionImpl(null);
    }

    private static Node append(LogicalNode logicalNode, Node node) {
        if (logicalNode != null) {
            List<Node> children = logicalNode.getChildren();
            children.add(node);
            return logicalNode.withChildren(children);
        }
        return node;
    }

    public interface Printer {

        String toString(Object value);
    }
}

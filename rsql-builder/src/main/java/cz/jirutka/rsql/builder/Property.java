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

import cz.jirutka.rsql.ComparisonOperator;

/**
 * Part of fluent interface of {@link SearchConditionBuilder}.
 */
public interface Property {

    /** Is textual property equal to given value? */
    CompleteCondition equalTo(Object value);

    /** Is textual property different than given value? */
    CompleteCondition notEqualTo(Object value);

    /** Is numeric property greater than given value? */
    CompleteCondition greaterThan(Object value);

    /** Is numeric property less than given value? */
    CompleteCondition lessThan(Object value);

    /** Is numeric property greater or equal to given value? */
    CompleteCondition greaterOrEqualTo(Object value);

    /** Is numeric property less or equal to given value? */
    CompleteCondition lessOrEqualTo(Object value);

    /** Generic */
    CompleteCondition comparesTo(ComparisonOperator op, Object value);
}

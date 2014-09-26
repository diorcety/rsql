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

/**
 * Part of fluent interface of {@link SearchConditionBuilder}.
 */
public interface CompleteCondition {

    /** Conjunct current expression with another */
    PartialCondition and();

    /** shortcut for and().is() */
    Property and(String name);

    /** Disjunct current expression with another */
    PartialCondition or();

    /** shortcut for or().is() */
    Property or(String name);

    /** Finalize condition construction and build search condition query. */
    String query();
}

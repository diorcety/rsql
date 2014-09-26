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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import cz.jirutka.rsql.RSQLOperators;

public class RSQLBuilderTest extends Assert {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final RSQLBuilder.Printer DATE_PRINTER = new RSQLBuilder.Printer() {
        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

        @Override
        public String toString(Object value) {
            if (value instanceof Date) {
                return simpleDateFormat.format((Date) value);
            }
            return value.toString();
        }
    };

    private static TimeZone tz;

    @BeforeClass
    public static void beforeClass() {
        tz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @AfterClass
    public static void afterClass() {
        // restoring defaults
        TimeZone.setDefault(tz);
    }

    @Test
    public void testEqualToString() {
        String ret = RSQLBuilder.create().is("foo").equalTo("literalOrPattern*").query();
        Assert.assertEquals("foo=='literalOrPattern*'", ret);
    }

    @Test
    public void testEqualToNumber() {
        String ret = RSQLBuilder.create().is("foo").equalTo(123.5).query();
        Assert.assertEquals("foo=='123.5'", ret);
    }

    @Test
    public void testEqualToNumberCondition() {
        String ret = RSQLBuilder.create().is("foo").comparesTo(RSQLOperators.LESS_THAN, 123.5).query();
        Assert.assertEquals("foo=lt='123.5'", ret);
    }

    private Date parseDate(String format, String value) throws ParseException {
        return new SimpleDateFormat(format).parse(value);
    }

    @Test
    public void testEqualToDateDefault() throws ParseException {
        Date d = parseDate(DEFAULT_DATE_FORMAT, "2011-03-01");
        String ret = RSQLBuilder.create(DATE_PRINTER).is("foo").equalTo(d).query();
        Assert.assertEquals("foo=='2011-03-01'", ret);
    }

    @Test
    public void testEqualToDuration() throws ParseException, DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0);
        String ret = RSQLBuilder.create().is("foo").equalTo(d).query();
        Assert.assertEquals("foo=='-P0Y0M1DT12H0M0S'", ret);
    }

    @Test
    public void testNotEqualToString() {
        String ret = RSQLBuilder.create().is("foo").notEqualTo("literalOrPattern*").query();
        Assert.assertEquals("foo!='literalOrPattern*'", ret);
    }

    @Test
    public void testNotEqualToNumber() {
        String ret = RSQLBuilder.create().is("foo").notEqualTo(123.5).query();
        Assert.assertEquals("foo!='123.5'", ret);
    }

    @Test
    public void testNotEqualToDateDefault() throws ParseException {
        Date d = parseDate(DEFAULT_DATE_FORMAT, "2011-03-01");
        String ret = RSQLBuilder.create(DATE_PRINTER).is("foo").notEqualTo(d).query();
        Assert.assertEquals("foo!='2011-03-01'", ret);
    }

    @Test
    public void testNotEqualToDuration() throws ParseException, DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0);
        String ret = RSQLBuilder.create().is("foo").notEqualTo(d).query();
        Assert.assertEquals("foo!='-P0Y0M1DT12H0M0S'", ret);
    }

    @Test
    public void testGreaterThanNumberDouble() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(25.0).query();
        Assert.assertEquals("foo=gt='25.0'", ret);
    }

    @Test
    public void testGreaterThanLong() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(25).query();
        Assert.assertEquals("foo=gt='25'", ret);
    }

    @Test
    public void testLessThanNumber() {
        String ret = RSQLBuilder.create().is("foo").lessThan(25.333).query();
        Assert.assertEquals("foo=lt='25.333'", ret);
    }

    @Test
    public void testLessOrEqualToNumberDouble() {
        String ret = RSQLBuilder.create().is("foo").lessOrEqualTo(0.0).query();
        Assert.assertEquals("foo=le='0.0'", ret);
    }

    @Test
    public void testLessOrEqualToNumberLong() {
        String ret = RSQLBuilder.create().is("foo").lessOrEqualTo(0).query();
        Assert.assertEquals("foo=le='0'", ret);
    }

    @Test
    public void testGreaterOrEqualToNumberDouble() {
        String ret = RSQLBuilder.create().is("foo").greaterOrEqualTo(-5.0).query();
        Assert.assertEquals("foo=ge='-5.0'", ret);
    }

    @Test
    public void testGreaterOrEqualToNumberLong() {
        String ret = RSQLBuilder.create().is("foo").greaterOrEqualTo(-5).query();
        Assert.assertEquals("foo=ge='-5'", ret);
    }

    @Test
    public void testOrSimple() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(20).or().is("foo").lessThan(10).query();
        Assert.assertEquals("(foo=gt='20',foo=lt='10')", ret);
    }

    @Test
    public void testOrSimpleShortcut() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(20).or("foo").lessThan(10).query();
        Assert.assertEquals("(foo=gt='20',foo=lt='10')", ret);
    }

    @Test
    public void testAndSimple() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(20).and().is("bar").equalTo("plonk").query();
        Assert.assertEquals("(foo=gt='20';bar=='plonk')", ret);
    }

    @Test
    public void testAndSimpleShortcut() {
        String ret = RSQLBuilder.create().is("foo").greaterThan(20).and("bar").equalTo("plonk").query();
        Assert.assertEquals("(foo=gt='20';bar=='plonk')", ret);
    }

    @Test
    public void testOrComplex() {
        String ret = RSQLBuilder.create().or(RSQLBuilder.create().is("foo").equalTo("aaa"),
                RSQLBuilder.create().is("bar").equalTo("bbb")).query();
        Assert.assertEquals("(foo=='aaa',bar=='bbb')", ret);
    }

    @Test
    public void testAndComplex() {
        String ret = RSQLBuilder.create().and(RSQLBuilder.create().is("foo").equalTo("aaa"),
                RSQLBuilder.create().is("bar").equalTo("bbb")).query();
        Assert.assertEquals("(foo=='aaa';bar=='bbb')", ret);
    }

    @Test
    public void testComplex1() {
        String ret = RSQLBuilder.create().is("foo").equalTo(123.4).or().and(
                RSQLBuilder.create().is("bar").equalTo("asadf*"),
                RSQLBuilder.create().is("baz").lessThan(20)).query();
        Assert.assertEquals("(foo=='123.4',(bar=='asadf*';baz=lt='20'))", ret);
    }

    @Test
    public void testComplex2() {
        String ret = RSQLBuilder.create().is("foo").equalTo(123L).or().is("foo").equalTo("null").and().or(
                RSQLBuilder.create().is("bar").equalTo("asadf*"),
                RSQLBuilder.create().is("baz").lessThan(20).and().or(
                        RSQLBuilder.create().is("sub1").equalTo(0L),
                        RSQLBuilder.create().is("sub2").equalTo(0L))).query();

        Assert.assertEquals("((foo=='123',foo=='null');(bar=='asadf*',(baz=lt='20';(sub1=='0',sub2=='0'))))", ret);
    }
}

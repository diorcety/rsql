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

import org.junit.Test;

import java.util.List;

import cz.jirutka.rsql.visitor.hibernate.persistence.Book;

public class HibernateVisitorTest extends AbstractHibernateVisitorTest {

    @Test
    public void testOrQuery() throws Exception {
        List<Book> books = queryBooks("id=lt=10,id=gt=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 11 == books.get(1).getId()
                || 11 == books.get(0).getId() && 9 == books.get(1).getId());
    }

    @Test
    public void testOrQueryNoMatch() throws Exception {
        List<Book> books = queryBooks("id==7,id==5");
        assertEquals(0, books.size());
    }

    @Test
    public void testAndQuery() throws Exception {
        List<Book> books = queryBooks("id==10;bookTitle==num10");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId() && "num10".equals(books.get(0).getBookTitle()));
    }

    @Test
    public void testQueryCollection() throws Exception {
        List<Book> books =
                queryBooks("reviews.authors.elements==Ted");
        assertEquals(3, books.size());
    }

    @Test
    public void testQueryCollection2() throws Exception {
        List<Book> books =
                queryBooks("reviews.book.id==10");
        assertEquals(1, books.size());
    }

    @Test
    public void testQueryCollection3() throws Exception {
        List<Book> books =
                queryBooks("reviews.book.ownerInfo.name.name==Barry");
        assertEquals(1, books.size());
    }

    @Test
    public void testQueryElementCollection() throws Exception {
        List<Book> books =
                queryBooks("authors.elements==John");
        assertEquals(2, books.size());
    }

    @Test
    public void testNumberOfReviews() throws Exception {
        List<Book> books =
                queryBooks("reviews=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfReviews2() throws Exception {
        List<Book> books =
                queryBooks("reviews=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testNumberOfReviewAuthors() throws Exception {
        List<Book> books =
                queryBooks("reviews.authors=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfReviewAuthors2() throws Exception {
        List<Book> books =
                queryBooks("reviews.authors=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testNumberOfAuthors() throws Exception {
        List<Book> books =
                queryBooks("authors=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfAuthors2() throws Exception {
        List<Book> books =
                queryBooks("authors=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testQueryCollectionSize2() throws Exception {
        List<Book> books =
                queryBooks("reviews.authors=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testAndQueryCollection() throws Exception {
        List<Book> books =
                queryBooks("id==10;authors.elements==John;reviews.review==GOOD;reviews.authors.elements==Ted");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId() && "num10".equals(books.get(0).getBookTitle()));
    }

    @Test
    public void testAndQueryNoMatch() throws Exception {
        List<Book> books = queryBooks("id==10;bookTitle==num9");
        assertEquals(0, books.size());
    }

    @Test
    public void testEqualsQuery() throws Exception {
        List<Book> books = queryBooks("id==10");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId());
    }

    @Test
    public void testEqualsCriteriaQueryCount() throws Exception {
        assertEquals(1L, criteriaQueryBooksCount("id==10"));
    }

    @Test
    public void testEqualsOwnerNameQuery() throws Exception {
        List<Book> books = queryBooks("ownerInfo.name.name==Fred");
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Fred", book.getOwnerInfo().getName().getName());
    }


    @Test
    // "ownerInfo.name" maps to Name class and this
    // does not work in OpenJPA, as opposed to Hibernate
    // "ownerInfo.name.name" will map to primitive type, see
    // testEqualsOwnerNameQuery3(), which also works in OpenJPA
    public void testEqualsOwnerNameQuery2() throws Exception {
        List<Book> books = queryBooks("ownerInfo.name.name==Fred");
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Fred", book.getOwnerInfo().getName().getName());
    }

    @Test
    public void testEqualsWildcard() throws Exception {
        List<Book> books = queryBooks("bookTitle==num1*");
        assertEquals(2, books.size());
        assertTrue(10 == books.get(0).getId() && 11 == books.get(1).getId()
                || 11 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testGreaterQuery() throws Exception {
        List<Book> books = queryBooks("id=gt=10");
        assertEquals(1, books.size());
        assertTrue(11 == books.get(0).getId());
    }

    @Test
    public void testGreaterEqualQuery() throws Exception {
        List<Book> books = queryBooks("id=ge=10");
        assertEquals(2, books.size());
        assertTrue(10 == books.get(0).getId() && 11 == books.get(1).getId()
                || 11 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testLessEqualQuery() throws Exception {
        List<Book> books = queryBooks("id=le=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 10 == books.get(1).getId()
                || 9 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testNotEqualsQuery() throws Exception {
        List<Book> books = queryBooks("id!=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 11 == books.get(1).getId()
                || 11 == books.get(0).getId() && 9 == books.get(1).getId());
    }
}

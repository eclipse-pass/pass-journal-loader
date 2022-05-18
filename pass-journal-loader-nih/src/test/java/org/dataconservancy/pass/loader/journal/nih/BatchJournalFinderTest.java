/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.pass.loader.journal.nih;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class BatchJournalFinderTest {

    private final String uri1 = "test:1";

    @Test
    public void journalFindTest() throws Exception {

        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNotNull(toTest.find(null, "Test 1 Journal", Collections.singletonList("0000-0001")));
            assertNotNull(toTest.find(null, "Test 2 Journal", Collections.singletonList("0000-0002")));
            //next would resolve to test:2, but that was just found, so we skip processing it
            assertEquals("SKIP", toTest.find(null, "Test 2 Journal", Collections.singletonList("0000-0002X")));

        }
    }

    @Test
    public void journalNotFoundTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNull(toTest.find(null, null, Collections.singletonList("0000-000")));

        }
    }

    @Test
    public void manualAddTest() {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        final URI ID = URI.create("test/uri");
        final String ISSN1 = "000-001";
        final String ISSN2 = "000-002";

        final Journal toAdd = new Journal();
        toAdd.setId(ID);
        toAdd.setIssns(Arrays.asList(ISSN1, ISSN2));
        toAdd.setPmcParticipation(PmcParticipation.A);

        toTest.add(toAdd);

        final String found = toTest.find(null, null, Arrays.asList(ISSN1, ISSN2));
        assertNotNull(found);

    }

    @Test
    public void insufficientMatchTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }

        //only one element matches - this should return null
        final String found = toTest.find(null, null, Collections.singletonList("0000-0001"));
        assertNull(found);
    }

    @Test
    public void nameAndOneIssnMatchTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }

        //two elements match -
        final String found = toTest.find(null, "Journal One", Collections.singletonList("0000-0001"));
        assertNotNull(found);
        assertEquals(uri1, found);
    }

    @Test
    public void twoIssnsMatchTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }

        //two elements match -
        final String found = toTest.find(null, null, Arrays.asList("0000-0001", "0000-0002"));
        assertNotNull(found);
        assertEquals(uri1, found);
    }

    @Test
    public void nlmtaAndIssnMatchTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }

        //two elements match -
        final String found = toTest.find("NLMTA1", null, Collections.singletonList("0000-0002"));
        assertNotNull(found);
        assertEquals(uri1, found);
    }

    @Test
    public void duplicateJournalTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }
        //two elements match -
        String found = toTest.find("NLMTA1", "Journal One", Collections.singletonList("0000-0001"));
        assertNotNull(found);
        assertEquals(uri1, found);

        //should be flagged as a duplicate
        found = toTest.find("NLMTA1", "Journal One", Collections.singletonList("0000-0001"));
        assertNotNull(found);
        assertEquals("SKIP", found);

    }

    @Test
    public void cascadingJournalTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }
        //two elements match -
        String found = toTest.find("NLMTA3", "Journal Three", Arrays.asList("0000-0005", "0000-0006"));
        assertNotNull(found);
        String uri3 = "test:3";
        assertEquals(uri3, found);

        //first uri is now removed from consideration - find next best qualifying match
        found = toTest.find("NLMTA3", "Journal Three", Arrays.asList("0000-0005", "0000-0006"));
        assertNotNull(found);
        String uri4 = "test:4";
        assertEquals(uri4, found);

        found = toTest.find("NLMTA3", "Journal Three", Arrays.asList("0000-0005", "0000-0006"));
        assertNotNull(found);
        String uri5 = "test:5";
        assertEquals(uri5, found);

    }

    @Test
    public void newStyleIssnTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }
        //two elements match -
        final String found = toTest.find("NLMTA2", null, Arrays.asList("Print:0000-0003", "Online:0000-0004"));
        assertNotNull(found);
        String uri2 = "test:2";
        assertEquals(uri2, found);

    }

}

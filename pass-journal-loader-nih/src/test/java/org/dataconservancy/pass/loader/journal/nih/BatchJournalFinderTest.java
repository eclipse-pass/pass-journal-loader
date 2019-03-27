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

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author apb@jhu.edu
 */
public class BatchJournalFinderTest {

    @Test
    public void journalFindTest() throws Exception {

        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNotNull(toTest.find(null, "Test 1 Journal", Arrays.asList("0000-0001")));
            assertNotNull(toTest.find(null, "Test 2 Journal", Arrays.asList("0000-0002")));
            assertEquals("INCONCLUSIVE",(toTest.find(null, null, Arrays.asList("0000-0002X"))));

        }
    }

    @Test
    public void journalNotFoundTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNull(toTest.find(null, null, Arrays.asList("0000-000")));

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
    public void generalFindTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/moreJournals.nt")) {

            toTest.load(in);
        }

        final String uri1 = "test:1";
        final String uri2 = "test:2";
        final String uri3 = "test:3";
        final String uri4 = "test:4";

        //nothing matches, retyrn null to make loader create a new journal
        String found = toTest.find(null, null, Arrays.asList("0000-0006"));
        assertNull(found);

        //matches only one data point - skip it?
        found = toTest.find(null, null, Arrays.asList("0000-0001"));
        assertNotNull(found);
        assertEquals("INCONCLUSIVE", found);

        //this matches test:1
        found = toTest.find(null, "Journal One", Arrays.asList("0000-0001"));
        assertNotNull(found);
        assertEquals(uri1, found);

        //test:1 already found, do not process again
        found = toTest.find(null, "Journal One", Arrays.asList("0000-0001"));
        assertNotNull(found);
        assertEquals("SKIP", found);

        //test that new issns are processed
        found = toTest.find("NLMTA2", null, Arrays.asList("Print:0000-0003"));
        assertNotNull(found);
        assertEquals(uri2, found);

        found = toTest.find("NLMTA3", "Journal Three", Arrays.asList("0000-0001"));
        assertNotNull(found);
        assertEquals(uri3, found);

    }
}

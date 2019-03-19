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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dataconservancy.pass.client.fedora.FedoraConfig;
import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class BatchJournalFinder implements JournalFinder {

    Logger LOG = LoggerFactory.getLogger(BatchJournalFinder.class);

    Map<String, String> issnMap = new HashMap<>();

    Map<String, String> nlmtaMap = new HashMap<>();

    Set<String> typeARefs = new HashSet<>();

    private static final String ISSNS = "http://oapass.org/ns/pass#issn";

    private static final String NLMTAS = "http://oapass.org/ns/pass#nlmta";

    private static final String PMC_PARTICIPATION = "http://oapass.org/ns/pass#pmcParticipation";

    void load(InputStream ntriples) throws IOException {
        try (InputStream in = ntriples) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                final String[] spo = line.split("\\s+");

                final String uri = ntripleUri(spo[0]);
                final String predicate = ntripleUri(spo[1]);

                if (predicate.equals(ISSNS)) {
                    final String issn = ntripLiteral(spo[2]);
                    if (issnMap.putIfAbsent(issn, uri) != null) {
                        LOG.warn("Two records contain the same issn {}: <{}>, <{}>", issn, uri, issnMap.get(
                                issn));
                    }
                }

                if (predicate.equals(NLMTAS)) {
                    final String nlmta = ntripLiteral(line);//spaces inside quotes mess split up - need to operate on line
                    if (nlmtaMap.putIfAbsent(nlmta, uri) != null) {
                        LOG.warn("Two records contain the same nlmta {}: <{}>, <{}>", nlmta, uri, nlmtaMap.get(
                                nlmta));
                    }
                }

                if (predicate.equals(PMC_PARTICIPATION) && "A".equals(ntripLiteral(spo[2]))) {
                    typeARefs.add(uri);
                }
            }
        }
    }

    public BatchJournalFinder() {

    }

    public BatchJournalFinder(String journalContainer) throws Exception {

        LOG.info("Analyzing journals in " + journalContainer);

        final HttpGet get = new HttpGet(journalContainer);
        get.setHeader("Accept", "application/n-triples");
        get.setHeader("Prefer",
                "return=representation; include=\"http://fedora.info/definitions/v4/repository#EmbedResources\"; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");

        try (CloseableHttpResponse response = getHttpClient().execute(get)) {
            load(response.getEntity().getContent());
        }

        LOG.info("Found {} existing ISSNs", issnMap.size());
        LOG.info("Found {} existing NLMTAs", nlmtaMap.size());
        LOG.info("Found {} PMC A journals", typeARefs.size());
    }

    @Override
    public synchronized Journal byIssn(String issn) {
        String id = getUriByIssn(issn);
        if (id != null) {
            final Journal j = new Journal();
            j.setId(URI.create(id));
            if (typeARefs.contains(j.getId().toString())) {
                j.setPmcParticipation(PmcParticipation.A);
            }
            return j;
        }

        return null;
    }

    @Override
    public synchronized Journal byNlmta(String nlmta) {
        String id = getUriByNlmta(nlmta);
        if (id != null) {
            final Journal j = new Journal();
            j.setId(URI.create(id));
            if (typeARefs.contains(j.getId().toString())) {
                j.setPmcParticipation(PmcParticipation.A);
            }
            return j;
        }

        return null;
    }

    private String getUriByIssn(String issn) {
        if (issnMap.containsKey(issn)) {
            return issnMap.get(issn);
        }
        
        String[] parts = issn.split(":");
        
        if (parts.length == 2) {
            return issnMap.get(parts[1]);
        }
        
        return null;
        
    }

    private String getUriByNlmta(String nlmta) {
        if (nlmtaMap.containsKey(nlmta)) {
            return nlmtaMap.get(nlmta);
        }

        return null;
    }

    static CloseableHttpClient getHttpClient() {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(FedoraConfig.getUserName(),
                FedoraConfig.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);

        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
    }

    static String ntripleUri(String token) {
        final int s = token.indexOf("<");
        final int f = token.indexOf(">");
        if (s != -1 && f != -1) {
            return token.substring(s + 1, f);
        }

        return null;
    }

    static String ntripLiteral(String token) {
        final int s = token.indexOf("\"");
        final int f = token.indexOf("\"", s + 1);
        if (s != -1 && f != -1) {
            return token.substring(s + 1, f);
        }

        return null;
    }

    @Override
    public synchronized void add(Journal j) {

        boolean copacetic = true;

        String nlmta = j.getNlmta();
        if(nlmta != null && nlmta.length() > 0 ) {
            LOG.debug("Adding nlmta " + nlmta);
            final String uri = nlmtaMap.putIfAbsent(nlmta, j.getId().toString());
            if(uri != null && !uri.equals(j.getId().toString())) {
                LOG.warn("Two records contain the same nlmta {}: <{}>, <{}>", nlmta, j.getId(), nlmtaMap.get(
                        nlmta));
                copacetic = false;
            }
        }

        for (final String issn : j.getIssns()) {
            LOG.debug("Adding issn " + issn);
            final String uri = issnMap.putIfAbsent(issn, j.getId().toString());

            if (uri != null && !uri.equals(j.getId().toString())) {
                LOG.warn("Two records contain the same issn {}: <{}>, <{}>", issn, j.getId(), issnMap.get(
                        issn));
            } else {
                copacetic = true;
            }
        }

        if (copacetic) {
            if (j.getPmcParticipation() == PmcParticipation.A) {
                typeARefs.add(j.getId().toString());
            }
        }
    }
}

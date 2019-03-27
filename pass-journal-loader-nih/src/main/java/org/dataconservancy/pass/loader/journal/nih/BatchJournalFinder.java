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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.dataconservancy.pass.client.fedora.FedoraConfig;
import org.dataconservancy.pass.model.Journal;

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

    Map<String, Set<String>> issnMap = new HashMap<>();

    Map<String, Set<String>> nlmtaMap = new HashMap<>();

    Map<String, Set<String>> nameMap = new HashMap<>();

    Set<String> foundUris = new HashSet<>();

    private static final String ISSNS = "http://oapass.org/ns/pass#issn";

    private static final String NLMTAS = "http://oapass.org/ns/pass#nlmta";

    private static final String NAMES = "http://oapass.org/ns/pass#journalName";

    void load(InputStream ntriples) throws IOException {
        try (InputStream in = ntriples) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                final String[] spo = line.split("\\s+");

                final String uri = ntripleUri(spo[0]);
                final String predicate = ntripleUri(spo[1]);

                if (predicate.equals(ISSNS)) {
                    final String issn = ntripLiteral(spo[2]);
                    if (!issnMap.containsKey(issn)) {
                        issnMap.put(issn, new HashSet<>());
                    }
                    issnMap.get(issn).add(uri);
                }

                if (predicate.equals(NLMTAS)) {
                    final String nlmta = ntripLiteral(line);//spaces inside quotes mess split up - need to operate on line
                    if (!nlmtaMap.containsKey(nlmta)) {
                       nlmtaMap.put(nlmta, new HashSet<>());
                    }
                    nlmtaMap.get(nlmta).add(uri);
                }

                if (predicate.equals(NAMES)) {
                    final String name = ntripLiteral(line);//spaces inside quotes mess split up - need to operate on line
                    if (!nameMap.containsKey(name)) {
                        nameMap.put(name, new HashSet<>());
                    }
                    nameMap.get(name).add(uri);
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
        LOG.info("Found {} existing NAMES", nameMap.size());
    }

    @Override
    public synchronized String find(String nlmta, String name, List<String> issns) {
        Set<String> nlmtaUriSet = getUrisByNlmta(nlmta);
        Set<String> nameUriSet = getUrisByName(name);

        Map<String, Integer> uriScores = new HashMap<>();

        if (!issns.isEmpty()) {
            for (String issn : issns) {
                if (getUrisByIssn(issn) != null) {
                    for(String uri : getUrisByIssn(issn)){
                        Integer i = uriScores.putIfAbsent(uri, 1);
                        if (i != null) {
                            uriScores.put(uri, i + 1);
                        }
                    }
                }
            }
        }


        if (nlmtaUriSet != null) {
            for (String uri : nlmtaUriSet) {
                Integer i = uriScores.putIfAbsent(uri, 1);
                if (i != null) {
                    uriScores.put(uri, i + 1);
                }
            }
        }

        if (nameUriSet != null) {
            for (String uri : nameUriSet) {
                Integer i = uriScores.putIfAbsent(uri, 1);
                if (i != null) {
                    uriScores.put(uri, i + 1);
                }
            }
        }


        if(uriScores.size()>0) {//we have a possible uri - find out if it is matchy enough
            Integer highScore = Collections.max(uriScores.values());
            int minimumQualifyingScore = 2;
            List<String> sortedUris = new ArrayList<>();

            for (int i = highScore; i >= minimumQualifyingScore; i--) {
                for (String uri : uriScores.keySet()) {
                    if(uriScores.get(uri) == i) {
                        sortedUris.add(uri);
                    }
                }
            }

            if (sortedUris.size() > 0 ) {// there are matching journals - decide if we have matched already
                String foundUri = null;
                for (int i = 0; i < sortedUris.size() ; i++) {
                    String candidate = sortedUris.get(i);
                    if ( !foundUris.contains(candidate)) {
                        foundUri = candidate;
                        break;
                    }
                }
                if (foundUri != null) {
                    foundUris.add(foundUri);
                    return foundUri;
                } else {//this journal has been processed already
                    return "SKIP";
                }
            }
            //TODO - had somthing matching, but not definitive enough
            return "INCONCLUSIVE";
        } //nothing matches, create a new journal
        return null;
    }


    private synchronized Set<String> getUrisByIssn(String issn) {
        if (issnMap.containsKey(issn)) {
            return issnMap.get(issn);
        }
        
        String[] parts = issn.split(":");
        
        if (parts.length == 2) {
            return issnMap.get(parts[1]);
        }
        
        return null;
        
    }

    private synchronized Set<String> getUrisByNlmta(String nlmta) {
        if (nlmta != null && nlmta.length()>0 && nlmtaMap.containsKey(nlmta)) {
            return nlmtaMap.get(nlmta);
        }

        return null;
    }


    private synchronized Set<String> getUrisByName(String name) {
        if (name != null && name.length()>0 &&  nameMap.containsKey(name)) {
            return nameMap.get(name);
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

        String uri = j.getId().toString();

        String nlmta = j.getNlmta();
        if (nlmta != null && nlmta.length() > 0) {
            LOG.debug("Adding nlmta " + nlmta);
            if (!nlmtaMap.containsKey(nlmta)) {
                nlmtaMap.put(nlmta, new HashSet<>());
            }
            nlmtaMap.get(nlmta).add(uri);
        }

        for (final String issn : j.getIssns()) {
            LOG.debug("Adding issn " + issn);
            if (!issnMap.containsKey(issn)) {
                issnMap.put(issn, new HashSet<>());
            }
            issnMap.get(issn).add(uri);
        }

        String name = j.getName();
        if (name != null && name.length() > 0) {
            if (!nameMap.containsKey(j.getName())) {
                nameMap.put(name, new HashSet<>());
        }
        nameMap.get(name).add(uri);
        }

        foundUris.add(uri);
    }
}

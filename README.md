# PASS Journal Loader

Parses the PMC type A journal `.csv` file, and/or the medline database `.txt` file, and syncs with the repository:

    * Adds journals if they do not already exist
    * Updates PMC method A participation if it differs from the corresponding resource in the repository

## Usage

Using java system properties to launch the journal:

    java -Dpmc=/path/to/pmc.csv -Dmedline=/path/to/medline.txt -Dpass.fedore.baseurl=http://localhost:8080/fcrepo/rest/ -jar pass-journal-loader-nih-0.1.0-exe.jar

### Properties or Environment Variables

The following may be provided as system properties on the command line `-Dprop-value`, or as environment
variables `PROP=value`:

The first four are needed by the PASS java client.

`pass.fedora.url`
BaseURL of Fedora. Must end in trailing slash e.g. `http://localhost:8080/fcrepo/rest/`

`pass.fedora.user`
The username for the Fedora user.

`pass.fedora.password`
The Fedora user's password

`pass.elasticsearch.url`
The url for the PASS elasticsearch service.

`dryRun`
Do not add or update resources in the repository, just give statistics of resources that would be added or updated

`pmc`
Location of the PMC "type A" journal .csv file, as retrieved
from [http://www.ncbi.nlm.nih.gov/pmc/front-page/NIH_PA_journal_list.csv]( http://www.ncbi.nlm.nih.gov/pmc/front-page/NIH_PA_journal_list.csv)

`medline`
Location of the Medline journal file, as retrieved
from [ftp://ftp.ncbi.nih.gov/pubmed/J_Medline.txt](ftp://ftp.ncbi.nih.gov/pubmed/J_Medline.txt)

`LOG.*`
Adjust the logging level of a particular component, e.g. `LOG.org.dataconservancy.pass.client=WARN`

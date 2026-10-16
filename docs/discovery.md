# Doomsday Radar: discovery

Status: step 4, exploratory backtest done 2026-10-15. The reading guide below states the choices and evidence limits before step 5.

Steps, agreed 2026-10-14:

1. Requirements brief, reviewed by the user.
2. Event list: past war starts with date and region, the three named in the success criteria and earlier ones, for example Nagorno-Karabakh in 2020. For each event: whether it came as a surprise or after open threats. Each date is checked at a source. The user agrees the list.
3. Source inventory with a history check. For each candidate source: access, licence, grain, delay, and whether it has data for the event dates. Candidates: adsb.lol, OpenSky, ADS-B Exchange, GDELT (news), IODA (internet outages), NASA FIRMS (fires), USGS (earthquakes), aisstream.io (ships). The bus matrix follows here too. Time box: 2 hours from 2026-10-15 05:50, at most 8 page reads for each source. A field not found by then stays "unknown".
4. Exploratory backtest: for each event with history, compare each source in the 30 days before the event with its normal level. Scripts, not production code. Steps 3 and 4 have a time box.
5. Architecture from the findings.

## 1. Requirements

The product brief from the interview. The user answers as the end user.

### Vision

Doomsday Radar warns private people early that a regional or a global war can start. It combines all useful open signals, for example air traffic, news, internet outages and ship movements, into one warning level with clear reasons, for the visitor's region and for the world. A person who is not an analyst understands the warning and gets it as soon as possible.

### Stakes

The main goal is a service that is useful to people. Learning is a secondary goal.

### Who this serves

Any private person anywhere in the world who thinks about a possible war and how to react to it. Businesses are not a target: the service uses only open sources, gives a warning level and no guarantee, and has no paid service.

### Visitor needs (first draft)

1. Regional risk: a visitor who lives in a region gets early signs that the risk of war in that region grows. Two time scales:
   - 1a. Near: the war can start in a few days.
   - 1b. Imminent: the war can start in a few hours.
2. Global war: a visitor learns that a global war, for example a thermonuclear war, comes. Two time scales:
   - 2a. Near: signs that the war can start in days or weeks.
   - 2b. Imminent: the war can start in hours or minutes.

The region of need 1 is, for now, an area within a set distance of the visitor. On a map, the visitor finds the own location and sees the warnings near it. More exact rules can come later.

### Form of the warning

Doomsday Radar shows a warning level, for example one of 3 or 5 levels. It does not show a probability: with few past wars, a reliable probability cannot be calculated. Each level shows its reasons, for example "air traffic over X is 60 % below normal", so the visitor can check them in other sources.

### Freshness

For each need, the warning reaches the visitor as soon as possible after the signal. Before the warning goes out, a quick check is to exclude probable false warnings. Push messages go only to visitors who asked for them. The time between the signal and the warning must be shorter than the time scale of the need. Example: for need 2b, the war can start minutes or hours after the signal. If the warning reaches the visitor hours after the signal, it comes after the war has started and therefore has no value. Source profiling checks what is possible.

### What makes this different

Other sources exist: news, Telegram channels and similar. The value of Doomsday Radar is that it combines many sources (air traffic, news, and later others, for example ships) into one warning. Need 2b stays in scope.

### Success criteria (first draft)

- A missed warning is worse than a false warning. After a false warning, the visitor can check other sources and decide.
- Evidence that the service works: a backtest of the signals on data from before past events, for example 2022-02-24 (Ukraine), 2023-10-07 (Israel), June 2025 (Israel and Iran). The sources must have history for such dates. The exploratory backtest of step 4 shows which signals moved before these events. To show that the service would have warned in time, and how many false warnings it would have given, a defined warning rule must exist first. The backtest gives evidence, but it is not a go or no-go gate: if past wars show no clear signal, the project can continue, for example for need 2, which no backtest can check.
- False warnings: at most one per month. This value is provisional. Feedback from users and tuning on real sources can change it.

### Scope

- The needs and sources of the first version are chosen after the backtest (step 4).
- The first version gives regional warnings only for regions with enough data. For the other regions, it tells the visitor clearly that it has no data.

### Out of scope

- Advice on what the visitor does after a warning. Information of this type can come later, not in the first stage. It is not the main job.

### Open questions

- How does the warning reach the visitor? Candidates: a website page, a map (for example squares with unusual air traffic, for regional and global risk), a messenger channel (Telegram, WhatsApp), email. More than one channel is possible. No decision yet.
- If a backtest shows that the service would have warned before past wars, add one sentence to the vision, and present the result to the visitors.
- The distance value for the region of need 1. A push message with a distance rule needs a place from the visitor: the current location or a place the visitor chooses. How the service gets and keeps it is open.
- Can the combined warning come earlier than other sources, or reach people they do not reach? Nobody knows now. The data discovery must give evidence.
- The user expects many sources, real time or near real time, and much data, and therefore the expectation is that Kafka, ClickHouse and Spark stay needed. This is a hypothesis, not a finding. Steps 3 to 5 check it.

## Event list (step 2)

Agreed 2026-10-15.

These ten rows are the agreed set, not a complete list of war starts since 2015. The class "tension" is an addition to the two classes of the plan (surprise and open threats). The user accepted it, and the 2015 cutoff, on 2026-10-15.

Basis: each cell was checked against pages opened with a fetch tool that returns summaries, not page text. Reuters, AP and BBC were not available.

### Inclusion criteria

1. The first day of the war is one date that at least two sources give.
2. It is a new war or a new phase, not a slow conflict.
3. It started in 2015 or later. This is provisional: step 3 checks the source history.
4. The class rests on dated public facts before the start.

### Classes

- surprise: no public warning of an attack before the start.
- tension: an open dispute with escalating steps, but no explicit warning of an attack.
- threats: public threats or warnings of an attack before the start.

The classes are a reading of the dated facts.

### Table

| # | Start | Region | Class | Sources for the date | Dated lead-up |
|---|---|---|---|---|---|
| 1 | 2020-09-27 | Nagorno-Karabakh | threats | [Al Jazeera](https://www.aljazeera.com/news/2020/9/27/heavy-fighting-erupts-in-disputed-nagorno-karabakh-region), [War on the Rocks](https://warontherocks.com/2020/10/the-second-nagorno-karabakh-war-two-weeks-in/) | Border clashes from 12 Jul ([CACI Analyst](https://www.cacianalyst.org/publications/analytical-articles/item/13640-cross-border-escalation-between-armenia-and-azerbaijan.html)), at least 16 killed ([France 24](https://www.france24.com/en/20200927-tensions-flare-between-azerbaijan-armenia-in-disputed-nagorno-karabakh-region)). A conditional threat to hit the Metsamor power plant on 16 Jul, retracted soon after (CACI Analyst). Azerbaijan began calling up reserves about a week before (War on the Rocks). |
| 2 | 2022-02-24 | Ukraine | threats | [PBS](https://www.pbs.org/newshour/amp/world/putin-announces-military-operation-in-ukraine), [Al Jazeera](https://www.aljazeera.com/news/2022/2/24/putin-orders-military-operations-in-eastern-ukraine-as-un-meets): before dawn | On 11 Feb, Sullivan told U.S. civilians in Ukraine to leave now. US intelligence saw an invasion possible within days ([WSHU/NPR](https://www.wshu.org/2022-02-12/national-security-adviser-jake-sullivan-told-u-s-staffers-in-ukraine-to-get-out-now)). |
| 3 | 2023-04-15 | Sudan | tension | [AOAV](https://aoav.org.uk/2023/escalating-power-struggle-between-sudans-military-and-paramilitary-forces-plunges-khartoum-into-chaos-threatens-regional-stability/): Saturday morning, [Al Jazeera](https://www.aljazeera.com/news/2023/4/15/fighting-erupts-in-sudan-following-months-of-simmering-tensions) | The RSF moved to Merowe about 13 Apr. The army told it to retreat. |
| 4 | 2023-09-19 | Karabakh, second offensive | tension | [Al Jazeera](https://www.aljazeera.com/news/2023/9/19/azerbaijan-launches-new-anti-terror-offensive-in-nagorno-karabakh), [Euronews](https://www.euronews.com/2023/09/19/azerbaijan-launches-military-action-in-disputed-nagorno-karabakh-region) | The Lachin blockade ran for months. Baku demanded that the Karabakh government dissolve. Russia says Baku warned its peacekeepers only minutes before. |
| 5 | 2023-10-07 | Israel and Gaza | surprise | [Lawfare](https://www.lawfaremedia.org/article/oct.-7-was-an-intelligence-failure--maybe-of-the-u.s.-), [Cipher Brief](https://www.thecipherbrief.com/column_article/one-year-later-lessons-from-israels-october-7-intelligence-failures); 06:29 from one source, [JPost](https://www.jpost.com/opinion/article-876482) | Internal warnings were reported but dismissed: SIGINT about three months before, an assessment in Sep 2023. |
| 6 | 2024-11-27 | Syria (HTS offensive on Aleppo) | surprise | [CSIS](https://www.csis.org/analysis/syrian-rebels-surprise-offensive-highlights-assad-regimes-weakness), [France 24](https://www.france24.com/en/middle-east/20241128-syrian-rebels-seize-territory-in-large-scale-offensive-against-army-in-aleppo-province), [ACLED](https://acleddata.com/2024/12/02/syrian-hts-led-forces-capture-over-200-locations-from-assad-forces-in-aleppo-idlib-and-hama/) | Front frozen for four years. 684 regime and Russian strikes from 1 Oct to 26 Nov. The Hezbollah–Israel ceasefire came hours before. |
| 7 | 2025-05-07 | India and Pakistan | threats | [Carnegie](https://carnegieendowment.org/posts/2025/05/indian-airstrikes-in-pakistan-may-7-2025): 01:05 to 01:30 IST, [Al Jazeera](https://www.aljazeera.com/news/2025/5/7/operation-sindoor-whats-the-significance-of-indias-pakistan-targets) | Pahalgam attack on 22 Apr. Indian Navy firings on 27 Apr. Modi gave "operational freedom" on 29 Apr. Pakistan warned of a strike "in 24 to 36 hours" on 30 Apr; that window ended before the strike of 7 May ([Arab News](https://www.arabnews.com/node/2598954/amp), [Al Jazeera](https://www.aljazeera.com/news/2025/4/30/kashmir-attack-how-india-might-strike-pakistan-what-history-tells-us)). |
| 8 | 2025-06-13 | Israel and Iran | threats | [Al Jazeera](https://www.aljazeera.com/news/2025/6/13/sounds-of-explosions-heard-in-irans-capital-tehran): early Friday, [HOZINT](https://www.hozint.com/2025/06/operation-rising-lion-israels-strike-on-irans-nuclear-facilities/) | A Trump letter with a two-month deadline, reported on 19 Mar ([Times of Israel](https://www.timesofisrael.com/report-trump-letter-to-iran-set-2-month-deadline-to-reach-nuclear-deal/)). Five rounds of talks since April. About 24 hours before, the US began moving nonessential staff out of its Baghdad embassy. A sixth round of talks was planned for 15 Jun ([Anadolu](https://aa.com.tr/en/americas/oman-says-next-round-of-iran-us-nuclear-talks-called-off/3598588)). |
| 9 | 2025-07-24 | Thailand and Cambodia | tension | [CSIS](https://www.csis.org/analysis/thailand-launches-airstrikes-amid-border-dispute-cambodia), [The Aviationist](https://theaviationist.com/2025/07/24/thai-f-16-strikes-cambodia/) | Skirmish on 28 May. A landmine blast injured five Thai soldiers. Thailand recalled its ambassador on 23 Jul (inferred from "a day after" on the CSIS page). |
| 10 | 2026-02-28 | US and Israel against Iran | threats | [CFR](https://www.cfr.org/global-conflict-tracker/conflict/confrontation-between-united-states-and-iran), [AJC](https://www.ajc.org/news/the-iran-strikes-explained-how-we-got-here-and-what-it-means) | Carrier group on 26 Jan. Talks on 6 Feb and 17 Feb ([PBS timeline](https://www.pbs.org/newshour/world/a-timeline-of-tensions-over-irans-nuclear-program-as-talks-with-u-s-approach)). Trump threats and sanctions on 25 Feb ([Al Jazeera](https://www.aljazeera.com/news/2026/2/26/iran-us-set-to-hold-talks-as-trump-threatens-force-imposes-sanctions)). The 26 Feb talks ended with no breakthrough and technical talks were set to resume in Vienna the next week. CENTCOM briefed Trump on strike options ([Iran International](https://www.iranintl.com/en/202602271572)). |

## 2. Business processes and bus matrix

Each source is one process. A row is one source, a column is a shared dimension. "x" means the source has the dimension.

| Source | Time | Region | Entity | Value |
|---|---|---|---|---|
| Aircraft (adsb.lol, OpenSky, ADS-B Exchange) | position time | position | aircraft | position, speed, altitude |
| GDELT | report time | country, place | actors, theme | event code, tone, mentions |
| IODA | 5 to 60 min | country | signal type | signal value (direction of an outage not read) |
| FIRMS | pass time | position | satellite | fire detection |
| USGS | event time | position | none | magnitude |
| aisstream.io | message time | position | vessel | position, static data |

Shared dimensions to conform: time (UTC), region (one grid or country list for all sources), baseline (normal level per region and time of day). Entity is shared only by the aircraft and ship sources. Grain and delay per source: section 3.

## Reading guide: findings and next choices

The next choices are to test an IODA rule with checked comparison periods, check aircraft coverage and history access, and measure the effect of missing GDELT days.

A **window** covers 30 calendar days before the test's cut date; a source can have fewer values when days are missing. When an event has no known clock time, the last checked day is two UTC dates before its stated date. A **baseline** covers the 60 earlier days used to estimate a normal level. The backtest covers eight event starts from April 2023 to February 2026. It describes changes in those windows. It does not test a warning rule or the last hours before a start. [Cuts and method](#plan) · [Results and limits](#results)

1. **Do not claim an early warning yet.** A rule for the first unusual day was added after the event results were known. It marks days for some IODA signals in both the India and Thailand 2024 comparison periods. In Thailand's 30 days from 2025-06-23 to 2025-07-22, the median daily ratio for internet addresses seen by IODA's passive sensor was 0.665 of the baseline level for the same weekday. This drop came before the 2025-07-24 escalation but after a 2025-05-28 clash. For a separate Thailand signal that counts responsive network blocks, the median daily ratio was 1.400 over 30 days from 2024-06-23 to 2024-07-22. The rule marked no day for that signal because its baseline range was wide. These periods are not verified quiet controls and cannot give a false warning rate. Set a rule before a new test, and use checked control periods. [First-move rule](#first-move-dates) · [Source-record audit](#source-record-audit-2026-10-15)
2. **Check aircraft coverage before a source choice.** The first adsb.lol daily release is 2023-02-16, after the 2022 Ukraine start. OpenSky has older records, but this public product needs a licence or approved access to its full history. For the June 2025 Iran event, the saved aircraft sample has four baseline days and two window days, with no window day in the last 14 days before the test's event start. The original aircraft positions were not saved, so the sampled counts remain unchecked. Decide whether older aircraft history merits an access request; test receiver coverage and recent-day completeness before using the open feed. [Source profiles](#profiles) · [Aircraft limits](#results) · [Source-record audit](#source-record-audit-2026-10-15)
3. **Audit news files before using their ratios.** On 2026-10-15, GDELT's daily files were unavailable from 2025-06-14 through 2025-07-01: nine missing days fall in the Thailand event baseline and nine in its window. Its selected codes also include assaults and abductions, not only war. The original 90-day news records behind the reported ratios were not kept. Recompute totals from available daily exports, leave missing days blank, and test how the gaps affect the ratios before a source choice. [GDELT method](#plan) · [Missing days and source records](#source-record-audit-2026-10-15)

| Source | What it measures | Use of this discovery |
|---|---|---|
| adsb.lol | Aircraft heard by volunteer receivers | Coverage test; daily archive starts in 2023 |
| OpenSky; ADS-B Exchange | Other aircraft history | Access or cost choice; not tested in step 4 |
| GDELT | Events coded from news reports | Audit missing days and codes |
| IODA | Internet connectivity and outages | Test an outage rule with controls |
| USGS | Earthquakes | Descriptive control only |
| NASA FIRMS; aisstream.io | Fires; live ship positions | Not tested in step 4 |

The daily backtest cannot answer the product's hours-scale needs. Source delay, live coverage, and the effect of combining sources remain unknown. [Plan limits](#plan) · [Latency work](#4-latency-and-refresh-rate)

## 3. Sources

Done 2026-10-15, time box 2 hours from 05:50, about 10 minutes used. Skill: `nw:research`, three researcher agents (aircraft, news and outages, fire and quake and ships), plus the checks below.

Basis and limits:

- The agents read pages with a fetch tool that returns summaries, not page text. Eight claims were then checked against the raw page or API with `curl`, and all eight held (the responses were not saved, so this line rests on the author's record): USGS count 719, IODA Ukraine 2022 data, IODA Azerbaijan 2020 empty, GDELT file of 2022-02-24 (10.1 MB), adsb.lol first release 2023-02-16, the aisstream.io "not durably replayed" text, the FIRMS archive start dates, and OpenSky "unlimited retention since 2013".
- Other profile cells (access, licence, delay, cost) remain agent reports except the adsb.lol access and delay probes, and the OpenSky, GDELT and adsb.lol terms linked below. The agent reports are leads, not facts, until a source page or API response is saved. Step 4 fetched GDELT, IODA, USGS and adsb.lol data without an account, but did not check their typical delay.
- The IODA coverage cells and the IODA start dates come from my own `curl` probes of the keyless API. The other cells come from the agents' reports.
- During step 3, no account was created and no file was downloaded. FIRMS, OpenSky, aisstream.io and the paid ADS-B Exchange data need an account, so they have no probe.
- The agents went over the cap of 8 page reads for adsb.lol (14), ADS-B Exchange (12) and FIRMS (9), because the release lists and empty pages needed more reads. The cap is not met for these three.
- Criteria were fixed before the research: access, licence, grain, delay, history start, coverage of window and baseline for each event, cost, probe. A field that was not found stays "unknown".

### Profiles

| Source | Access | Licence | Grain | Delay | History start | Cost |
|---|---|---|---|---|---|---|
| adsb.lol | Two routes. Daily archive on GitHub releases, no account. Heatmap file per 30 minutes at `adsb.lol/globe_history/YYYY/MM/DD/heatmap/NN.bin.ttf`, no key (a header-only request for one file returned HTTP 200 on 2026-10-15; the response was not saved). The heatmap path is not a documented API, and `2026/03/01/heatmap/00` returned HTTP 404 ([walking-skeleton.md](walking-skeleton.md)). | [ODbL 1.0](https://www.adsb.lol/docs/open-data/historical/). Terms for derived numbers on a public site were not checked. | Daily archive of about 2 to 4 GB with one file per aircraft per day (agent report, not checked at the source), plus a `heatmap` folder of 48 files a day, one per 30 minutes with positions every 10 seconds; step 4 reads only the heatmap files | Archive: about 1 day (probe). Heatmap: the 13:00 to 13:30 UTC file of 2026-10-05 was available at 13:47 UTC; the 13:30 to 14:00 UTC file of the same day showed a last-modified time of 14:00:04 GMT in the unsaved header response. Two samples, so a typical delay is unknown. | [2023-02-16](../scripts/backtest/evidence/adsb-2023-RELEASES.md) | Free |
| OpenSky | OAuth2 REST for live data. Full history is available through Trino with an approved account. | [OpenSky says](https://opensky-network.org/about/faq) free full access is for eligible institutions; private and continued operational users must contact OpenSky for a licence. The [saved FAQ](../scripts/backtest/evidence/provider/opensky-faq.html) and [terms](../scripts/backtest/evidence/provider/opensky-terms.html) record the access rule. | State vector, 5 to 10 s | Unknown | [2013 for state vectors](https://openskynetwork.github.io/opensky-api/trino.html) | Free only for eligible groups; price unknown |
| ADS-B Exchange | Paid subscription. Free sample: first day of each month. | Terms moved to jetnet.com, not read | Snapshot 5 s from 2020, 60 s from 2016 | Unknown | 2016 | Paid, no price published |
| GDELT | Daily and 15 minute files, no key. DOC API timelines from 2017, but limited by HTTP 429. | [Free use with a citation and link](https://gdeltproject.org/about.html) | Event record; file per day (1.0) or per 15 minutes (2.0) | Files: 15 min to 1 day. Delay from real event to report: unknown. | 2013-04-01 (1.0), 2015-02-19 (2.0) | Free |
| IODA | Keyless HTTP API, country series | Unknown | Series per country and signal, steps 5 to 60 min | Unknown | See below: late 2021 to early 2022 | Free, limits unknown |
| NASA FIRMS | Free key (MAP_KEY), 5000 requests per 10 min | Open data with an acknowledgement (second hand, not read at the page) | One fire detection, 375 m (VIIRS) or 1 km (MODIS) | Near real time: about 3 hours (second hand) | 2000-11-01 (MODIS), 2012-01-20 (VIIRS S-NPP), 2018-04-01 (NOAA-20), 2024-01-17 (NOAA-21) | Free |
| USGS | Keyless HTTP API, limit 20000 records per call | Public domain, credit asked | One earthquake | Aim 30 min for large events. Recent months are revised. | 1900 (M4.5+ probe); stated catalog data from 1973 | Free |
| aisstream.io | Live WebSocket, free key | None written. A 2023 owner reply: "no restrictions" | One AIS message | Unknown | None: no history | Free, no uptime promise |

IODA signals: BGP (`bgp`) tracks routing reachability, active probing (`ping-slash24`) checks which address blocks answer, darknet (`merit-nt`) counts source addresses seen in passive traffic, and `gtr` reports Google product usage. The saved [IODA measurement report](../scripts/backtest/evidence/provider/ioda-measurement-report.html) explains BGP, probing and darknet limits; its [Google report](../scripts/backtest/evidence/provider/ioda-google-report.html) explains the usage source. Google's source values are normalized, but the [raw API record](../scripts/backtest/evidence/ioda/TH-event-2025-0.json) also has a separate `gtr-norm` series. The scale of the tested `gtr` series was not checked. The saved glossary page contains no definitions, so these reports are the local source records. This backtest did not implement IODA's alert method. First open data for Ukraine by signal (probe): `gtr` 2021-11-29, `merit-nt` 2022-01-09, `bgp` 2022-01-18.

### Coverage of window (W) and baseline (B)

Y = data exist, P = partial, N = none, ? = unknown. "Y (doc)" is a statement of the documentation, not a probe.

| # | Event | adsb.lol | OpenSky | ADS-B Exchange | GDELT | IODA | FIRMS | USGS | aisstream.io |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Karabakh 2020 | N / N | Y (doc) | paid Y (doc), free P | Y / Y | N / N | Y (doc) | Y / Y | N / N |
| 2 | Ukraine 2022 | N / N | Y (doc) | paid Y (doc), free P | Y / Y | Y (ping 44 of 60 days) / P (`gtr` 57, `merit-nt` 11, `bgp` 7 of 60 days) | Y (doc) | Y / Y | N / N |
| 3 | Sudan 2023 | Y (thin) / P | Y (doc) | paid Y (doc), free P | P / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 4 | Karabakh 2023 | Y / Y | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 5 | Israel, Gaza 2023 | Y / Y | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 6 | Syria 2024 | Y / Y | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 7 | India, Pakistan 2025 | Y / Y | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 8 | Israel, Iran 2025 | P / Y | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |
| 9 | Thailand, Cambodia 2025 | Y / P | Y (doc) | paid Y (doc), free P | P / P | Y / Y | Y (doc) | Y / Y | N / N |
| 10 | US, Israel, Iran 2026 | P / P (step 4 read 8 sample days) | Y (doc) | paid Y (doc), free P | Y / Y | Y / Y | Y (doc) | Y / Y | N / N |

Notes on the cells:

- GDELT: "Y" is by range. The file of 2022-02-24 exists (probe). A later [daily file check](../scripts/backtest/evidence/gdelt/status.tsv) found one missing Sudan window day and nine missing days in each Thailand period. Other rows were not checked day by day.
- IODA: probed by the country of the event (both countries for events 1, 4, 5, 7, 8, 9, 10 where two are named). Event 1: empty for Azerbaijan and Armenia. Event 2: Ukraine, `bgp` and `merit-nt` start in January 2022.
- adsb.lol: a "Y" means a release file exists for each day, not that the data are complete. Gaps: 2025-05-28 to 2025-06-10 (only `tmp` files), 2025-12-14 to 2025-12-17 (two lists disagree), and 2026-01-02 to 2026-02-18 was not listed in step 3. Step 4 then read valid 48-file days on 2025-12-11, 2025-12-25, 2026-01-08, 2026-01-22, 2026-02-05, 2026-02-12, 2026-02-19 and 2026-02-26 (eight sample days), so event 10 is partial.
- ADS-B Exchange free: one sample day in each window and two in each baseline. The sample listing is built by JavaScript, so the listing was not read.
- FIRMS: a start date per instrument, not a check of each day. NOAA-21 reaches only events 6 to 10.
- USGS: counts of M4.5+ events, about 16 to 21 a day.

### Findings

- Air traffic is the lead signal of the vision, but the open adsb.lol archive starts on 2023-02-16, after the 2022 Ukraine start named in the success criteria. OpenSky has older records through approved access. Its [saved terms](../scripts/backtest/evidence/provider/opensky-terms.html) require a written licence for operational use of its REST API. ADS-B Exchange's reported paid history was not probed.
- All aircraft sources count only aircraft that volunteer receivers hear. The saved material does not measure receiver coverage. A raw regional count can change as receivers change, so coverage needs a check before setting a baseline.
- No source states whether military or filtered aircraft are dropped. adsb.lol says "unfiltered" and says nothing more.
- aisstream.io has no history. A ship baseline can only come from our own recording, from the day we start.
- IODA has no 2020 data and little before 2022. Events 3 to 10 are fully covered.
- The 2015 cutoff of the event list changes nothing: all ten events are from 2020 or later.

### For step 4

| Source | Events | Remark |
|---|---|---|
| USGS | 1 to 10 | Ready, no account |
| GDELT | 3 to 10 | Ready. Columns checked on one file (`DATEADDED`, root code, action country, tone). Files for 2023-03-23 and 2025-06-14 to 2025-07-01 do not exist on the server. |
| IODA | 2 (window only) to 10 | Ready, no account. Event 1 has no data. |
| adsb.lol | 3 to 9, and 10 after a check | Ready, no account. Only the heatmap slice is read (48 files a day). The reported archive sizes were not checked here. Receiver coverage was not measured; check it before setting a baseline. |
| FIRMS | 1 to 10 | Needs a free key. An account is for the user to create. |
| OpenSky | 1 and 2 if access is approved | Historical data were not tested. An account and eligibility review are needed. |
| ADS-B Exchange | None on free days; paid history untested | Free days are too few for a 30 day window. Paid price unknown. |
| aisstream.io | none | No history. Start recording now if ships stay in scope. |

### Open questions for the user

- Request OpenSky history access for events 1 and 2, and for Ukraine 2022 in particular? The user registers the account. If access is refused, paid ADS-B Exchange history remains an untested option.
- Create a free FIRMS key for step 4?
- Start recording aisstream.io now, so a ship baseline exists later?

### Strongest fact against this plan

Step 4 can use adsb.lol only for events 3 to 9, and event 10 after a check. Events 1 and 2 have no open air traffic data, and Ukraine 2022 is the case that the success criteria name first. A backtest without it cannot show whether air traffic moved before a large invasion.

### Not found

- Licence of IODA. FIRMS licence at the page. OpenSky rights for derived numbers. ADS-B Exchange terms and price.
- Delay from real event to record for GDELT, IODA, OpenSky and ADS-B Exchange.
- Rate limits: adsb.lol API, GDELT DOC API (HTTP 429 on all three probes), IODA, USGS, OpenSky history.
- Continuity day by day: GDELT, FIRMS, USGS completeness for M4.5+ outside the USA.
- Official statement on dropped aircraft (military, LADD, PIA) for all three aircraft sources.

## Backtest (step 4)

### Plan

Fixed 2026-10-15 before any window data was read. Revised the same day after a Grok review (round 1: 17 findings, round 2: 13, round 3: 8, round 4: 6). Time box: 4 hours, 06:10 to 10:10 (CEST). Scripts in `scripts/backtest/`, not production code. The user decided on 2026-10-15: events 3 to 10 only (no OpenSky), no FIRMS key, no aisstream.io recording. FIRMS, OpenSky, ADS-B Exchange and aisstream.io are out of this step.

Question: how does each source in the 30 days before an event (window) compare with its normal level, taken from the 60 days before the window (baseline)? The result is one descriptive table. There is no "moved" flag, no pass mark, no warning sentence and no false rate. The warning rule is not defined yet.

Window cut. E is the start in UTC. The window is the 30 days that end before E, the baseline is the 60 days before the window. Where the sources give a clock time or a part of the day that fixes the UTC date, E is that instant or that date (event 3: "Saturday morning" in Sudan, UTC+2; event 5; event 7). Where they give none, E is the table date minus one day, because an early morning local time can fall on the day before in UTC. The window keeps 30 days, and the cut changes which days are in it.

| # | Class | E (UTC date) | Window | Baseline | Remark |
|---|---|---|---|---|---|
| 3 | tension | 2023-04-15 | 2023-03-16 to 2023-04-14 | 2023-01-15 to 2023-03-15 | Saturday morning in Sudan (UTC+2) is the same UTC date. adsb.lol has no data before 2023-02-16 |
| 4 | tension | 2023-09-18 | 2023-08-19 to 2023-09-17 | 2023-06-20 to 2023-08-18 | No clock time |
| 5 | surprise | 2023-10-07 | 2023-09-07 to 2023-10-06 | 2023-07-09 to 2023-09-06 | Clock time sourced: 06:29 local, which is 03:29 UTC on the table date |
| 6 | surprise | 2024-11-26 | 2024-10-27 to 2024-11-25 | 2024-08-28 to 2024-10-26 | No clock time |
| 7 | threats | 2025-05-06 | 2025-04-06 to 2025-05-05 | 2025-02-05 to 2025-04-05 | Clock time sourced: 01:05 IST on 2025-05-07, which is 19:35 UTC on 2025-05-06 |
| 8 | threats | 2025-06-12 | 2025-05-13 to 2025-06-11 | 2025-03-14 to 2025-05-12 | No clock time |
| 9 | tension | 2025-07-23 | 2025-06-23 to 2025-07-22 | 2025-04-24 to 2025-06-22 | No clock time |
| 10 | threats | 2026-02-27 | 2026-01-28 to 2026-02-26 | 2025-11-29 to 2026-01-27 | No clock time |

Series. Each is a daily value per row of the region table.

- adsb.lol: distinct aircraft with at least one airborne position in the box that day, from the `heatmap` files of the daily archive (positions every 10 seconds, 48 files a day, read as a stream, nothing kept on disk). The earlier 0.8 GB daily size estimate was not rechecked. Also the count in a reference box over western Europe (lat 44 to 54, lon -5 to 15) and the ratio box / reference. The ratio is a candidate way to account for receiver changes. This backtest did not measure receiver coverage, so it cannot check whether the ratio corrects a change in one region. The Europe box is on routes that a war can disturb.
- GDELT 1.0 daily files, by `DATEADDED` (column index 56; a row is kept when that field starts with the file day): share of rows with root code 18, 19 or 20 among rows whose action country is in the row, and the mean tone (tone sum divided by tone count), each on its own row. Codes 18 and 19 include ordinary assault and fighting. A missing day is left out.
- IODA: daily mean of the raw points of `bgp`, `ping-slash24` (active probing), `merit-nt` (darknet) and `gtr` (Google product usage; API scale unchecked) for the country, one row per signal. A day with no points is left out. This backtest did not test the provider's outage method.
- USGS: daily count of M4.5+ quakes in the same box, with a count of 0 for every day without a quake. A descriptive control only: quake counts cluster, and completeness outside the USA is unknown. If the baseline median is 0, the counts are shown and no ratio.

Rows (the boxes were drawn from the known event locations, so they carry that knowledge; this step does not use a fixed world grid):

| # | Row | GDELT | IODA | Box (lat, lon) |
|---|---|---|---|---|
| 3 | Sudan | SU | SD | 8.5 to 23.5, 21.8 to 38.6 |
| 4 | Azerbaijan | AJ | AZ | 38.3 to 41.9, 44.7 to 50.4 |
| 4 | Armenia | AM | AM | 38.8 to 41.3, 43.4 to 46.7 |
| 5 | Israel | IS | IL | 29.4 to 33.4, 34.2 to 35.9 |
| 5 | Gaza | GZ | PS | 31.2 to 31.6, 34.2 to 34.6 |
| 6 | Syria | SY | SY | 32.3 to 37.3, 35.7 to 42.4 |
| 7 | India | IN | IN | 6.7 to 35.7, 68.1 to 97.4 |
| 7 | Pakistan | PK | PK | 23.5 to 37.1, 60.8 to 77.9 |
| 8, 10 | Iran | IR | IR | 25.0 to 39.8, 44.0 to 63.4 |
| 8, 10 | Israel | IS | IL | 29.4 to 33.4, 34.2 to 35.9 |
| 9 | Thailand | TH | TH | 5.6 to 20.5, 97.3 to 105.6 |
| 9 | Cambodia | CB | KH | 10.4 to 14.7, 102.3 to 107.6 |

The Israel box contains the Gaza box, so aircraft and USGS counts for Israel include Gaza; GDELT and IODA do not. The India box also holds parts of neighbouring states. Sea approaches, Baghdad, the Gulf and the carrier group of event 10 are outside every box.

Computation.

1. The normal level is the baseline median of the same weekday. For a window day d, r(d) = value(d) / baseline median of the weekday of d.
2. Per series the table shows: median of the 30 window days, median of the 60 baseline days, the median of r over the window days used, the median of r over the last 14 window days (a late rise shows up here and not in the first), the 5th and 95th percentile of r over the baseline days, and the days used. The percentiles are the spread of one day around its weekday level. They are not a yardstick for a median of many days. The comparison is the median of r against 1, with the day count beside it. A zero median gives no ratio.
3. adsb.lol sample. D is the day before E. Window days: D, D-7, D-14, D-21. Baseline days: D-35, D-49, D-63, D-77. All have the weekday of D. A day is used only if a `prod-0`, `prod-1`, `staging-0`, `staging-1` or early 2023 `test` release has 48 heatmap files and its archive can be read to the end. A day with only `tmp` files, with a short set, or with no release is replaced by the same weekday one week later, then one week earlier, inside the same period, skipping a day already used, up to three weeks. If none is valid, the slot stays empty. A skipped day is never stored as zero aircraft. Result per box: the eight days in date order, each as date, box count, Europe count and ratio, then the median of the window ratios and the median of the baseline ratios. The limit column says: one weekday, sample only. Event 3 also shows the raw counts, because the feed is thin.
4. Order of work. First GDELT, IODA and USGS for all events (small). Then aircraft, one day at a time, in the order 4, 5, 6, 7, 3, 8, 9, 10. Event 10 only after its days are checked against the release list. Budget: 64 sample days plus at most 8 substitutes. The initial probe reported 75 seconds and about 0.8 GB for one day, streamed, but its files and timing log were not kept. At that reported speed, 72 days would take about 90 minutes; aircraft work stops there. Rows that were not read are empty, and they are the tail of the order.

Known limits, written before the run:

- Eight events are too few for a rate. The regions and the summaries are a guess. There is no shared pass mark.
- Event 4: both blocks fall in the Lachin blockade, so the baseline is not a normal level. Event 6: the window is inside the 684 strikes of 1 Oct to 26 Nov, and the baseline holds part of that run.
- The aircraft number counts what the receivers hear. Whether military aircraft are present is unknown. Receiver coverage in some boxes may be near zero; the counts show it.
- Without a control period there is no false rate. The table says nothing about false warnings.
- The GDELT share mixes ordinary violence with war (India especially).
- GDELT has no file for 2023-03-23 and for 2025-06-14 to 2025-07-01 (checked on the server). Event 3 loses one day. Event 9 loses 9 baseline days and the first 9 window days (21 window files remain); its last 14 window days are complete.

Output of this step: one table. Columns: event, class, source, series, row, window median, baseline median, median of r over the window days used, median of r over the last 14 window days, baseline r 5th percentile, baseline r 95th percentile, days used, sample, limit. For an aircraft sample the sample cell holds the eight slots (date, box count, Europe count, ratio; an unread slot is shown as empty), the median cells hold the medians of the ratios, the last-14 cell uses only the sample days D and D-7 when the used day lies inside the last 14 window days, and the percentiles are those of the four baseline ratios divided by their median. Other sources leave sample empty.

### Results

Done 2026-10-15, inside the 4 hour box (06:10 to 10:10); the finish time was not logged. The full table (112 rows, with the spread of a baseline day and the aircraft samples) is in [discovery-step4-table.md](discovery-step4-table.md). The data came from the scripts in `scripts/backtest/`, which fetched them on the same day. The original full news and aircraft responses and the derived daily files are unavailable in this tree. The source-record audit below checks selected rows and arithmetic; it does not verify all 112 rows.

Each cell shows the median r over the window, then the median r over the last 14 window days (r = a day divided by the baseline median of the same weekday; 1.0 means no change). Tone is signed, so r above 1 means more negative. Aircraft cells are not 30-day medians: each is the ratio of the median of up to 4 same-weekday sample days in the window to the median of up to 4 in the baseline, and the last-14 value uses only the sample days D and D-7 that fall in the last 14 days. "n/a" means no ratio: a thin aircraft feed (the largest sampled box count is 10 or less; this rule is in `analyze.py` and is not in the Plan), a zero baseline, a sign change, or no aircraft sample in that part of the window. USGS shows the counts of M4.5+ quakes, not a ratio.

| # | Class | Row | GDELT conflict share | GDELT tone | IODA bgp | IODA ping | IODA darknet | IODA gtr | Aircraft | USGS quakes (window / baseline) |
|---|---|---|---|---|---|---|---|---|---|---|
| 3 | tension | Sudan | 1.34 / 2.353 | 1.852 / 1.916 | 1.0 / 1.0 | 0.923 / 0.919 | 0.95 / 0.972 | 0.778 / 0.78 | n/a | 0 / 0 |
| 4 | tension | Azerbaijan | 1.411 / 1.411 | 1.84 / 1.764 | 1.001 / 1.002 | 0.975 / 0.98 | 0.806 / 0.884 | 0.974 / 0.975 | n/a | 2 / 2 |
| 4 | tension | Armenia | 1.23 / 0.975 | 1.597 / 1.639 | 1.001 / 1.041 | 0.985 / 0.984 | 1.026 / 1.071 | 0.963 / 1.039 | n/a | 0 / 0 |
| 5 | surprise | Israel | 0.89 / 0.919 | 0.842 / 0.84 | 1.0 / 1.0 | 0.992 / 0.99 | 0.882 / 0.819 | 0.936 / 0.938 | 0.943 / 0.943 | 0 / 0 |
| 5 | surprise | Gaza | 1.144 / 0.888 | 1.098 / 0.957 | 1.0 / 0.998 | 0.982 / 0.97 | 0.885 / 0.768 | 1.028 / 1.038 | n/a | 0 / 0 |
| 6 | surprise | Syria | 0.872 / 0.633 | 0.837 / 0.766 | 1.0 / 1.0 | 0.916 / 0.917 | 0.663 / 0.582 | 0.975 / 0.979 | 2.012 / 2.266 | 0 / 0 |
| 7 | threats | India | 1.459 / 1.791 | 1.532 / 1.948 | 0.994 / 0.994 | 0.988 / 0.984 | 1.027 / 1.048 | 0.901 / 0.907 | 0.979 / 0.936 | 12 / 45 |
| 7 | threats | Pakistan | 1.014 / 1.162 | 1.07 / 1.717 | 1.003 / 1.003 | 0.987 / 0.968 | 1.072 / 1.072 | 0.961 / 0.953 | 0.97 / 0.892 | 5 / 8 |
| 8 | threats | Iran | 0.712 / 0.79 | 0.719 / 0.782 | 1.0 / 0.99 | 0.978 / 0.964 | 1.728 / 1.876 | 0.992 / 0.982 | 0.894 / n/a | 3 / 7 |
| 8 | threats | Israel | 1.003 / 0.956 | 0.966 / 0.987 | 1.0 / 1.0 | 0.985 / 0.979 | 0.961 / 1.02 | 1.0 / 1.019 | 0.956 / n/a | 0 / 0 |
| 9 | tension | Thailand | 1.019 / 0.994 | 0.978 / 1.026 | 0.999 / 0.999 | 0.993 / 0.991 | 0.665 / 0.532 | 1.08 / 1.074 | 1.044 / 1.108 | 0 / 0 |
| 9 | tension | Cambodia | 1.578 / 1.728 | n/a | 1.0 / 0.994 | 0.996 / 0.992 | 0.927 / 0.763 | 1.043 / 1.028 | 1.023 / 1.08 | 0 / 0 |
| 10 | threats | Iran | 0.653 / 0.653 | 0.77 / 0.702 | 0.995 / 0.995 | 0.97 / 0.971 | 0.9 / 0.868 | 0.914 / 0.914 | 1.029 / 0.846 | 6 / 8 |
| 10 | threats | Israel | 0.825 / 0.802 | 0.865 / 0.778 | 0.993 / 0.993 | 0.983 / 0.976 | 0.589 / 0.562 | 1.03 / 1.019 | 1.031 / 0.924 | 0 / 0 |

Readings. These are descriptions, not a verdict, and not a warning rule.

- Daily `bgp`: the window median r is 0.993 to 1.003 in all 14 rows, so it adds nothing at this grain. The last-14 value is 0.990 to 1.041 (largest: Armenia 1.041). `ping-slash24` r is 0.916 to 0.996 (window) and 0.917 to 0.992 (last 14) in all 14 rows.
- Cells where the window or the last-14 value is 1.5 or more, or 0.67 or less (a reading threshold chosen after the table was built, not a pass mark). Tone r above 1 means more negative.
  - GDELT conflict share: Sudan 1.34 / 2.353, India 1.459 / 1.791, Cambodia 1.578 / 1.728, Syria 0.872 / 0.633, Iran (event 10) 0.653 / 0.653.
  - GDELT tone: Sudan 1.852 / 1.916, Azerbaijan 1.84 / 1.764, Armenia 1.597 / 1.639, India 1.532 / 1.948, Pakistan 1.07 / 1.717.
  - IODA darknet (`merit-nt`): Iran (event 8) 1.728 / 1.876, Syria 0.663 / 0.582, Thailand 0.665 / 0.532, Israel (event 10) 0.589 / 0.562.
  - Aircraft: Syria 2.012 / 2.266, on box counts of 6 to 33 a day.
- Position of each threshold cell against the spread of one baseline day (the 5th to 95th percentile of r, in the full table), for the window value and the last-14 value:
  - Inside the band on both: Sudan conflict share (0.282 to 2.929), Cambodia conflict share (0.072 to 5.185), Iran event 10 conflict share (0.561 to 1.684), Sudan tone (0.2 to 2.728), Azerbaijan tone (-0.724 to 3.621), Armenia tone (0.037 to 2.938), Pakistan tone (0.503 to 1.927).
  - Inside on the window, outside on the last 14 days: Syria conflict share (0.633 against 0.64 to 1.624), Iran darknet event 8 (1.876 against 0.568 to 1.855), Israel darknet event 10 (0.562 against 0.584 to 2.089).
  - Outside the band on both: India conflict share (0.729 to 1.32), India tone (0.568 to 1.308), Syria darknet (0.846 to 1.445), Thailand darknet (0.749 to 1.298), Syria aircraft (0.506 to 1.533).
  - Not a threshold cell but below its band on both: Israel darknet event 5 (0.882 / 0.819 against 0.903 to 1.28).
  - A baseline day spread is not a yardstick for a median. Of the other conflict share values, Pakistan (event 7), Iran (event 8) and Israel (event 10) also lie inside their bands.
- Surprise events (5, 6): every GDELT and IODA value is between 0.58 and 1.144. Gaza conflict share (1.144), Gaza tone (1.098) and Gaza `gtr` (1.028 / 1.038) are slightly above 1. The aircraft row of Syria is the large rise (2.012 / 2.266). Syria darknet (0.663 / 0.582) and Israel darknet of event 5 (0.882 / 0.819) fall below their bands. This only matches the class label; it is not a test.
- Threats events (7, 8, 10): the GDELT conflict share rises before event 7 (India 1.791, Pakistan 1.162 in the last 14 days) and falls before events 8 (Iran 0.79) and 10 (Iran 0.653, Israel 0.802). The window of event 7 holds the Pahalgam attack of 22 April (event list, row 7).
- Event 10, Iran: the airborne count on 2026-02-26, two days before the start, is 1,510, the lowest of the eight sampled days (the others are 1,625 to 1,815). The ratio to Europe is noisier: the Europe count that day is 9,238 against 7,043 to 7,209 on the three Thursdays before it.
- USGS: in 9 of 14 boxes no M4.5+ quake occurred in the 90 days. As a control it says little.

Limits that apply to these results. The plan listed these before the run (see its known limits): eight events are too few for a rate, there is no false rate and no control period, the baselines of events 4 and 6 are not normal levels, the aircraft number counts what receivers hear and military presence is unknown, and the GDELT share mixes ordinary violence with war. Also: `gtr` reports Google product usage, but its exact API scale and use as a war signal were not checked; quake counts cluster and completeness outside the USA is unknown; the Israel box contains the Gaza box, so Israel aircraft and USGS counts include Gaza; sea approaches, Baghdad, the Gulf and the carrier group of event 10 are outside every box. Measured here:

- Ten events at most (eight here), regions drawn from the known locations, no false rate, no control periods. Nothing here says that a source "would have warned".
- The aircraft rows are samples of 8 days on one weekday. 59 of 64 slots were read; 9 slots used a substitute day (event 3: 3, event 8: 2, event 9: 4), one more than the 8 substitutes that the Plan allowed; 5 stayed empty (event 3 baseline slots D-49, D-63 and D-77; event 8 window slots D-14 and D-21). The output does not record why a day was skipped.
- Event 8 has no valid aircraft day in the last 14 window days. Its two window days are 2025-05-14 and 2025-05-21, and its last-14 cell is empty.
- The Europe reference is not stable: its daily count runs from 4,704 (2025-12-25) to 11,641 (2025-06-17). The ratio is therefore noisy. The saved counts do not show whether receivers changed in one region.
- On the sampled days the airborne count is 0 for Sudan (event 3, 5 days), 0 to 1 for Azerbaijan and Armenia (event 4, 8 days), and 0 to 3 for Gaza (event 5, 8 days). The feed is nearly empty on these sampled days, so these rows have no aircraft ratio.
- GDELT has no files for 2023-03-23 and 2025-06-14 to 2025-07-01. Event 3 has 29 of 30 window days. Event 9 has 21 of 30 window days and 51 of 60 baseline days; the nine missing window days are the first nine (2025-06-23 to 2025-07-01), so its last 14 window days are complete. Gaza (event 5) has 59 of 60 baseline days in both GDELT series; the named file gaps do not explain that.
- IODA `merit-nt` for Iran in event 10 has 88 of 90 days. The Iran baseline for event 10 holds days with values near zero (baseline r 5th percentile 0.004 to 0.035 for `merit-nt`, `gtr` and `ping-slash24`), which widens the spread.
- The tone ratio for Cambodia (event 9) is not defined: the baseline median (0.4675) is positive and the window median (-0.8891) is negative.
- The step asked which signal moved and how early. The median table gives no first-move date, because the medians cannot show an onset day. The Plan defined no rule for a move; a post hoc rule was added later and is in the section "First-move dates".
- The original source responses were not kept. The `data/` daily totals cited by the earlier run are not in this tree. A sample was fetched again on 2026-10-15 with the same scripts (`scripts/backtest/refetch_check.py`, output in `scripts/backtest/results/refetch_check.txt`): 3 GDELT days (2023-03-16, 2024-11-10, 2026-02-12), 3 USGS rows (events 4, 7, 10) and 3 IODA rows with 4 signals each (events 5, 8, 10). The GDELT records and the USGS daily series matched exactly in that run. For the 12 IODA series, no daily difference exceeded 0.01, and each largest difference rounded to 0.000. Because the same parser ran, this checks repeatability of those stored values, not parsing accuracy. The new independent check below uses fresh source records.
- The day check counts 48 heatmap members and stops reading the archive after the heatmap group. It does not read the archive to the end, as the Plan says, and it does not check that the 48 slot names are different.
- This backtest did not test IODA's outage method. All 14 `ping-slash24` window ratios in the saved table are below 1; this aggregate was not checked from all original records. In the Sudan series (event 3) the baseline falls from a mean of 185.4 in the first 7 days (2023-01-15 to 2023-01-21) to 150.2 in the last 7 days (2023-03-09 to 2023-03-15), and the window has a low of 132.0 on 2023-04-06. The independent check below reproduces those three values. A weekday baseline median does not remove such a trend.
- The daily grain cannot answer the needs of "hours" (1b, 2b). This step did not test the last 72 hours before an event.

The earlier run reported 34.4 GB across 59 adsb.lol heatmap days and 64,446,380 GDELT rows across 534 daily files. The full source files and logs for those totals are unavailable here, so this audit did not check them.

### Exceptions to the Plan and deferrals

The user approved the following on 2026-10-15 ("record the three plan deviations as approved exceptions"):

- Nine aircraft slots used a substitute day, one more than the Plan's budget of 8.
- The aircraft day check counts 48 heatmap members and does not read the archive to the end or check the slot names (Plan rule 3 says it reads to the end).
- The thin-feed cutoff of 10 (largest sampled box count) is applied in `analyze.py` and is not in the Plan.

The user also approved this path: the licence, cost and delay cells of section 3 stay agent reports until step 5, and are checked at the source only for the sources that step 5 selects.

### First-move dates

Added 2026-10-15 after the results, because step 4 asked how early a signal moved and the Plan had no rule for a move. The rule below was written before the first run. One condition (a full 7-day span) was added after the first run, as noted below. It is post hoc: it was chosen after the daily results were known, and it has no false rate.

Rule. Series: GDELT conflict share and mean tone, and the four IODA signals. Aircraft (4 sample days) and USGS (counts, mostly zero) are not used. For each day, r is as in the Plan (the day divided by the baseline median of the same weekday). M(d) is the median of r over the 7 days d-6 to d, when the whole 7-day span lies inside the baseline plus the window and at least 4 of those 7 days have data. (The first run used shorter spans at the start of the baseline and flagged 64 series; the full-span condition was added before the final run, and the text and tables use the final run.) The band is the 5th to the 95th percentile of M(d) over the baseline days. The first move is the first window day with M(d) outside the band. The table gives that date, the days before the cut E, and the number of window days with M(d) outside the band. The baseline is in the sample: the weekday medians and the band come from the same 60 days.

Result (84 series: 14 rows with 2 GDELT and 4 IODA series each). The full table is in [discovery-step4-first-move.md](discovery-step4-first-move.md) and the data in `scripts/backtest/results/first_move.csv`.

- 65 of 84 series have a first-move date. 19 stay inside the band on every window day.
- 22 of the 65 are outside on the first day of the window (30 days before E). 16 start in the last 14 days, and 7 in the last 7 days.
- The surprise events (5, 6) are flagged in 15 of 18 series, tension events in 24 of 30, and threats events in 26 of 36. The rule does not separate the classes.
- For flat series the band is very narrow. `bgp` has bands such as 1.000 to 1.000 (Israel, event 5) and 0.999 to 1.000 (India), so a change of a few thousandths counts as a move. 9 of 14 `bgp` series are flagged.
- Reading: this rule flags most series in most events, so a first date is not a sign of an early warning. It describes when each series first left its own baseline band. A usable rule needs a control period and a band set by false alarms, which this step does not have.

### Source-record audit, 2026-10-15

This audit uses original IODA JSON and USGS CSV responses, GDELT daily exports, and the adsb.lol release index. The [record manifest](../scripts/backtest/evidence/manifest.json) gives each IODA and USGS request, date range, file size and SHA-256 hash. The [provider page manifest](../scripts/backtest/evidence/provider/manifest.json) gives the saved page URLs and hashes. The separate [verification script](../scripts/backtest/verify_discovery.py) checks each IODA file hash, full interval and native resolution before it reads the points and saved aircraft sample rows. Its [result](../scripts/backtest/evidence/verification.json) is the check, not another warning rule. It averages each IODA signal's raw points by UTC day, divides by the median for the same weekday in the 60-day baseline, and applies the existing seven-day first-move rule without tuning it to the comparison periods. The USGS check counts each CSV event date in the two periods. The aircraft check divides each saved box count by its Europe count and compares sample medians.

- **Internet series and comparison periods.** For India before the 2025-05-07 strike, `ping-slash24` has a median ratio of 0.988 over 30 of 30 window days; its first band exit is 2025-04-08, with 21 exit days. The same calculation for 2024-04-06 to 2024-05-05 gives 0.978 and 23 exit days. For Thailand before the 2025-07-24 escalation, `merit-nt` is 0.665 over 30 of 30 days, with 28 exit days. A [dated report of the earlier clash](https://apnews.com/article/a4b8547559ff1fe493bdac60aad008eb) places it on 2025-05-28; the 2025 IODA drop follows that clash. In 2024-06-23 to 2024-07-22, `merit-nt` is 1.274 over 30 of 30 days, with 23 exit days and 59 of 60 baseline days. Thailand's `ping-slash24` is 1.400 over the same 30 days, but has no band exit: its baseline band is wide, from 0.967 to 1.407. The 2024 periods have no start in the agreed event list; they were not checked as conflict-free or free of sensor changes. A [Thai government report from 2024-07-01](https://thailand.prd.go.th/en/content/category/detail/id/49/iid/302539) describes a cooperative bilateral visit on 2024-06-27, but does not certify every day as quiet. These are selected same-season comparisons, not controls for a false warning rate. The 2025 Thailand drop is a lead for more IODA testing, but the 2024 results show that a band exit alone is not a warning and the rule can miss a large shift. [Saved IODA measurement report](../scripts/backtest/evidence/provider/ioda-measurement-report.html).
- **Unusual baseline.** In Sudan, the original IODA `ping-slash24` points give a mean of 185.4 responsive network blocks on the first seven baseline days, 150.2 on the last seven, and a low of 132.0 on 2023-04-06. The reported 0.923 window ratio therefore rests on a falling baseline. The 2023 Karabakh baseline is within the months-long Lachin blockade, as the [dated report of the offensive](https://www.aljazeera.com/news/2023/9/19/azerbaijan-launches-new-anti-terror-offensive-in-nagorno-karabakh) states; this audit did not check its trend from original records. Neither is an established normal period. The Syria baseline shares the reported strike period, but the count of strikes was not checked again at its source in this audit.
- **News records and gaps.** The [dated URL and status log](../scripts/backtest/evidence/gdelt/status.tsv) was made by a [repeatable probe](../scripts/backtest/probe_gdelt_status.py). It shows HTTP 404 for 2023-03-23 and every day from 2025-06-14 through 2025-07-01, and HTTP 200 on the adjacent checked days. Thus the Sudan window loses one of 30 days; the Thailand event loses nine of 60 baseline days and nine of 30 window days. Two original [GDELT exports](https://data.gdeltproject.org/events/20250424.export.CSV.zip) were sampled: the [2025-04-24 India rows](../scripts/backtest/evidence/gdelt/20250424-IN.tsv) give 1,503 codes 18–20 among 12,132 rows, or 12.39%; the [2024-04-24 India rows](../scripts/backtest/evidence/gdelt/20240424-IN.tsv) give 306 among 6,057, or 5.05%. The [extraction script](../scripts/backtest/extract_gdelt_evidence.py) checks each source ZIP hash and selects columns 0, 56, 51, 28 and 34. The [first summary](../scripts/backtest/evidence/gdelt/20250424-IN-summary.json) and [second summary](../scripts/backtest/evidence/gdelt/20240424-IN-summary.json) give the source URLs and ZIP hashes. These two days check the field selection and division, not the 90-day ratios in the results table. `DATEADDED` is the date a report entered GDELT, which can differ from the event date ([field guide](https://data.gdeltproject.org/documentation/GDELT-Data_Format_Codebook.pdf)). Codes 18–20 include assault, abduction and fighting ([CAMEO codebook](https://data.gdeltproject.org/documentation/CAMEO.Manual.1.1b3.pdf)). The 2025 sample follows the Pahalgam attack of 2025-04-22 in the [dated report](https://carnegieendowment.org/posts/2025/05/indian-airstrikes-in-pakistan-may-7-2025); it does not establish a new early warning.
- **Aircraft and earthquakes.** The [adsb.lol release index](../scripts/backtest/evidence/adsb-2023-RELEASES.md) begins on 2023-02-16. From the saved [aircraft samples](../scripts/backtest/results/step4.csv), the Iran row for the June 2025 event has four baseline days and two window days, all before the final 14 days; it has no late aircraft value. The Iran row for February 2026 gives a 1.029 window ratio from four sample days and a 0.846 late ratio from two days. The Europe reference count ranges from 4,704 to 9,238 on its eight sample days. These calculations check arithmetic only; the original aircraft positions and receiver coverage were not saved. The [USGS CSV](../scripts/backtest/evidence/usgs/IN-event-2025.csv) gives 45 earthquakes of magnitude 4.5 or more in India's baseline and 12 in its window, as in the saved table. Twelve of the 45 baseline quakes occurred on 2025-03-28, the day of the magnitude 7.7 Mandalay earthquake inside the wide India box. A quake count is only a descriptive control here.

Unavailable evidence: the full original GDELT and adsb.lol responses for the 112 result rows, the former derived `data/` daily totals, receiver locations and coverage, a full audit of ordinary periods, a warning rule fixed before testing, and the last 72 hours before each event. The source records here cannot establish lead time, a false warning rate, or how the combined product would behave. The section 3 licence, cost and delay gaps for sources not selected in step 4 remain open.

## 4. Latency and refresh rate

Not started (step 5).

## 5. Transformations and mapping

Not started (step 5).

## 6. Stored data and retention

Not started (step 5).

## 7. Architecture and stack

Not started (step 5).

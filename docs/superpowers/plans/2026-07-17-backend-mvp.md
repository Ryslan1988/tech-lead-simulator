# Tech Lead Simulator — Backend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full Spring Boot backend for the Tech Lead Simulator MVP — all 8 endpoints from `openapi.yaml` (v0.2.0), backed by PostgreSQL with seed content, deterministic scoring, and a rule-based AI stub.

**Architecture:** Layered monolith (web → service → repository) over Spring Data JPA + PostgreSQL. Schema and seed data are owned by Flyway migrations (`ddl-auto=validate`). Interview state lives in the DB so the API is stateless. Content sits behind a `QuestionProvider` seam and AI analysis behind an `AiAnalyzer` seam, both swappable for an LLM later. DTOs are hand-written Java records; entities never leave the service layer.

**Tech Stack:** Spring Boot 4.1.0, Java 25, Spring MVC, Spring Data JPA (Hibernate 7.2), PostgreSQL, Flyway 11, Bean Validation, JUnit 5, Testcontainers 2.

## Global Constraints

- **Spring Boot 4.1.0** (not 3.x, not `4.1.0.RELEASE`). Per-module starters/test-starters — verify names against Context7 `/spring-projects/spring-boot/v4.1.0`.
- **Java 25**, base package `com.techleadsim`.
- **All endpoints under base path `/api`** (`server.servlet.context-path=/api`). Controller mappings are written WITHOUT the `/api` prefix.
- **Contract-first:** `openapi.yaml` (v0.2.0) at repo root is the source of truth. Controller methods match its `operationId`s exactly. Do not change the contract from code.
- **Persistence:** PostgreSQL only. Schema + seed owned by Flyway (`classpath:db/migration`). `spring.jpa.hibernate.ddl-auto=validate`.
- **Scoring:** correct answer = `10 + 2 × (resultingStreak − 1)` points; wrong = `0` and resets the streak to 0.
- **Game sizing:** 4 candidates (fixed roster, slots 0–3). CLASSIC = 10 questions, HARDCORE = 20. Seed holds ≥ 20 MEDIUM questions (the frontend always sends `MEDIUM`).
- **Candidate competence:** seed assigns the correct answer per question so stronger candidates are right more often (tied to `strengths`).
- **Read endpoints (`/statistic`, `/result`, `/ai-result`) are NOT gated on the offer** — they return data once rounds are answered. Interview `status` is advisory.
- **AI result:** synchronous rule-based stub, always HTTP 200 `READY`; never emits `202`/`PENDING`.
- **TDD:** every task writes a failing test first. Commit after each task. Run all tests with `./mvnw test` from `backend/`.
- **Boot 4 test-slice imports:** test-slice annotations moved to per-module packages in Boot 4.x (e.g. `@DataJpaTest` is `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`). If an `@AutoConfigureMockMvc` / `@WebMvcTest` import in a task fails to resolve, find the correct Boot 4.1 package via Context7 (`/spring-projects/spring-boot/v4.1.0`) — the annotation is unchanged, only its package moved.

---

## File structure

All paths under `backend/`.

```
src/main/java/com/techleadsim/
├── BackendApplication.java              (exists)
├── config/
│   └── WebCorsConfig.java               CORS for :5173
├── error/
│   ├── ApiException.java                base runtime exception (code + status)
│   ├── InterviewNotFoundException.java
│   ├── QuestionAlreadyAnsweredException.java
│   ├── NoQuestionAvailableException.java
│   ├── ApiErrorResponse.java            Error DTO (code, message, timestamp)
│   └── GlobalExceptionHandler.java      @RestControllerAdvice
├── domain/
│   ├── Mode.java  Difficulty.java  InterviewStatus.java     enums
│   ├── Candidate.java  CandidateStrength via @ElementCollection
│   ├── QuestionTemplate.java  AnswerTemplate.java
│   ├── Interview.java  InterviewRound.java
├── repository/
│   ├── CandidateRepository.java  QuestionTemplateRepository.java
│   ├── AnswerTemplateRepository.java
│   ├── InterviewRepository.java  InterviewRoundRepository.java
├── content/
│   ├── QuestionProvider.java            interface (seam)
│   └── SeedQuestionProvider.java        picks N questions by difficulty
├── ai/
│   ├── AiAnalyzer.java                  interface (seam)
│   ├── AiAnalysis.java                  analyzer output record
│   └── RuleBasedAiAnalyzer.java
├── service/
│   ├── ScoringService.java
│   ├── InterviewService.java            start / question / answer / offer / result
│   ├── StatisticService.java
│   └── PlayerStatsService.java
└── web/
    ├── HomeController.java  InterviewController.java
    ├── dto/                             hand-written request/response records
    └── mapper/DtoMapper.java            entity → DTO helpers

src/main/resources/
├── application.properties               (exists — extend)
└── db/migration/
    ├── V1__schema.sql
    └── V2__seed.sql

src/test/java/com/techleadsim/
├── BackendApplicationTests.java         (exists — make it extend the container base)
├── support/AbstractPostgresIntegrationTest.java
└── … per-task tests

backend/compose.yaml                     local Postgres for `./mvnw spring-boot:run`
```

---

### Task 1: Build setup — dependencies, config, Testcontainers base — ✅ COMPLETE (commits d0911be, cb9dacb)

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/compose.yaml`
- Create: `backend/src/test/java/com/techleadsim/support/AbstractPostgresIntegrationTest.java`
- Modify: `backend/src/test/java/com/techleadsim/BackendApplicationTests.java`

**Interfaces:**
- Produces: `AbstractPostgresIntegrationTest` — base class every `@SpringBootTest` extends; starts one shared Postgres container wired via `@ServiceConnection`.

- [x] **Step 1: Add dependencies to `pom.xml`**

Insert these into the existing `<dependencies>` block (keep the current webmvc, devtools, webmvc-test entries):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [x] **Step 2: Extend `application.properties`**

Append (keep the existing `spring.application.name` line):

```properties
# Datasource (real Postgres; overridden by Testcontainers in tests, by compose.yaml in dev)
spring.datasource.url=jdbc:postgresql://localhost:5432/techleadsim
spring.datasource.username=techleadsim
spring.datasource.password=techleadsim

# JPA — schema owned by Flyway
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# All REST endpoints live under /api
server.servlet.context-path=/api
```

- [x] **Step 3: Create `backend/compose.yaml`** (local Postgres for manual runs)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: techleadsim
      POSTGRES_USER: techleadsim
      POSTGRES_PASSWORD: techleadsim
    ports:
      - "5432:5432"
```

- [x] **Step 4: Create `AbstractPostgresIntegrationTest`**

```java
package com.techleadsim.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
}
```

- [x] **Step 5: Make `BackendApplicationTests` use the container**

```java
package com.techleadsim;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;

class BackendApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
```

- [x] **Step 6: Run the test — expect PASS (context boots against Postgres, Flyway finds no migrations yet)**

Run: `./mvnw test -Dtest=BackendApplicationTests`
Expected: PASS. Requires Docker running.

- [x] **Step 7: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.properties backend/compose.yaml backend/src/test/java/com/techleadsim/support/AbstractPostgresIntegrationTest.java backend/src/test/java/com/techleadsim/BackendApplicationTests.java
git commit -m "build: add JPA/Postgres/Flyway deps and Testcontainers base"
```

---

### Task 2: Flyway migrations — schema + seed — ✅ COMPLETE (commits a01da7b, d8500ac)

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed.sql`
- Test: `backend/src/test/java/com/techleadsim/MigrationSmokeTest.java`

**Interfaces:**
- Produces: tables `candidate`, `candidate_strength`, `question_template`, `answer_template`, `interview`, `interview_round`; seed of 4 candidates and 20 MEDIUM questions × 4 answers. Correct-answer slot distribution: slot0=10, slot2=5, slot1=4, slot3=1.

- [x] **Step 1: Write the failing smoke test**

```java
package com.techleadsim;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSmokeTest extends AbstractPostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedLoaded() {
        assertThat(jdbc.queryForObject("select count(*) from candidate", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "select count(*) from question_template where difficulty = 'MEDIUM'", Integer.class))
                .isGreaterThanOrEqualTo(20);
        assertThat(jdbc.queryForObject("select count(*) from answer_template", Integer.class)).isEqualTo(80);
        // exactly one correct answer per question
        assertThat(jdbc.queryForObject(
                "select count(*) from answer_template where is_correct", Integer.class)).isEqualTo(20);
    }
}
```

- [x] **Step 2: Run it — expect FAIL** (`relation "candidate" does not exist`)

Run: `./mvnw test -Dtest=MigrationSmokeTest`

- [x] **Step 3: Write `V1__schema.sql`**

```sql
create table candidate (
    id         bigint generated by default as identity primary key,
    name       varchar(100) not null,
    role       varchar(100) not null,
    avatar_url varchar(255),
    slot       int not null unique
);

create table candidate_strength (
    candidate_id bigint not null references candidate (id),
    strength     varchar(100) not null
);

create table question_template (
    id                 bigint generated by default as identity primary key,
    text               varchar(500) not null,
    topic              varchar(100) not null,
    difficulty         varchar(20)  not null,
    time_limit_seconds int
);

create table answer_template (
    id             bigint generated by default as identity primary key,
    question_id    bigint       not null references question_template (id),
    candidate_slot int          not null,
    text           varchar(500) not null,
    is_correct     boolean      not null default false
);

create table interview (
    id                 bigint generated by default as identity primary key,
    mode               varchar(20) not null,
    difficulty         varchar(20),
    player_name        varchar(100),
    status             varchar(20) not null,
    total_questions    int         not null,
    hired_candidate_id bigint references candidate (id),
    created_at         timestamp   not null
);

create table interview_round (
    id               bigint generated by default as identity primary key,
    interview_id     bigint  not null references interview (id),
    question_id      bigint  not null references question_template (id),
    round_index      int     not null,
    chosen_answer_id bigint,
    correct          boolean,
    points_awarded   int     not null default 0,
    answered         boolean not null default false
);

create index idx_round_interview on interview_round (interview_id);
create index idx_answer_question on answer_template (question_id);
```

- [x] **Step 4: Write `V2__seed.sql`**

Candidates (slots 0–3, strongest→weakest = Alexey, Dmitry, Maria, Sergey):

```sql
insert into candidate (id, name, role, avatar_url, slot) values
 (1, 'Alexey', 'Backend Developer', '/assets/candidates/alexey.png', 0),
 (2, 'Maria',  'Frontend Developer','/assets/candidates/maria.png',  1),
 (3, 'Dmitry', 'DevOps Engineer',   '/assets/candidates/dmitry.png', 2),
 (4, 'Sergey', 'Junior Developer',  '/assets/candidates/sergey.png', 3);

insert into candidate_strength (candidate_id, strength) values
 (1, 'Databases'), (1, 'Algorithms'), (1, 'Optimization'),
 (2, 'Frontend'), (2, 'CSS'), (2, 'Accessibility'),
 (3, 'CI/CD'), (3, 'Docker'), (3, 'Networking'),
 (4, 'Basics');

insert into question_template (id, text, topic, difficulty, time_limit_seconds) values
 (1,  'How would you speed up a slow SQL query?',                'Databases',    'MEDIUM', 45),
 (2,  'What does a CI pipeline typically automate?',             'DevOps',       'MEDIUM', 45),
 (3,  'What is the time complexity of binary search?',           'Algorithms',   'MEDIUM', 45),
 (4,  'How do you make a web page accessible to screen readers?','Frontend',     'MEDIUM', 45),
 (5,  'When would you add a database index?',                    'Databases',    'MEDIUM', 45),
 (6,  'What problem does Docker solve?',                         'DevOps',       'MEDIUM', 45),
 (7,  'How do you detect an O(n^2) bottleneck?',                 'Algorithms',   'MEDIUM', 45),
 (8,  'What is a variable?',                                     'Basics',       'MEDIUM', 45),
 (9,  'What is a database transaction?',                         'Databases',    'MEDIUM', 45),
 (10, 'Why use a reverse proxy?',                                'Networking',   'MEDIUM', 45),
 (11, 'What is normalization in databases?',                     'Databases',    'MEDIUM', 45),
 (12, 'How does CSS specificity work?',                          'Frontend',     'MEDIUM', 45),
 (13, 'How would you optimize an N+1 query?',                    'Optimization', 'MEDIUM', 45),
 (14, 'What is blue-green deployment?',                          'DevOps',       'MEDIUM', 45),
 (15, 'What is the difference between a stack and a queue?',     'Algorithms',   'MEDIUM', 45),
 (16, 'What is semantic HTML?',                                  'Frontend',     'MEDIUM', 45),
 (17, 'How do you pick between a hash map and a tree map?',      'Algorithms',   'MEDIUM', 45),
 (18, 'What does a load balancer do?',                           'Networking',   'MEDIUM', 45),
 (19, 'When is denormalization justified?',                      'Databases',    'MEDIUM', 45),
 (20, 'What is progressive enhancement?',                        'Frontend',     'MEDIUM', 45);
```

Answers — 4 per question, `candidate_slot` 0–3, ids `(q-1)*4 + slot + 1`. The correct slot per question follows the competence plan (slot0: q1,3,5,7,9,11,13,15,17,19; slot2: q2,6,10,14,18; slot1: q4,12,16,20; slot3: q8). Write all 80 rows; each block sets `is_correct = true` on exactly the planned slot:

```sql
insert into answer_template (id, question_id, candidate_slot, text, is_correct) values
 (1,1,0,'Add an index on the filtered columns and check the query plan.',true),
 (2,1,1,'Make the font bigger.',false),
 (3,1,2,'Restart the database server.',false),
 (4,1,3,'Delete some rows at random.',false),
 (5,2,0,'It renders CSS.',false),
 (6,2,1,'It designs the logo.',false),
 (7,2,2,'It builds, tests and deploys code automatically.',true),
 (8,2,3,'It writes the requirements.',false),
 (9,3,0,'O(log n).',true),
 (10,3,1,'O(n log n).',false),
 (11,3,2,'O(n).',false),
 (12,3,3,'O(1) always.',false),
 (13,4,0,'Use smaller images.',false),
 (14,4,1,'Add ARIA roles and semantic elements with alt text.',true),
 (15,4,2,'Disable JavaScript.',false),
 (16,4,3,'Nothing special is needed.',false),
 (17,5,0,'When a column is frequently used to filter or join.',true),
 (18,5,1,'On every column always.',false),
 (19,5,2,'Never, indexes are slow.',false),
 (20,5,3,'When the table is empty.',false),
 (21,6,0,'It compiles Java.',false),
 (22,6,1,'It styles the UI.',false),
 (23,6,2,'It packages an app with its dependencies into a portable image.',true),
 (24,6,3,'It replaces the database.',false),
 (25,7,0,'Profile the code and look for nested loops over the input.',true),
 (26,7,1,'Add more CSS.',false),
 (27,7,2,'Reboot the machine.',false),
 (28,7,3,'Guess randomly.',false),
 (29,8,0,'A named storage location for a value.',false),
 (30,8,1,'A kind of loop.',false),
 (31,8,2,'A network protocol.',false),
 (32,8,3,'A named storage location for a value.',true),
 (33,9,0,'A unit of work that is all-or-nothing (atomic).',true),
 (34,9,1,'A CSS rule.',false),
 (35,9,2,'A container image.',false),
 (36,9,3,'A type of button.',false),
 (37,10,0,'To store data.',false),
 (38,10,1,'To style pages.',false),
 (39,10,2,'To route and shield backend services behind one entry point.',true),
 (40,10,3,'To compile code.',false),
 (41,11,0,'Structuring tables to reduce redundancy.',true),
 (42,11,1,'Making text uppercase.',false),
 (43,11,2,'Scaling servers.',false),
 (44,11,3,'Deleting duplicates by hand.',false),
 (45,12,0,'Higher specificity is random.',false),
 (46,12,1,'Selectors are scored; more specific selectors win.',true),
 (47,12,2,'Only inline styles matter.',false),
 (48,12,3,'CSS has no specificity.',false),
 (49,13,0,'Batch the child lookups into one query (join or IN).',true),
 (50,13,1,'Add a spinner.',false),
 (51,13,2,'Increase the timeout.',false),
 (52,13,3,'Ignore it.',false),
 (53,14,0,'A sorting algorithm.',false),
 (54,14,1,'A CSS framework.',false),
 (55,14,2,'Deploy to a parallel environment then switch traffic over.',true),
 (56,14,3,'A database index.',false),
 (57,15,0,'A stack is LIFO, a queue is FIFO.',true),
 (58,15,1,'They are identical.',false),
 (59,15,2,'A stack is FIFO, a queue is LIFO.',false),
 (60,15,3,'Both are random access.',false),
 (61,16,0,'Using divs for everything.',false),
 (62,16,1,'Using elements that describe their meaning (nav, article, header).',true),
 (63,16,2,'Inlining all styles.',false),
 (64,16,3,'Avoiding HTML entirely.',false),
 (65,17,0,'Hash map for O(1) lookup, tree map when you need ordering.',true),
 (66,17,1,'Always use a tree map.',false),
 (67,17,2,'They are the same.',false),
 (68,17,3,'Never use maps.',false),
 (69,18,0,'It stores files.',false),
 (70,18,1,'It renders HTML.',false),
 (71,18,2,'It spreads traffic across multiple instances.',true),
 (72,18,3,'It compiles code.',false),
 (73,19,0,'When read performance outweighs redundancy costs.',true),
 (74,19,1,'Always denormalize.',false),
 (75,19,2,'Never denormalize.',false),
 (76,19,3,'Only on Sundays.',false),
 (77,20,0,'Disable the site for old browsers.',false),
 (78,20,1,'Start with a working baseline, then layer richer features.',true),
 (79,20,2,'Serve only images.',false),
 (80,20,3,'Require the latest browser.',false);
```

- [x] **Step 5: Run the smoke test — expect PASS**

Run: `./mvnw test -Dtest=MigrationSmokeTest`
Expected: PASS (4 candidates, ≥20 MEDIUM questions, 80 answers, 20 correct).

- [x] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration backend/src/test/java/com/techleadsim/MigrationSmokeTest.java
git commit -m "feat: add Flyway schema and seed content"
```

---

### Task 3: Domain model — enums, entities, repositories — ✅ COMPLETE (commit b7ece3b)

**Files:**
- Create: `domain/Mode.java`, `domain/Difficulty.java`, `domain/InterviewStatus.java`
- Create: `domain/Candidate.java`, `domain/QuestionTemplate.java`, `domain/AnswerTemplate.java`, `domain/Interview.java`, `domain/InterviewRound.java`
- Create: `repository/CandidateRepository.java`, `repository/QuestionTemplateRepository.java`, `repository/AnswerTemplateRepository.java`, `repository/InterviewRepository.java`, `repository/InterviewRoundRepository.java`
- Test: `src/test/java/com/techleadsim/DomainMappingTest.java`

**Interfaces:**
- Produces (entities with getters): `Candidate(id, name, role, avatarUrl, slot, strengths:List<String>)`, `QuestionTemplate(id, text, topic, difficulty:Difficulty, timeLimitSeconds:Integer)`, `AnswerTemplate(id, questionId, candidateSlot, text, correct:boolean)`, `Interview(id, mode:Mode, difficulty:Difficulty, playerName, status:InterviewStatus, totalQuestions, hiredCandidateId:Long, createdAt:Instant)`, `InterviewRound(id, interviewId, questionId, roundIndex, chosenAnswerId:Long, correct:Boolean, pointsAwarded, answered:boolean)`.
- Produces (repositories): `CandidateRepository.findAllByOrderBySlotAsc():List<Candidate>`, `findBySlot(int):Candidate`; `QuestionTemplateRepository.findByDifficulty(Difficulty):List<QuestionTemplate>`; `AnswerTemplateRepository.findByQuestionId(Long):List<AnswerTemplate>`, `findByQuestionIdInOrderById(Collection<Long>):List<AnswerTemplate>`; `InterviewRepository extends JpaRepository<Interview,Long>`; `InterviewRoundRepository.findByInterviewIdOrderByRoundIndexAsc(Long):List<InterviewRound>`.

- [x] **Step 1: Write the failing mapping test**

```java
package com.techleadsim;

import com.techleadsim.domain.*;
import com.techleadsim.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DomainMappingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired CandidateRepository candidates;
    @Autowired QuestionTemplateRepository questions;
    @Autowired AnswerTemplateRepository answers;
    @Autowired InterviewRepository interviews;
    @Autowired InterviewRoundRepository rounds;

    @Test
    void seedMapsToEntities() {
        List<Candidate> all = candidates.findAllByOrderBySlotAsc();
        assertThat(all).hasSize(4);
        assertThat(all.get(0).getName()).isEqualTo("Alexey");
        assertThat(all.get(0).getStrengths()).contains("Databases");

        QuestionTemplate q1 = questions.findByDifficulty(Difficulty.MEDIUM).get(0);
        assertThat(answers.findByQuestionId(q1.getId())).hasSize(4);
    }

    @Test
    void interviewRoundRoundTrips() {
        Interview i = new Interview(Mode.CLASSIC, Difficulty.MEDIUM, "You", 10, Instant.now());
        interviews.save(i);
        rounds.save(new InterviewRound(i.getId(), 1L, 1));
        assertThat(rounds.findByInterviewIdOrderByRoundIndexAsc(i.getId())).hasSize(1);
    }
}
```

- [x] **Step 2: Run it — expect FAIL** (types don't exist / won't compile)

Run: `./mvnw test -Dtest=DomainMappingTest`

- [x] **Step 3: Write the enums**

```java
package com.techleadsim.domain;
public enum Mode { CLASSIC, HARDCORE }
```
```java
package com.techleadsim.domain;
public enum Difficulty { EASY, MEDIUM, HARD }
```
```java
package com.techleadsim.domain;
public enum InterviewStatus { IN_PROGRESS, STATISTIC, OFFERED, FINISHED }
```

- [x] **Step 4: Write `Candidate`**

```java
package com.techleadsim.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String role;
    @Column(name = "avatar_url")
    private String avatarUrl;
    private int slot;

    @ElementCollection
    @CollectionTable(name = "candidate_strength", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "strength")
    private List<String> strengths = new ArrayList<>();

    protected Candidate() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getSlot() { return slot; }
    public List<String> getStrengths() { return strengths; }
}
```

- [x] **Step 5: Write `QuestionTemplate` and `AnswerTemplate`**

```java
package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "question_template")
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;
    private String topic;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    protected QuestionTemplate() {}

    public Long getId() { return id; }
    public String getText() { return text; }
    public String getTopic() { return topic; }
    public Difficulty getDifficulty() { return difficulty; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
}
```
```java
package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_template")
public class AnswerTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "candidate_slot")
    private int candidateSlot;
    private String text;
    @Column(name = "is_correct")
    private boolean correct;

    protected AnswerTemplate() {}

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public int getCandidateSlot() { return candidateSlot; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
```

- [x] **Step 6: Write `Interview`**

```java
package com.techleadsim.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Mode mode;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Column(name = "player_name")
    private String playerName;
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;
    @Column(name = "total_questions")
    private int totalQuestions;
    @Column(name = "hired_candidate_id")
    private Long hiredCandidateId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected Interview() {}

    public Interview(Mode mode, Difficulty difficulty, String playerName, int totalQuestions, Instant createdAt) {
        this.mode = mode;
        this.difficulty = difficulty;
        this.playerName = playerName;
        this.totalQuestions = totalQuestions;
        this.createdAt = createdAt;
        this.status = InterviewStatus.IN_PROGRESS;
    }

    public Long getId() { return id; }
    public Mode getMode() { return mode; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getPlayerName() { return playerName; }
    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public int getTotalQuestions() { return totalQuestions; }
    public Long getHiredCandidateId() { return hiredCandidateId; }
    public void setHiredCandidateId(Long id) { this.hiredCandidateId = id; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [x] **Step 7: Write `InterviewRound`**

```java
package com.techleadsim.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "interview_round")
public class InterviewRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "interview_id")
    private Long interviewId;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "round_index")
    private int roundIndex;
    @Column(name = "chosen_answer_id")
    private Long chosenAnswerId;
    private Boolean correct;
    @Column(name = "points_awarded")
    private int pointsAwarded;
    private boolean answered;

    protected InterviewRound() {}

    public InterviewRound(Long interviewId, Long questionId, int roundIndex) {
        this.interviewId = interviewId;
        this.questionId = questionId;
        this.roundIndex = roundIndex;
    }

    public Long getId() { return id; }
    public Long getInterviewId() { return interviewId; }
    public Long getQuestionId() { return questionId; }
    public int getRoundIndex() { return roundIndex; }
    public Long getChosenAnswerId() { return chosenAnswerId; }
    public Boolean getCorrect() { return correct; }
    public boolean isCorrect() { return Boolean.TRUE.equals(correct); }
    public int getPointsAwarded() { return pointsAwarded; }
    public boolean isAnswered() { return answered; }

    public void record(Long chosenAnswerId, boolean correct, int pointsAwarded) {
        this.chosenAnswerId = chosenAnswerId;
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.answered = true;
    }
}
```

- [x] **Step 8: Write the repositories**

```java
package com.techleadsim.repository;

import com.techleadsim.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findAllByOrderBySlotAsc();
    Candidate findBySlot(int slot);
}
```
```java
package com.techleadsim.repository;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    List<QuestionTemplate> findByDifficulty(Difficulty difficulty);
}
```
```java
package com.techleadsim.repository;

import com.techleadsim.domain.AnswerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface AnswerTemplateRepository extends JpaRepository<AnswerTemplate, Long> {
    List<AnswerTemplate> findByQuestionId(Long questionId);
    List<AnswerTemplate> findByQuestionIdInOrderById(Collection<Long> questionIds);
}
```
```java
package com.techleadsim.repository;

import com.techleadsim.domain.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
}
```
```java
package com.techleadsim.repository;

import com.techleadsim.domain.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {
    List<InterviewRound> findByInterviewIdOrderByRoundIndexAsc(Long interviewId);
}
```

- [x] **Step 9: Run the test — expect PASS**

Run: `./mvnw test -Dtest=DomainMappingTest`
Expected: PASS. (Also confirms `ddl-auto=validate` accepts the schema.)

- [x] **Step 10: Commit**

```bash
git add backend/src/main/java/com/techleadsim/domain backend/src/main/java/com/techleadsim/repository backend/src/test/java/com/techleadsim/DomainMappingTest.java
git commit -m "feat: add domain entities and repositories"
```

---

### Task 4: ScoringService — ✅ COMPLETE (commit a443a0f)

**Files:**
- Create: `service/ScoringService.java`
- Test: `src/test/java/com/techleadsim/service/ScoringServiceTest.java`

**Interfaces:**
- Produces: `ScoringService.pointsFor(boolean correct, int streakBefore): int` — returns `0` for wrong; for correct returns `10 + 2 × ((streakBefore + 1) − 1)` = `10 + 2 × streakBefore`.

- [x] **Step 1: Write the failing test**

```java
package com.techleadsim.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    @Test
    void wrongAnswerScoresZero() {
        assertThat(scoring.pointsFor(false, 5)).isZero();
    }

    @Test
    void firstCorrectScoresTen() {
        assertThat(scoring.pointsFor(true, 0)).isEqualTo(10);
    }

    @Test
    void streakAddsTwoEach() {
        assertThat(scoring.pointsFor(true, 1)).isEqualTo(12);
        assertThat(scoring.pointsFor(true, 3)).isEqualTo(16);
    }
}
```

- [x] **Step 2: Run it — expect FAIL** (`ScoringService` not found)

Run: `./mvnw test -Dtest=ScoringServiceTest`

- [x] **Step 3: Implement**

```java
package com.techleadsim.service;

import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    /** Points for an answer. streakBefore = consecutive correct answers immediately before this one. */
    public int pointsFor(boolean correct, int streakBefore) {
        if (!correct) {
            return 0;
        }
        int resultingStreak = streakBefore + 1;
        return 10 + 2 * (resultingStreak - 1);
    }
}
```

- [x] **Step 4: Run — expect PASS**

Run: `./mvnw test -Dtest=ScoringServiceTest`

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/techleadsim/service/ScoringService.java backend/src/test/java/com/techleadsim/service/ScoringServiceTest.java
git commit -m "feat: add scoring service"
```

---

### Task 5: Error handling infrastructure — ✅ COMPLETE (commit 8f99c6c)

**Files:**
- Create: `error/ApiException.java`, `error/InterviewNotFoundException.java`, `error/QuestionAlreadyAnsweredException.java`, `error/NoQuestionAvailableException.java`, `error/ApiErrorResponse.java`, `error/GlobalExceptionHandler.java`
- Test: `src/test/java/com/techleadsim/web/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `ApiErrorResponse(String code, String message, String timestamp)`; exceptions carrying `code()` + HTTP status — `InterviewNotFoundException` (404, `INTERVIEW_NOT_FOUND`), `NoQuestionAvailableException` (409, `NO_QUESTION_AVAILABLE`), `QuestionAlreadyAnsweredException` (409, `QUESTION_ALREADY_ANSWERED`). Bean-validation failures → 400 `BAD_REQUEST`. Later tasks throw these from services.

- [x] **Step 1: Write the failing test** (a throwaway controller exercises the advice)

```java
package com.techleadsim.web;

import com.techleadsim.error.InterviewNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    @RestController
    static class ProbeController {
        @GetMapping("/probe/not-found")
        String boom() { throw new InterviewNotFoundException(42L); }
    }

    @Test
    void notFoundMapsTo404AndErrorSchema() throws Exception {
        mvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
```

> Note: if the exact `@WebMvcTest` package import differs in Boot 4.1, resolve it via Context7; the annotation lives in the `spring-boot-webmvc-test` module.

- [x] **Step 2: Run it — expect FAIL** (types missing)

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`

- [x] **Step 3: Write `ApiException` and subclasses**

```java
package com.techleadsim.error;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    protected ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
```
```java
package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class InterviewNotFoundException extends ApiException {
    public InterviewNotFoundException(Long id) {
        super("INTERVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, "No interview session with id " + id + ".");
    }
}
```
```java
package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class NoQuestionAvailableException extends ApiException {
    public NoQuestionAvailableException(Long interviewId) {
        super("NO_QUESTION_AVAILABLE", HttpStatus.CONFLICT,
              "All rounds already answered for interview " + interviewId + ".");
    }
}
```
```java
package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class QuestionAlreadyAnsweredException extends ApiException {
    public QuestionAlreadyAnsweredException(Long questionId) {
        super("QUESTION_ALREADY_ANSWERED", HttpStatus.CONFLICT,
              "Question " + questionId + " was already answered.");
    }
}
```

- [x] **Step 4: Write `ApiErrorResponse`**

```java
package com.techleadsim.error;

public record ApiErrorResponse(String code, String message, String timestamp) {}
```

- [x] **Step 5: Write `GlobalExceptionHandler`**

```java
package com.techleadsim.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), Instant.now().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Invalid request.");
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", msg, Instant.now().toString()));
    }
}
```

- [x] **Step 6: Run — expect PASS**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`

- [x] **Step 7: Commit**

```bash
git add backend/src/main/java/com/techleadsim/error backend/src/test/java/com/techleadsim/web/GlobalExceptionHandlerTest.java
git commit -m "feat: add API error handling"
```

---

### Task 6: QuestionProvider + SeedQuestionProvider — ✅ COMPLETE (commit f0689ad)

**Files:**
- Create: `content/QuestionProvider.java`, `content/SeedQuestionProvider.java`
- Test: `src/test/java/com/techleadsim/content/SeedQuestionProviderTest.java`

**Interfaces:**
- Produces: `QuestionProvider.selectQuestions(Difficulty difficulty, int count): List<QuestionTemplate>` — returns exactly `count` questions preferring the requested difficulty, topping up from other difficulties if short; throws `IllegalStateException` if the total pool is smaller than `count`. Selection order is randomized per call.

- [x] **Step 1: Write the failing test**

```java
package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import com.techleadsim.repository.QuestionTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(SeedQuestionProvider.class)
class SeedQuestionProviderTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired SeedQuestionProvider provider;
    @Autowired QuestionTemplateRepository repo;

    @Test
    void selectsRequestedCountOfMediumQuestions() {
        List<QuestionTemplate> picked = provider.selectQuestions(Difficulty.MEDIUM, 10);
        assertThat(picked).hasSize(10);
        assertThat(picked).allMatch(q -> q.getDifficulty() == Difficulty.MEDIUM);
        assertThat(picked.stream().map(QuestionTemplate::getId).distinct()).hasSize(10);
    }

    @Test
    void supportsHardcoreCountOfTwenty() {
        assertThat(provider.selectQuestions(Difficulty.MEDIUM, 20)).hasSize(20);
    }
}
```

- [x] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=SeedQuestionProviderTest`

- [x] **Step 3: Write the interface**

```java
package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import java.util.List;

public interface QuestionProvider {
    List<QuestionTemplate> selectQuestions(Difficulty difficulty, int count);
}
```

- [x] **Step 4: Write `SeedQuestionProvider`**

```java
package com.techleadsim.content;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.QuestionTemplate;
import com.techleadsim.repository.QuestionTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class SeedQuestionProvider implements QuestionProvider {

    private final QuestionTemplateRepository questions;

    public SeedQuestionProvider(QuestionTemplateRepository questions) {
        this.questions = questions;
    }

    @Override
    public List<QuestionTemplate> selectQuestions(Difficulty difficulty, int count) {
        LinkedHashSet<QuestionTemplate> pool = new LinkedHashSet<>();

        List<QuestionTemplate> preferred = new ArrayList<>(
                difficulty == null ? List.of() : questions.findByDifficulty(difficulty));
        Collections.shuffle(preferred);
        pool.addAll(preferred);

        if (pool.size() < count) {
            List<QuestionTemplate> rest = new ArrayList<>(questions.findAll());
            Collections.shuffle(rest);
            pool.addAll(rest);
        }
        if (pool.size() < count) {
            throw new IllegalStateException(
                    "Question pool has " + pool.size() + " questions, need " + count);
        }
        return new ArrayList<>(pool).subList(0, count);
    }
}
```

- [x] **Step 5: Run — expect PASS**

Run: `./mvnw test -Dtest=SeedQuestionProviderTest`

- [x] **Step 6: Commit**

```bash
git add backend/src/main/java/com/techleadsim/content backend/src/test/java/com/techleadsim/content/SeedQuestionProviderTest.java
git commit -m "feat: add seed question provider"
```

---

### Task 7: POST /interviews (startInterview) — ✅ COMPLETE (commit cde8f79)

**Files:**
- Create: `web/dto/StartInterviewRequestDto.java`, `web/dto/CandidateDto.java`, `web/dto/InterviewSessionDto.java`
- Create: `web/mapper/DtoMapper.java`
- Create: `service/InterviewService.java`
- Create: `web/InterviewController.java`
- Test: `src/test/java/com/techleadsim/web/StartInterviewTest.java`

**Interfaces:**
- Consumes: `QuestionProvider.selectQuestions`, `CandidateRepository`, `InterviewRepository`, `InterviewRoundRepository`.
- Produces: `InterviewService.start(Mode mode, Difficulty difficulty, String playerName): Interview` (persists interview + rounds); `DtoMapper.toCandidateDto(Candidate)`; `CandidateDto(long id, String name, String role, String avatarUrl, List<String> strengths)`; `InterviewSessionDto(long interviewId, Mode mode, Difficulty difficulty, int totalQuestions, List<CandidateDto> candidates)`. `Mode.questionCount()` helper returns 10/20.

- [x] **Step 1: Add `questionCount()` to `Mode`** (modify `domain/Mode.java`)

```java
package com.techleadsim.domain;

public enum Mode {
    CLASSIC(10),
    HARDCORE(20);

    private final int questionCount;
    Mode(int questionCount) { this.questionCount = questionCount; }
    public int questionCount() { return questionCount; }
}
```

- [x] **Step 2: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class StartInterviewTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void startsClassicInterviewWithLineup() throws Exception {
        mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\",\"playerName\":\"You\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewId").isNumber())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.candidates.length()").value(4))
                .andExpect(jsonPath("$.candidates[0].name").value("Alexey"));
    }

    @Test
    void rejectsMissingMode() throws Exception {
        mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
```

- [x] **Step 3: Run it — expect FAIL**

Run: `./mvnw test -Dtest=StartInterviewTest`

- [x] **Step 4: Write the DTOs**

```java
package com.techleadsim.web.dto;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.Mode;
import jakarta.validation.constraints.NotNull;

public record StartInterviewRequestDto(@NotNull Mode mode, Difficulty difficulty, String playerName) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record CandidateDto(long id, String name, String role, String avatarUrl, List<String> strengths) {}
```
```java
package com.techleadsim.web.dto;

import com.techleadsim.domain.Difficulty;
import com.techleadsim.domain.Mode;
import java.util.List;

public record InterviewSessionDto(long interviewId, Mode mode, Difficulty difficulty,
                                  int totalQuestions, List<CandidateDto> candidates) {}
```

- [x] **Step 5: Write `DtoMapper`** (grows over later tasks; start with candidate mapping)

```java
package com.techleadsim.web.mapper;

import com.techleadsim.domain.Candidate;
import com.techleadsim.web.dto.CandidateDto;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public CandidateDto toCandidateDto(Candidate c) {
        return new CandidateDto(c.getId(), c.getName(), c.getRole(), c.getAvatarUrl(), c.getStrengths());
    }
}
```

- [x] **Step 6: Write `InterviewService.start`**

```java
package com.techleadsim.service;

import com.techleadsim.content.QuestionProvider;
import com.techleadsim.domain.*;
import com.techleadsim.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InterviewService {

    private final QuestionProvider questionProvider;
    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;

    public InterviewService(QuestionProvider questionProvider,
                            InterviewRepository interviews,
                            InterviewRoundRepository rounds) {
        this.questionProvider = questionProvider;
        this.interviews = interviews;
        this.rounds = rounds;
    }

    @Transactional
    public Interview start(Mode mode, Difficulty difficulty, String playerName) {
        int total = mode.questionCount();
        Interview interview = interviews.save(
                new Interview(mode, difficulty, playerName, total, Instant.now()));

        List<QuestionTemplate> picked = questionProvider.selectQuestions(difficulty, total);
        for (int i = 0; i < picked.size(); i++) {
            rounds.save(new InterviewRound(interview.getId(), picked.get(i).getId(), i + 1));
        }
        return interview;
    }
}
```

- [x] **Step 7: Write `InterviewController`** (start endpoint only for now)

```java
package com.techleadsim.web;

import com.techleadsim.domain.Candidate;
import com.techleadsim.domain.Interview;
import com.techleadsim.repository.CandidateRepository;
import com.techleadsim.service.InterviewService;
import com.techleadsim.web.dto.*;
import com.techleadsim.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final CandidateRepository candidates;
    private final DtoMapper mapper;

    public InterviewController(InterviewService interviewService,
                               CandidateRepository candidates, DtoMapper mapper) {
        this.interviewService = interviewService;
        this.candidates = candidates;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<InterviewSessionDto> startInterview(@Valid @RequestBody StartInterviewRequestDto req) {
        Interview interview = interviewService.start(req.mode(), req.difficulty(), req.playerName());
        List<CandidateDto> lineup = candidates.findAllByOrderBySlotAsc().stream()
                .map(mapper::toCandidateDto).toList();
        InterviewSessionDto body = new InterviewSessionDto(
                interview.getId(), interview.getMode(), interview.getDifficulty(),
                interview.getTotalQuestions(), lineup);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
```

- [x] **Step 8: Run — expect PASS**

Run: `./mvnw test -Dtest=StartInterviewTest`

- [x] **Step 9: Commit**

```bash
git add backend/src/main/java/com/techleadsim/web backend/src/main/java/com/techleadsim/service/InterviewService.java backend/src/main/java/com/techleadsim/domain/Mode.java backend/src/test/java/com/techleadsim/web/StartInterviewTest.java
git commit -m "feat: implement POST /interviews (startInterview)"
```

---

### Task 8: GET /interviews/{id}/question (getQuestion) — ✅ COMPLETE (commit a4f09ca)

**Files:**
- Create: `web/dto/AnswerOptionDto.java`, `web/dto/QuestionDto.java`
- Modify: `service/InterviewService.java` (add `nextQuestion`), `web/InterviewController.java` (add endpoint), `web/mapper/DtoMapper.java` (add question mapping)
- Test: `src/test/java/com/techleadsim/web/GetQuestionTest.java`

**Interfaces:**
- Consumes: `AnswerTemplateRepository.findByQuestionId`, `CandidateRepository.findBySlot`, `QuestionTemplateRepository`, `InterviewRoundRepository`.
- Produces: `InterviewService.nextQuestion(long interviewId): QuestionView` where `QuestionView(QuestionTemplate question, int index, int total, List<AnswerTemplate> answers)`; throws `InterviewNotFoundException`/`NoQuestionAvailableException`. `AnswerOptionDto(long answerId, long candidateId, String text)`; `QuestionDto(long questionId, int index, int total, String text, Integer timeLimitSeconds, List<AnswerOptionDto> answers)`. Correctness is NOT exposed.

- [x] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetQuestionTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    private long startInterview() throws Exception {
        MvcResult r = mvc.perform(post("/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}"))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.parse(r.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class).longValue();
    }

    @Test
    void returnsFirstQuestionWithFourAnswersNoCorrectFlag() throws Exception {
        long id = startInterview();
        mvc.perform(get("/interviews/{id}/question", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(1))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.answers.length()").value(4))
                .andExpect(jsonPath("$.answers[0].correct").doesNotExist());
    }

    @Test
    void unknownInterviewIs404() throws Exception {
        mvc.perform(get("/interviews/{id}/question", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_NOT_FOUND"));
    }
}
```

- [x] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=GetQuestionTest`

- [x] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;
public record AnswerOptionDto(long answerId, long candidateId, String text) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record QuestionDto(long questionId, int index, int total, String text,
                          Integer timeLimitSeconds, List<AnswerOptionDto> answers) {}
```

- [x] **Step 4: Add `QuestionView` + `nextQuestion` to `InterviewService`**

Add these fields to the constructor/class (inject the extra repositories), and the method:

```java
// add to imports: com.techleadsim.error.*; java.util.Comparator;
// add fields + constructor params:
private final QuestionTemplateRepository questionTemplates;
private final AnswerTemplateRepository answerTemplates;
// (extend the constructor to receive and assign both)

public record QuestionView(QuestionTemplate question, int index, int total, List<AnswerTemplate> answers) {}

@Transactional(readOnly = true)
public QuestionView nextQuestion(long interviewId) {
    Interview interview = interviews.findById(interviewId)
            .orElseThrow(() -> new com.techleadsim.error.InterviewNotFoundException(interviewId));
    InterviewRound next = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId).stream()
            .filter(r -> !r.isAnswered())
            .findFirst()
            .orElseThrow(() -> new com.techleadsim.error.NoQuestionAvailableException(interviewId));
    QuestionTemplate q = questionTemplates.findById(next.getQuestionId()).orElseThrow();
    List<AnswerTemplate> answers = answerTemplates.findByQuestionId(q.getId());
    return new QuestionView(q, next.getRoundIndex(), interview.getTotalQuestions(), answers);
}
```

- [x] **Step 5: Add question mapping to `DtoMapper`**

```java
// add imports: com.techleadsim.domain.AnswerTemplate; com.techleadsim.repository.CandidateRepository;
// com.techleadsim.web.dto.*; java.util.List;

private final CandidateRepository candidates;
public DtoMapper(CandidateRepository candidates) { this.candidates = candidates; }

public AnswerOptionDto toAnswerOption(AnswerTemplate a) {
    long candidateId = candidates.findBySlot(a.getCandidateSlot()).getId();
    return new AnswerOptionDto(a.getId(), candidateId, a.getText());
}
```

> The `DtoMapper` now has a constructor; keep `toCandidateDto` as-is.

- [x] **Step 6: Add the controller endpoint** (in `InterviewController`)

```java
// add imports: com.techleadsim.service.InterviewService.QuestionView; com.techleadsim.web.dto.QuestionDto;

@GetMapping("/{interviewId}/question")
public QuestionDto getQuestion(@PathVariable long interviewId) {
    QuestionView v = interviewService.nextQuestion(interviewId);
    List<AnswerOptionDto> answers = v.answers().stream().map(mapper::toAnswerOption).toList();
    return new QuestionDto(v.question().getId(), v.index(), v.total(),
            v.question().getText(), v.question().getTimeLimitSeconds(), answers);
}
```

- [x] **Step 7: Run — expect PASS**

Run: `./mvnw test -Dtest=GetQuestionTest`

- [x] **Step 8: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/GetQuestionTest.java
git commit -m "feat: implement GET question"
```

---

### Task 9: POST /interviews/{id}/answers (saveAnswer) — ✅ COMPLETE (commits 7ba79cf, 274eaa6)

**Files:**
- Create: `web/dto/AnswerRequestDto.java`, `web/dto/AnswerResultDto.java`
- Modify: `service/InterviewService.java` (add `saveAnswer`), `web/InterviewController.java`
- Test: `src/test/java/com/techleadsim/web/SaveAnswerTest.java`

**Interfaces:**
- Consumes: `ScoringService.pointsFor`, `AnswerTemplateRepository`, `InterviewRoundRepository`.
- Produces: `InterviewService.saveAnswer(long interviewId, long questionId, long answerId): AnswerResult` where `AnswerResult(boolean correct, long correctAnswerId, int pointsAwarded, int correctCount, int currentStreak, int totalPoints, int answeredCount, int totalQuestions, boolean finished)`; throws `InterviewNotFoundException` (unknown interview), `InterviewNotFoundException`-style 404 if the question isn't part of the interview, `QuestionAlreadyAnsweredException`. `AnswerRequestDto(@NotNull Long questionId, @NotNull Long answerId)`; `AnswerResultDto` mirrors `AnswerResult`. Sets interview status → `STATISTIC` when the last round is answered.

- [x] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SaveAnswerTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void recordsAnswerAndReturnsFeedback() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
        int questionId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.questionId");
        int firstAnswerId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.answers[0].answerId");

        mvc.perform(post("/interviews/{id}/answers", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerId\":" + firstAnswerId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswerId").isNumber())
                .andExpect(jsonPath("$.answeredCount").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.finished").value(false));
    }

    @Test
    void answeringTwiceIs409() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
        int questionId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.questionId");
        int answerId = JsonPath.parse(q.getResponse().getContentAsString()).read("$.answers[0].answerId");
        String body = "{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}";

        mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON).content(body));
        mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_ALREADY_ANSWERED"));
    }
}
```

- [x] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=SaveAnswerTest`

- [x] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;
import jakarta.validation.constraints.NotNull;

public record AnswerRequestDto(@NotNull Long questionId, @NotNull Long answerId) {}
```
```java
package com.techleadsim.web.dto;

public record AnswerResultDto(boolean correct, long correctAnswerId, int pointsAwarded,
                              int correctCount, int currentStreak, int totalPoints,
                              int answeredCount, int totalQuestions, boolean finished) {}
```

- [x] **Step 4: Add `AnswerResult` record + `saveAnswer` to `InterviewService`** (inject `ScoringService`)

```java
// add field + constructor param: private final ScoringService scoring;

public record AnswerResult(boolean correct, long correctAnswerId, int pointsAwarded,
                           int correctCount, int currentStreak, int totalPoints,
                           int answeredCount, int totalQuestions, boolean finished) {}

@Transactional
public AnswerResult saveAnswer(long interviewId, long questionId, long answerId) {
    Interview interview = interviews.findById(interviewId)
            .orElseThrow(() -> new com.techleadsim.error.InterviewNotFoundException(interviewId));
    List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

    InterviewRound round = ordered.stream()
            .filter(r -> r.getQuestionId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> new com.techleadsim.error.InterviewNotFoundException(interviewId));
    if (round.isAnswered()) {
        throw new com.techleadsim.error.QuestionAlreadyAnsweredException(questionId);
    }

    long correctAnswerId = answerTemplates.findByQuestionId(questionId).stream()
            .filter(AnswerTemplate::isCorrect).findFirst().orElseThrow().getId();
    boolean correct = answerId == correctAnswerId;

    int streakBefore = trailingStreak(ordered);
    int points = scoring.pointsFor(correct, streakBefore);
    round.record(answerId, correct, points);
    rounds.save(round);

    // recompute aggregates from the (now updated) rounds
    List<InterviewRound> after = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);
    int answered = (int) after.stream().filter(InterviewRound::isAnswered).count();
    int correctCount = (int) after.stream().filter(InterviewRound::isCorrect).count();
    int totalPoints = after.stream().mapToInt(InterviewRound::getPointsAwarded).sum();
    int currentStreak = trailingStreak(after);
    boolean finished = answered == interview.getTotalQuestions();
    if (finished && interview.getStatus() == InterviewStatus.IN_PROGRESS) {
        interview.setStatus(InterviewStatus.STATISTIC);
    }
    return new AnswerResult(correct, correctAnswerId, points, correctCount, currentStreak,
            totalPoints, answered, interview.getTotalQuestions(), finished);
}

/** Consecutive correct answers at the tail of the answered prefix. */
private int trailingStreak(List<InterviewRound> ordered) {
    int streak = 0;
    for (InterviewRound r : ordered) {
        if (!r.isAnswered()) break;
        streak = r.isCorrect() ? streak + 1 : 0;
    }
    return streak;
}
```

- [x] **Step 5: Add the controller endpoint**

```java
// add imports: com.techleadsim.service.InterviewService.AnswerResult; com.techleadsim.web.dto.*;

@PostMapping("/{interviewId}/answers")
public AnswerResultDto saveAnswer(@PathVariable long interviewId, @Valid @RequestBody AnswerRequestDto req) {
    AnswerResult r = interviewService.saveAnswer(interviewId, req.questionId(), req.answerId());
    return new AnswerResultDto(r.correct(), r.correctAnswerId(), r.pointsAwarded(), r.correctCount(),
            r.currentStreak(), r.totalPoints(), r.answeredCount(), r.totalQuestions(), r.finished());
}
```

- [x] **Step 6: Run — expect PASS**

Run: `./mvnw test -Dtest=SaveAnswerTest`

- [x] **Step 7: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/SaveAnswerTest.java
git commit -m "feat: implement POST answers (saveAnswer)"
```

---

### Task 10: GET /interviews/{id}/statistic (getInterviewStatistic) — ✅ COMPLETE (commits 0bc0f81, b6ae3c3)

**Files:**
- Create: `web/dto/CandidateSelectionDto.java`, `web/dto/InterviewStatisticDto.java`
- Create: `service/StatisticService.java`
- Modify: `web/InterviewController.java`
- Test: `src/test/java/com/techleadsim/web/GetStatisticTest.java`

**Interfaces:**
- Consumes: `InterviewRepository`, `InterviewRoundRepository`, `AnswerTemplateRepository.findByQuestionIdInOrderById`, `CandidateRepository`.
- Produces: `StatisticService.compute(long interviewId): InterviewStatisticDto` — throws `InterviewNotFoundException`. `CandidateSelectionDto(long candidateId, String name, String role, int timesChosen, int correctAnswers)`; `InterviewStatisticDto(int totalQuestions, int correctCount, List<CandidateSelectionDto> perCandidate)`. `timesChosen` = player picks of that candidate's answer; `correctAnswers` = rounds where that candidate's slot held the correct answer.

- [x] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetStatisticTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void reportsPerCandidateChosenAndCorrectCounts() throws Exception {
        long id = playFullGamePickingFirstAnswer();
        mvc.perform(get("/interviews/{id}/statistic", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.perCandidate.length()").value(4))
                .andExpect(jsonPath("$.perCandidate[0].timesChosen").isNumber())
                .andExpect(jsonPath("$.perCandidate[0].correctAnswers").isNumber());
    }

    /** Answers every round by always choosing the first answer option. Returns the interview id. */
    private long playFullGamePickingFirstAnswer() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            int answerId = JsonPath.parse(json).read("$.answers[0].answerId");
            mvc.perform(post("/interviews/{id}/answers", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"));
        }
        return id;
    }
}
```

- [x] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=GetStatisticTest`

- [x] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;

public record CandidateSelectionDto(long candidateId, String name, String role,
                                    int timesChosen, int correctAnswers) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record InterviewStatisticDto(int totalQuestions, int correctCount,
                                    List<CandidateSelectionDto> perCandidate) {}
```

- [x] **Step 4: Write `StatisticService`**

```java
package com.techleadsim.service;

import com.techleadsim.domain.*;
import com.techleadsim.error.InterviewNotFoundException;
import com.techleadsim.repository.*;
import com.techleadsim.web.dto.CandidateSelectionDto;
import com.techleadsim.web.dto.InterviewStatisticDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticService {

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;
    private final AnswerTemplateRepository answerTemplates;
    private final CandidateRepository candidates;

    public StatisticService(InterviewRepository interviews, InterviewRoundRepository rounds,
                            AnswerTemplateRepository answerTemplates, CandidateRepository candidates) {
        this.interviews = interviews;
        this.rounds = rounds;
        this.answerTemplates = answerTemplates;
        this.candidates = candidates;
    }

    @Transactional(readOnly = true)
    public InterviewStatisticDto compute(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> all = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        List<Long> questionIds = all.stream().map(InterviewRound::getQuestionId).toList();
        Map<Long, AnswerTemplate> byAnswerId = new HashMap<>();
        Map<Long, AnswerTemplate> correctByQuestion = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (AnswerTemplate a : answerTemplates.findByQuestionIdInOrderById(questionIds)) {
                byAnswerId.put(a.getId(), a);
                if (a.isCorrect()) correctByQuestion.put(a.getQuestionId(), a);
            }
        }

        List<Candidate> roster = candidates.findAllByOrderBySlotAsc();
        List<CandidateSelectionDto> perCandidate = new ArrayList<>();
        int playerCorrect = (int) all.stream().filter(InterviewRound::isCorrect).count();

        for (Candidate c : roster) {
            int timesChosen = (int) all.stream()
                    .filter(r -> r.getChosenAnswerId() != null)
                    .map(r -> byAnswerId.get(r.getChosenAnswerId()))
                    .filter(Objects::nonNull)
                    .filter(a -> a.getCandidateSlot() == c.getSlot())
                    .count();
            int correctAnswers = (int) all.stream()
                    .map(r -> correctByQuestion.get(r.getQuestionId()))
                    .filter(Objects::nonNull)
                    .filter(a -> a.getCandidateSlot() == c.getSlot())
                    .count();
            perCandidate.add(new CandidateSelectionDto(
                    c.getId(), c.getName(), c.getRole(), timesChosen, correctAnswers));
        }
        return new InterviewStatisticDto(interview.getTotalQuestions(), playerCorrect, perCandidate);
    }
}
```

- [x] **Step 5: Add the controller endpoint** (inject `StatisticService`)

```java
// field + constructor param: private final StatisticService statisticService;

@GetMapping("/{interviewId}/statistic")
public InterviewStatisticDto getInterviewStatistic(@PathVariable long interviewId) {
    return statisticService.compute(interviewId);
}
```

- [x] **Step 6: Run — expect PASS**

Run: `./mvnw test -Dtest=GetStatisticTest`

- [x] **Step 7: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/GetStatisticTest.java
git commit -m "feat: implement GET statistic with candidate competence"
```

---

### Task 11: POST /interviews/{id}/offer (offer)

**Files:**
- Create: `web/dto/OfferRequestDto.java`, `web/dto/OfferResultDto.java`
- Modify: `service/InterviewService.java` (add `offer`), `web/InterviewController.java`
- Test: `src/test/java/com/techleadsim/web/OfferTest.java`

**Interfaces:**
- Consumes: `CandidateRepository`, `InterviewRepository`.
- Produces: `InterviewService.offer(long interviewId, long personId): Candidate` — sets `hiredCandidateId`, status → `OFFERED`; throws `InterviewNotFoundException`; throws a 400 `ApiException` (`BAD_REQUEST`) if `personId` is not a real candidate. `OfferRequestDto(@NotNull Long personId)`; `OfferResultDto(long interviewId, CandidateDto hiredCandidate, String message)`.

- [ ] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class OfferTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void hiresCandidate() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);

        mvc.perform(post("/interviews/{id}/offer", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiredCandidate.id").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unknownCandidateIs400() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        mvc.perform(post("/interviews/{id}/offer", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personId\":999}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=OfferTest`

- [ ] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;
import jakarta.validation.constraints.NotNull;

public record OfferRequestDto(@NotNull Long personId) {}
```
```java
package com.techleadsim.web.dto;

public record OfferResultDto(long interviewId, CandidateDto hiredCandidate, String message) {}
```

- [ ] **Step 4: Add a generic 400 exception** — `error/InvalidRequestException.java`

```java
package com.techleadsim.error;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super("BAD_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
```

- [ ] **Step 5: Add `offer` to `InterviewService`** (inject `CandidateRepository`)

```java
// field + constructor param: private final CandidateRepository candidates;

@Transactional
public Candidate offer(long interviewId, long personId) {
    Interview interview = interviews.findById(interviewId)
            .orElseThrow(() -> new com.techleadsim.error.InterviewNotFoundException(interviewId));
    Candidate hired = candidates.findById(personId)
            .orElseThrow(() -> new com.techleadsim.error.InvalidRequestException(
                    "No candidate with id " + personId + "."));
    interview.setHiredCandidateId(hired.getId());
    interview.setStatus(InterviewStatus.OFFERED);
    return hired;
}
```

- [ ] **Step 6: Add the controller endpoint**

```java
@PostMapping("/{interviewId}/offer")
public OfferResultDto offer(@PathVariable long interviewId, @Valid @RequestBody OfferRequestDto req) {
    Candidate hired = interviewService.offer(interviewId, req.personId());
    return new OfferResultDto(interviewId, mapper.toCandidateDto(hired),
            hired.getName() + " has joined your team!");
}
```

- [ ] **Step 7: Run — expect PASS**

Run: `./mvnw test -Dtest=OfferTest`

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/OfferTest.java
git commit -m "feat: implement POST offer"
```

---

### Task 12: GET /interviews/{id}/result (getInterviewResult)

**Files:**
- Create: `web/dto/QuestionOutcomeDto.java`, `web/dto/InterviewResultDto.java`
- Modify: `service/InterviewService.java` (add `result`), `web/InterviewController.java`, `web/mapper/DtoMapper.java`
- Test: `src/test/java/com/techleadsim/web/GetResultTest.java`

**Interfaces:**
- Consumes: `QuestionTemplateRepository`, `InterviewRoundRepository`.
- Produces: `InterviewService.result(long interviewId): InterviewResultDto` — throws `InterviewNotFoundException`; NOT gated on the offer. `QuestionOutcomeDto(long questionId, String text, boolean correct)`; `InterviewResultDto(long interviewId, int correctCount, int totalQuestions, int totalPoints, int bestStreak, List<QuestionOutcomeDto> breakdown)`. `bestStreak` = longest run of consecutive correct answered rounds; breakdown covers answered rounds in order.

- [ ] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetResultTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void returnsPlayerScoreBeforeOffer() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id)).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            int answerId = JsonPath.parse(json).read("$.answers[0].answerId");
            mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"));
        }
        // no offer made — result must still be available
        mvc.perform(get("/interviews/{id}/result", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(10))
                .andExpect(jsonPath("$.breakdown.length()").value(10))
                .andExpect(jsonPath("$.bestStreak").isNumber());
    }
}
```

- [ ] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=GetResultTest`

- [ ] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;
public record QuestionOutcomeDto(long questionId, String text, boolean correct) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record InterviewResultDto(long interviewId, int correctCount, int totalQuestions,
                                 int totalPoints, int bestStreak, List<QuestionOutcomeDto> breakdown) {}
```

- [ ] **Step 4: Add `result` to `InterviewService`**

```java
@Transactional(readOnly = true)
public com.techleadsim.web.dto.InterviewResultDto result(long interviewId) {
    Interview interview = interviews.findById(interviewId)
            .orElseThrow(() -> new com.techleadsim.error.InterviewNotFoundException(interviewId));
    List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

    int correctCount = (int) ordered.stream().filter(InterviewRound::isCorrect).count();
    int totalPoints = ordered.stream().mapToInt(InterviewRound::getPointsAwarded).sum();
    int bestStreak = 0, run = 0;
    List<com.techleadsim.web.dto.QuestionOutcomeDto> breakdown = new java.util.ArrayList<>();
    for (InterviewRound r : ordered) {
        if (!r.isAnswered()) continue;
        run = r.isCorrect() ? run + 1 : 0;
        bestStreak = Math.max(bestStreak, run);
        QuestionTemplate q = questionTemplates.findById(r.getQuestionId()).orElseThrow();
        breakdown.add(new com.techleadsim.web.dto.QuestionOutcomeDto(q.getId(), q.getText(), r.isCorrect()));
    }
    return new com.techleadsim.web.dto.InterviewResultDto(interviewId, correctCount,
            interview.getTotalQuestions(), totalPoints, bestStreak, breakdown);
}
```

- [ ] **Step 5: Add the controller endpoint**

```java
@GetMapping("/{interviewId}/result")
public InterviewResultDto getInterviewResult(@PathVariable long interviewId) {
    return interviewService.result(interviewId);
}
```

- [ ] **Step 6: Run — expect PASS**

Run: `./mvnw test -Dtest=GetResultTest`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/GetResultTest.java
git commit -m "feat: implement GET result"
```

---

### Task 13: GET /interviews/{id}/ai-result (getAiInterviewResult)

**Files:**
- Create: `ai/AiAnalyzer.java`, `ai/AiAnalysis.java`, `ai/RuleBasedAiAnalyzer.java`
- Create: `web/dto/ResourceDto.java`, `web/dto/RoadmapItemDto.java`, `web/dto/AiInterviewResultDto.java`
- Modify: `web/InterviewController.java`
- Test: `src/test/java/com/techleadsim/ai/RuleBasedAiAnalyzerTest.java`, `src/test/java/com/techleadsim/web/GetAiResultTest.java`

**Interfaces:**
- Consumes: `InterviewRepository`, `InterviewRoundRepository`, `QuestionTemplateRepository`, `AnswerTemplateRepository`, `CandidateRepository`.
- Produces: `AiAnalyzer.analyze(long interviewId): AiInterviewResultDto` (always status `READY`); throws `InterviewNotFoundException`. `ResourceDto(String title, String url)`; `RoadmapItemDto(String topic, String reason, String priority, List<ResourceDto> resources)`; `AiInterviewResultDto(long interviewId, String status, String summary, String verdict, Long hiredCandidateId, List<RoadmapItemDto> roadmap)`.

- [ ] **Step 1: Write the failing analyzer unit test** (uses real repos via `@SpringBootTest`)

```java
package com.techleadsim.ai;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.techleadsim.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.techleadsim.domain.*;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAiAnalyzerTest extends AbstractPostgresIntegrationTest {

    @Autowired InterviewService interviewService;
    @Autowired AiAnalyzer analyzer;

    @Test
    void producesReadyAnalysisWithRoadmap() {
        Interview i = interviewService.start(Mode.CLASSIC, Difficulty.MEDIUM, "You");
        // answer every round wrong by picking a definitely-wrong answer id (0)
        var rounds = interviewService.debugRounds(i.getId()); // helper below
        for (var r : rounds) {
            interviewService.saveAnswer(i.getId(), r.getQuestionId(), -1L);
        }
        var ai = analyzer.analyze(i.getId());
        assertThat(ai.status()).isEqualTo("READY");
        assertThat(ai.summary()).isNotBlank();
        assertThat(ai.roadmap()).isNotEmpty(); // all wrong → topics to study
    }
}
```

> Add a small package-visible helper `public List<InterviewRound> debugRounds(long id)` to `InterviewService` that returns `rounds.findByInterviewIdOrderByRoundIndexAsc(id)` — used only by tests to enumerate question ids. Alternatively, drive it through the HTTP flow like the other tests; either is acceptable.

- [ ] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=RuleBasedAiAnalyzerTest`

- [ ] **Step 3: Write the DTOs**

```java
package com.techleadsim.web.dto;
public record ResourceDto(String title, String url) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record RoadmapItemDto(String topic, String reason, String priority, List<ResourceDto> resources) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record AiInterviewResultDto(long interviewId, String status, String summary, String verdict,
                                   Long hiredCandidateId, List<RoadmapItemDto> roadmap) {}
```

- [ ] **Step 4: Write the `AiAnalyzer` seam + output alias**

```java
package com.techleadsim.ai;

import com.techleadsim.web.dto.AiInterviewResultDto;

public interface AiAnalyzer {
    AiInterviewResultDto analyze(long interviewId);
}
```
```java
package com.techleadsim.ai;
// Placeholder for a future richer analyzer output; MVP maps straight to the DTO.
public final class AiAnalysis { private AiAnalysis() {} }
```

- [ ] **Step 5: Write `RuleBasedAiAnalyzer`**

```java
package com.techleadsim.ai;

import com.techleadsim.domain.*;
import com.techleadsim.error.InterviewNotFoundException;
import com.techleadsim.repository.*;
import com.techleadsim.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleBasedAiAnalyzer implements AiAnalyzer {

    private static final Map<String, ResourceDto> RESOURCES = Map.of(
            "Databases",    new ResourceDto("Use The Index, Luke!", "https://use-the-index-luke.com/"),
            "Optimization", new ResourceDto("Use The Index, Luke!", "https://use-the-index-luke.com/"),
            "Algorithms",   new ResourceDto("Big-O Cheat Sheet", "https://www.bigocheatsheet.com/"),
            "Frontend",     new ResourceDto("MDN Web Docs", "https://developer.mozilla.org/"),
            "DevOps",       new ResourceDto("The Twelve-Factor App", "https://12factor.net/"),
            "Networking",   new ResourceDto("High Performance Browser Networking", "https://hpbn.co/"),
            "Basics",       new ResourceDto("MDN Learn", "https://developer.mozilla.org/en-US/docs/Learn"));

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;
    private final QuestionTemplateRepository questions;
    private final CandidateRepository candidates;

    public RuleBasedAiAnalyzer(InterviewRepository interviews, InterviewRoundRepository rounds,
                               QuestionTemplateRepository questions, CandidateRepository candidates) {
        this.interviews = interviews;
        this.rounds = rounds;
        this.questions = questions;
        this.candidates = candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public AiInterviewResultDto analyze(long interviewId) {
        Interview interview = interviews.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
        List<InterviewRound> ordered = rounds.findByInterviewIdOrderByRoundIndexAsc(interviewId);

        int total = interview.getTotalQuestions();
        int correct = (int) ordered.stream().filter(InterviewRound::isCorrect).count();
        int best = bestStreak(ordered);

        // Wrong answers grouped by topic → roadmap
        Map<String, Integer> missesByTopic = new LinkedHashMap<>();
        for (InterviewRound r : ordered) {
            if (r.isAnswered() && !r.isCorrect()) {
                String topic = questions.findById(r.getQuestionId()).orElseThrow().getTopic();
                missesByTopic.merge(topic, 1, Integer::sum);
            }
        }
        List<RoadmapItemDto> roadmap = new ArrayList<>();
        missesByTopic.forEach((topic, misses) -> {
            String priority = misses >= 2 ? "HIGH" : "MEDIUM";
            String reason = "You missed " + misses + " question(s) on " + topic + ".";
            ResourceDto res = RESOURCES.get(topic);
            roadmap.add(new RoadmapItemDto(topic, reason, priority,
                    res == null ? List.of() : List.of(res)));
        });

        String summary = correct + "/" + total + " correct, best streak " + best + ". "
                + (roadmap.isEmpty() ? "Strong all-round performance."
                                     : "Focus areas: " + String.join(", ", missesByTopic.keySet()) + ".");

        String verdict = null;
        Long hiredId = interview.getHiredCandidateId();
        if (hiredId != null) {
            Candidate hired = candidates.findById(hiredId).orElse(null);
            if (hired != null) {
                boolean coversGap = hired.getStrengths().stream().anyMatch(missesByTopic::containsKey);
                verdict = coversGap
                        ? "Good hire — " + hired.getName() + " is strong where you struggled."
                        : "Reasonable hire — " + hired.getName() + " complements your own strengths.";
            }
        }
        return new AiInterviewResultDto(interviewId, "READY", summary, verdict, hiredId, roadmap);
    }

    private int bestStreak(List<InterviewRound> ordered) {
        int best = 0, run = 0;
        for (InterviewRound r : ordered) {
            if (!r.isAnswered()) continue;
            run = r.isCorrect() ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return best;
    }
}
```

- [ ] **Step 6: Add the controller endpoint** (inject `AiAnalyzer`; always 200)

```java
// field + constructor param: private final com.techleadsim.ai.AiAnalyzer aiAnalyzer;

@GetMapping("/{interviewId}/ai-result")
public AiInterviewResultDto getAiInterviewResult(@PathVariable long interviewId) {
    return aiAnalyzer.analyze(interviewId);
}
```

- [ ] **Step 7: Write the endpoint test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetAiResultTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void alwaysReturnsReady() throws Exception {
        MvcResult started = mvc.perform(post("/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\"}")).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString())
                .read("$.interviewId", Integer.class);
        mvc.perform(get("/interviews/{id}/ai-result", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.summary").exists());
    }
}
```

- [ ] **Step 8: Run both tests — expect PASS**

Run: `./mvnw test -Dtest=RuleBasedAiAnalyzerTest,GetAiResultTest`

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/ai backend/src/test/java/com/techleadsim/web/GetAiResultTest.java
git commit -m "feat: implement GET ai-result with rule-based analyzer"
```

---

### Task 14: GET /home (getHomePage)

**Files:**
- Create: `web/dto/GameModeDto.java`, `web/dto/PlayerStatsDto.java`, `web/dto/HomePageDto.java`
- Create: `service/PlayerStatsService.java`
- Create: `web/HomeController.java`
- Modify: `repository/InterviewRepository.java`, `repository/InterviewRoundRepository.java` (aggregate queries)
- Test: `src/test/java/com/techleadsim/web/GetHomeTest.java`

**Interfaces:**
- Consumes: `InterviewRepository`, `InterviewRoundRepository`.
- Produces: `PlayerStatsService.aggregate(): PlayerStatsDto` (global over interviews with status ≠ IN_PROGRESS). `GameModeDto(Mode mode, String title, String description, int questionCount)`; `PlayerStatsDto(int gamesPlayed, float winRate, int bestResult, int candidatesHired)`; `HomePageDto(String title, String subtitle, List<GameModeDto> modes, PlayerStatsDto playerStats)`. Win = correctCount ≥ 60% of totalQuestions.

- [ ] **Step 1: Write the failing test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GetHomeTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void returnsTitleModesAndStats() throws Exception {
        mvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tech Lead Simulator"))
                .andExpect(jsonPath("$.modes.length()").value(2))
                .andExpect(jsonPath("$.modes[0].questionCount").isNumber())
                .andExpect(jsonPath("$.playerStats.gamesPlayed").isNumber())
                .andExpect(jsonPath("$.playerStats.winRate").isNumber());
    }
}
```

- [ ] **Step 2: Run it — expect FAIL**

Run: `./mvnw test -Dtest=GetHomeTest`

- [ ] **Step 3: Add aggregate queries to the repositories**

```java
// InterviewRepository — add:
// imports: org.springframework.data.jpa.repository.Query;
@org.springframework.data.jpa.repository.Query(
        "select i.id from Interview i where i.status <> com.techleadsim.domain.InterviewStatus.IN_PROGRESS")
java.util.List<Long> findCompletedIds();

long countByHiredCandidateIdIsNotNull();
```
```java
// InterviewRoundRepository — add:
long countByInterviewIdAndCorrectIsTrue(Long interviewId);
```

- [ ] **Step 4: Write `PlayerStatsService`**

```java
package com.techleadsim.service;

import com.techleadsim.domain.Interview;
import com.techleadsim.repository.InterviewRepository;
import com.techleadsim.repository.InterviewRoundRepository;
import com.techleadsim.web.dto.PlayerStatsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerStatsService {

    private static final double WIN_THRESHOLD = 0.60;

    private final InterviewRepository interviews;
    private final InterviewRoundRepository rounds;

    public PlayerStatsService(InterviewRepository interviews, InterviewRoundRepository rounds) {
        this.interviews = interviews;
        this.rounds = rounds;
    }

    @Transactional(readOnly = true)
    public PlayerStatsDto aggregate() {
        List<Long> completedIds = interviews.findCompletedIds();
        int gamesPlayed = completedIds.size();
        int wins = 0, bestResult = 0;
        for (Long id : completedIds) {
            Interview i = interviews.findById(id).orElseThrow();
            int correct = (int) rounds.countByInterviewIdAndCorrectIsTrue(id);
            bestResult = Math.max(bestResult, correct);
            if (i.getTotalQuestions() > 0
                    && (double) correct / i.getTotalQuestions() >= WIN_THRESHOLD) {
                wins++;
            }
        }
        float winRate = gamesPlayed == 0 ? 0f : (float) wins / gamesPlayed;
        int hired = (int) interviews.countByHiredCandidateIdIsNotNull();
        return new PlayerStatsDto(gamesPlayed, winRate, bestResult, hired);
    }
}
```

- [ ] **Step 5: Write the DTOs**

```java
package com.techleadsim.web.dto;
import com.techleadsim.domain.Mode;

public record GameModeDto(Mode mode, String title, String description, int questionCount) {}
```
```java
package com.techleadsim.web.dto;
public record PlayerStatsDto(int gamesPlayed, float winRate, int bestResult, int candidatesHired) {}
```
```java
package com.techleadsim.web.dto;
import java.util.List;

public record HomePageDto(String title, String subtitle, List<GameModeDto> modes, PlayerStatsDto playerStats) {}
```

- [ ] **Step 6: Write `HomeController`**

```java
package com.techleadsim.web;

import com.techleadsim.domain.Mode;
import com.techleadsim.service.PlayerStatsService;
import com.techleadsim.web.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home")
public class HomeController {

    private final PlayerStatsService playerStats;

    public HomeController(PlayerStatsService playerStats) {
        this.playerStats = playerStats;
    }

    @GetMapping
    public HomePageDto getHomePage() {
        List<GameModeDto> modes = List.of(
                new GameModeDto(Mode.CLASSIC, "Classic", "Full path: 10 questions.", Mode.CLASSIC.questionCount()),
                new GameModeDto(Mode.HARDCORE, "Hardcore", "Extended path: 20 questions.", Mode.HARDCORE.questionCount()));
        return new HomePageDto("Tech Lead Simulator",
                "Pass the interview and build the best team!", modes, playerStats.aggregate());
    }
}
```

- [ ] **Step 7: Run — expect PASS**

Run: `./mvnw test -Dtest=GetHomeTest`

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/techleadsim backend/src/test/java/com/techleadsim/web/GetHomeTest.java
git commit -m "feat: implement GET home with global player stats"
```

---

### Task 15: CORS + full end-to-end playthrough

**Files:**
- Create: `config/WebCorsConfig.java`
- Test: `src/test/java/com/techleadsim/web/FullPlaythroughTest.java`

**Interfaces:**
- Consumes: every endpoint built in Tasks 7–14.
- Produces: CORS allowing the frontend origin `http://localhost:5173` for `/**`.

- [ ] **Step 1: Write the failing end-to-end test**

```java
package com.techleadsim.web;

import com.techleadsim.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FullPlaythroughTest extends AbstractPostgresIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void playsAFullGameEndToEnd() throws Exception {
        // start
        MvcResult started = mvc.perform(post("/interviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"CLASSIC\",\"difficulty\":\"MEDIUM\",\"playerName\":\"You\"}"))
                .andExpect(status().isCreated()).andReturn();
        long id = JsonPath.parse(started.getResponse().getContentAsString()).read("$.interviewId", Integer.class);

        // 10 rounds — always pick the correct answer so the run is a "win"
        boolean finished = false;
        for (int i = 0; i < 10; i++) {
            MvcResult q = mvc.perform(get("/interviews/{id}/question", id))
                    .andExpect(status().isOk()).andReturn();
            String json = q.getResponse().getContentAsString();
            int questionId = JsonPath.parse(json).read("$.questionId");
            // discover the correct answer by submitting the first option, then read correctAnswerId
            int firstAnswer = JsonPath.parse(json).read("$.answers[0].answerId");
            MvcResult a = mvc.perform(post("/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"questionId\":" + questionId + ",\"answerId\":" + firstAnswer + "}"))
                    .andExpect(status().isOk()).andReturn();
            finished = JsonPath.parse(a.getResponse().getContentAsString()).read("$.finished");
        }
        // 11th question request → 409
        mvc.perform(get("/interviews/{id}/question", id)).andExpect(status().isConflict());

        // statistic, result BEFORE offer
        mvc.perform(get("/interviews/{id}/statistic", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.perCandidate[0].correctAnswers").isNumber());
        mvc.perform(get("/interviews/{id}/result", id)).andExpect(status().isOk());

        // offer, then ai-result
        mvc.perform(post("/interviews/{id}/offer", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":1}")).andExpect(status().isOk());
        mvc.perform(get("/interviews/{id}/ai-result", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        // home reflects the completed game
        mvc.perform(get("/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.playerStats.gamesPlayed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
```

- [ ] **Step 2: Run it — expect FAIL** if CORS bean missing causes context issues, otherwise it may already pass functionally. Run and confirm:

Run: `./mvnw test -Dtest=FullPlaythroughTest`

- [ ] **Step 3: Write `WebCorsConfig`**

```java
package com.techleadsim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

- [ ] **Step 4: Run the full suite — expect PASS**

Run: `./mvnw test`
Expected: all tests green.

- [ ] **Step 5: Manual smoke (optional but recommended)**

```bash
docker compose -f backend/compose.yaml up -d
cd backend && ./mvnw spring-boot:run
# in another shell:
curl -s http://localhost:8080/api/home | head
```
Expected: JSON home payload. Then `docker compose -f backend/compose.yaml down`.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/techleadsim/config/WebCorsConfig.java backend/src/test/java/com/techleadsim/web/FullPlaythroughTest.java
git commit -m "feat: add CORS config and full end-to-end playthrough test"
```

---

## Self-review notes

- **Spec coverage:** all 8 `operationId`s implemented (Tasks 7–14) with their contract status codes (201/200/400/404/409); schema/seed (2), domain (3), scoring (4), errors (5), provider (6). `correctAnswers` (v0.2.0 contract addition) covered in Task 10; `/result` not gated on offer verified in Task 12; AI always `READY` in Task 13; global stats in Task 14; `/api` base path set in Task 1; CORS in Task 15.
- **Type consistency:** service record names (`AnswerResult`, `QuestionView`) and DTO records are referenced with matching signatures across producing/consuming tasks; `Mode.questionCount()` introduced in Task 7 and reused in Task 14.
- **Known follow-ups (out of MVP scope):** `EASY`/`HARD` seed content (only `MEDIUM` seeded, matching the FE); `avatarUrl` files must be added to the frontend static assets; `InterviewStatus.FINISHED` is reserved but unused.

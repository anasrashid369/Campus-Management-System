# Campus Management System — Requirement Checklist

Traceability against Assignment 1 PDF, class diagrams (Figures 1–8), and use case diagrams (Admin, Instructors, Student/TA).

## Academic Office Admin (U3)

| ID | Requirement | Implementation | Tests |
|----|-------------|----------------|-------|
| A1 | Create course | `AcademicOfficeAdmin.createCourse`, `CampusCli.createCourse` | `AnasCoreSmokeTest`, CLI restart |
| A2 | Update course | `AcademicOfficeAdmin.updateCourse`, `CampusCli.updateCourse` | `AnasCoreSmokeTest` |
| A3 | Search course | `AcademicOfficeAdmin.searchCourse`, `CampusCli.searchCourse` | `AnasCoreSmokeTest` |
| A4 | Create section | `createSection`, CLI | Smoke + persistence |
| A5 | Update section | `updateSection`, CLI capacity/schedule | Smoke |
| A6 | Set section capacity | `setCapacity`, CLI | Smoke |
| A7 | Assign room / schedule | `assignRoom`, `Schedule`, CLI | Smoke |
| A8 | Assign instructor | `assignInstructor`, CLI | TA workflow smoke |
| A9 | View pending requests | `viewRequests`, priority sort | Smoke |
| A10 | Approve request | `approveRequest`, CLI | Student/admin workflow |
| A11 | Reject request | `rejectRequest`, CLI | Request workflow tests |
| A12 | Course prerequisites | `Course.addPrerequisite`, persistence `PREREQUISITE` | Persistence + CLI |

## Normal Student (U1)

| ID | Requirement | Implementation | Tests |
|----|-------------|----------------|-------|
| S1 | View available courses / credits | CLI 5–6 | Smoke |
| S2 | Register / drop | `Student.register/drop`, validation | Smoke + enrollment tests |
| S3 | Timetable / registered courses | `viewTimetable`, CLI | Smoke |
| S4 | Attendance view / percentage | Instructor calc + CLI | Smoke |
| S5 | Assignments / submit | `submitAssignment`, CLI | TA workflow |
| S6 | Course clash request | `CourseClashRequest`, CLI | Smoke |
| S7 | Generic academic request | `GenericRequest`, CLI | Generic request tests |
| S8 | View own requests | CLI + `RequestDateComparator` | Smoke |

## Instructors (U2)

| ID | Requirement | Implementation | Tests |
|----|-------------|----------------|-------|
| I1 | View courses/sections/students | `Instructor` methods, CLI | Smoke |
| I2 | Mark/update attendance | `markAttendance`, `updateAttendance` | Smoke |
| I3 | Attendance percentage | PRESENT/LATE count as attended | Smoke |
| I4 | Permanent: assign TA | `PermanentInstructor.assignTA` | Smoke |
| I5 | Permanent: FYP lifecycle | FYP classes + CLI | FYP smoke |
| I6 | Visiting: no TA/FYP | CLI menu gating | Smoke |

## Teaching Assistant (U1)

| ID | Requirement | Implementation | Tests |
|----|-------------|----------------|-------|
| T1 | Create/set marks/deadline | `TeachingAssistant`, CLI | Smoke |
| T2 | View/late submissions | CLI | Smoke |
| T3 | Evaluate / marks / feedback | `evaluateSubmission`, `giveFeedback` | Smoke |
| T4 | Permission boundaries | `UnauthorizedActionException` | Exception tests |

## Persistence & logging (Assignment §8)

| ID | Requirement | Implementation | Tests |
|----|-------------|----------------|-------|
| P1 | File storage under `data/` | `CampusPersistence` | Smoke |
| P2 | Load on startup | `CampusCli.run` | CLI restart |
| P3 | Malformed file handling | IOException with line numbers | Smoke |
| P4 | Logs under `logs/` | `ApplicationLogger` | Smoke |

## Class diagram & comparators (Fig 7)

All enums, five comparators, exception hierarchy classes present under `src/`.

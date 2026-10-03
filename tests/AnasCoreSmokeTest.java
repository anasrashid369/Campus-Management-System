import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AnasCoreSmokeTest {
    /** CLI runs persist here so tests never touch the real data/catalog.txt. */
    private static final Path CLI_CATALOG = Path.of(System.getProperty("java.io.tmpdir"), "cms-smoke-cli-catalog.txt");

    public static void main(String[] args) throws Exception {
        Files.deleteIfExists(CLI_CATALOG);
        testCourseCreateSearchAndUpdate();
        testComparators();
        testSectionAndScheduleRules();
        testCatalogPersistence();
        testCliRestartWorkflow();
        testStudentRequestAdminApprovalWorkflow();
        testTeachingAssistantAssignmentWorkflow();
        testFypCliWorkflow();
        testApplicationLogging();
        Files.deleteIfExists(CLI_CATALOG);
        System.out.println("AnasCoreSmokeTest: PASS");
    }

    private static void testCourseCreateSearchAndUpdate() {
        AcademicOfficeAdmin admin = new AcademicOfficeAdmin("Test Admin", "", "", "A-1");
        Course course = new Course("CS101", "Old Title", 3);
        admin.createCourse(course);
        check(admin.searchCourse("cs101") == course, "course lookup should ignore code case");

        admin.createCourse(new Course("CS101", "Duplicate", 5));
        check(admin.searchCourse("CS101").getTitle().equals("Old Title"),
                "duplicate course creation should not replace an existing course");

        Section section = new Section("SEC-A", 30, course);
        admin.createSection(section);
        admin.updateCourse(new Course("cs101", "Updated Title", 4));
        check(admin.searchCourse("CS101") == course, "course update should preserve object identity");
        check(section.getCourse() == course, "course update should preserve section relationship");
        check(course.getSections().contains(section), "course should retain its section");
        check(course.getTitle().equals("Updated Title") && course.getCreditHours() == 4,
                "course details should be updated");
    }

        private static void testComparators() {
        StudentNameComparator studentComparator = new StudentNameComparator();
        Student amy = new NormalStudent("Amy", "amy@example.test", "", "S1", 0);
        Student zoe = new NormalStudent("Zoe", "zoe@example.test", "", "S2", 0);
        check(studentComparator.compare(amy, zoe) < 0, "student names should sort ascending");
        check(studentComparator.compare(amy, amy) == 0, "equal student names should compare equally");

        RequestPriorityComparator priorityComparator = new RequestPriorityComparator();
        Request olderHighPriority = new GenericRequest("R1", LocalDate.of(2026, 1, 1), "", 5,
            amy, RequestCategory.OTHER);
        Request newerHighPriority = new GenericRequest("R2", LocalDate.of(2026, 2, 1), "", 5,
            amy, RequestCategory.OTHER);
        Request lowerPriority = new GenericRequest("R3", LocalDate.of(2025, 1, 1), "", 2,
            amy, RequestCategory.OTHER);
        check(priorityComparator.compare(olderHighPriority, lowerPriority) < 0,
            "higher request priority should sort first");
        check(priorityComparator.compare(olderHighPriority, newerHighPriority) < 0,
            "equal priority should use ascending request date");
        check(priorityComparator.compare(olderHighPriority, olderHighPriority) == 0,
            "equal request priority/date keys should compare equally");

        RequestDateComparator requestDateComparator = new RequestDateComparator();
        check(requestDateComparator.compare(olderHighPriority, newerHighPriority) < 0,
            "request dates should sort ascending");

        Course course = new Course("CMP101", "Comparators", 3);
        Section section = new Section("CMP-A", 10, course);
        TeachingAssistant assistant = new TeachingAssistant("TA", "ta@example.test", "", "TA1", 0);
        Assignment firstAssignment = new Assignment("A1", "A1", "", LocalDate.of(2026, 3, 1),
            10, section, assistant);
        Assignment laterAssignment = new Assignment("A2", "A2", "", LocalDate.of(2026, 4, 1),
            10, section, assistant);
        AssignmentDeadlineComparator assignmentComparator = new AssignmentDeadlineComparator();
        check(assignmentComparator.compare(firstAssignment, laterAssignment) < 0,
            "assignment deadlines should sort ascending");
        check(assignmentComparator.compare(firstAssignment, firstAssignment) == 0,
            "equal assignment deadlines should compare equally");

        FYPMeeting firstMeeting = new FYPMeeting("M1", LocalDate.of(2026, 5, 1), "");
        FYPMeeting laterMeeting = new FYPMeeting("M2", LocalDate.of(2026, 6, 1), "");
        FYPMeetingDateComparator meetingComparator = new FYPMeetingDateComparator();
        check(meetingComparator.compare(firstMeeting, laterMeeting) < 0,
            "FYP meeting dates should sort ascending");
        check(meetingComparator.compare(firstMeeting, firstMeeting) == 0,
            "equal FYP meeting dates should compare equally");
        }

    private static void testSectionAndScheduleRules() {
        Course course = new Course("EE201", "Circuits", 3);
        Section section = new Section("EE-A", 20, course);
        section.setCapacity(18);
        check(section.getCapacity() == 18, "section capacity should update");

        Schedule first = new Schedule(Day.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "R1");
        Schedule overlapping = new Schedule(Day.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "R2");
        Schedule adjacent = new Schedule(Day.MONDAY, LocalTime.of(10, 30), LocalTime.of(11, 30), "R3");
        Schedule otherDay = new Schedule(Day.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "R4");
        check(first.hasClash(overlapping), "overlapping same-day schedules should clash");
        check(!first.hasClash(adjacent), "adjacent schedules should not clash");
        check(!first.hasClash(otherDay), "different-day schedules should not clash");
    }

    private static void testApplicationLogging() throws Exception {
        ApplicationLogger.info("test.smoke_event");
        ApplicationLogger.error("test.smoke_error", new IllegalStateException("expected"));
        Path logFile = Path.of("logs", "app.log");
        check(Files.isRegularFile(logFile), "logger should create logs/app.log");
        String log = Files.readString(logFile);
        check(log.contains("test.smoke_event"), "logger should append info events");
        check(log.contains("test.smoke_error | IllegalStateException: expected"),
                "logger should append error details");
    }

    private static void testCatalogPersistence() throws Exception {
        Path catalog = Path.of("catalog-test.txt");
        AcademicOfficeAdmin source = new AcademicOfficeAdmin("Source", "", "", "A-2");
        Course prerequisite = new Course("MATH101", "Math", 3);
        source.createCourse(prerequisite);
        Course course = new Course("CS202", "Data\tStructures\nLab\\", 4);
        course.addPrerequisite(prerequisite);
        source.createCourse(course);
        Section prerequisiteSection = new Section("MATH101-A", 35, prerequisite);
        source.createSection(prerequisiteSection);
        Section section = new Section("CS202-A", 35, course);
        source.createSection(section);
        section.setSchedule(new Schedule(Day.FRIDAY, LocalTime.of(13, 0),
            LocalTime.of(14, 30), "Room 2\tEast"));
        Section requestedSection = new Section("CS202-B", 35, course);
        source.createSection(requestedSection);
        requestedSection.setSchedule(new Schedule(Day.FRIDAY, LocalTime.of(14, 0),
            LocalTime.of(15, 0), "Room 3"));
        NormalStudent student = new NormalStudent("Test Student", "student@example.test",
            "555-0300", "ST-202", 0);
        student.register(prerequisiteSection);
        student.register(section);
        PermanentInstructor permanentInstructor = new PermanentInstructor(
            "Permanent Teacher", "permanent@example.test", "555-0400", "T-P");
        VisitingInstructor visitingInstructor = new VisitingInstructor(
            "Visiting Teacher", "visiting@example.test", "555-0401", "T-V");
        source.assignInstructor(section, permanentInstructor);
        source.assignInstructor(requestedSection, visitingInstructor);
        permanentInstructor.assignTA(student, requestedSection);
        TeachingAssistant teachingAssistant = requestedSection.getTeachingAssistant();
        section.addAttendance(new Attendance(student, section, LocalDate.of(2026, 10, 3),
            AttendanceStatus.LATE));
        CourseClashRequest clashRequest = new CourseClashRequest("REQ-1", LocalDate.of(2026, 10, 3),
            "Schedule conflict", 2, student, section, requestedSection);
        clashRequest.submit();
        source.addRequest(clashRequest);
        source.approveRequest(clashRequest);
        GenericRequest genericRequest = new GenericRequest("REQ-2", LocalDate.of(2026, 10, 3),
            "Classmate concern", 1, student, RequestCategory.CLASSMATE);
        genericRequest.submit();
        source.addRequest(genericRequest);
        FYPGroup fypGroup = new FYPGroup("FYP-1", "Campus App", "Student project");
        fypGroup.addMember(student);
        fypGroup.assignSupervisor(permanentInstructor);
        FYPMeeting meeting = new FYPMeeting("MEET-1", LocalDate.of(2026, 10, 5), "Progress review");
        meeting.updateNotes("Reviewed prototype");
        fypGroup.addMeeting(meeting);
        FYPEvaluation evaluation = new FYPEvaluation("EVAL-1", LocalDate.of(2026, 10, 6),
            permanentInstructor);
        evaluation.evaluate(92);
        evaluation.addFeedback("Strong progress");
        fypGroup.addEvaluation(evaluation);

        CampusPersistence persistence = new CampusPersistence(catalog);
        List<Instructor> instructors = List.of(permanentInstructor, visitingInstructor);
        persistence.saveCatalog(source, List.of(student, teachingAssistant), instructors);
        AcademicOfficeAdmin restored = new AcademicOfficeAdmin("Restored", "", "", "A-3");
        List<Student> restoredStudents = new java.util.ArrayList<>();
        List<Instructor> restoredInstructors = new java.util.ArrayList<>();
        var restoredSections = persistence.loadCatalog(restored, restoredStudents, restoredInstructors);
        check(restoredSections.size() == 3, "sections should restore");
        Course restoredCourse = restored.searchCourse("cs202");
        check(restoredCourse != null, "course should restore case-insensitively");
        check(restoredCourse.getPrerequisites().contains(restored.searchCourse("MATH101")),
            "prerequisite should reference the restored prerequisite course");
        check(restoredCourse.getTitle().equals(course.getTitle()), "escaped course title should round-trip");
        check(restoredSections.get(1).getCourse() == restoredCourse,
            "restored section should reference the restored course instance");
        check(restoredSections.get(1).getSchedule().getRoom().equals("Room 2\tEast"),
            "escaped room name should round-trip");
        check(restoredStudents.size() == 2
                && restoredStudents.get(0) instanceof NormalStudent
                && restoredSections.get(0).getEnrolledStudents().contains(restoredStudents.get(0)),
            "student enrollment relationship should restore");
        check(restoredStudents.get(0).calculateTotalCreditHours() == 7,
            "registered credit-hour total should restore");
        check(restoredStudents.get(1) instanceof TeachingAssistant
            && ((TeachingAssistant) restoredStudents.get(1)).getAssignedSection() == restoredSections.get(2)
            && restoredSections.get(2).getTeachingAssistant() == restoredStudents.get(1),
            "TA role profile and assigned section should restore");
        check(restoredSections.get(1).getAttendanceRecords().size() == 1
            && restoredSections.get(1).getAttendanceRecords().get(0).getStatus()
                == AttendanceStatus.LATE
            && restoredSections.get(1).getAttendanceRecords().get(0).getStudent()
                == restoredStudents.get(0),
            "attendance status and student relationship should restore");
        check(restoredInstructors.size() == 2
            && restoredSections.get(1).getInstructor() == restoredInstructors.get(0)
            && restoredSections.get(2).getInstructor() == restoredInstructors.get(1),
            "permanent and visiting instructor assignments should restore");
        PermanentInstructor restoredPermanent = (PermanentInstructor) restoredInstructors.get(0);
        check(restoredPermanent.viewFYPGroups().size() == 1,
            "supervised FYP group should restore");
        FYPGroup restoredGroup = restoredPermanent.viewFYPGroups().get(0);
        check(restoredGroup.getMembers().size() == 1
                && restoredGroup.getMembers().get(0) == restoredStudents.get(0),
            "FYP member should resolve to the restored student");
        check(restoredGroup.getMeetings().size() == 1
                && restoredGroup.getMeetings().get(0).getNotes().equals("Reviewed prototype"),
            "FYP meeting date, agenda, and notes should restore");
        check(restoredGroup.getEvaluations().size() == 1
                && restoredGroup.getEvaluations().get(0).getScore() == 92
                && restoredGroup.getEvaluations().get(0).getFeedback().equals("Strong progress")
                && restoredGroup.getEvaluations().get(0).getEvaluator() == restoredPermanent,
            "FYP evaluation, feedback, and evaluator should restore");
        check(restored.viewRequests().size() == 2, "requests should restore");
        Request restoredClash = restored.viewRequests().get(0);
        check(restoredClash instanceof CourseClashRequest
                && restoredClash.getStatus() == RequestStatus.APPROVED
                && restoredClash.getProcessedBy() == restored,
            "processed clash request status and processor should restore");
        CourseClashRequest restoredClashRequest = (CourseClashRequest) restoredClash;
        check(restoredClashRequest.getConflictingSection() == restoredSections.get(1)
            && restoredClashRequest.getRequestedSection() == restoredSections.get(2),
            "clash request should reference restored sections");
        check(restored.viewRequests().get(1) instanceof GenericRequest
                && restored.viewRequests().get(1).getStatus() == RequestStatus.PENDING,
            "pending generic request should restore");

        Path malformedCatalog = Path.of("malformed-catalog-test.txt");
        Files.writeString(malformedCatalog,
            "COURSE\tOK1\tValid\t3\nCOURSE\tBROKEN\n");
        AcademicOfficeAdmin emptyAdmin = new AcademicOfficeAdmin("Empty", "", "", "A-4");
        try {
            new CampusPersistence(malformedCatalog).loadCatalog(emptyAdmin);
            throw new AssertionError("malformed catalog should fail to load");
        } catch (java.io.IOException expected) {
            check(expected.getMessage().contains("line 2"), "error should identify the malformed line");
        }
        check(emptyAdmin.viewCourses().isEmpty(), "malformed catalog should not partially load records");

        Path malformedSchedule = Path.of("malformed-schedule-test.txt");
        Files.writeString(malformedSchedule,
                "COURSE\tOK2\tValid\t3\nSECTION\tSEC2\tOK2\t10\tMONDAY\tbad\t11:00\tR2\n");
        AcademicOfficeAdmin scheduleAdmin = new AcademicOfficeAdmin("Schedule", "", "", "A-5");
        try {
            new CampusPersistence(malformedSchedule).loadCatalog(scheduleAdmin);
            throw new AssertionError("malformed schedule should fail to load");
        } catch (java.io.IOException expected) {
            check(expected.getMessage().contains("line 2"), "schedule error should identify its line");
        }
        check(scheduleAdmin.viewCourses().isEmpty(), "bad schedule should not partially load courses");
        }

        private static void testCliRestartWorkflow() {
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
        CampusCli firstRun = new CampusCli(new BufferedReader(new StringReader(
            "1\n1\nCLI101\nCLI Course\n3\n\n4\nCLI-A\nCLI101\n20\n0\n0\n")),
            new PrintStream(firstOutput), new CampusPersistence(CLI_CATALOG));
        firstRun.run();
        check(firstOutput.toString().contains("Section CLI-A created for course CLI101."),
            "first CLI run should create a course and section");

        ByteArrayOutputStream secondOutput = new ByteArrayOutputStream();
        CampusCli secondRun = new CampusCli(new BufferedReader(new StringReader(
            "1\n3\ncli101\n6\nCLI-A\n25\n0\n0\n")),
            new PrintStream(secondOutput), new CampusPersistence(CLI_CATALOG));
        secondRun.run();
        String result = secondOutput.toString();
        check(result.contains("Course found: CLI101 | CLI Course | 3 credit hour(s)"),
            "second CLI run should find the persisted course");
        check(result.contains("Capacity for section CLI-A set to 25."),
            "second CLI run should update the restored section");
        }

        private static void testStudentRequestAdminApprovalWorkflow() {
        String adminSetup = runCli("1\n1\nCR101\nClash Course\n3\n\n4\nS-A\nCR101\n30\n"
            + "7\nS-A\nMonday\n09:00\n10:00\nR101\n4\nS-B\nCR101\n30\n"
            + "7\nS-B\nMonday\n09:30\n10:30\nR102\n0\n0\n");
        check(adminSetup.contains("Section S-B created for course CR101."),
            "admin should create sections before student registration");

        String studentActions = runCli("5\nST1\ny\nStudent One\nstudent@example.test\n555-0101\n"
            + "7\nCR101\n1\n2\n1\nS-B\nNeed a non-clashing section\n2\n0\n0\n");
        check(studentActions.contains("Registered in section S-A (3 total credit hour(s))."),
            "student should register in the first section");
        check(studentActions.contains("submitted."), "student should submit a course clash request");

        String adminDecision = runCli("1\n9\n10\n1\n0\n0\n");
        check(adminDecision.contains("approved."), "admin should approve the submitted request");

        String studentReload = runCli("5\nST1\n4\n0\n0\n");
        check(studentReload.contains("Status: APPROVED"),
            "student should see the approved request after another reload");
        }

        private static String runCli(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CampusCli cli = new CampusCli(new BufferedReader(new StringReader(input)),
            new PrintStream(output), new CampusPersistence(CLI_CATALOG));
        cli.run();
        return output.toString();
        }

        private static void testTeachingAssistantAssignmentWorkflow() throws Exception {
        String setup = runCli("1\n1\nCS301\nSystems\n3\n\n4\nSEC1\nCS301\n20\n"
            + "8\nSEC1\nT1\n1\nTeacher One\nteacher@example.test\n555-0100\n"
            + "4\nSEC2\nCS301\n20\n8\nSEC2\nV1\n2\nTeacher Visitor\n"
            + "visitor@example.test\n555-0102\n0\n0\n");
        check(setup.contains("Permanent Instructor assigned to section SEC1."),
            "admin should assign the permanent instructor");
        check(setup.contains("Visiting Instructor assigned to section SEC2."),
            "admin should assign the visiting instructor; output was: " + setup);

        String studentRegistration = runCli("5\nST1\ny\nStudent One\nstudent@example.test\n555-0101\n"
            + "7\nCS301\n1\n0\n0\n");
        check(studentRegistration.contains("Registered in section SEC1"),
            "student should register before the assignment workflow");

        String attendance = runCli("2\nT1\n4\n1\n1\n1\n8\n1\n1\n2\n9\n1\n1\n0\n0\n");
        check(attendance.contains("Attendance marked PRESENT for ST1 in section SEC1."),
            "Permanent Instructor should mark attendance");
        check(attendance.contains("Attendance updated to ABSENT."),
            "Permanent Instructor should update attendance");
        check(attendance.contains("Attendance percentage: 0.00%"),
            "attendance percentage should use the updated status");

        String visitingMenu = runCli("3\nV1\n10\n0\n0\n");
        check(visitingMenu.contains("Visiting Instructor Menu"),
            "Visiting Instructor should receive an instructor menu; output was: " + visitingMenu);
        check(!visitingMenu.contains("Assign Teaching Assistant")
                && !visitingMenu.contains("View FYP Groups"),
            "Visiting Instructor menu must not expose Permanent-only TA/FYP actions");

        String taAssignment = runCli("2\nT1\n10\n1\n1\n0\n0\n");
        check(taAssignment.contains("assigned as TA to section SEC1"),
            "permanent instructor should assign the student as a TA");

        String assignmentCreation = runCli("4\nST1\n2\nLab 1\nInitial lab\n2027-01-01\n10\n0\n0\n");
        check(assignmentCreation.contains("Assignment CS301-Lab 1 created"),
            "TA should create an assignment");

        String submission = runCli("5\nST1\n13\n1\nCompleted lab\n0\n0\n");
        check(submission.contains("submitted with status SUBMITTED"),
            "student should submit the assignment");

        String grading = runCli("4\nST1\n7\n1\n1\n8\nGood work\n0\n0\n");
        check(grading.contains("evaluated with 8.00 marks and feedback"),
            "TA should evaluate and provide feedback");

        AcademicOfficeAdmin restoredAdmin = new AcademicOfficeAdmin("Reload", "", "", "RELOAD");
        List<Student> restoredStudents = new java.util.ArrayList<>();
        List<Instructor> restoredInstructors = new java.util.ArrayList<>();
        new CampusPersistence(CLI_CATALOG).loadCatalog(
            restoredAdmin, restoredStudents, restoredInstructors);
        TeachingAssistant restoredAssistant = null;
        for (Student restoredStudent : restoredStudents) {
            if (restoredStudent instanceof TeachingAssistant assistant) {
            restoredAssistant = assistant;
            break;
            }
        }
        check(restoredAssistant != null, "TA profile should restore");
        check(restoredAssistant.getCreatedAssignments().size() == 1,
            "TA assignment should restore");
        Submission restoredSubmission = restoredAssistant.getCreatedAssignments().get(0)
            .getSubmissions().get(0);
        check(restoredSubmission.getStatus() == SubmissionStatus.EVALUATED
                && restoredSubmission.getMarks() == 8.0,
            "evaluated submission state should restore");
        check(restoredSubmission.getFeedback() != null
                && restoredSubmission.getFeedback().getComments().equals("Good work")
                && restoredSubmission.getFeedback().getEvaluator() == restoredAssistant,
            "feedback text and TA evaluator relationship should restore");
        }

            private static void testFypCliWorkflow() throws Exception {
            String fypActions = runCli("2\nT1\n17\nFYP-G1\nCampus App\nStudent project\ny\n1\nn\n"
                + "14\n1\nMEET-1\n2027-01-02\nProgress review\n"
                + "15\n1\nEVAL-1\n2027-01-03\n91\nPromising idea\n"
                + "16\n1\n1\nStrong work\n"
                + "18\n1\n1\nAgreed on project scope\n"
                + "11\n12\n1\n13\n1\n0\n0\n");
            check(fypActions.contains("FYP group FYP-G1 created and supervised by T1."),
                "Permanent Instructor should create and supervise an FYP group");
            check(fypActions.contains("Meeting MEET-1 scheduled for group FYP-G1."),
                "Permanent Instructor should schedule FYP meetings");
            check(fypActions.contains("FYP idea evaluated with score 91.00 and feedback."),
                "Evaluate FYP Idea should include feedback");
            check(fypActions.contains("Notes updated for meeting MEET-1."),
                "Permanent Instructor should update FYP meeting notes");
            check(fypActions.contains("FYP feedback saved."),
                "Permanent Instructor should save evaluation feedback");

            AcademicOfficeAdmin restoredAdmin = new AcademicOfficeAdmin("FYP Reload", "", "", "RELOAD-FYP");
            List<Student> restoredStudents = new java.util.ArrayList<>();
            List<Instructor> restoredInstructors = new java.util.ArrayList<>();
            new CampusPersistence(CLI_CATALOG).loadCatalog(
                restoredAdmin, restoredStudents, restoredInstructors);
            PermanentInstructor permanent = null;
            for (Instructor instructor : restoredInstructors) {
                if (instructor instanceof PermanentInstructor candidate
                    && instructor.getTeacherId().equals("T1")) {
                permanent = candidate;
                }
            }
            check(permanent != null && permanent.viewFYPGroups().size() == 1,
                "FYP group should restore under its Permanent Instructor");
            FYPGroup restoredGroup = permanent.viewFYPGroups().get(0);
            check(restoredGroup.getMembers().size() == 1
                    && restoredGroup.getMembers().get(0).getStudentId().equals("ST1"),
                "FYP group member should restore");
            check(restoredGroup.getMeetings().size() == 1
                    && restoredGroup.getMeetings().get(0).getAgenda().equals("Progress review")
                    && restoredGroup.getMeetings().get(0).getNotes().equals("Agreed on project scope"),
                "FYP meeting and its notes should restore");
            check(restoredGroup.getEvaluations().size() == 1
                    && restoredGroup.getEvaluations().get(0).getScore() == 91
                    && restoredGroup.getEvaluations().get(0).getFeedback().equals("Strong work"),
                "FYP evaluation and feedback should restore");
            }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
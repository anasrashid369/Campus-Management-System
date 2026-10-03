import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CampusCli {
    private static final List<RoleMenu> ROLE_MENUS = List.of(
            new RoleMenu("Academic Office Admin", List.of(
                    "Create Course",
                    "Update Course",
                    "Search Course",
                    "Create Section",
                    "Update Section",
                    "Set Section Capacity",
                    "Assign Room",
                    "Assign Instructor",
                    "View Requests",
                    "Approve Request",
                    "Reject Request")),
            new RoleMenu("Permanent Instructor", List.of(
                    "View Assigned Courses",
                    "View Assigned Sections",
                    "View Enrolled Students",
                    "Mark Attendance",
                    "Mark Present",
                    "Mark Absent",
                    "Mark Late",
                    "Update Attendance",
                    "Calculate Attendance Percentage",
                    "Assign Teaching Assistant",
                    "View FYP Groups",
                    "View FYP Group Details",
                    "View FYP Members",
                    "Schedule FYP Meeting",
                    "Evaluate FYP Idea",
                    "Provide FYP Feedback",
                    "Create FYP Group")),
            new RoleMenu("Visiting Instructor", List.of(
                    "View Assigned Courses",
                    "View Assigned Sections",
                    "View Enrolled Students",
                    "Mark Attendance",
                    "Mark Present",
                    "Mark Absent",
                    "Mark Late",
                    "Update Attendance",
                    "Calculate Attendance Percentage")),
            new RoleMenu("Teaching Assistant", List.of(
                    "View Enrolled Students",
                    "Create Assignment",
                    "Set Total Marks",
                    "Set Assignment Deadline",
                    "View Submissions",
                    "Check Late Submissions",
                    "Evaluate Submission",
                    "Assign Marks",
                    "Give Feedback",
                    "View Assigned Section")),
            new RoleMenu("Normal Student", List.of(
                    "View Attendance Percentage",
                    "Submit Course Clash Request",
                    "Check Section Clash",
                    "View Requests",
                    "View Available Courses",
                    "View Course Credit Hours",
                    "Register Course",
                    "Calculate Total Credit Hours",
                    "Drop Course",
                    "View Registered Courses",
                    "View Timetable",
                    "View Assignments",
                    "Submit Assignment",
                    "View Attendance")));

    private final BufferedReader input;
    private final PrintStream output;
    private final AcademicOfficeAdmin academicOfficeAdmin = new AcademicOfficeAdmin(
            "Console Admin", "console-admin@localhost", "", "CLI-ADMIN");
    private final CampusPersistence persistence = new CampusPersistence();
    private final List<Section> managedSections = new ArrayList<>();
    private final List<Instructor> managedInstructors = new ArrayList<>();
    private final List<Student> managedStudents = new ArrayList<>();

    public CampusCli() {
        this(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    CampusCli(BufferedReader input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void run() {
        ApplicationLogger.info("application.started");
        try {
                managedSections.addAll(persistence.loadCatalog(
                    academicOfficeAdmin, managedStudents, managedInstructors));
            ApplicationLogger.info("catalog.loaded courses=" + academicOfficeAdmin.viewCourses().size()
                    + " sections=" + managedSections.size());
        } catch (IOException exception) {
            ApplicationLogger.error("catalog.load_failed", exception);
            output.println("Unable to load the saved catalog: " + exception.getMessage());
            return;
        }
        try {
            runMenus();
        } catch (IOException exception) {
            ApplicationLogger.error("application.input_failure", exception);
            output.println("Unable to continue reading input. Goodbye.");
        }
    }

    private void runMenus() throws IOException {
        while (true) {
            output.println("\nCampus Management System");
            output.println("0. Exit");
            for (int index = 0; index < ROLE_MENUS.size(); index++) {
                output.printf("%d. %s%n", index + 1, ROLE_MENUS.get(index).role());
            }

            Integer selection = readChoice("Select a role: ", 0, ROLE_MENUS.size());
            if (selection == null || selection == 0) {
                ApplicationLogger.info("application.stopped");
                output.println("Goodbye.");
                return;
            }

            runRoleMenu(ROLE_MENUS.get(selection - 1));
        }
    }

    private void runRoleMenu(RoleMenu roleMenu) throws IOException {
        Student selectedStudent = null;
        Instructor selectedInstructor = null;
        TeachingAssistant selectedAssistant = null;
        if (roleMenu.role().equals("Normal Student")) {
            selectedStudent = selectStudent();
            if (selectedStudent == null) {
                return;
            }
        } else if (roleMenu.role().equals("Permanent Instructor")
                || roleMenu.role().equals("Visiting Instructor")) {
            selectedInstructor = selectInstructor(roleMenu.role());
            if (selectedInstructor == null) {
                return;
            }
        } else if (roleMenu.role().equals("Teaching Assistant")) {
            selectedAssistant = selectTeachingAssistant();
            if (selectedAssistant == null) {
                return;
            }
        }
        while (true) {
            output.printf("%n%s Menu%n", roleMenu.role());
            output.println("0. Back to role selection");
            for (int index = 0; index < roleMenu.actions().size(); index++) {
                output.printf("%d. %s%n", index + 1, roleMenu.actions().get(index));
            }

            Integer selection = readChoice("Select an option: ", 0, roleMenu.actions().size());
            if (selection == null) {
                output.println("Goodbye.");
                return;
            }
            if (selection == 0) {
                return;
            }

            if (roleMenu.role().equals("Academic Office Admin")) {
                handleAdminAction(selection);
            } else if (roleMenu.role().equals("Normal Student")) {
                handleStudentAction(selection, selectedStudent);
            } else if (roleMenu.role().equals("Permanent Instructor")
                    || roleMenu.role().equals("Visiting Instructor")) {
                handleInstructorAction(selection, selectedInstructor);
            } else if (roleMenu.role().equals("Teaching Assistant")) {
                handleTeachingAssistantAction(selection, selectedAssistant);
            } else {
                output.println("This workflow is not connected yet.");
            }
        }
    }

    private void handleAdminAction(int selection) throws IOException {
        switch (selection) {
            case 1 -> createCourse();
            case 2 -> updateCourse();
            case 3 -> searchCourse();
            case 4 -> createSection();
            case 5 -> updateSection();
            case 6 -> setSectionCapacity();
            case 7 -> assignRoom();
            case 8 -> assignInstructor();
            case 9 -> viewRequests();
            case 10 -> processRequest(RequestStatus.APPROVED);
            case 11 -> processRequest(RequestStatus.REJECTED);
            default -> output.println("This workflow is not connected yet.");
        }
    }

    private void handleStudentAction(int selection, Student student) throws IOException {
        switch (selection) {
            case 1 -> viewStudentAttendancePercentage(student);
            case 2 -> submitCourseClashRequest(student);
            case 3 -> checkSectionClash();
            case 4 -> viewStudentRequests(student);
            case 5 -> viewAvailableCourses();
            case 6 -> viewCourseCreditHours();
            case 7 -> registerCourse(student);
            case 8 -> output.printf("Total registered credit hours: %d%n",
                    student.calculateTotalCreditHours());
            case 9 -> dropCourse(student);
            case 10 -> viewRegisteredCourses(student);
            case 11 -> viewStudentTimetable(student);
            case 12 -> viewStudentAssignments(student);
            case 13 -> submitAssignment(student);
            case 14 -> viewStudentAttendance(student);
            default -> output.println("This workflow is not connected yet.");
        }
    }

    private void handleInstructorAction(int selection, Instructor instructor) throws IOException {
        switch (selection) {
            case 1 -> viewInstructorCourses(instructor);
            case 2 -> viewInstructorSections(instructor);
            case 3 -> viewInstructorStudents(instructor);
            case 4 -> markAttendanceWithPrompt(instructor);
            case 5 -> recordAttendance(instructor, AttendanceStatus.PRESENT);
            case 6 -> recordAttendance(instructor, AttendanceStatus.ABSENT);
            case 7 -> recordAttendance(instructor, AttendanceStatus.LATE);
            case 8 -> updateAttendance(instructor);
            case 9 -> calculateInstructorAttendance(instructor);
            case 10 -> {
                if (instructor instanceof PermanentInstructor permanentInstructor) {
                    assignTeachingAssistant(permanentInstructor);
                }
            }
            case 11 -> withPermanentInstructor(instructor, this::viewFypGroups);
            case 12 -> withPermanentInstructor(instructor, this::viewFypGroupDetails);
            case 13 -> withPermanentInstructor(instructor, this::viewFypMembers);
            case 14 -> withPermanentInstructor(instructor, this::scheduleFypMeeting);
            case 15 -> withPermanentInstructor(instructor, this::evaluateFypIdea);
            case 16 -> withPermanentInstructor(instructor, this::provideFypFeedback);
            case 17 -> withPermanentInstructor(instructor, this::createFypGroup);
            default -> output.println("This workflow is not connected yet.");
        }
    }

    private void withPermanentInstructor(Instructor instructor, PermanentInstructorAction action)
            throws IOException {
        if (instructor instanceof PermanentInstructor permanentInstructor) {
            action.run(permanentInstructor);
        }
    }

    private TeachingAssistant selectTeachingAssistant() throws IOException {
        List<TeachingAssistant> assistants = new ArrayList<>();
        for (Student student : managedStudents) {
            if (student instanceof TeachingAssistant assistant) {
                assistants.add(assistant);
            }
        }
        if (assistants.isEmpty()) {
            output.println("No teaching assistants have been assigned to sections yet.");
            return null;
        }
        for (TeachingAssistant assistant : assistants) {
            output.printf("%s | %s%n", assistant.getStudentId(), assistant.getName());
        }
        String studentId = readRequiredText("Teaching Assistant ID: ");
        if (studentId == null) {
            return null;
        }
        for (TeachingAssistant assistant : assistants) {
            if (assistant.getStudentId().equalsIgnoreCase(studentId)) {
                return assistant;
            }
        }
        output.printf("No Teaching Assistant found with ID %s.%n", studentId);
        return null;
    }

    private void handleTeachingAssistantAction(int selection, TeachingAssistant assistant)
            throws IOException {
        switch (selection) {
            case 1 -> viewTeachingAssistantStudents(assistant);
            case 2 -> createAssignment(assistant);
            case 3 -> updateAssignmentTotalMarks(assistant);
            case 4 -> updateAssignmentDeadline(assistant);
            case 5 -> viewSubmissions(assistant);
            case 6 -> viewLateSubmissions(assistant);
            case 7 -> evaluateSubmission(assistant);
            case 8 -> assignSubmissionMarks(assistant);
            case 9 -> provideSubmissionFeedback(assistant);
            case 10 -> viewAssignedSection(assistant);
            default -> output.println("This workflow is not connected yet.");
        }
    }

    private void viewTeachingAssistantStudents(TeachingAssistant assistant) {
        Section section = assistant.getAssignedSection();
        if (section == null || section.getEnrolledStudents().isEmpty()) {
            output.println("Assigned section has no enrolled students.");
            return;
        }
        List<Student> students = section.getEnrolledStudents();
        students.sort(new StudentNameComparator());
        for (Student student : students) {
            output.printf("%s | %s%n", student.getStudentId(), student.getName());
        }
    }

    private void viewAssignedSection(TeachingAssistant assistant) {
        Section section = assistant.getAssignedSection();
        if (section == null) {
            output.println("No section is assigned to this Teaching Assistant.");
            return;
        }
        output.printf("%s | %s | %d student(s)%n", section.getSectionId(),
                section.getCourse().getCourseCode(), section.getEnrolledStudents().size());
    }

    private void createAssignment(TeachingAssistant assistant) throws IOException {
        if (assistant.getAssignedSection() == null) {
            output.println("Cannot create an assignment without an assigned section.");
            return;
        }
        String title = readRequiredText("Assignment title: ");
        if (title == null) {
            return;
        }
        String description = readRequiredText("Assignment description: ");
        if (description == null) {
            return;
        }
        java.time.LocalDate deadline = readLocalDate("Deadline (YYYY-MM-DD): ");
        if (deadline == null) {
            return;
        }
        Double totalMarks = readPositiveDouble("Total marks: ");
        if (totalMarks == null) {
            return;
        }
        String assignmentId = assistant.getAssignedSection().getCourse().getCourseCode() + "-" + title;
        for (Assignment assignment : assistant.getCreatedAssignments()) {
            if (assignment.getId().equalsIgnoreCase(assignmentId)) {
                output.printf("Assignment %s already exists.%n", assignmentId);
                return;
            }
        }
        Assignment assignment = assistant.createAssignment(title, description, deadline, totalMarks);
        if (saveCatalog("assignment.create")) {
            ApplicationLogger.info("assignment.created id=" + assignment.getId()
                    + " ta=" + assistant.getStudentId());
            output.printf("Assignment %s created for section %s.%n",
                    assignment.getId(), assistant.getAssignedSection().getSectionId());
        }
    }

    private void updateAssignmentTotalMarks(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        Double totalMarks = readPositiveDouble("New total marks: ");
        if (totalMarks == null) {
            return;
        }
        for (Submission submission : assignment.getSubmissions()) {
            if (submission.getStatus() == SubmissionStatus.EVALUATED
                    && submission.getMarks() > totalMarks) {
                output.println("Total marks cannot be lower than an evaluated submission score.");
                return;
            }
        }
        try {
            assignment.setTotalMarks(totalMarks);
            if (saveCatalog("assignment.total_marks")) {
                ApplicationLogger.info("assignment.total_marks_changed id=" + assignment.getId());
                output.printf("Total marks for %s set to %.2f.%n", assignment.getId(), totalMarks);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("assignment.total_marks_failed id=" + assignment.getId(), exception);
            output.println("Total marks were not changed: " + exception.getMessage());
        }
    }

    private void updateAssignmentDeadline(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        java.time.LocalDate deadline = readLocalDate("New deadline (YYYY-MM-DD): ");
        if (deadline == null) {
            return;
        }
        try {
            assignment.setDeadline(deadline);
            if (saveCatalog("assignment.deadline")) {
                ApplicationLogger.info("assignment.deadline_changed id=" + assignment.getId());
                output.printf("Deadline for %s set to %s.%n", assignment.getId(), deadline);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("assignment.deadline_failed id=" + assignment.getId(), exception);
            output.println("Deadline was not changed: " + exception.getMessage());
        }
    }

    private Assignment selectTeachingAssistantAssignment(TeachingAssistant assistant) throws IOException {
        List<Assignment> assignments = assistant.getCreatedAssignments();
        if (assignments.isEmpty()) {
            output.println("This Teaching Assistant has not created assignments.");
            return null;
        }
        for (int index = 0; index < assignments.size(); index++) {
            Assignment assignment = assignments.get(index);
            output.printf("%d. %s | deadline %s | %.2f marks%n", index + 1,
                    assignment.getId(), assignment.getDeadline(), assignment.getTotalMarks());
        }
        Integer selection = readChoice("Select an assignment (0 to cancel): ", 0, assignments.size());
        return selection == null || selection == 0 ? null : assignments.get(selection - 1);
    }

    private Submission selectSubmission(Assignment assignment) throws IOException {
        List<Submission> submissions = assignment.getSubmissions();
        if (submissions.isEmpty()) {
            output.println("No submissions are available for this assignment.");
            return null;
        }
        for (int index = 0; index < submissions.size(); index++) {
            Submission submission = submissions.get(index);
            output.printf("%d. %s | %s | %s%n", index + 1, submission.getStudent().getStudentId(),
                    submission.getStatus(), submission.getSubmissionDate());
        }
        Integer selection = readChoice("Select a submission (0 to cancel): ", 0, submissions.size());
        return selection == null || selection == 0 ? null : submissions.get(selection - 1);
    }

    private void viewSubmissions(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        for (Submission submission : assistant.viewSubmissions(assignment)) {
            output.printf("%s | %s | %s | marks %.2f%n", submission.getStudent().getStudentId(),
                    submission.getStatus(), submission.getSubmissionDate(), submission.getMarks());
            if (submission.getFeedback() != null) {
                output.println("  Feedback: " + submission.getFeedback().getComments());
            }
        }
    }

    private void viewLateSubmissions(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        boolean found = false;
        for (Submission submission : assistant.viewSubmissions(assignment)) {
            if (submission.isLate()) {
                found = true;
                output.printf("%s | %s%n", submission.getStudent().getStudentId(),
                        submission.getSubmissionDate());
            }
        }
        if (!found) {
            output.println("No late submissions were found.");
        }
    }

    private void evaluateSubmission(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        Submission submission = selectSubmission(assignment);
        if (submission == null) {
            return;
        }
        Double marks = readNonNegativeDouble("Marks: ");
        if (marks == null) {
            return;
        }
        String comments = readRequiredText("Feedback: ");
        if (comments == null) {
            return;
        }
        if (!assignMarks(assistant, submission, marks)) {
            return;
        }
        assistant.giveFeedback(submission, comments);
        if (saveCatalog("submission.evaluate")) {
            ApplicationLogger.info("submission.evaluated id=" + submission.getSubmissionId()
                    + " ta=" + assistant.getStudentId());
            output.printf("Submission evaluated with %.2f marks and feedback.%n", marks);
        }
    }

    private void assignSubmissionMarks(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        Submission submission = selectSubmission(assignment);
        if (submission == null) {
            return;
        }
        Double marks = readNonNegativeDouble("Marks: ");
        if (marks != null && assignMarks(assistant, submission, marks)
                && saveCatalog("submission.assign_marks")) {
            ApplicationLogger.info("submission.marks_assigned id=" + submission.getSubmissionId());
            output.printf("Marks assigned: %.2f.%n", marks);
        }
    }

    private boolean assignMarks(TeachingAssistant assistant, Submission submission, double marks) {
        try {
            assistant.evaluateSubmission(submission, marks);
            return true;
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("submission.marks_rejected id=" + submission.getSubmissionId(), exception);
            output.println("Marks were not assigned: " + exception.getMessage());
            return false;
        }
    }

    private void provideSubmissionFeedback(TeachingAssistant assistant) throws IOException {
        Assignment assignment = selectTeachingAssistantAssignment(assistant);
        if (assignment == null) {
            return;
        }
        Submission submission = selectSubmission(assignment);
        if (submission == null) {
            return;
        }
        String comments = readRequiredText("Feedback: ");
        if (comments == null) {
            return;
        }
        assistant.giveFeedback(submission, comments);
        if (saveCatalog("submission.feedback")) {
            ApplicationLogger.info("submission.feedback_added id=" + submission.getSubmissionId());
            output.println("Feedback added.");
        }
    }

    private List<Assignment> getAssignmentsForStudent(Student student) {
        List<Assignment> assignments = new ArrayList<>();
        for (Student managedStudent : managedStudents) {
            if (managedStudent instanceof TeachingAssistant assistant) {
                for (Assignment assignment : assistant.getCreatedAssignments()) {
                    if (assignment.getSection().getEnrolledStudents().contains(student)) {
                        assignments.add(assignment);
                    }
                }
            }
        }
        assignments.sort(new AssignmentDeadlineComparator());
        return assignments;
    }

    private void viewStudentAssignments(Student student) {
        List<Assignment> assignments = getAssignmentsForStudent(student);
        if (assignments.isEmpty()) {
            output.println("No assignments are available for the student's registered sections.");
            return;
        }
        for (Assignment assignment : assignments) {
            output.printf("%s | %s | due %s | %.2f marks%n", assignment.getId(),
                    assignment.getTitle(), assignment.getDeadline(), assignment.getTotalMarks());
        }
    }

    private void submitAssignment(Student student) throws IOException {
        List<Assignment> assignments = getAssignmentsForStudent(student);
        Assignment assignment = selectAssignment(assignments, "Select an assignment (0 to cancel): ");
        if (assignment == null) {
            return;
        }
        for (Submission existing : assignment.getSubmissions()) {
            if (existing.getStudent() == student) {
                output.println("A submission already exists for this assignment.");
                return;
            }
        }
        String content = readRequiredText("Submission content: ");
        if (content == null) {
            return;
        }
        Submission submission = student.submitAssignment(assignment, content);
        if (saveCatalog("assignment.submit")) {
            ApplicationLogger.info("assignment.submitted student=" + student.getStudentId()
                    + " assignment=" + assignment.getId() + " status=" + submission.getStatus());
            output.printf("Assignment %s submitted with status %s.%n",
                    assignment.getId(), submission.getStatus());
        }
    }

    private Assignment selectAssignment(List<Assignment> assignments, String prompt) throws IOException {
        if (assignments.isEmpty()) {
            output.println("No assignments are available.");
            return null;
        }
        assignments.sort(new AssignmentDeadlineComparator());
        for (int index = 0; index < assignments.size(); index++) {
            Assignment assignment = assignments.get(index);
            output.printf("%d. %s | %s | due %s%n", index + 1,
                    assignment.getId(), assignment.getTitle(), assignment.getDeadline());
        }
        Integer selection = readChoice(prompt, 0, assignments.size());
        return selection == null || selection == 0 ? null : assignments.get(selection - 1);
    }

    private java.time.LocalDate readLocalDate(String prompt) throws IOException {
        while (true) {
            String value = readRequiredText(prompt);
            if (value == null) {
                return null;
            }
            try {
                return java.time.LocalDate.parse(value);
            } catch (DateTimeException exception) {
                output.println("Enter a date in YYYY-MM-DD format.");
            }
        }
    }

    private Double readPositiveDouble(String prompt) throws IOException {
        while (true) {
            String value = readRequiredText(prompt);
            if (value == null) {
                return null;
            }
            try {
                double number = Double.parseDouble(value);
                if (Double.isFinite(number) && number > 0) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println("Enter a positive finite number.");
        }
    }

    private Double readNonNegativeDouble(String prompt) throws IOException {
        while (true) {
            String value = readRequiredText(prompt);
            if (value == null) {
                return null;
            }
            try {
                double number = Double.parseDouble(value);
                if (Double.isFinite(number) && number >= 0) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println("Enter a non-negative finite number.");
        }
    }

    private Instructor selectInstructor(String role) throws IOException {
        List<Instructor> candidates = new ArrayList<>();
        for (Instructor instructor : managedInstructors) {
            if (instructor.getRole().equals(role)) {
                candidates.add(instructor);
            }
        }
        if (candidates.isEmpty()) {
            output.printf("No %s has been assigned to a section yet.%n", role.toLowerCase(Locale.ROOT));
            return null;
        }
        for (Instructor instructor : candidates) {
            output.printf("%s | %s%n", instructor.getTeacherId(), instructor.getName());
        }
        String teacherId = readRequiredText("Instructor ID: ");
        if (teacherId == null) {
            return null;
        }
        for (Instructor instructor : candidates) {
            if (instructor.getTeacherId().equalsIgnoreCase(teacherId)) {
                return instructor;
            }
        }
        output.printf("No assigned %s found with ID %s.%n", role.toLowerCase(Locale.ROOT), teacherId);
        return null;
    }

    private void assignTeachingAssistant(PermanentInstructor instructor) throws IOException {
        Section section = selectSection(instructor.viewSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        if (section.getTeachingAssistant() != null) {
            output.printf("Section %s already has an assigned teaching assistant.%n",
                    section.getSectionId());
            return;
        }
        List<NormalStudent> normalStudents = new ArrayList<>();
        for (Student student : managedStudents) {
            if (student instanceof NormalStudent normalStudent) {
                normalStudents.add(normalStudent);
            }
        }
        if (normalStudents.isEmpty()) {
            output.println("No Normal Student profiles are available to assign.");
            return;
        }
        for (int index = 0; index < normalStudents.size(); index++) {
            NormalStudent student = normalStudents.get(index);
            output.printf("%d. %s | %s%n", index + 1, student.getStudentId(), student.getName());
        }
        Integer selection = readChoice("Select a student (0 to cancel): ", 0, normalStudents.size());
        if (selection == null || selection == 0) {
            return;
        }
        NormalStudent student = normalStudents.get(selection - 1);
        instructor.assignTA(student, section);
        TeachingAssistant assistant = section.getTeachingAssistant();
        if (assistant == null) {
            output.println("Teaching assistant assignment was not completed.");
            return;
        }
        managedStudents.add(assistant);
        if (saveCatalog("instructor.assign_ta")) {
            ApplicationLogger.info("instructor.ta_assigned teacher=" + instructor.getTeacherId()
                    + " student=" + student.getStudentId() + " section=" + section.getSectionId());
            output.printf("Student %s assigned as TA to section %s.%n",
                    student.getStudentId(), section.getSectionId());
        }
    }

    private void viewFypGroups(PermanentInstructor instructor) {
        List<FYPGroup> groups = instructor.viewFYPGroups();
        if (groups.isEmpty()) {
            output.println("No FYP groups are assigned to this instructor.");
            return;
        }
        for (FYPGroup group : groups) {
            output.printf("%s | %s%n", group.getGroupId(), group.getTitle());
        }
    }

    private void viewFypGroupDetails(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectFypGroup(instructor);
        if (group != null) {
            output.println(instructor.viewFYPGroupDetails(group));
            List<FYPMeeting> meetings = group.getMeetings();
            meetings.sort(new FYPMeetingDateComparator());
            for (FYPMeeting meeting : meetings) {
                output.println(meeting.getMeetingDetails());
            }
        }
    }

    private void viewFypMembers(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectFypGroup(instructor);
        if (group == null) {
            return;
        }
        List<Student> members = group.getMembers();
        if (members.isEmpty()) {
            output.println("FYP group has no members.");
            return;
        }
        members.sort(new StudentNameComparator());
        for (Student member : members) {
            output.printf("%s | %s%n", member.getStudentId(), member.getName());
        }
    }

    private void scheduleFypMeeting(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectFypGroup(instructor);
        if (group == null) {
            return;
        }
        String meetingId = readRequiredText("Meeting ID: ");
        if (meetingId == null) {
            return;
        }
        java.time.LocalDate date = readLocalDate("Meeting date (YYYY-MM-DD): ");
        if (date == null) {
            return;
        }
        String agenda = readRequiredText("Agenda: ");
        if (agenda == null) {
            return;
        }
        FYPMeeting meeting = new FYPMeeting(meetingId, date, agenda);
        try {
            instructor.scheduleFYPMeeting(group, meeting);
        } catch (InvalidFYPGroupException exception) {
            ApplicationLogger.error("fyp.meeting_rejected group=" + group.getGroupId(), exception);
            output.println("Meeting was not scheduled: " + exception.getMessage());
            return;
        }
        if (saveCatalog("fyp.meeting.schedule")) {
            ApplicationLogger.info("fyp.meeting_scheduled group=" + group.getGroupId()
                    + " meeting=" + meetingId);
            output.printf("Meeting %s scheduled for group %s.%n", meetingId, group.getGroupId());
        }
    }

    private void evaluateFypIdea(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectFypGroup(instructor);
        if (group == null) {
            return;
        }
        String evaluationId = readRequiredText("Evaluation ID: ");
        if (evaluationId == null) {
            return;
        }
        java.time.LocalDate date = readLocalDate("Evaluation date (YYYY-MM-DD): ");
        if (date == null) {
            return;
        }
        Double score = readNonNegativeDouble("Score: ");
        if (score == null) {
            return;
        }
        FYPEvaluation evaluation = new FYPEvaluation(evaluationId, date, instructor);
        try {
            evaluation.evaluate(score);
            instructor.evaluateFYPIdea(group, evaluation);
            if (saveCatalog("fyp.idea.evaluate")) {
                ApplicationLogger.info("fyp.idea_evaluated group=" + group.getGroupId()
                        + " evaluation=" + evaluationId);
                output.printf("FYP idea evaluated with score %.2f.%n", score);
            }
        } catch (InvalidFYPGroupException | InvalidFYPEvaluationException exception) {
            ApplicationLogger.error("fyp.evaluation_rejected id=" + evaluationId, exception);
            output.println("FYP evaluation was not added: " + exception.getMessage());
        }
    }

    private void provideFypFeedback(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectFypGroup(instructor);
        if (group == null) {
            return;
        }
        List<FYPEvaluation> evaluations = group.getEvaluations();
        if (evaluations.isEmpty()) {
            output.println("The group has no evaluations to provide feedback on.");
            return;
        }
        for (int index = 0; index < evaluations.size(); index++) {
            FYPEvaluation evaluation = evaluations.get(index);
            output.printf("%d. %s | score %.2f%n", index + 1,
                    evaluation.getEvaluationId(), evaluation.getScore());
        }
        Integer selection = readChoice("Select an evaluation (0 to cancel): ", 0, evaluations.size());
        if (selection == null || selection == 0) {
            return;
        }
        FYPEvaluation evaluation = evaluations.get(selection - 1);
        String feedback = readRequiredText("Feedback: ");
        if (feedback == null) {
            return;
        }
        try {
            instructor.provideFYPFeedback(evaluation, feedback);
        } catch (InvalidFYPGroupException | InvalidFYPEvaluationException exception) {
            ApplicationLogger.error("fyp.feedback_rejected id=" + evaluation.getEvaluationId(), exception);
            output.println("FYP feedback was not saved: " + exception.getMessage());
            return;
        }
        if (saveCatalog("fyp.feedback")) {
            ApplicationLogger.info("fyp.feedback_added group=" + group.getGroupId()
                    + " evaluation=" + evaluation.getEvaluationId());
            output.println("FYP feedback saved.");
        }
    }

    private void createFypGroup(PermanentInstructor instructor) throws IOException {
        String groupId = readRequiredText("FYP group ID: ");
        if (groupId == null) {
            return;
        }
        for (Instructor managedInstructor : managedInstructors) {
            if (managedInstructor instanceof PermanentInstructor permanent) {
                for (FYPGroup group : permanent.viewFYPGroups()) {
                    if (group.getGroupId().equalsIgnoreCase(groupId)) {
                        output.printf("FYP group %s already exists.%n", groupId);
                        return;
                    }
                }
            }
        }
        String title = readRequiredText("Group title: ");
        if (title == null) {
            return;
        }
        String description = readRequiredText("Group description: ");
        if (description == null) {
            return;
        }
        FYPGroup group = new FYPGroup(groupId, title, description);
        while (true) {
            Boolean addMember = readYesNo("Add a Normal Student member? (y/n): ");
            if (addMember == null || !addMember) {
                break;
            }
            NormalStudent member = selectNormalStudent();
            if (member == null) {
                return;
            }
            try {
                group.addMember(member);
            } catch (InvalidFYPGroupException exception) {
                output.println("Member was not added: " + exception.getMessage());
                continue;
            }
        }
        try {
            group.assignSupervisor(instructor);
        } catch (InvalidFYPGroupException exception) {
            output.println("FYP group was not assigned: " + exception.getMessage());
            return;
        }
        if (saveCatalog("fyp.group.create")) {
            ApplicationLogger.info("fyp.group_created id=" + groupId
                    + " supervisor=" + instructor.getTeacherId());
            output.printf("FYP group %s created and supervised by %s.%n",
                    groupId, instructor.getTeacherId());
        }
    }

    private NormalStudent selectNormalStudent() throws IOException {
        List<NormalStudent> students = new ArrayList<>();
        for (Student student : managedStudents) {
            if (student instanceof NormalStudent normalStudent) {
                students.add(normalStudent);
            }
        }
        if (students.isEmpty()) {
            output.println("No Normal Student profiles are available.");
            return null;
        }
        for (int index = 0; index < students.size(); index++) {
            NormalStudent student = students.get(index);
            output.printf("%d. %s | %s%n", index + 1, student.getStudentId(), student.getName());
        }
        Integer selection = readChoice("Select a student (0 to cancel): ", 0, students.size());
        return selection == null || selection == 0 ? null : students.get(selection - 1);
    }

    private FYPGroup selectFypGroup(PermanentInstructor instructor) throws IOException {
        List<FYPGroup> groups = instructor.viewFYPGroups();
        if (groups.isEmpty()) {
            output.println("No FYP groups are assigned to this instructor.");
            return null;
        }
        for (int index = 0; index < groups.size(); index++) {
            FYPGroup group = groups.get(index);
            output.printf("%d. %s | %s%n", index + 1, group.getGroupId(), group.getTitle());
        }
        Integer selection = readChoice("Select a group (0 to cancel): ", 0, groups.size());
        return selection == null || selection == 0 ? null : groups.get(selection - 1);
    }

    private void viewInstructorCourses(Instructor instructor) {
        List<Course> courses = instructor.viewCourses();
        if (courses.isEmpty()) {
            output.println("Instructor has no assigned courses.");
            return;
        }
        for (Course course : courses) {
            output.printf("%s | %s%n", course.getCourseCode(), course.getTitle());
        }
    }

    private void viewInstructorSections(Instructor instructor) {
        List<Section> sections = instructor.viewSections();
        if (sections.isEmpty()) {
            output.println("Instructor has no assigned sections.");
            return;
        }
        for (Section section : sections) {
            output.printf("%s | %s%n", section.getSectionId(), section.getCourse().getCourseCode());
        }
    }

    private void viewInstructorStudents(Instructor instructor) throws IOException {
        Section section = selectSection(instructor.viewSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        List<Student> students = instructor.viewEnrolledStudents(section);
        if (students.isEmpty()) {
            output.println("Section has no enrolled students.");
            return;
        }
        students.sort(new StudentNameComparator());
        for (Student student : students) {
            output.printf("%s | %s%n", student.getStudentId(), student.getName());
        }
    }

    private void markAttendanceWithPrompt(Instructor instructor) throws IOException {
        output.println("1. Present");
        output.println("2. Absent");
        output.println("3. Late");
        Integer selection = readChoice("Select attendance status: ", 1, 3);
        if (selection == null) {
            return;
        }
        AttendanceStatus status = switch (selection) {
            case 1 -> AttendanceStatus.PRESENT;
            case 2 -> AttendanceStatus.ABSENT;
            default -> AttendanceStatus.LATE;
        };
        recordAttendance(instructor, status);
    }

    private void recordAttendance(Instructor instructor, AttendanceStatus status) throws IOException {
        Section section = selectSection(instructor.viewSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        Student student = selectSectionStudent(section);
        if (student == null) {
            return;
        }
        Attendance attendance = new Attendance(student, section, java.time.LocalDate.now(), status);
        instructor.markAttendance(attendance, status);
        if (saveCatalog("attendance.mark")) {
            ApplicationLogger.info("attendance.marked teacher=" + instructor.getTeacherId()
                    + " student=" + student.getStudentId() + " section=" + section.getSectionId()
                    + " status=" + status);
            output.printf("Attendance marked %s for %s in section %s.%n",
                    status, student.getStudentId(), section.getSectionId());
        }
    }

    private Student selectSectionStudent(Section section) throws IOException {
        List<Student> students = section.getEnrolledStudents();
        if (students.isEmpty()) {
            output.println("Section has no enrolled students.");
            return null;
        }
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            output.printf("%d. %s | %s%n", index + 1, student.getStudentId(), student.getName());
        }
        Integer selection = readChoice("Select a student (0 to cancel): ", 0, students.size());
        return selection == null || selection == 0 ? null : students.get(selection - 1);
    }

    private void updateAttendance(Instructor instructor) throws IOException {
        Section section = selectSection(instructor.viewSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        List<Attendance> records = section.getAttendanceRecords();
        if (records.isEmpty()) {
            output.println("Section has no attendance records to update.");
            return;
        }
        for (int index = 0; index < records.size(); index++) {
            Attendance record = records.get(index);
            output.printf("%d. %s | %s | %s%n", index + 1,
                    record.getStudent().getStudentId(), record.getDate(), record.getStatus());
        }
        Integer recordChoice = readChoice("Select an attendance record (0 to cancel): ", 0, records.size());
        if (recordChoice == null || recordChoice == 0) {
            return;
        }
        Attendance record = records.get(recordChoice - 1);
        output.println("1. Present");
        output.println("2. Absent");
        output.println("3. Late");
        Integer statusChoice = readChoice("Select new status: ", 1, 3);
        if (statusChoice == null) {
            return;
        }
        AttendanceStatus status = switch (statusChoice) {
            case 1 -> AttendanceStatus.PRESENT;
            case 2 -> AttendanceStatus.ABSENT;
            default -> AttendanceStatus.LATE;
        };
        instructor.updateAttendance(record, status);
        if (saveCatalog("attendance.update")) {
            ApplicationLogger.info("attendance.updated teacher=" + instructor.getTeacherId()
                    + " student=" + record.getStudent().getStudentId()
                    + " section=" + section.getSectionId() + " status=" + status);
            output.printf("Attendance updated to %s.%n", status);
        }
    }

    private void calculateInstructorAttendance(Instructor instructor) throws IOException {
        Section section = selectSection(instructor.viewSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        Student student = selectSectionStudent(section);
        if (student == null) {
            return;
        }
        output.printf("Attendance percentage: %.2f%%%n",
                instructor.calculateAttendancePercentage(student, section));
    }

    private Student selectStudent() throws IOException {
        String studentId = readRequiredText("Student ID: ");
        if (studentId == null) {
            return null;
        }
        for (Student student : managedStudents) {
            if (student instanceof NormalStudent
                    && student.getStudentId().equalsIgnoreCase(studentId)) {
                output.printf("Selected student %s.%n", student.getStudentId());
                return student;
            }
        }

        output.println("No saved student profile has that ID.");
        Boolean createProfile = readYesNo("Create a new student profile? (y/n): ");
        if (createProfile == null || !createProfile) {
            return null;
        }
        String name = readRequiredText("Name: ");
        if (name == null) {
            return null;
        }
        String email = readRequiredText("Email: ");
        if (email == null) {
            return null;
        }
        String phone = readRequiredText("Phone: ");
        if (phone == null) {
            return null;
        }

        Student student = new NormalStudent(name, email, phone, studentId, 0);
        managedStudents.add(student);
        if (saveCatalog("student.create")) {
            ApplicationLogger.info("student.created id=" + studentId);
            output.printf("Student profile %s created.%n", studentId);
        }
        return student;
    }

    private Boolean readYesNo(String prompt) throws IOException {
        while (true) {
            String answer = readRequiredText(prompt);
            if (answer == null) {
                return null;
            }
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            output.println("Enter y or n.");
        }
    }

    private void viewAvailableCourses() {
        List<Course> courses = academicOfficeAdmin.viewCourses();
        if (courses.isEmpty()) {
            output.println("No courses are currently available.");
            return;
        }
        for (Course course : courses) {
            output.printf("%s | %s | %d credit hour(s)%n",
                    course.getCourseCode(), course.getTitle(), course.getCreditHours());
            for (Section section : course.getSections()) {
                output.printf("  Section %s | %d seat(s) available%n",
                        section.getSectionId(), section.getAvailableSeats());
            }
        }
    }

    private void viewCourseCreditHours() throws IOException {
        Course course = selectCourseByCode("Course code: ");
        if (course != null) {
            output.printf("%s has %d credit hour(s).%n",
                    course.getCourseCode(), course.getCreditHours());
        }
    }

    private Course selectCourseByCode(String prompt) throws IOException {
        String courseCode = readRequiredText(prompt);
        if (courseCode == null) {
            return null;
        }
        Course course = academicOfficeAdmin.searchCourse(courseCode);
        if (course == null) {
            output.printf("No course found with code %s.%n", courseCode);
        }
        return course;
    }

    private Section selectSection(List<Section> sections, String prompt) throws IOException {
        if (sections.isEmpty()) {
            output.println("No sections are available for this operation.");
            return null;
        }
        for (int index = 0; index < sections.size(); index++) {
            Section section = sections.get(index);
            output.printf("%d. %s | %s | %d seat(s) available%n", index + 1,
                    section.getSectionId(), section.getCourse().getCourseCode(), section.getAvailableSeats());
        }
        Integer selection = readChoice(prompt, 0, sections.size());
        if (selection == null || selection == 0) {
            return null;
        }
        return sections.get(selection - 1);
    }

    private void registerCourse(Student student) throws IOException {
        Course course = selectCourseByCode("Course code to register: ");
        if (course == null) {
            return;
        }
        Section section = selectSection(course.getSections(), "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        boolean alreadyEnrolled = section.getEnrolledStudents().contains(student);
        try {
            student.register(section);
        } catch (CourseFullException exception) {
            ApplicationLogger.error("student.registration_rejected id=" + student.getStudentId(), exception);
            output.println("Registration failed: " + exception.getMessage());
            return;
        } catch (CourseClashException exception) {
            ApplicationLogger.error("student.registration_rejected id=" + student.getStudentId(), exception);
            output.println("Registration failed: " + exception.getMessage());
            return;
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("student.registration_rejected id=" + student.getStudentId(), exception);
            output.println("Registration failed: " + exception.getMessage());
            return;
        }
        if (alreadyEnrolled) {
            output.printf("Student is already registered in section %s.%n", section.getSectionId());
            return;
        }
        if (!section.getEnrolledStudents().contains(student)) {
            if (section.isFull()) {
                output.printf("Section %s is full; registration was not completed.%n", section.getSectionId());
            } else if (hasRegisteredSectionClash(student, section)) {
                output.printf("Section %s clashes with the student's timetable.%n", section.getSectionId());
            } else {
                output.println("Registration was not completed by the domain rules.");
            }
            return;
        }
        if (saveCatalog("student.register")) {
            ApplicationLogger.info("student.registered id=" + student.getStudentId()
                    + " section=" + section.getSectionId());
            output.printf("Registered in section %s (%d total credit hour(s)).%n",
                    section.getSectionId(), student.calculateTotalCreditHours());
        }
    }

    private boolean hasRegisteredSectionClash(Student student, Section requestedSection) {
        for (Section section : managedSections) {
            if (section != requestedSection && section.getEnrolledStudents().contains(student)
                    && section.hasClash(requestedSection)) {
                return true;
            }
        }
        return false;
    }

    private void dropCourse(Student student) throws IOException {
        List<Section> enrolledSections = getEnrolledSections(student);
        Section section = selectSection(enrolledSections, "Select a section to drop (0 to cancel): ");
        if (section == null) {
            return;
        }
        student.drop(section);
        if (section.getEnrolledStudents().contains(student)) {
            output.printf("Student remains registered in section %s.%n", section.getSectionId());
            return;
        }
        if (saveCatalog("student.drop")) {
            ApplicationLogger.info("student.dropped id=" + student.getStudentId()
                    + " section=" + section.getSectionId());
            output.printf("Dropped section %s. Total credit hours: %d.%n",
                    section.getSectionId(), student.calculateTotalCreditHours());
        }
    }

    private List<Section> getEnrolledSections(Student student) {
        List<Section> enrolledSections = new ArrayList<>();
        for (Section section : managedSections) {
            if (section.getEnrolledStudents().contains(student)) {
                enrolledSections.add(section);
            }
        }
        return enrolledSections;
    }

    private void viewRegisteredCourses(Student student) {
        List<Course> courses = student.viewCourses();
        if (courses.isEmpty()) {
            output.println("Student has no registered courses.");
            return;
        }
        for (Course course : courses) {
            output.printf("%s | %s | %d credit hour(s)%n",
                    course.getCourseCode(), course.getTitle(), course.getCreditHours());
        }
    }

    private void viewStudentTimetable(Student student) {
        List<Schedule> timetable = student.viewTimetable();
        if (timetable.isEmpty()) {
            output.println("Student has no scheduled courses.");
            return;
        }
        for (Schedule schedule : timetable) {
            output.println(schedule.getScheduleInfo());
        }
    }

    private void viewStudentAttendance(Student student) {
        boolean found = false;
        for (Section section : managedSections) {
            for (Attendance attendance : section.getAttendanceRecords()) {
                if (attendance.getStudent() == student) {
                    found = true;
                    output.printf("%s | %s | %s%n", section.getSectionId(),
                            attendance.getDate(), attendance.getStatus());
                }
            }
        }
        if (!found) {
            output.println("No attendance records were found for this student.");
        }
    }

    private void viewStudentAttendancePercentage(Student student) throws IOException {
        Section section = selectSection(getEnrolledSections(student),
                "Select a section (0 to cancel): ");
        if (section == null) {
            return;
        }
        Instructor instructor = section.getInstructor();
        if (instructor == null) {
            output.println("Attendance percentage is unavailable because no instructor is assigned.");
            return;
        }
        output.printf("Attendance percentage: %.2f%%%n",
                instructor.calculateAttendancePercentage(student, section));
    }

    private void checkSectionClash() throws IOException {
        Section first = selectSection(managedSections, "Select the first section (0 to cancel): ");
        if (first == null) {
            return;
        }
        Section second = selectSection(managedSections, "Select the second section (0 to cancel): ");
        if (second == null) {
            return;
        }
        output.println(first.hasClash(second) ? "The sections have a schedule clash."
                : "The sections do not have a schedule clash.");
    }

    private void viewStudentRequests(Student student) {
        List<Request> requests = academicOfficeAdmin.viewRequests();
        List<Request> studentRequests = new ArrayList<>();
        for (Request request : requests) {
            if (request.getStudent() == student) {
                studentRequests.add(request);
            }
        }
        studentRequests.sort(new RequestDateComparator());
        int count = 0;
        for (Request request : studentRequests) {
            output.println(request.getDetails());
            count++;
        }
        if (count == 0) {
            output.println("Student has no academic requests.");
        }
    }

    private void submitCourseClashRequest(Student student) throws IOException {
        Section conflictingSection = selectSection(getEnrolledSections(student),
                "Select the conflicting registered section (0 to cancel): ");
        if (conflictingSection == null) {
            return;
        }
        String requestedId = readRequiredText("Requested section ID: ");
        if (requestedId == null) {
            return;
        }
        Section requestedSection = null;
        for (Section section : managedSections) {
            if (section.getSectionId().equalsIgnoreCase(requestedId)) {
                requestedSection = section;
                break;
            }
        }
        if (requestedSection == null) {
            output.printf("No section found with ID %s.%n", requestedId);
            return;
        }
        if (!conflictingSection.hasClash(requestedSection)) {
            output.println("No schedule clash exists; no request was submitted.");
            return;
        }
        String description = readRequiredText("Request description: ");
        if (description == null) {
            return;
        }
        Integer priority = readPositiveInteger("Priority: ", "Enter a positive whole-number priority.");
        if (priority == null) {
            return;
        }

        CourseClashRequest request = new CourseClashRequest("REQ-" + UUID.randomUUID(),
                java.time.LocalDate.now(), description, priority, student,
                conflictingSection, requestedSection);
        student.submitCourseClashRequest(request);
        try {
            academicOfficeAdmin.addRequest(request);
        } catch (InvalidRequestException exception) {
            ApplicationLogger.error("request.submit_failed student=" + student.getStudentId(), exception);
            output.println("Request was not submitted: " + exception.getMessage());
            return;
        }
        if (saveCatalog("request.submit")) {
            ApplicationLogger.info("request.submitted id=" + request.getRequestId()
                    + " student=" + student.getStudentId());
            output.printf("Course clash request %s submitted.%n", request.getRequestId());
        }
    }

    private void createCourse() throws IOException {
        String courseCode = readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }

        String title = readRequiredText("Course title: ");
        if (title == null) {
            return;
        }

        Integer creditHours = readPositiveInteger(
            "Credit hours: ", "Enter a positive whole number of credit hours.");
        if (creditHours == null) {
            return;
        }

        if (academicOfficeAdmin.searchCourse(courseCode) != null) {
            ApplicationLogger.info("course.create_rejected duplicate_code=" + courseCode);
            output.printf("Course %s already exists; no course was created.%n", courseCode);
            return;
        }

        try {
            Course course = new Course(courseCode, title, creditHours);
            academicOfficeAdmin.createCourse(course);
            if (academicOfficeAdmin.searchCourse(courseCode) == course) {
                if (saveCatalog("course.create")) {
                    ApplicationLogger.info("course.created code=" + courseCode);
                    output.printf("Course %s created successfully.%n", courseCode);
                }
            } else {
                ApplicationLogger.info("course.create_failed code=" + courseCode);
                output.printf("Course %s was not created.%n", courseCode);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("course.create_failed code=" + courseCode, exception);
            output.printf("Course was not created: %s%n", exception.getMessage());
        }
    }

    private void searchCourse() throws IOException {
        String courseCode = readRequiredText("Course code to search: ");
        if (courseCode == null) {
            return;
        }

        Course course = academicOfficeAdmin.searchCourse(courseCode);
        if (course == null) {
            ApplicationLogger.info("course.search_not_found code=" + courseCode);
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        ApplicationLogger.info("course.searched code=" + course.getCourseCode());
        output.printf("Course found: %s | %s | %d credit hour(s)%n",
                course.getCourseCode(), course.getTitle(), course.getCreditHours());
    }

    private void updateCourse() throws IOException {
        String courseCode = readRequiredText("Course code to update: ");
        if (courseCode == null) {
            return;
        }
        Course existingCourse = academicOfficeAdmin.searchCourse(courseCode);
        if (existingCourse == null) {
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        String title = readRequiredText("New course title: ");
        if (title == null) {
            return;
        }
        Integer creditHours = readPositiveInteger(
                "New credit hours: ", "Enter a positive whole number of credit hours.");
        if (creditHours == null) {
            return;
        }

        Course updatedDetails = new Course(existingCourse.getCourseCode(), title, creditHours);
        academicOfficeAdmin.updateCourse(updatedDetails);
        if (academicOfficeAdmin.searchCourse(courseCode) == existingCourse
                && existingCourse.getTitle().equals(title)
                && existingCourse.getCreditHours() == creditHours) {
            if (saveCatalog("course.update")) {
                ApplicationLogger.info("course.updated code=" + existingCourse.getCourseCode());
                output.printf("Course %s updated successfully.%n", existingCourse.getCourseCode());
            }
        } else {
            ApplicationLogger.info("course.update_failed code=" + courseCode);
            output.printf("Course %s was not updated.%n", courseCode);
        }
    }

    private String readRequiredText(String prompt) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            String value = line.trim();
            if (!value.isEmpty()) {
                return value;
            }
            output.println("This value cannot be blank.");
        }
    }

    private void createSection() throws IOException {
        String sectionId = readRequiredText("Section ID: ");
        if (sectionId == null) {
            return;
        }
        for (Section section : managedSections) {
            if (section.getSectionId().equalsIgnoreCase(sectionId)) {
                ApplicationLogger.info("section.create_rejected duplicate_id=" + sectionId);
                output.printf("Section %s already exists.%n", sectionId);
                return;
            }
        }

        String courseCode = readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }
        Course course = academicOfficeAdmin.searchCourse(courseCode);
        if (course == null) {
            ApplicationLogger.info("section.create_rejected unknown_course=" + courseCode);
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        Integer capacity = readPositiveInteger(
                "Section capacity: ", "Enter a positive whole number for capacity.");
        if (capacity == null) {
            return;
        }

        Section section = new Section(sectionId, capacity, course);
        academicOfficeAdmin.createSection(section);
        if (course.getSections().contains(section)) {
            managedSections.add(section);
            if (saveCatalog("section.create")) {
                ApplicationLogger.info("section.created id=" + sectionId + " course=" + course.getCourseCode());
                output.printf("Section %s created for course %s.%n", sectionId, course.getCourseCode());
            }
        } else {
            ApplicationLogger.info("section.create_failed id=" + sectionId);
            output.printf("Section %s was not created.%n", sectionId);
        }
    }

    private void setSectionCapacity() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        updateSectionCapacity(section);
    }

    private void updateSectionCapacity(Section section) throws IOException {
        Integer capacity = readPositiveInteger(
                "New capacity: ", "Enter a positive whole number for capacity.");
        if (capacity == null) {
            return;
        }

        try {
            academicOfficeAdmin.setCapacity(section, capacity);
            if (saveCatalog("section.capacity")) {
            ApplicationLogger.info("section.capacity_changed id=" + section.getSectionId()
                + " capacity=" + section.getCapacity());
            output.printf("Capacity for section %s set to %d.%n",
                section.getSectionId(), section.getCapacity());
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("section.capacity_change_failed id=" + section.getSectionId(), exception);
            output.printf("Capacity was not changed: %s%n", exception.getMessage());
        }
    }

    private void assignRoom() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        assignRoom(section);
    }

    private void assignRoom(Section section) throws IOException {
        String dayInput = readRequiredText("Day (Monday-Saturday): ");
        if (dayInput == null) {
            return;
        }
        Day day;
        try {
            day = Day.valueOf(dayInput.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            output.println("Enter a day from Monday through Saturday.");
            return;
        }

        LocalTime startTime = readTime("Start time (HH:mm): ");
        if (startTime == null) {
            return;
        }
        LocalTime endTime = readTime("End time (HH:mm): ");
        if (endTime == null) {
            return;
        }
        if (!endTime.isAfter(startTime)) {
            output.println("End time must be after start time.");
            return;
        }

        String room = readRequiredText("Room: ");
        if (room == null) {
            return;
        }

        Schedule schedule = new Schedule(day, startTime, endTime, room);
        academicOfficeAdmin.assignRoom(section, schedule);
        if (saveCatalog("section.room")) {
            ApplicationLogger.info("section.room_assigned id=" + section.getSectionId()
                + " schedule=" + schedule.getScheduleInfo());
            output.printf("Room assigned to section %s: %s%n",
                section.getSectionId(), schedule.getScheduleInfo());
        }
    }

    private void updateSection() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        output.printf("Update Section %s%n", section.getSectionId());
        output.println("0. Cancel");
        output.println("1. Update capacity");
        output.println("2. Update room and schedule");
        Integer selection = readChoice("Select a field to update: ", 0, 2);
        if (selection == null || selection == 0) {
            return;
        }
        if (selection == 1) {
            updateSectionCapacity(section);
        } else {
            assignRoom(section);
        }
    }

    private void assignInstructor() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        String teacherId = readRequiredText("Instructor ID: ");
        if (teacherId == null) {
            return;
        }

        Instructor instructor = findManagedInstructor(teacherId);
        if (instructor == null) {
            Integer instructorType = readChoice(
                    "1. Permanent Instructor\n2. Visiting Instructor\nSelect type: ", 1, 2);
            if (instructorType == null) {
                return;
            }
            String name = readRequiredText("Name: ");
            if (name == null) {
                return;
            }
            String email = readRequiredText("Email: ");
            if (email == null) {
                return;
            }
            String phone = readRequiredText("Phone: ");
            if (phone == null) {
                return;
            }

            if (instructorType == 1) {
                instructor = new PermanentInstructor(name, email, phone, teacherId);
            } else {
                instructor = new VisitingInstructor(name, email, phone, teacherId);
            }
            managedInstructors.add(instructor);
        }

        academicOfficeAdmin.assignInstructor(section, instructor);
        if (section.getInstructor() == instructor) {
            if (saveCatalog("instructor.assign")) {
            ApplicationLogger.info("instructor.assigned teacher_id=" + instructor.getTeacherId()
                + " section=" + section.getSectionId());
            output.printf("%s assigned to section %s.%n",
                instructor.getRole(), section.getSectionId());
            }
        } else {
            output.printf("Instructor was not assigned to section %s.%n", section.getSectionId());
        }
    }

    private Instructor findManagedInstructor(String teacherId) {
        for (Instructor instructor : managedInstructors) {
            if (instructor.getTeacherId().equalsIgnoreCase(teacherId)) {
                return instructor;
            }
        }
        return null;
    }

    private void viewRequests() {
        List<Request> requests = getPendingRequests();
        ApplicationLogger.info("requests.viewed pending_count=" + requests.size());
        if (requests.isEmpty()) {
            output.println("No pending requests are currently available.");
            return;
        }
        for (int index = 0; index < requests.size(); index++) {
            output.printf("%d. %s%n", index + 1, requests.get(index).getDetails());
        }
    }

    private void processRequest(RequestStatus status) throws IOException {
        List<Request> requests = getPendingRequests();
        ApplicationLogger.info("requests.processing_menu status=" + status + " count=" + requests.size());
        if (requests.isEmpty()) {
            output.println("No pending requests are currently available to process.");
            return;
        }
        for (int index = 0; index < requests.size(); index++) {
            output.printf("%d. %s%n", index + 1, requests.get(index).getDetails());
        }

        Integer selection = readChoice("Select a request (0 to cancel): ", 0, requests.size());
        if (selection == null || selection == 0) {
            return;
        }
        Request request = requests.get(selection - 1);
        if (request.getStatus() != RequestStatus.PENDING) {
            output.printf("Request %s has already been processed as %s.%n",
                    request.getRequestId(), request.getStatus());
            return;
        }

        try {
            if (status == RequestStatus.APPROVED) {
                academicOfficeAdmin.approveRequest(request);
            } else {
                academicOfficeAdmin.rejectRequest(request);
            }
        } catch (InvalidRequestException exception) {
            ApplicationLogger.error("request.processing_failed id=" + request.getRequestId(), exception);
            output.println("Request was not processed: " + exception.getMessage());
            return;
        }
        if (request.getStatus() == status) {
            if (saveCatalog("request.process")) {
                ApplicationLogger.info("request.processed id=" + request.getRequestId() + " status=" + status);
                output.printf("Request %s %s.%n", request.getRequestId(),
                        status.name().toLowerCase(Locale.ROOT));
            }
        } else {
            ApplicationLogger.info("request.processing_failed id=" + request.getRequestId());
            output.printf("Request %s was not processed.%n", request.getRequestId());
        }
    }

    private Section findManagedSection() throws IOException {
        String sectionId = readRequiredText("Section ID: ");
        if (sectionId == null) {
            return null;
        }
        for (Section section : managedSections) {
            if (section.getSectionId().equalsIgnoreCase(sectionId)) {
                return section;
            }
        }
        output.printf("No section found with ID %s in this CLI session.%n", sectionId);
        return null;
    }

    private List<Request> getPendingRequests() {
        List<Request> pendingRequests = new ArrayList<>();
        for (Request request : academicOfficeAdmin.viewRequests()) {
            if (request.getStatus() == RequestStatus.PENDING) {
                pendingRequests.add(request);
            }
        }
        pendingRequests.sort(new RequestPriorityComparator());
        return pendingRequests;
    }

    private boolean saveCatalog(String operation) {
        try {
            persistence.saveCatalog(academicOfficeAdmin, managedStudents, managedInstructors);
            return true;
        } catch (IOException exception) {
            ApplicationLogger.error("catalog.save_failed operation=" + operation, exception);
            output.println("Change is active for this session only; saving the catalog failed.");
            return false;
        }
    }

    private LocalTime readTime(String prompt) throws IOException {
        String value = readRequiredText(prompt);
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException exception) {
            output.println("Enter time in 24-hour HH:mm format.");
            return null;
        }
    }

    private Integer readPositiveInteger(String prompt, String errorMessage) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            try {
                int value = Integer.parseInt(line.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println(errorMessage);
        }
    }

    private Integer readChoice(String prompt, int minimum, int maximum) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            try {
                int selection = Integer.parseInt(line.trim());
                if (selection >= minimum && selection <= maximum) {
                    return selection;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input is reported below and the menu remains active.
            }

            output.printf("Enter a number from %d to %d.%n", minimum, maximum);
        }
    }

    private record RoleMenu(String role, List<String> actions) {
    }

    @FunctionalInterface
    private interface PermanentInstructorAction {
        void run(PermanentInstructor instructor) throws IOException;
    }
}
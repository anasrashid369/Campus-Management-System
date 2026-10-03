import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Normal Student workflows: registration, timetable, attendance, assignments, and requests. */
final class StudentCliHandler implements RoleCliHandler {
    private enum Option {
        VIEW_ATTENDANCE_PERCENTAGE("View Attendance Percentage"),
        SUBMIT_COURSE_CLASH_REQUEST("Submit Course Clash Request"),
        CHECK_SECTION_CLASH("Check Section Clash"),
        VIEW_REQUESTS("View Requests"),
        VIEW_AVAILABLE_COURSES("View Available Courses"),
        VIEW_COURSE_CREDIT_HOURS("View Course Credit Hours"),
        REGISTER_COURSE("Register Course"),
        CALCULATE_TOTAL_CREDIT_HOURS("Calculate Total Credit Hours"),
        DROP_COURSE("Drop Course"),
        VIEW_REGISTERED_COURSES("View Registered Courses"),
        VIEW_TIMETABLE("View Timetable"),
        VIEW_ASSIGNMENTS("View Assignments"),
        SUBMIT_ASSIGNMENT("Submit Assignment"),
        VIEW_ATTENDANCE("View Attendance"),
        SUBMIT_GENERIC_ACADEMIC_REQUEST("Submit Generic Academic Request");

        private final String label;

        Option(String label) {
            this.label = label;
        }
    }

    private static final String REQUEST_ID_PREFIX = "REQ-";
    private static final String PRIORITY_PROMPT = "Priority: ";
    private static final String PRIORITY_ERROR = "Enter a positive whole-number priority.";

    private final CliContext context;
    private Student student;

    StudentCliHandler(CliContext context) {
        this.context = context;
    }

    @Override
    public String roleName() {
        return "Normal Student";
    }

    @Override
    public List<String> menuOptions() {
        List<String> labels = new ArrayList<>();
        for (Option option : Option.values()) {
            labels.add(option.label);
        }
        return labels;
    }

    /** Selects a saved Normal Student by ID, offering to create the profile if it does not exist. */
    @Override
    public boolean signIn() throws IOException {
        student = null;
        String studentId = context.readRequiredText("Student ID: ");
        if (studentId == null) {
            return false;
        }
        for (NormalStudent candidate : context.normalStudents()) {
            if (candidate.getStudentId().equalsIgnoreCase(studentId)) {
                context.out().printf("Selected student %s.%n", candidate.getStudentId());
                student = candidate;
                return true;
            }
        }

        context.out().println("No saved student profile has that ID.");
        Boolean createProfile = context.readYesNo("Create a new student profile? (y/n): ");
        if (createProfile == null || !createProfile) {
            return false;
        }
        String name = context.readRequiredText("Name: ");
        if (name == null) {
            return false;
        }
        String email = context.readRequiredText("Email: ");
        if (email == null) {
            return false;
        }
        String phone = context.readRequiredText("Phone: ");
        if (phone == null) {
            return false;
        }

        student = new NormalStudent(name, email, phone, studentId, 0);
        context.students().add(student);
        if (context.saveCatalog("student.create")) {
            ApplicationLogger.info("student.created id=" + studentId);
            context.out().printf("Student profile %s created.%n", studentId);
        }
        return true;
    }

    @Override
    public void handle(int option) throws IOException {
        switch (Option.values()[option - 1]) {
            case VIEW_ATTENDANCE_PERCENTAGE -> viewAttendancePercentage();
            case SUBMIT_COURSE_CLASH_REQUEST -> submitCourseClashRequest();
            case CHECK_SECTION_CLASH -> checkSectionClash();
            case VIEW_REQUESTS -> viewRequests();
            case VIEW_AVAILABLE_COURSES -> viewAvailableCourses();
            case VIEW_COURSE_CREDIT_HOURS -> viewCourseCreditHours();
            case REGISTER_COURSE -> registerCourse();
            case CALCULATE_TOTAL_CREDIT_HOURS -> context.out().printf(
                    "Total registered credit hours: %d%n", student.calculateTotalCreditHours());
            case DROP_COURSE -> dropCourse();
            case VIEW_REGISTERED_COURSES -> viewRegisteredCourses();
            case VIEW_TIMETABLE -> viewTimetable();
            case VIEW_ASSIGNMENTS -> viewAssignments();
            case SUBMIT_ASSIGNMENT -> submitAssignment();
            case VIEW_ATTENDANCE -> viewAttendance();
            case SUBMIT_GENERIC_ACADEMIC_REQUEST -> submitGenericAcademicRequest();
        }
    }

    // ---- Courses and registration ----

    private void viewAvailableCourses() {
        List<Course> courses = context.admin().viewCourses();
        if (courses.isEmpty()) {
            context.out().println("No courses are currently available.");
            return;
        }
        for (Course course : courses) {
            context.out().printf("%s | %s | %d credit hour(s)%n",
                    course.getCourseCode(), course.getTitle(), course.getCreditHours());
            for (Section section : course.getSections()) {
                context.out().printf("  Section %s | %d seat(s) available%n",
                        section.getSectionId(), section.getAvailableSeats());
            }
        }
    }

    private void viewCourseCreditHours() throws IOException {
        Course course = context.selectCourseByCode("Course code: ");
        if (course != null) {
            context.out().printf("%s has %d credit hour(s).%n",
                    course.getCourseCode(), course.getCreditHours());
        }
    }

    private void registerCourse() throws IOException {
        Course course = context.selectCourseByCode("Course code to register: ");
        if (course == null) {
            return;
        }
        Section section = context.selectSection(course.getSections(), "Select a section" + CliContext.CANCEL_SUFFIX);
        if (section == null) {
            return;
        }
        try {
            student.register(section);
        } catch (CourseFullException | CourseClashException | IllegalArgumentException exception) {
            ApplicationLogger.error("student.registration_rejected id=" + student.getStudentId(), exception);
            context.out().println("Registration failed: " + exception.getMessage());
            return;
        }
        if (context.saveCatalog("student.register")) {
            ApplicationLogger.info("student.registered id=" + student.getStudentId()
                    + " section=" + section.getSectionId());
            context.out().printf("Registered in section %s (%d total credit hour(s)).%n",
                    section.getSectionId(), student.calculateTotalCreditHours());
        }
    }

    private void dropCourse() throws IOException {
        Section section = context.selectSection(context.enrolledSections(student),
                "Select a section to drop" + CliContext.CANCEL_SUFFIX);
        if (section == null) {
            return;
        }
        student.drop(section);
        if (section.getEnrolledStudents().contains(student)) {
            context.out().printf("Student remains registered in section %s.%n", section.getSectionId());
            return;
        }
        if (context.saveCatalog("student.drop")) {
            ApplicationLogger.info("student.dropped id=" + student.getStudentId()
                    + " section=" + section.getSectionId());
            context.out().printf("Dropped section %s. Total credit hours: %d.%n",
                    section.getSectionId(), student.calculateTotalCreditHours());
        }
    }

    private void viewRegisteredCourses() {
        List<Course> courses = student.viewCourses();
        if (courses.isEmpty()) {
            context.out().println("Student has no registered courses.");
            return;
        }
        for (Course course : courses) {
            context.out().printf("%s | %s | %d credit hour(s)%n",
                    course.getCourseCode(), course.getTitle(), course.getCreditHours());
        }
    }

    private void viewTimetable() {
        List<Schedule> timetable = student.viewTimetable();
        if (timetable.isEmpty()) {
            context.out().println("Student has no scheduled courses.");
            return;
        }
        for (Schedule schedule : timetable) {
            context.out().println(schedule.getScheduleInfo());
        }
    }

    // ---- Attendance ----

    private void viewAttendance() {
        boolean found = false;
        for (Section section : context.sections()) {
            for (Attendance attendance : section.getAttendanceRecords()) {
                if (attendance.getStudent() == student) {
                    found = true;
                    context.out().printf("%s | %s | %s%n", section.getSectionId(),
                            attendance.getDate(), attendance.getStatus());
                }
            }
        }
        if (!found) {
            context.out().println("No attendance records were found for this student.");
        }
    }

    private void viewAttendancePercentage() throws IOException {
        Section section = context.selectSection(context.enrolledSections(student),
                "Select a section" + CliContext.CANCEL_SUFFIX);
        if (section == null) {
            return;
        }
        Instructor instructor = section.getInstructor();
        if (instructor == null) {
            context.out().println("Attendance percentage is unavailable because no instructor is assigned.");
            return;
        }
        context.out().printf("Attendance percentage: %.2f%%%n",
                instructor.calculateAttendancePercentage(student, section));
    }

    // ---- Assignments ----

    /** Assignments for every section the student is enrolled in, earliest deadline first. */
    private List<Assignment> studentAssignments() {
        List<Assignment> assignments = new ArrayList<>();
        for (TeachingAssistant assistant : context.teachingAssistants()) {
            for (Assignment assignment : assistant.getCreatedAssignments()) {
                if (assignment.getSection().getEnrolledStudents().contains(student)) {
                    assignments.add(assignment);
                }
            }
        }
        assignments.sort(new AssignmentDeadlineComparator());
        return assignments;
    }

    private void viewAssignments() {
        List<Assignment> assignments = studentAssignments();
        if (assignments.isEmpty()) {
            context.out().println("No assignments are available for the student's registered sections.");
            return;
        }
        for (Assignment assignment : assignments) {
            context.out().printf("%s | %s | due %s | %.2f marks%n", assignment.getId(),
                    assignment.getTitle(), assignment.getDeadline(), assignment.getTotalMarks());
        }
    }

    private void submitAssignment() throws IOException {
        Assignment assignment = context.choose(studentAssignments(), "No assignments are available.",
                "Select an assignment" + CliContext.CANCEL_SUFFIX,
                item -> item.getId() + " | " + item.getTitle() + " | due " + item.getDeadline());
        if (assignment == null) {
            return;
        }
        for (Submission existing : assignment.getSubmissions()) {
            if (existing.getStudent() == student) {
                context.out().println("A submission already exists for this assignment.");
                return;
            }
        }
        String content = context.readRequiredText("Submission content: ");
        if (content == null) {
            return;
        }
        Submission submission = student.submitAssignment(assignment, content);
        if (context.saveCatalog("assignment.submit")) {
            ApplicationLogger.info("assignment.submitted student=" + student.getStudentId()
                    + " assignment=" + assignment.getId() + " status=" + submission.getStatus());
            context.out().printf("Assignment %s submitted with status %s.%n",
                    assignment.getId(), submission.getStatus());
        }
    }

    // ---- Requests ----

    private void checkSectionClash() throws IOException {
        Section first = context.selectSection(context.sections(), "Select the first section" + CliContext.CANCEL_SUFFIX);
        if (first == null) {
            return;
        }
        Section second = context.selectSection(context.sections(), "Select the second section" + CliContext.CANCEL_SUFFIX);
        if (second == null) {
            return;
        }
        context.out().println(first.hasClash(second) ? "The sections have a schedule clash."
                : "The sections do not have a schedule clash.");
    }

    private void viewRequests() {
        List<Request> studentRequests = new ArrayList<>();
        for (Request request : context.admin().viewRequests()) {
            if (request.getStudent() == student) {
                studentRequests.add(request);
            }
        }
        if (studentRequests.isEmpty()) {
            context.out().println("Student has no academic requests.");
            return;
        }
        studentRequests.sort(new RequestDateComparator());
        for (Request request : studentRequests) {
            context.out().println(request.getDetails());
        }
    }

    private void submitCourseClashRequest() throws IOException {
        Section conflictingSection = context.selectSection(context.enrolledSections(student),
                "Select the conflicting registered section" + CliContext.CANCEL_SUFFIX);
        if (conflictingSection == null) {
            return;
        }
        String requestedId = context.readRequiredText("Requested section ID: ");
        if (requestedId == null) {
            return;
        }
        Section requestedSection = context.findSection(requestedId);
        if (requestedSection == null) {
            context.out().printf("No section found with ID %s.%n", requestedId);
            return;
        }
        if (!conflictingSection.hasClash(requestedSection)) {
            context.out().println("No schedule clash exists; no request was submitted.");
            return;
        }
        String description = context.readRequiredText("Request description: ");
        if (description == null) {
            return;
        }
        Integer priority = context.readPositiveInteger(PRIORITY_PROMPT, PRIORITY_ERROR);
        if (priority == null) {
            return;
        }

        CourseClashRequest request = new CourseClashRequest(newRequestId(), LocalDate.now(), description,
                priority, student, conflictingSection, requestedSection);
        student.submitCourseClashRequest(request);
        if (queueRequest(request, "request.submit")) {
            ApplicationLogger.info("request.submitted id=" + request.getRequestId()
                    + " student=" + student.getStudentId());
            context.out().printf("Course clash request %s submitted.%n", request.getRequestId());
        }
    }

    private void submitGenericAcademicRequest() throws IOException {
        context.out().println("1. Professor concern");
        context.out().println("2. Classmate concern");
        context.out().println("3. Other academic concern");
        Integer categoryChoice = context.readChoice("Select request category" + CliContext.CANCEL_SUFFIX, 0, 3);
        if (categoryChoice == null || categoryChoice == 0) {
            return;
        }
        RequestCategory category = switch (categoryChoice) {
            case 1 -> RequestCategory.PROFESSOR;
            case 2 -> RequestCategory.CLASSMATE;
            default -> RequestCategory.OTHER;
        };
        String description = context.readRequiredText("Request description: ");
        if (description == null) {
            return;
        }
        Integer priority = context.readPositiveInteger(PRIORITY_PROMPT, PRIORITY_ERROR);
        if (priority == null) {
            return;
        }

        GenericRequest request = new GenericRequest(newRequestId(), LocalDate.now(), description,
                priority, student, category);
        request.submit();
        if (queueRequest(request, "request.generic_submit")) {
            ApplicationLogger.info("request.generic_submitted id=" + request.getRequestId()
                    + " category=" + category);
            context.out().printf("Academic request %s submitted.%n", request.getRequestId());
        }
    }

    /** Hands the request to the Academic Office and saves; false if either step failed. */
    private boolean queueRequest(Request request, String operation) {
        try {
            context.admin().addRequest(request);
        } catch (InvalidRequestException exception) {
            ApplicationLogger.error(operation + "_failed student=" + student.getStudentId(), exception);
            context.out().println("Request was not submitted: " + exception.getMessage());
            return false;
        }
        return context.saveCatalog(operation);
    }

    private static String newRequestId() {
        return REQUEST_ID_PREFIX + UUID.randomUUID();
    }
}

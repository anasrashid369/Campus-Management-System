import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Instructor workflows. Both instructor types share the attendance options; a Permanent Instructor
 * additionally gets TA assignment and FYP supervision, which are never shown to a Visiting Instructor.
 */
final class InstructorCliHandler implements RoleCliHandler {
    private enum Option {
        VIEW_ASSIGNED_COURSES("View Assigned Courses", false),
        VIEW_ASSIGNED_SECTIONS("View Assigned Sections", false),
        VIEW_ENROLLED_STUDENTS("View Enrolled Students", false),
        MARK_ATTENDANCE("Mark Attendance", false),
        MARK_PRESENT("Mark Present", false),
        MARK_ABSENT("Mark Absent", false),
        MARK_LATE("Mark Late", false),
        UPDATE_ATTENDANCE("Update Attendance", false),
        CALCULATE_ATTENDANCE_PERCENTAGE("Calculate Attendance Percentage", false),
        // Permanent-only options must stay after the shared ones so both menus number identically.
        ASSIGN_TEACHING_ASSISTANT("Assign Teaching Assistant", true),
        VIEW_FYP_GROUPS("View FYP Groups", true),
        VIEW_FYP_GROUP_DETAILS("View FYP Group Details", true),
        VIEW_FYP_MEMBERS("View FYP Members", true),
        SCHEDULE_FYP_MEETING("Schedule FYP Meeting", true),
        EVALUATE_FYP_IDEA("Evaluate FYP Idea", true),
        PROVIDE_FYP_FEEDBACK("Provide FYP Feedback", true),
        CREATE_FYP_GROUP("Create FYP Group", true),
        UPDATE_FYP_MEETING_NOTES("Update FYP Meeting Notes", true);

        private final String label;
        private final boolean permanentOnly;

        Option(String label, boolean permanentOnly) {
            this.label = label;
            this.permanentOnly = permanentOnly;
        }
    }

    private static final String PERMANENT_ROLE = "Permanent Instructor";
    private static final String VISITING_ROLE = "Visiting Instructor";
    private static final String SELECT_SECTION_PROMPT = "Select a section" + CliContext.CANCEL_SUFFIX;

    private final CliContext context;
    private final String role;
    private final List<Option> options = new ArrayList<>();
    private final FypCliWorkflow fypWorkflow;
    private Instructor instructor;

    private InstructorCliHandler(CliContext context, String role, boolean permanent) {
        this.context = context;
        this.role = role;
        this.fypWorkflow = new FypCliWorkflow(context);
        for (Option option : Option.values()) {
            if (permanent || !option.permanentOnly) {
                options.add(option);
            }
        }
    }

    static InstructorCliHandler permanent(CliContext context) {
        return new InstructorCliHandler(context, PERMANENT_ROLE, true);
    }

    static InstructorCliHandler visiting(CliContext context) {
        return new InstructorCliHandler(context, VISITING_ROLE, false);
    }

    @Override
    public String roleName() {
        return role;
    }

    @Override
    public List<String> menuOptions() {
        List<String> labels = new ArrayList<>();
        for (Option option : options) {
            labels.add(option.label);
        }
        return labels;
    }

    @Override
    public boolean signIn() throws IOException {
        instructor = null;
        List<Instructor> candidates = new ArrayList<>();
        for (Instructor candidate : context.instructors()) {
            if (candidate.getRole().equals(role)) {
                candidates.add(candidate);
            }
        }
        String roleText = role.toLowerCase(Locale.ROOT);
        if (candidates.isEmpty()) {
            context.out().printf("No %s has been assigned to a section yet.%n", roleText);
            return false;
        }
        for (Instructor candidate : candidates) {
            context.out().printf("%s | %s%n", candidate.getTeacherId(), candidate.getName());
        }
        String teacherId = context.readRequiredText("Instructor ID: ");
        if (teacherId == null) {
            return false;
        }
        for (Instructor candidate : candidates) {
            if (candidate.getTeacherId().equalsIgnoreCase(teacherId)) {
                instructor = candidate;
                return true;
            }
        }
        context.out().printf("No assigned %s found with ID %s.%n", roleText, teacherId);
        return false;
    }

    @Override
    public void handle(int option) throws IOException {
        Option selected = options.get(option - 1);
        if (selected.permanentOnly) {
            handlePermanentOnly(selected, (PermanentInstructor) instructor);
            return;
        }
        switch (selected) {
            case VIEW_ASSIGNED_COURSES -> viewCourses();
            case VIEW_ASSIGNED_SECTIONS -> viewSections();
            case VIEW_ENROLLED_STUDENTS -> viewEnrolledStudents();
            case MARK_ATTENDANCE -> {
                AttendanceStatus status = context.readAttendanceStatus("Select attendance status: ");
                if (status != null) {
                    recordAttendance(status);
                }
            }
            case MARK_PRESENT -> recordAttendance(AttendanceStatus.PRESENT);
            case MARK_ABSENT -> recordAttendance(AttendanceStatus.ABSENT);
            case MARK_LATE -> recordAttendance(AttendanceStatus.LATE);
            case UPDATE_ATTENDANCE -> updateAttendance();
            case CALCULATE_ATTENDANCE_PERCENTAGE -> calculateAttendancePercentage();
            default -> throw new IllegalStateException("Unhandled instructor option " + selected);
        }
    }

    private void handlePermanentOnly(Option selected, PermanentInstructor permanent) throws IOException {
        switch (selected) {
            case ASSIGN_TEACHING_ASSISTANT -> assignTeachingAssistant(permanent);
            case VIEW_FYP_GROUPS -> fypWorkflow.viewGroups(permanent);
            case VIEW_FYP_GROUP_DETAILS -> fypWorkflow.viewGroupDetails(permanent);
            case VIEW_FYP_MEMBERS -> fypWorkflow.viewMembers(permanent);
            case SCHEDULE_FYP_MEETING -> fypWorkflow.scheduleMeeting(permanent);
            case EVALUATE_FYP_IDEA -> fypWorkflow.evaluateIdea(permanent);
            case PROVIDE_FYP_FEEDBACK -> fypWorkflow.provideFeedback(permanent);
            case CREATE_FYP_GROUP -> fypWorkflow.createGroup(permanent);
            case UPDATE_FYP_MEETING_NOTES -> fypWorkflow.updateMeetingNotes(permanent);
            default -> throw new IllegalStateException("Unhandled permanent instructor option " + selected);
        }
    }

    // ---- Courses, sections, and students ----

    private void viewCourses() {
        List<Course> courses = instructor.viewCourses();
        if (courses.isEmpty()) {
            context.out().println("Instructor has no assigned courses.");
            return;
        }
        for (Course course : courses) {
            context.out().printf("%s | %s%n", course.getCourseCode(), course.getTitle());
        }
    }

    private void viewSections() {
        List<Section> sections = instructor.viewSections();
        if (sections.isEmpty()) {
            context.out().println("Instructor has no assigned sections.");
            return;
        }
        for (Section section : sections) {
            context.out().printf("%s | %s%n", section.getSectionId(), section.getCourse().getCourseCode());
        }
    }

    private void viewEnrolledStudents() throws IOException {
        Section section = context.selectSection(instructor.viewSections(), SELECT_SECTION_PROMPT);
        if (section == null) {
            return;
        }
        List<Student> students = instructor.viewEnrolledStudents(section);
        if (students.isEmpty()) {
            context.out().println("Section has no enrolled students.");
            return;
        }
        students.sort(new StudentNameComparator());
        for (Student student : students) {
            context.out().println(CliContext.describeStudent(student));
        }
    }

    // ---- Attendance ----

    private void recordAttendance(AttendanceStatus status) throws IOException {
        Section section = context.selectSection(instructor.viewSections(), SELECT_SECTION_PROMPT);
        if (section == null) {
            return;
        }
        Student student = context.selectSectionStudent(section);
        if (student == null) {
            return;
        }
        Attendance attendance = new Attendance(student, section, LocalDate.now(), status);
        instructor.markAttendance(attendance, status);
        if (context.saveCatalog("attendance.mark")) {
            ApplicationLogger.info("attendance.marked teacher=" + instructor.getTeacherId()
                    + " student=" + student.getStudentId() + " section=" + section.getSectionId()
                    + " status=" + status);
            context.out().printf("Attendance marked %s for %s in section %s.%n",
                    status, student.getStudentId(), section.getSectionId());
        }
    }

    private void updateAttendance() throws IOException {
        Section section = context.selectSection(instructor.viewSections(), SELECT_SECTION_PROMPT);
        if (section == null) {
            return;
        }
        Attendance record = context.choose(section.getAttendanceRecords(),
                "Section has no attendance records to update.",
                "Select an attendance record" + CliContext.CANCEL_SUFFIX,
                item -> item.getStudent().getStudentId() + " | " + item.getDate() + " | " + item.getStatus());
        if (record == null) {
            return;
        }
        AttendanceStatus status = context.readAttendanceStatus("Select new status: ");
        if (status == null) {
            return;
        }
        instructor.updateAttendance(record, status);
        if (context.saveCatalog("attendance.update")) {
            ApplicationLogger.info("attendance.updated teacher=" + instructor.getTeacherId()
                    + " student=" + record.getStudent().getStudentId()
                    + " section=" + section.getSectionId() + " status=" + status);
            context.out().printf("Attendance updated to %s.%n", status);
        }
    }

    private void calculateAttendancePercentage() throws IOException {
        Section section = context.selectSection(instructor.viewSections(), SELECT_SECTION_PROMPT);
        if (section == null) {
            return;
        }
        Student student = context.selectSectionStudent(section);
        if (student != null) {
            context.out().printf("Attendance percentage: %.2f%%%n",
                    instructor.calculateAttendancePercentage(student, section));
        }
    }

    // ---- Teaching assistants ----

    private void assignTeachingAssistant(PermanentInstructor permanent) throws IOException {
        Section section = context.selectSection(permanent.viewSections(), SELECT_SECTION_PROMPT);
        if (section == null) {
            return;
        }
        if (section.getTeachingAssistant() != null) {
            context.out().printf("Section %s already has an assigned teaching assistant.%n",
                    section.getSectionId());
            return;
        }
        NormalStudent student = context.selectNormalStudent("No Normal Student profiles are available to assign.");
        if (student == null) {
            return;
        }
        permanent.assignTA(student, section);
        TeachingAssistant assistant = section.getTeachingAssistant();
        if (assistant == null) {
            context.out().println("Teaching assistant assignment was not completed.");
            return;
        }
        context.students().add(assistant);
        if (context.saveCatalog("instructor.assign_ta")) {
            ApplicationLogger.info("instructor.ta_assigned teacher=" + permanent.getTeacherId()
                    + " student=" + student.getStudentId() + " section=" + section.getSectionId());
            context.out().printf("Student %s assigned as TA to section %s.%n",
                    student.getStudentId(), section.getSectionId());
        }
    }
}

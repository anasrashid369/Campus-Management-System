import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Teaching Assistant workflows: assignments, submissions, grading, and feedback. */
final class TeachingAssistantCliHandler implements RoleCliHandler {
    private enum Option {
        VIEW_ENROLLED_STUDENTS("View Enrolled Students"),
        CREATE_ASSIGNMENT("Create Assignment"),
        SET_TOTAL_MARKS("Set Total Marks"),
        SET_ASSIGNMENT_DEADLINE("Set Assignment Deadline"),
        VIEW_SUBMISSIONS("View Submissions"),
        CHECK_LATE_SUBMISSIONS("Check Late Submissions"),
        EVALUATE_SUBMISSION("Evaluate Submission"),
        ASSIGN_MARKS("Assign Marks"),
        GIVE_FEEDBACK("Give Feedback"),
        VIEW_ASSIGNED_SECTION("View Assigned Section");

        private final String label;

        Option(String label) {
            this.label = label;
        }
    }

    private final CliContext context;
    private TeachingAssistant assistant;

    TeachingAssistantCliHandler(CliContext context) {
        this.context = context;
    }

    @Override
    public String roleName() {
        return "Teaching Assistant";
    }

    @Override
    public List<String> menuOptions() {
        List<String> labels = new ArrayList<>();
        for (Option option : Option.values()) {
            labels.add(option.label);
        }
        return labels;
    }

    @Override
    public boolean signIn() throws IOException {
        assistant = null;
        List<TeachingAssistant> assistants = context.teachingAssistants();
        if (assistants.isEmpty()) {
            context.out().println("No teaching assistants have been assigned to sections yet.");
            return false;
        }
        for (TeachingAssistant candidate : assistants) {
            context.out().println(CliContext.describeStudent(candidate));
        }
        String studentId = context.readRequiredText("Teaching Assistant ID: ");
        if (studentId == null) {
            return false;
        }
        for (TeachingAssistant candidate : assistants) {
            if (candidate.getStudentId().equalsIgnoreCase(studentId)) {
                assistant = candidate;
                return true;
            }
        }
        context.out().printf("No Teaching Assistant found with ID %s.%n", studentId);
        return false;
    }

    @Override
    public void handle(int option) throws IOException {
        switch (Option.values()[option - 1]) {
            case VIEW_ENROLLED_STUDENTS -> viewEnrolledStudents();
            case CREATE_ASSIGNMENT -> createAssignment();
            case SET_TOTAL_MARKS -> updateTotalMarks();
            case SET_ASSIGNMENT_DEADLINE -> updateDeadline();
            case VIEW_SUBMISSIONS -> viewSubmissions();
            case CHECK_LATE_SUBMISSIONS -> viewLateSubmissions();
            case EVALUATE_SUBMISSION -> evaluateSubmission();
            case ASSIGN_MARKS -> assignSubmissionMarks();
            case GIVE_FEEDBACK -> provideSubmissionFeedback();
            case VIEW_ASSIGNED_SECTION -> viewAssignedSection();
        }
    }

    // ---- Section ----

    private void viewEnrolledStudents() {
        Section section = assistant.getAssignedSection();
        if (section == null || section.getEnrolledStudents().isEmpty()) {
            context.out().println("Assigned section has no enrolled students.");
            return;
        }
        List<Student> students = section.getEnrolledStudents();
        students.sort(new StudentNameComparator());
        for (Student student : students) {
            context.out().println(CliContext.describeStudent(student));
        }
    }

    private void viewAssignedSection() {
        Section section = assistant.getAssignedSection();
        if (section == null) {
            context.out().println("No section is assigned to this Teaching Assistant.");
            return;
        }
        context.out().printf("%s | %s | %d student(s)%n", section.getSectionId(),
                section.getCourse().getCourseCode(), section.getEnrolledStudents().size());
    }

    // ---- Assignments ----

    private void createAssignment() throws IOException {
        Section section = assistant.getAssignedSection();
        if (section == null) {
            context.out().println("Cannot create an assignment without an assigned section.");
            return;
        }
        String title = context.readRequiredText("Assignment title: ");
        if (title == null) {
            return;
        }
        String description = context.readRequiredText("Assignment description: ");
        if (description == null) {
            return;
        }
        LocalDate deadline = context.readLocalDate("Deadline (YYYY-MM-DD): ");
        if (deadline == null) {
            return;
        }
        Double totalMarks = context.readPositiveDouble("Total marks: ");
        if (totalMarks == null) {
            return;
        }
        // Mirrors the ID TeachingAssistant.createAssignment generates, so duplicates are rejected up front.
        String assignmentId = section.getCourse().getCourseCode() + "-" + title;
        for (Assignment existing : assistant.getCreatedAssignments()) {
            if (existing.getId().equalsIgnoreCase(assignmentId)) {
                context.out().printf("Assignment %s already exists.%n", assignmentId);
                return;
            }
        }
        Assignment assignment = assistant.createAssignment(title, description, deadline, totalMarks);
        if (context.saveCatalog("assignment.create")) {
            ApplicationLogger.info("assignment.created id=" + assignment.getId()
                    + " ta=" + assistant.getStudentId());
            context.out().printf("Assignment %s created for section %s.%n",
                    assignment.getId(), section.getSectionId());
        }
    }

    private void updateTotalMarks() throws IOException {
        Assignment assignment = selectOwnAssignment();
        if (assignment == null) {
            return;
        }
        Double totalMarks = context.readPositiveDouble("New total marks: ");
        if (totalMarks == null) {
            return;
        }
        for (Submission submission : assignment.getSubmissions()) {
            if (submission.getStatus() == SubmissionStatus.EVALUATED && submission.getMarks() > totalMarks) {
                context.out().println("Total marks cannot be lower than an evaluated submission score.");
                return;
            }
        }
        try {
            assignment.setTotalMarks(totalMarks);
            if (context.saveCatalog("assignment.total_marks")) {
                ApplicationLogger.info("assignment.total_marks_changed id=" + assignment.getId());
                context.out().printf("Total marks for %s set to %.2f.%n", assignment.getId(), totalMarks);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("assignment.total_marks_failed id=" + assignment.getId(), exception);
            context.out().println("Total marks were not changed: " + exception.getMessage());
        }
    }

    private void updateDeadline() throws IOException {
        Assignment assignment = selectOwnAssignment();
        if (assignment == null) {
            return;
        }
        LocalDate deadline = context.readLocalDate("New deadline (YYYY-MM-DD): ");
        if (deadline == null) {
            return;
        }
        try {
            assignment.setDeadline(deadline);
            if (context.saveCatalog("assignment.deadline")) {
                ApplicationLogger.info("assignment.deadline_changed id=" + assignment.getId());
                context.out().printf("Deadline for %s set to %s.%n", assignment.getId(), deadline);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("assignment.deadline_failed id=" + assignment.getId(), exception);
            context.out().println("Deadline was not changed: " + exception.getMessage());
        }
    }

    private Assignment selectOwnAssignment() throws IOException {
        return context.choose(assistant.getCreatedAssignments(),
                "This Teaching Assistant has not created assignments.",
                "Select an assignment" + CliContext.CANCEL_SUFFIX,
                assignment -> String.format("%s | deadline %s | %.2f marks",
                        assignment.getId(), assignment.getDeadline(), assignment.getTotalMarks()));
    }

    // ---- Submissions ----

    /** Lets the TA pick one of their assignments and then one of its submissions. */
    private Submission selectSubmission() throws IOException {
        Assignment assignment = selectOwnAssignment();
        if (assignment == null) {
            return null;
        }
        return context.choose(assignment.getSubmissions(), "No submissions are available for this assignment.",
                "Select a submission" + CliContext.CANCEL_SUFFIX,
                submission -> submission.getStudent().getStudentId() + " | " + submission.getStatus()
                        + " | " + submission.getSubmissionDate());
    }

    /** Submissions of an own assignment through the authorization check; null if denied or cancelled. */
    private List<Submission> viewableSubmissions(String deniedEvent) throws IOException {
        Assignment assignment = selectOwnAssignment();
        if (assignment == null) {
            return null;
        }
        try {
            return assistant.viewSubmissions(assignment);
        } catch (UnauthorizedActionException exception) {
            ApplicationLogger.error(deniedEvent + " ta=" + assistant.getStudentId(), exception);
            context.out().println(exception.getMessage());
            return null;
        }
    }

    private void viewSubmissions() throws IOException {
        List<Submission> submissions = viewableSubmissions("submission.view_denied");
        if (submissions == null) {
            return;
        }
        for (Submission submission : submissions) {
            context.out().printf("%s | %s | %s | marks %.2f%n", submission.getStudent().getStudentId(),
                    submission.getStatus(), submission.getSubmissionDate(), submission.getMarks());
            if (submission.getFeedback() != null) {
                context.out().println("  Feedback: " + submission.getFeedback().getComments());
            }
        }
    }

    private void viewLateSubmissions() throws IOException {
        List<Submission> submissions = viewableSubmissions("submission.late_view_denied");
        if (submissions == null) {
            return;
        }
        boolean found = false;
        for (Submission submission : submissions) {
            if (submission.isLate()) {
                found = true;
                context.out().printf("%s | %s%n", submission.getStudent().getStudentId(),
                        submission.getSubmissionDate());
            }
        }
        if (!found) {
            context.out().println("No late submissions were found.");
        }
    }

    /** Evaluate Submission includes Assign Marks and Give Feedback. */
    private void evaluateSubmission() throws IOException {
        Submission submission = selectSubmission();
        if (submission == null) {
            return;
        }
        Double marks = context.readNonNegativeDouble("Marks: ");
        if (marks == null) {
            return;
        }
        String comments = context.readRequiredText("Feedback: ");
        if (comments == null) {
            return;
        }
        if (assignMarks(submission, marks) && addFeedback(submission, comments)
                && context.saveCatalog("submission.evaluate")) {
            ApplicationLogger.info("submission.evaluated id=" + submission.getSubmissionId()
                    + " ta=" + assistant.getStudentId());
            context.out().printf("Submission evaluated with %.2f marks and feedback.%n", marks);
        }
    }

    private void assignSubmissionMarks() throws IOException {
        Submission submission = selectSubmission();
        if (submission == null) {
            return;
        }
        Double marks = context.readNonNegativeDouble("Marks: ");
        if (marks != null && assignMarks(submission, marks) && context.saveCatalog("submission.assign_marks")) {
            ApplicationLogger.info("submission.marks_assigned id=" + submission.getSubmissionId());
            context.out().printf("Marks assigned: %.2f.%n", marks);
        }
    }

    private void provideSubmissionFeedback() throws IOException {
        Submission submission = selectSubmission();
        if (submission == null) {
            return;
        }
        String comments = context.readRequiredText("Feedback: ");
        if (comments != null && addFeedback(submission, comments) && context.saveCatalog("submission.feedback")) {
            ApplicationLogger.info("submission.feedback_added id=" + submission.getSubmissionId());
            context.out().println("Feedback added.");
        }
    }

    private boolean assignMarks(Submission submission, double marks) {
        try {
            assistant.evaluateSubmission(submission, marks);
            return true;
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("submission.marks_rejected id=" + submission.getSubmissionId(), exception);
            context.out().println("Marks were not assigned: " + exception.getMessage());
        } catch (UnauthorizedActionException exception) {
            ApplicationLogger.error("submission.marks_denied id=" + submission.getSubmissionId(), exception);
            context.out().println(exception.getMessage());
        }
        return false;
    }

    private boolean addFeedback(Submission submission, String comments) {
        try {
            assistant.giveFeedback(submission, comments);
            return true;
        } catch (UnauthorizedActionException exception) {
            ApplicationLogger.error("submission.feedback_denied id=" + submission.getSubmissionId(), exception);
            context.out().println(exception.getMessage());
            return false;
        }
    }
}

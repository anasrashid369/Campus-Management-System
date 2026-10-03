import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TeachingAssistant extends Student implements Evaluator {

    private Section assignedSection;
    private List<Assignment> createdAssignments;

    TeachingAssistant(String name, String email, String phone, String studentId, int totalCreditHours) {
        super(name, email, phone, studentId, totalCreditHours);
        this.createdAssignments = new ArrayList<>();
    }

    public Section getAssignedSection() {
        return assignedSection;
    }

    public void setAssignedSection(Section assignedSection) {
        this.assignedSection = assignedSection;
    }

    public Assignment createAssignment(String title, String description, LocalDate deadline, double totalMarks) {
        String id = assignedSection.getCourse().getCourseCode() + "-" + title;
        Assignment assignment = new Assignment(id, title, description, deadline, totalMarks, assignedSection, this);
        createdAssignments.add(assignment);
        return assignment;
    }

    public List<Submission> viewSubmissions(Assignment assignment) throws UnauthorizedActionException {
        requireOwnAssignment(assignment);
        return assignment.getSubmissions();
    }

    public List<Assignment> getCreatedAssignments() {
        return new ArrayList<>(createdAssignments);
    }

    void restoreAssignment(Assignment assignment) {
        if (assignment != null && !createdAssignments.contains(assignment)) {
            createdAssignments.add(assignment);
        }
    }

    public void evaluateSubmission(Submission submission, double marks) throws UnauthorizedActionException {
        requireOwnSubmission(submission);
        submission.assignMarks(marks);
    }

    public void giveFeedback(Submission submission, String comments) throws UnauthorizedActionException {
        requireOwnSubmission(submission);
        String id = "FB-" + System.currentTimeMillis();
        submission.addFeedback(new Feedback(id, this, comments, LocalDate.now()));
    }

    private void requireOwnAssignment(Assignment assignment) throws UnauthorizedActionException {
        if (assignment == null || !createdAssignments.contains(assignment)) {
            throw new UnauthorizedActionException(
                    "Teaching Assistant is not authorized to access assignment " + (assignment == null
                            ? "(null)" : assignment.getId()));
        }
    }

    private void requireOwnSubmission(Submission submission) throws UnauthorizedActionException {
        if (submission == null || submission.getAssignment() == null) {
            throw new UnauthorizedActionException("Teaching Assistant is not authorized to access this submission");
        }
        requireOwnAssignment(submission.getAssignment());
    }

    // The diagram only gives evaluate() : void, so this reports what is waiting to be evaluated
    @Override
    public void evaluate() {
        int pending = 0;
        for (Assignment a : createdAssignments) {
            for (Submission s : a.getSubmissions()) {
                if (s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.LATE) {
                    pending++;
                }
            }
        }
        System.out.println(getName() + ": " + pending + " submission(s) awaiting evaluation");
    }

    @Override
    public String getRole() {
        return "Teaching Assistant";
    }
}
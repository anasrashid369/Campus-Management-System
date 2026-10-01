import java.time.LocalDate;
import java.util.List;

public class TeachingAssistant extends Student implements Evaluator {

    private Section assignedSection;

    TeachingAssistant(String name, String email, String phone, String studentId, int totalCreditHours) {
        super(name, email, phone, studentId, totalCreditHours);
    }

    public Section getAssignedSection() {
        return assignedSection;
    }

    public void setAssignedSection(Section assignedSection) {
        this.assignedSection = assignedSection;
    }

    public Assignment createAssignment(String title, String description, LocalDate deadline, double totalMarks) {
        String id = assignedSection.getCourse().getCourseCode() + "-" + title;
        return new Assignment(id, title, description, deadline, totalMarks, assignedSection, this);
    }

    public List<Submission> viewSubmissions(Assignment assignment) {
        return assignment.getSubmissions();
    }

    public void evaluateSubmission(Submission submission, double marks) {
        submission.assignMarks(marks);
    }

    public void giveFeedback(Submission submission, String comments) {
        String id = "FB-" + System.currentTimeMillis();
        submission.addFeedback(new Feedback(id, this, comments, LocalDate.now()));
    }

    @Override
    public void evaluate() {
        // TODO: implement later
    }

    @Override
    public String getRole() {
        return "Teaching Assistant";
    }
}
import java.time.LocalDate;

public class Submission {
    private String submissionId;
    private Assignment assignment;
    private Student student;
    private LocalDate submissionDate;
    private String content;
    private double marks;
    private Feedback feedback;
    private SubmissionStatus status;

    Submission(String submissionId, Assignment assignment, Student student, String content) {
        this.submissionId = submissionId;
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.status = SubmissionStatus.PENDING;
    }

    public void submit() {
        this.submissionDate = LocalDate.now();
        if (isLate()) {
            this.status = SubmissionStatus.LATE;
        } else {
            this.status = SubmissionStatus.SUBMITTED;
        }
    }

    public boolean isLate() {
        LocalDate date = (submissionDate != null) ? submissionDate : LocalDate.now();
        return date.isAfter(assignment.getDeadline());
    }

    public void assignMarks(double marks) {
        this.marks = marks;
        this.status = SubmissionStatus.EVALUATED;
    }

    public void addFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public double getMarks() {
        return marks;
    }

    public SubmissionStatus getStatus() {
        return status;
    }
}
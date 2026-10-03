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
        if (!Double.isFinite(marks) || marks < 0 || marks > assignment.getTotalMarks()) {
            throw new IllegalArgumentException("Marks must be between zero and the assignment total");
        }
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

    public String getSubmissionId() {
        return submissionId;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Student getStudent() {
        return student;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public String getContent() {
        return content;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    void restoreState(LocalDate submissionDate, String content, double marks,
                      SubmissionStatus status, Feedback feedback) {
        this.submissionDate = submissionDate;
        this.content = content;
        this.marks = marks;
        this.status = status;
        this.feedback = feedback;
    }
}
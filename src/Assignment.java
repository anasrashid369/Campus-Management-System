import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Assignment extends Assessment {
    private Section section;
    private TeachingAssistant createdBy;
    private List<Submission> submissions;

    Assignment(String id, String title, String description, LocalDate deadline, double totalMarks,
               Section section, TeachingAssistant createdBy) {
        super(id, title, description, deadline, totalMarks);
        this.section = section;
        this.createdBy = createdBy;
        this.submissions = new ArrayList<>();
    }

    public void addSubmission(Submission submission) {
        submissions.add(submission);
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }

    public Section getSection() {
        return section;
    }

    public TeachingAssistant getCreatedBy() {
        return createdBy;
    }

    public boolean isDeadlinePassed() {
        return LocalDate.now().isAfter(getDeadline());
    }
}
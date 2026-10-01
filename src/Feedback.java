import java.time.LocalDate;

public class Feedback {
    private String feedbackId;
    private Evaluator evaluator;
    private String comments;
    private LocalDate date;

    Feedback(String feedbackId, Evaluator evaluator, String comments, LocalDate date) {
        this.feedbackId = feedbackId;
        this.evaluator = evaluator;
        this.comments = comments;
        this.date = date;
    }

    public String getComments() {
        return comments;
    }
}
import java.time.LocalDate;

public class FYPEvaluation {
    private String evaluationId;
    private LocalDate evaluationDate;
    private double score;
    private String feedback;
    private Evaluator evaluator; // "evaluated by" association (1)

    public FYPEvaluation(String evaluationId, LocalDate evaluationDate, Evaluator evaluator) {
        this.evaluationId = evaluationId;
        this.evaluationDate = evaluationDate;
        this.evaluator = evaluator;
        this.score = 0;
        this.feedback = "";
    }

    public void evaluate(double score) throws InvalidFYPEvaluationException {
        if (!Double.isFinite(score) || score < 0) {
            throw new InvalidFYPEvaluationException("Score must be a non-negative finite number");
        }
        this.score = score;
    }

    public void addFeedback(String feedback) {
        this.feedback = feedback;
    }

    public double getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getEvaluationId() {
        return evaluationId;
    }

    public LocalDate getEvaluationDate() {
        return evaluationDate;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }
}
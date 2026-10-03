import java.time.LocalDate;

public abstract class Assessment {
    private String id;
    private String title;
    private String description;
    private LocalDate deadline;
    private double totalMarks;

    Assessment(String id, String title, String description, LocalDate deadline, double totalMarks) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.totalMarks = totalMarks;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline cannot be null");
        }
        this.deadline = deadline;
    }

    public void setTotalMarks(double totalMarks) {
        if (!Double.isFinite(totalMarks) || totalMarks <= 0) {
            throw new IllegalArgumentException("Total marks must be a positive finite number");
        }
        this.totalMarks = totalMarks;
    }
}
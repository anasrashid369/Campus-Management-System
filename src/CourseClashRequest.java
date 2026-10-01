import java.time.LocalDate;

public class CourseClashRequest extends Request {
    private Section conflictingSection;
    private Section requestedSection;

    CourseClashRequest(String requestId, LocalDate requestDate, String description, int priority,
                       Student student, Section conflictingSection, Section requestedSection) {
        super(requestId, requestDate, description, priority, student);
        this.conflictingSection = conflictingSection;
        this.requestedSection = requestedSection;
    }

    public String getConflictDetails() {
        return "Conflicting: " + conflictingSection.getCourse().getCourseCode()
                + " | Requested: " + requestedSection.getCourse().getCourseCode();
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | " + getConflictDetails();
    }
}
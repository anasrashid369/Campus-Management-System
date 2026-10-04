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
        Course conflictCourse = conflictingSection.getPrimaryCourse();
        Course requestedCourse = requestedSection.getPrimaryCourse();
        String conflictCode = conflictCourse != null ? conflictCourse.getCourseCode() : "Unknown";
        String requestedCode = requestedCourse != null ? requestedCourse.getCourseCode() : "Unknown";
        return "Conflicting: " + conflictCode + " | Requested: " + requestedCode;
    }

    public Section getConflictingSection() {
        return conflictingSection;
    }

    public Section getRequestedSection() {
        return requestedSection;
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | " + getConflictDetails();
    }
}
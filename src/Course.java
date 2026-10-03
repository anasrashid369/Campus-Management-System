import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an academic course within the Campus Management System.
 */
public class Course {
    private String courseCode;
    private String title;
    private int creditHours;
    private Set<Course> prerequisites;
    private List<Section> sections;

    public Course(String courseCode, String title, int creditHours) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be null or blank");
        }
        if (creditHours <= 0) {
            throw new IllegalArgumentException("Credit hours must be positive");
        }
        this.courseCode = courseCode;
        this.title = title;
        this.creditHours = creditHours;
        this.prerequisites = new HashSet<>();
        this.sections = new ArrayList<>();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public int getCreditHours() {
        return creditHours;
    }

    void updateDetails(String title, int creditHours) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (creditHours > 0) {
            this.creditHours = creditHours;
        }
    }

    public void addPrerequisite(Course course) {
        if (course != null && course != this) {
            prerequisites.add(course);
        }
    }

    public Set<Course> getPrerequisites() {
        return Set.copyOf(prerequisites);
    }

    public void addSection(Section section) {
        if (section != null && !sections.contains(section)) {
            sections.add(section);
        }
    }

    public List<Section> getSections() {
        return new ArrayList<>(sections);
    }
}
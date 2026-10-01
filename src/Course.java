import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Course {
    private String courseCode;
    private String title;
    private int creditHours;
    private Set<Course> prerequisites;
    private List<Section> sections;

    Course(String courseCode, String title, int creditHours) {
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

    public void addPrerequisite(Course course) {
        prerequisites.add(course);
    }

    public void addSection(Section section) {
        sections.add(section);
    }

    public List<Section> getSections() {
        return sections;
    }
}
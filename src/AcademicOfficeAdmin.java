import java.util.ArrayList;
import java.util.List;

public class AcademicOfficeAdmin extends Administrator {

    private List<Course> courses;
    private List<Section> sections;
    private List<Request> requests;

    AcademicOfficeAdmin(String name, String email, String phone, String adminId) {
        super(name, email, phone, adminId);
        this.courses = new ArrayList<>();
        this.sections = new ArrayList<>();
        this.requests = new ArrayList<>();
    }

    // ---- Courses ----
    public void createCourse(Course course) {
        if (course != null && searchCourse(course.getCourseCode()) == null) {
            courses.add(course);
        }
    }

    // Replaces the stored course that has the same course code
    public void updateCourse(Course course) {
        for (Course existingCourse : courses) {
            if (existingCourse.getCourseCode().equalsIgnoreCase(course.getCourseCode())) {
                existingCourse.updateDetails(course.getTitle(), course.getCreditHours());
                return;
            }
        }
    }

    // Returns null if no course has this code
    public Course searchCourse(String courseCode) {
        for (Course c : courses) {
            if (c.getCourseCode().equalsIgnoreCase(courseCode)) {
                return c;
            }
        }
        return null;
    }

    // ---- Sections ----
    public void createSection(Section section) {
        if (section == null || sections.contains(section)) {
            return;
        }
        sections.add(section);
        section.getCourse().addSection(section);
    }

    // Replaces the stored section that has the same section id
    public void updateSection(Section section) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getSectionId().equals(section.getSectionId())) {
                Section old = sections.set(i, section);
                List<Section> courseSections = old.getCourse().getSections();
                int index = courseSections.indexOf(old);
                if (index >= 0) {
                    courseSections.set(index, section);
                }
                return;
            }
        }
    }

    public void setCapacity(Section section, int capacity) {
        section.setCapacity(capacity);
    }

    public void assignRoom(Section section, Schedule schedule) {
        section.setSchedule(schedule);
    }

    public void assignInstructor(Section section, Instructor instructor) {
        section.assignInstructor(instructor);
    }

    // ---- Requests ----
    // Added: students don't know the admin, so requests are handed to the office here
    public void addRequest(Request request) {
        if (request != null && !requests.contains(request)) {
            requests.add(request);
        }
    }

    public List<Request> viewRequests() {
        return new ArrayList<>(requests);
    }

    public void approveRequest(Request request) {
        process(request, RequestStatus.APPROVED);
    }

    public void rejectRequest(Request request) {
        process(request, RequestStatus.REJECTED);
    }

    private void process(Request request, RequestStatus newStatus) {
        if (request.getStatus() != RequestStatus.PENDING) {
            return; // already processed
        }
        request.setStatus(newStatus);
        request.setProcessedBy(this);
    }

    @Override
    public String getRole() {
        return "Academic Office Admin";
    }
}
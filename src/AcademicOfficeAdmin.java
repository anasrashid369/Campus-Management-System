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

    public List<Course> viewCourses() {
        return new ArrayList<>(courses);
    }

    public List<Section> viewSections() {
        return new ArrayList<>(sections);
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
    public void addRequest(Request request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request cannot be null");
        }
        for (Request existing : requests) {
            if (existing.getRequestId().equalsIgnoreCase(request.getRequestId())) {
                throw new InvalidRequestException("Request ID already exists: " + request.getRequestId());
            }
        }
        requests.add(request);
    }

    public List<Request> viewRequests() {
        return new ArrayList<>(requests);
    }

    public void approveRequest(Request request) throws InvalidRequestException {
        process(request, RequestStatus.APPROVED);
    }

    public void rejectRequest(Request request) throws InvalidRequestException {
        process(request, RequestStatus.REJECTED);
    }

    private void process(Request request, RequestStatus newStatus) throws InvalidRequestException {
        if (request == null || !requests.contains(request)) {
            throw new InvalidRequestException("Request is not in this admin's queue");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidRequestException("Request " + request.getRequestId() + " is not pending");
        }
        request.setStatus(newStatus);
        request.setProcessedBy(this);
    }

    @Override
    public String getRole() {
        return "Academic Office Admin";
    }
}
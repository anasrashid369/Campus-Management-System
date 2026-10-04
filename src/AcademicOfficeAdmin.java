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
    }

    public void assignCourseToSection(Section section, Course course) throws IllegalArgumentException {
        if (section == null || course == null) {
            throw new IllegalArgumentException("Section and Course cannot be null");
        }
        if (!sections.contains(section)) {
            throw new IllegalArgumentException("Section does not exist");
        }
        if (!courses.contains(course)) {
            throw new IllegalArgumentException("Course does not exist");
        }
        if (section.getCourses().contains(course)) {
            throw new IllegalArgumentException("Course is already assigned to this section");
        }
        section.addCourse(course);
    }

    public void updateCourseInSection(Section section, Course course, String newTitle, int newCreditHours) throws IllegalArgumentException {
        if (section == null || course == null) {
            throw new IllegalArgumentException("Section and Course cannot be null");
        }
        if (!section.getCourses().contains(course)) {
            throw new IllegalArgumentException("Course is not assigned to this section");
        }
        course.updateDetails(newTitle, newCreditHours);
    }

    public void removeCourseFromSection(Section section, Course course) throws IllegalArgumentException {
        if (section == null || course == null) {
            throw new IllegalArgumentException("Section and Course cannot be null");
        }
        if (!section.getCourses().contains(course)) {
            throw new IllegalArgumentException("Course is not assigned to this section");
        }
        section.removeCourse(course);
    }

    public void deleteSection(Section section) throws IllegalArgumentException {
        if (section == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }
        if (!sections.contains(section)) {
            throw new IllegalArgumentException("Section does not exist");
        }
        for (Course course : section.getCourses()) {
            section.removeCourse(course);
        }
        sections.remove(section);
    }

    // Replaces the stored section that has the same section id
    public void updateSection(Section section) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getSectionId().equals(section.getSectionId())) {
                Section old = sections.set(i, section);
                for (Course course : old.getCourses()) {
                    section.addCourse(course);
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
        if (request instanceof CourseClashRequest clashRequest) {
            applyApprovedCourseClash(clashRequest);
        }
    }

    public void rejectRequest(Request request) throws InvalidRequestException {
        process(request, RequestStatus.REJECTED);
    }

    private void applyApprovedCourseClash(CourseClashRequest request) throws InvalidRequestException {
        Student student = request.getStudent();
        Section conflicting = request.getConflictingSection();
        Section requested = request.getRequestedSection();
        if (student == null || conflicting == null || requested == null) {
            throw new InvalidRequestException("Course clash request is missing section or student details");
        }
        try {
            if (conflicting.getEnrolledStudents().contains(student)) {
                student.drop(conflicting);
            }
            student.register(requested);
        } catch (CourseFullException | CourseClashException exception) {
            throw new InvalidRequestException(
                    "Approved clash request could not be applied: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "Approved clash request could not be applied: " + exception.getMessage());
        }
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
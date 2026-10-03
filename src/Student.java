import java.util.List;
import java.util.ArrayList;


public abstract class Student extends Person{
    private String studentId;
    private int totalCreditHours;
    private List<Enrollment> enrollments;


    // Constructor
    Student(String name,String email,String phone,String studentId,int totalCreditHours){
        super(name,email,phone);
        this.studentId = studentId;
        this.totalCreditHours = totalCreditHours;
        this.enrollments = new ArrayList<>();
    }

    //Methods
    public void setStudentId(String studentId){
        this.studentId = studentId;
    }


    public void setTotalCreditHours(int totalCreditHours){
        this.totalCreditHours = totalCreditHours;
    }


    public String getStudentId(){
        return studentId;
    }


    public void register(Section section) throws CourseFullException, CourseClashException {
        if (section == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }
        if (isEnrolledIn(section)) {
            throw new IllegalArgumentException("Student is already enrolled in section "
                    + section.getSectionId());
        }
        for (Course prerequisite : section.getCourse().getPrerequisites()) {
            if (!viewCourses().contains(prerequisite)) {
                throw new IllegalArgumentException("Register for prerequisite "
                        + prerequisite.getCourseCode() + " first");
            }
        }
        for (Enrollment e : enrollments) {
            if (e.getSection().hasClash(section)) {
                throw new CourseClashException("Section " + section.getSectionId()
                        + " clashes with registered section " + e.getSection().getSectionId());
            }
        }
        section.enroll(this);
        totalCreditHours += section.getCourse().getCreditHours();
    }


    public void drop(Section section){
        if (section == null || !isEnrolledIn(section)) {
            return;
        }
        section.drop(this);
        totalCreditHours -= section.getCourse().getCreditHours();
    }

    public int calculateTotalCreditHours(){
        return totalCreditHours;
    }

    public List<Course> viewCourses(){
        List<Course> courses = new ArrayList<>();
        for (Enrollment e : enrollments) {
            courses.add(e.getCourse());
        }
        return courses;
    }

    public List<Schedule> viewTimetable(){
        List<Schedule> timetable = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Schedule schedule = e.getSection().getSchedule();
            if (schedule != null) {
                timetable.add(schedule);
            }
        }
        return timetable;
    }
    public Submission submitAssignment(Assignment assignment, String content){
        Submission submission = new Submission(
                "SUB-" + studentId + "-" + assignment.getId(), assignment, this, content);
        submission.submit();
        assignment.addSubmission(submission);
        return submission;
    }

    public void submitCourseClashRequest(CourseClashRequest request){
        request.submit();
    }

    // Added: called by Section.enroll / Section.drop to keep both sides in sync
    void addEnrollment(Enrollment enrollment){
        enrollments.add(enrollment);
    }

    void removeEnrollment(Enrollment enrollment){
        enrollments.remove(enrollment);
    }

    private boolean isEnrolledIn(Section section){
        for (Enrollment e : enrollments) {
            if (e.getSection() == section) {
                return true;
            }
        }
        return false;
    }
}
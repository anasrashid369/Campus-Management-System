import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Section {
    private String sectionId;
    private int capacity;
    private Course course;
    private Instructor instructor;
    private TeachingAssistant teachingAssistant;
    private Schedule schedule;
    private List<Enrollment> enrollments;

    Section(String sectionId, int capacity, Course course) {
        this.sectionId = sectionId;
        this.capacity = capacity;
        this.course = course;
        this.enrollments = new ArrayList<>();
    }

    public void enroll(Student student) {
        if (isFull()) {
            return;
        }
        String id = sectionId + "-" + student.getStudentId();
        enrollments.add(new Enrollment(id, student, this, LocalDate.now()));
    }

    public void drop(Student student) {
        for (Enrollment e : enrollments) {
            if (e.getStudent().equals(student)) {
                e.cancel();
                enrollments.remove(e);
                return;
            }
        }
    }

    public boolean isFull() {
        return enrollments.size() >= capacity;
    }

    public int getAvailableSeats() {
        return capacity - enrollments.size();
    }

    public void assignInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public void assignTA(TeachingAssistant ta) {
        this.teachingAssistant = ta;
    }

    public List<Student> getEnrolledStudents() {
        List<Student> students = new ArrayList<>();
        for (Enrollment e : enrollments) {
            students.add(e.getStudent());
        }
        return students;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public boolean hasClash(Section section) {
        return schedule.hasClash(section.getSchedule());
    }

    // Added: used by Instructor and Enrollment
    public Course getCourse() {
        return course;
    }
}
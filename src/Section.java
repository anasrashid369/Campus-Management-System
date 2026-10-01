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
    private List<Attendance> attendanceRecords; // Section 1 --- 0..* Attendance

    Section(String sectionId, int capacity, Course course) {
        this.sectionId = sectionId;
        this.capacity = capacity;
        this.course = course;
        this.enrollments = new ArrayList<>();
        this.attendanceRecords = new ArrayList<>();
    }

    public void enroll(Student student) {
        if (isFull() || getEnrolledStudents().contains(student)) {
            return;
        }
        String id = sectionId + "-" + student.getStudentId();
        Enrollment enrollment = new Enrollment(id, student, this, LocalDate.now());
        enrollments.add(enrollment);
        student.addEnrollment(enrollment);
    }

    public void drop(Student student) {
        for (Enrollment e : enrollments) {
            if (e.getStudent().equals(student)) {
                e.cancel();
                enrollments.remove(e);
                student.removeEnrollment(e);
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
        if (this.instructor != null && this.instructor != instructor) {
            this.instructor.removeSection(this);
        }
        this.instructor = instructor;
        if (instructor != null) {
            instructor.addSection(this);
        }
    }

    public void assignTA(TeachingAssistant ta) {
        if (this.teachingAssistant != null && this.teachingAssistant != ta) {
            this.teachingAssistant.setAssignedSection(null);
        }
        this.teachingAssistant = ta;
        if (ta != null) {
            ta.setAssignedSection(this);
        }
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
        if (schedule == null || section.getSchedule() == null) {
            return false; // nothing scheduled yet, so no clash
        }
        return schedule.hasClash(section.getSchedule());
    }

    // Added: used by Instructor and Enrollment
    public Course getCourse() {
        return course;
    }

    // Added: fills the "schedule is always null" gap (used by AcademicOfficeAdmin.assignRoom)
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    // Added: used by AcademicOfficeAdmin.setCapacity
    public void setCapacity(int capacity) {
        if (capacity < enrollments.size()) {
            throw new IllegalArgumentException(
                    "Capacity cannot be less than current enrollment (" + enrollments.size() + ")");
        }
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getSectionId() {
        return sectionId;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public TeachingAssistant getTeachingAssistant() {
        return teachingAssistant;
    }

    // Added: attendance records live in the section (needed for calculateAttendancePercentage)
    public void addAttendance(Attendance attendance) {
        if (attendance != null && !attendanceRecords.contains(attendance)) {
            attendanceRecords.add(attendance);
        }
    }

    public List<Attendance> getAttendanceRecords() {
        return new ArrayList<>(attendanceRecords);
    }
}
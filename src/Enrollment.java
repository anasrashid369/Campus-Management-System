import java.time.LocalDate;

public class Enrollment {
    private String enrollmentId;
    private Student student;
    private Section section;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;

    Enrollment(String enrollmentId, Student student, Section section, LocalDate enrollmentDate) {
        this.enrollmentId = enrollmentId;
        this.student = student;
        this.section = section;
        this.enrollmentDate = enrollmentDate;
        this.status = EnrollmentStatus.ACTIVE;
    }

    public void cancel() {
        this.status = EnrollmentStatus.DROPPED;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public Student getStudent() {
        return student;
    }

    public Section getSection() {
        return section;
    }

    public Course getCourse() {
        return section.getPrimaryCourse();
    }
}
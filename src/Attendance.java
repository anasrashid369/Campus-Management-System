import java.time.LocalDate;

public class Attendance {
    private Student student;
    private Section section;
    private LocalDate date;
    private AttendanceStatus status;

    Attendance(Student student, Section section, LocalDate date, AttendanceStatus status) {
        this.student = student;
        this.section = section;
        this.date = date;
        this.status = status;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
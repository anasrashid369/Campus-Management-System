import java.util.List;
import java.util.ArrayList;

public abstract class Instructor extends Person{

    String teacherId;
    private List<Section> assignedSections;

    //Constructor
    Instructor(String name,String email,String phone,String teacherId){
        super(name,email,phone);
        this.teacherId = teacherId;
        this.assignedSections = new ArrayList<>();
    }

    //Methods
    public String getTeacherId(){
        return teacherId;
    }

    public List<Course> viewCourses(){
        List<Course> courses = new ArrayList<>();
        for (Section s : assignedSections) {
            for (Course c : s.getCourses()) {
                if (!courses.contains(c)) {
                    courses.add(c);
                }
            }
        }
        return courses;
    }

    public List<Section> viewSections(){
        return assignedSections;
    }

    public List<Student> viewEnrolledStudents(Section section){
        return section.getEnrolledStudents();
    }

    public void markAttendance(Attendance attendance,AttendanceStatus status){
        attendance.setStatus(status);
        attendance.getSection().addAttendance(attendance); // record it in the section (ignored if already there)
    }

    public void updateAttendance(Attendance attendance,AttendanceStatus status){
        attendance.setStatus(status);
    }

    public double calculateAttendancePercentage(Student student,Section section){
        int total = 0;
        int attended = 0;
        for (Attendance a : section.getAttendanceRecords()) {
            if (a.getStudent().equals(student)) {
                total++;
                if (a.getStatus() != AttendanceStatus.ABSENT) { // PRESENT and LATE both count as attended
                    attended++;
                }
            }
        }
        return total == 0 ? 0.0 : (attended * 100.0) / total;
    }

    // Added: so sections can be assigned to an instructor (called by Section.assignInstructor)
    public void addSection(Section section){
        if (!assignedSections.contains(section)) {
            assignedSections.add(section);
        }
    }

    public void removeSection(Section section){
        assignedSections.remove(section);
    }
}
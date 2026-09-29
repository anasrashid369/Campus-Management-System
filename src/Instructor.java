import java.util.List;
import java.util.ArrayList;


public abstract class Instructor extends Person{

    String teacherId;
    private List<Section> assignedSections;

    //Constructor
    Instructor(String name,String email,String phone,String teacherId){
        super(name,email,phone);
        this.teacherId = teacherId;
    }

    //Methods
    public String getTeacherId(){
        return teacherId;
    }
    public List<Course> viewCourses(){
        List<Course> courses = new ArrayList<>();
        for (Section s : assignedSections) {
            Course c = s.getCourse();
            if (!courses.contains(c)) {
                courses.add(c);
            }
        }
        return courses;
    }


    public List<Student> viewEnrolledStudents(Section section){
        return section.getEnrolledStudents();
    }

    public void markAttendance(Attendance attendance,AttendanceStatus status){

        // TODO: implement later
    }

    public void updateAttendance(Attendance attendance,AttendanceStatus status){

        // TODO: implement later
    }

    public double calculateAttendancePercentage(Student student,Section section){

        // TODO: implement later
        return 0.0;
    }



}


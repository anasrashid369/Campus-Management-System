import java.util.List;
import java.util.ArrayList;


public class Student extends Person{
    private String studentId;
    private int totalCreditHours;
    private List<Enrollement> enrollements;


    // Constructor
    Student(String name,String email,String phone,String studentId,int totalCreditHours){
        super(name,email,phone);
        this.studentId = studentId;
        this.totalCreditHours = totalCreditHours;

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


    public void register(Section section){
        // TODO: implement later
    }


    public void drop(Section section){
        // TODO: implement later
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
            timetable.add(e.getSection().getSchedule());
        }
        return timetable;
    }
    public Submission submitAssignment(Assignment assignment, String content){
        // TODO: implement later

        return null;
    }

    public void submitCourseClashRequest(CourseClashRequest request){
        // TODO: implement later
    }





}

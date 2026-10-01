public class VisitingInstructor extends Instructor {

    VisitingInstructor(String name, String email, String phone, String teacherId) {
        super(name, email, phone, teacherId);
    }

    @Override
    public String getRole() {
        return "Visiting Instructor";
    }
}
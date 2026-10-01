public class NormalStudent extends Student {

    NormalStudent(String name, String email, String phone, String studentId, int totalCreditHours) {
        super(name, email, phone, studentId, totalCreditHours);
    }

    @Override
    public String getRole() {
        return "Normal Student";
    }
}
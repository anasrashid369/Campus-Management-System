public class TeachingAssistant extends Student implements Evaluator {

    // TODO: private Section assignedSection;  (Section not in this UCS yet)

    TeachingAssistant(String name, String email, String phone, String studentId, int totalCreditHours) {
        super(name, email, phone, studentId, totalCreditHours);
    }

    // TODO: getAssignedSection()
    // TODO: createAssignment(...)
    // TODO: viewSubmissions(...)
    // TODO: evaluateSubmission(...)
    // TODO: giveFeedback(...)

    @Override
    public void evaluate() {
        // TODO: implement later
    }

    @Override
    public String getRole() {
        return "Teaching Assistant";
    }
}
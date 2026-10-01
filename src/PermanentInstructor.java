public class PermanentInstructor extends Instructor implements Evaluator {

    PermanentInstructor(String name, String email, String phone, String teacherId) {
        super(name, email, phone, teacherId);
    }

    // TODO: assignTA(...)          (Section not in this UCS)
    // TODO: viewFYPGroups()        (FYPGroup not in this UCS)
    // TODO: viewFYPGroupDetails(...)
    // TODO: scheduleFYPMeeting(...)
    // TODO: evaluateFYPIdea(...)
    // TODO: provideFYPFeedback(...)

    @Override
    public void evaluate() {
        // TODO: implement later
    }

    @Override
    public String getRole() {
        return "Permanent Instructor";
    }
}
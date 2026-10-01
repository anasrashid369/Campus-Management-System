import java.util.ArrayList;
import java.util.List;

public class PermanentInstructor extends Instructor implements Evaluator {

    private List<FYPGroup> fypGroups;

    PermanentInstructor(String name, String email, String phone, String teacherId) {
        super(name, email, phone, teacherId);
        this.fypGroups = new ArrayList<>();
    }

    // Promotes a normal student to TA for the given section
    public void assignTA(NormalStudent student, Section section) {
        TeachingAssistant ta = new TeachingAssistant(
                student.getName(), student.getEmail(), student.getPhone(),
                student.getStudentId(), student.calculateTotalCreditHours());
        section.assignTA(ta);
    }

    public List<FYPGroup> viewFYPGroups() {
        return new ArrayList<>(fypGroups);
    }

    public String viewFYPGroupDetails(FYPGroup group) {
        return group.getDetails();
    }

    public void scheduleFYPMeeting(FYPGroup group, FYPMeeting meeting) {
        group.addMeeting(meeting);
    }

    public void evaluateFYPIdea(FYPGroup group, FYPEvaluation evaluation) {
        group.addEvaluation(evaluation);
    }

    public void provideFYPFeedback(FYPEvaluation evaluation, String feedback) {
        evaluation.addFeedback(feedback);
    }

    // Added: called by FYPGroup.assignSupervisor to keep both sides in sync
    public void addFYPGroup(FYPGroup group) {
        if (!fypGroups.contains(group)) {
            fypGroups.add(group);
        }
    }

    public void removeFYPGroup(FYPGroup group) {
        fypGroups.remove(group);
    }

    // The diagram only gives evaluate() : void, so this reports the evaluations on supervised groups
    @Override
    public void evaluate() {
        for (FYPGroup g : fypGroups) {
            System.out.println(g.getGroupId() + ": " + g.getEvaluations().size() + " evaluation(s)");
        }
    }

    @Override
    public String getRole() {
        return "Permanent Instructor";
    }
}
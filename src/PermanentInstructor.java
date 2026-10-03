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

    public void scheduleFYPMeeting(FYPGroup group, FYPMeeting meeting) throws InvalidFYPGroupException {
        requireSupervisedGroup(group);
        group.addMeeting(meeting);
    }

    public void evaluateFYPIdea(FYPGroup group, FYPEvaluation evaluation)
            throws InvalidFYPGroupException, InvalidFYPEvaluationException {
        requireSupervisedGroup(group);
        group.addEvaluation(evaluation);
    }

    public void provideFYPFeedback(FYPEvaluation evaluation, String feedback)
            throws InvalidFYPGroupException, InvalidFYPEvaluationException {
        if (evaluation == null || evaluation.getEvaluator() != this) {
            throw new InvalidFYPEvaluationException("Evaluation was not created by this instructor");
        }
        if (feedback == null || feedback.isBlank()) {
            throw new InvalidFYPEvaluationException("Feedback cannot be blank");
        }
        evaluation.addFeedback(feedback);
    }

    private void requireSupervisedGroup(FYPGroup group) throws InvalidFYPGroupException {
        if (group == null || group.getSupervisor() != this || !fypGroups.contains(group)) {
            throw new InvalidFYPGroupException("FYP group is not supervised by this instructor");
        }
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
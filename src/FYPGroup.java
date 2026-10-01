import java.util.ArrayList;
import java.util.List;

public class FYPGroup {
    private String groupId;
    private String title;
    private String description;
    private List<Student> members;
    private PermanentInstructor supervisor;
    private List<FYPMeeting> meetings;
    private List<FYPEvaluation> evaluations;

    public FYPGroup(String groupId, String title, String description) {
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.members = new ArrayList<>();
        this.meetings = new ArrayList<>();
        this.evaluations = new ArrayList<>();
    }

    public void addMember(Student student) {
        if (student != null && !members.contains(student)) {
            members.add(student);
        }
    }

    public void removeMember(Student student) {
        members.remove(student);
    }

    public List<Student> getMembers() {
        return new ArrayList<>(members);
    }

    public void assignSupervisor(PermanentInstructor supervisor) {
        if (this.supervisor != null && this.supervisor != supervisor) {
            this.supervisor.removeFYPGroup(this);
        }
        this.supervisor = supervisor;
        if (supervisor != null) {
            supervisor.addFYPGroup(this);
        }
    }

    public void addMeeting(FYPMeeting meeting) {
        if (meeting != null) {
            meetings.add(meeting);
        }
    }

    public void addEvaluation(FYPEvaluation evaluation) {
        if (evaluation != null) {
            evaluations.add(evaluation);
        }
    }

    public String getDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("FYP Group ").append(groupId).append(": ").append(title).append("\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Supervisor: ").append(supervisor == null ? "(not assigned)" : supervisor.getName()).append("\n");
        sb.append("Members:");
        if (members.isEmpty()) {
            sb.append(" (none)");
        }
        for (Student s : members) {
            sb.append("\n  - ").append(s.getName()).append(" (").append(s.getStudentId()).append(")");
        }
        sb.append("\nMeetings: ").append(meetings.size());
        sb.append("\nEvaluations: ").append(evaluations.size());
        return sb.toString();
    }

    // Extra getters (not in diagram, needed by PermanentInstructor / comparators later)
    public String getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public PermanentInstructor getSupervisor() {
        return supervisor;
    }

    public List<FYPMeeting> getMeetings() {
        return new ArrayList<>(meetings);
    }

    public List<FYPEvaluation> getEvaluations() {
        return new ArrayList<>(evaluations);
    }
}
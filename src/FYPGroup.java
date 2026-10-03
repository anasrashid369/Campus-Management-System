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

    public void addMember(Student student) throws InvalidFYPGroupException {
        if (student == null) {
            throw new InvalidFYPGroupException("FYP member cannot be null");
        }
        if (members.contains(student)) {
            throw new InvalidFYPGroupException("Student is already an FYP group member");
        }
        members.add(student);
    }

    public void removeMember(Student student) {
        members.remove(student);
    }

    public List<Student> getMembers() {
        return new ArrayList<>(members);
    }

    public void assignSupervisor(PermanentInstructor supervisor) throws InvalidFYPGroupException {
        if (supervisor == null) {
            throw new InvalidFYPGroupException("FYP supervisor cannot be null");
        }
        if (this.supervisor != null && this.supervisor != supervisor) {
            this.supervisor.removeFYPGroup(this);
        }
        this.supervisor = supervisor;
        if (supervisor != null) {
            supervisor.addFYPGroup(this);
        }
    }

    public void addMeeting(FYPMeeting meeting) throws InvalidFYPGroupException {
        if (meeting == null) {
            throw new InvalidFYPGroupException("FYP meeting cannot be null");
        }
        for (FYPMeeting existing : meetings) {
            if (existing.getMeetingId().equalsIgnoreCase(meeting.getMeetingId())) {
                throw new InvalidFYPGroupException("Duplicate FYP meeting ID: " + meeting.getMeetingId());
            }
        }
        meetings.add(meeting);
    }

    public void addEvaluation(FYPEvaluation evaluation) throws InvalidFYPEvaluationException {
        if (evaluation == null) {
            throw new InvalidFYPEvaluationException("FYP evaluation cannot be null");
        }
        for (FYPEvaluation existing : evaluations) {
            if (existing.getEvaluationId().equalsIgnoreCase(evaluation.getEvaluationId())) {
                throw new InvalidFYPEvaluationException(
                        "Duplicate FYP evaluation ID: " + evaluation.getEvaluationId());
            }
        }
        evaluations.add(evaluation);
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

    public String getDescription() {
        return description;
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
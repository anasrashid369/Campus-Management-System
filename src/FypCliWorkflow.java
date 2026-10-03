import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Final Year Project supervision workflows for a Permanent Instructor. */
final class FypCliWorkflow {
    private final CliContext context;

    FypCliWorkflow(CliContext context) {
        this.context = context;
    }

    void viewGroups(PermanentInstructor instructor) {
        List<FYPGroup> groups = instructor.viewFYPGroups();
        if (groups.isEmpty()) {
            context.out().println("No FYP groups are assigned to this instructor.");
            return;
        }
        for (FYPGroup group : groups) {
            context.out().printf("%s | %s%n", group.getGroupId(), group.getTitle());
        }
    }

    void viewGroupDetails(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectGroup(instructor);
        if (group == null) {
            return;
        }
        context.out().println(instructor.viewFYPGroupDetails(group));
        List<FYPMeeting> meetings = group.getMeetings();
        meetings.sort(new FYPMeetingDateComparator());
        for (FYPMeeting meeting : meetings) {
            context.out().println(meeting.getMeetingDetails());
        }
    }

    void viewMembers(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectGroup(instructor);
        if (group == null) {
            return;
        }
        List<Student> members = group.getMembers();
        if (members.isEmpty()) {
            context.out().println("FYP group has no members.");
            return;
        }
        members.sort(new StudentNameComparator());
        for (Student member : members) {
            context.out().println(CliContext.describeStudent(member));
        }
    }

    void scheduleMeeting(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectGroup(instructor);
        if (group == null) {
            return;
        }
        String meetingId = context.readRequiredText("Meeting ID: ");
        if (meetingId == null) {
            return;
        }
        LocalDate date = context.readLocalDate("Meeting date (YYYY-MM-DD): ");
        if (date == null) {
            return;
        }
        String agenda = context.readRequiredText("Agenda: ");
        if (agenda == null) {
            return;
        }
        try {
            instructor.scheduleFYPMeeting(group, new FYPMeeting(meetingId, date, agenda));
        } catch (InvalidFYPGroupException exception) {
            ApplicationLogger.error("fyp.meeting_rejected group=" + group.getGroupId(), exception);
            context.out().println("Meeting was not scheduled: " + exception.getMessage());
            return;
        }
        if (context.saveCatalog("fyp.meeting.schedule")) {
            ApplicationLogger.info("fyp.meeting_scheduled group=" + group.getGroupId() + " meeting=" + meetingId);
            context.out().printf("Meeting %s scheduled for group %s.%n", meetingId, group.getGroupId());
        }
    }

    void evaluateIdea(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectGroup(instructor);
        if (group == null) {
            return;
        }
        String evaluationId = context.readRequiredText("Evaluation ID: ");
        if (evaluationId == null) {
            return;
        }
        LocalDate date = context.readLocalDate("Evaluation date (YYYY-MM-DD): ");
        if (date == null) {
            return;
        }
        Double score = context.readNonNegativeDouble("Score: ");
        if (score == null) {
            return;
        }
        FYPEvaluation evaluation = new FYPEvaluation(evaluationId, date, instructor);
        try {
            evaluation.evaluate(score);
            instructor.evaluateFYPIdea(group, evaluation);
            if (context.saveCatalog("fyp.idea.evaluate")) {
                ApplicationLogger.info("fyp.idea_evaluated group=" + group.getGroupId()
                        + " evaluation=" + evaluationId);
                context.out().printf("FYP idea evaluated with score %.2f.%n", score);
            }
        } catch (InvalidFYPGroupException | InvalidFYPEvaluationException exception) {
            ApplicationLogger.error("fyp.evaluation_rejected id=" + evaluationId, exception);
            context.out().println("FYP evaluation was not added: " + exception.getMessage());
        }
    }

    void provideFeedback(PermanentInstructor instructor) throws IOException {
        FYPGroup group = selectGroup(instructor);
        if (group == null) {
            return;
        }
        FYPEvaluation evaluation = context.choose(group.getEvaluations(),
                "The group has no evaluations to provide feedback on.",
                "Select an evaluation" + CliContext.CANCEL_SUFFIX,
                item -> String.format("%s | score %.2f", item.getEvaluationId(), item.getScore()));
        if (evaluation == null) {
            return;
        }
        String feedback = context.readRequiredText("Feedback: ");
        if (feedback == null) {
            return;
        }
        try {
            instructor.provideFYPFeedback(evaluation, feedback);
        } catch (InvalidFYPGroupException | InvalidFYPEvaluationException exception) {
            ApplicationLogger.error("fyp.feedback_rejected id=" + evaluation.getEvaluationId(), exception);
            context.out().println("FYP feedback was not saved: " + exception.getMessage());
            return;
        }
        if (context.saveCatalog("fyp.feedback")) {
            ApplicationLogger.info("fyp.feedback_added group=" + group.getGroupId()
                    + " evaluation=" + evaluation.getEvaluationId());
            context.out().println("FYP feedback saved.");
        }
    }

    void createGroup(PermanentInstructor instructor) throws IOException {
        String groupId = context.readRequiredText("FYP group ID: ");
        if (groupId == null) {
            return;
        }
        if (groupExists(groupId)) {
            context.out().printf("FYP group %s already exists.%n", groupId);
            return;
        }
        String title = context.readRequiredText("Group title: ");
        if (title == null) {
            return;
        }
        String description = context.readRequiredText("Group description: ");
        if (description == null) {
            return;
        }
        FYPGroup group = new FYPGroup(groupId, title, description);
        if (!addMembersFromPrompt(group)) {
            return;
        }
        try {
            group.assignSupervisor(instructor);
        } catch (InvalidFYPGroupException exception) {
            context.out().println("FYP group was not assigned: " + exception.getMessage());
            return;
        }
        if (context.saveCatalog("fyp.group.create")) {
            ApplicationLogger.info("fyp.group_created id=" + groupId + " supervisor=" + instructor.getTeacherId());
            context.out().printf("FYP group %s created and supervised by %s.%n",
                    groupId, instructor.getTeacherId());
        }
    }

    /** Adds Normal Student members until the user declines; false if the user cancelled group creation. */
    private boolean addMembersFromPrompt(FYPGroup group) throws IOException {
        while (true) {
            Boolean addMember = context.readYesNo("Add a Normal Student member? (y/n): ");
            if (addMember == null || !addMember) {
                return true;
            }
            NormalStudent member = context.selectNormalStudent("No Normal Student profiles are available.");
            if (member == null) {
                return false;
            }
            try {
                group.addMember(member);
            } catch (InvalidFYPGroupException exception) {
                context.out().println("Member was not added: " + exception.getMessage());
            }
        }
    }

    private boolean groupExists(String groupId) {
        for (Instructor instructor : context.instructors()) {
            if (instructor instanceof PermanentInstructor permanent) {
                for (FYPGroup group : permanent.viewFYPGroups()) {
                    if (group.getGroupId().equalsIgnoreCase(groupId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private FYPGroup selectGroup(PermanentInstructor instructor) throws IOException {
        return context.choose(instructor.viewFYPGroups(), "No FYP groups are assigned to this instructor.",
                "Select a group" + CliContext.CANCEL_SUFFIX,
                group -> group.getGroupId() + " | " + group.getTitle());
    }
}

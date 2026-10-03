import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExtendedWorkflowTest {
    public static void main(String[] args) throws Exception {
        testGenericRequestSubmissionAndRejection();
        testTeachingAssistantUnauthorizedAccess();
        testCourseClashApprovalEnrollmentChange();
        testPrerequisiteRegistrationBlock();
        System.out.println("ExtendedWorkflowTest: PASS");
    }

    private static void testGenericRequestSubmissionAndRejection() {
        String setup = runCli("1\n1\nGEN101\nGeneral Course\n3\n\n0\n0\n");
        check(setup.contains("Course GEN101 created successfully."), "admin should create a course");

        String studentRequest = runCli("5\nS-GEN\ny\nSam Student\nsam@example.test\n555-0200\n"
                + "15\n2\nNeed help with a classmate issue\n1\n4\n0\n0\n");
        check(studentRequest.contains("Academic request REQ-"), "student should submit a generic request");

        String adminReject = runCli("1\n11\n1\n0\n0\n");
        check(adminReject.contains("rejected."), "admin should reject the generic request");

        String studentView = runCli("5\nS-GEN\n4\n0\n0\n");
        check(studentView.contains("Status: REJECTED"), "student should see rejected generic request");
    }

    private static void testTeachingAssistantUnauthorizedAccess() throws UnauthorizedActionException {
        TeachingAssistant assistant = new TeachingAssistant("TA", "ta@example.test", "", "TA-X", 0);
        Course course = new Course("UNAUTH", "Unauthorized", 3);
        Section section = new Section("UN-A", 10, course);
        assistant.setAssignedSection(section);
        Assignment own = assistant.createAssignment("Own", "desc", java.time.LocalDate.of(2027, 1, 1), 10);
        TeachingAssistant other = new TeachingAssistant("Other", "other@example.test", "", "TA-Y", 0);
        other.setAssignedSection(section);
        Assignment foreign = other.createAssignment("Foreign", "desc", java.time.LocalDate.of(2027, 1, 2), 10);

        try {
            assistant.viewSubmissions(foreign);
            throw new AssertionError("TA should not view foreign assignment submissions");
        } catch (UnauthorizedActionException expected) {
            check(expected.getMessage().contains("not authorized"), "unauthorized message should explain denial");
        }
        check(assistant.viewSubmissions(own).isEmpty(), "TA should still access own assignment");
    }

    private static void testCourseClashApprovalEnrollmentChange() throws Exception {
        Path catalog = Path.of("clash-approval-test.txt");
        AcademicOfficeAdmin admin = new AcademicOfficeAdmin("Admin", "", "", "A-CA");
        Course course = new Course("CL200", "Clash Approval", 3);
        admin.createCourse(course);
        Section first = new Section("CL-A", 20, course);
        Section second = new Section("CL-B", 20, course);
        admin.createSection(first);
        admin.createSection(second);
        first.setSchedule(new Schedule(Day.MONDAY, java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(10, 0), "R1"));
        second.setSchedule(new Schedule(Day.MONDAY, java.time.LocalTime.of(9, 30),
                java.time.LocalTime.of(10, 30), "R2"));
        NormalStudent student = new NormalStudent("Pat", "pat@example.test", "", "S-CA", 0);
        student.register(first);
        CourseClashRequest request = new CourseClashRequest("REQ-CA", java.time.LocalDate.now(),
                "Need section B", 2, student, first, second);
        request.submit();
        admin.addRequest(request);
        admin.approveRequest(request);
        check(second.getEnrolledStudents().contains(student), "approved clash should enroll requested section");
        check(!first.getEnrolledStudents().contains(student), "approved clash should drop conflicting section");
        new CampusPersistence(catalog).saveCatalog(admin, List.of(student), List.of());
        Files.deleteIfExists(catalog);
    }

    private static void testPrerequisiteRegistrationBlock() throws CourseFullException, CourseClashException {
        AcademicOfficeAdmin admin = new AcademicOfficeAdmin("Admin", "", "", "A-PRE");
        Course prerequisite = new Course("PRE100", "Prerequisite", 3);
        Course advanced = new Course("ADV200", "Advanced", 3);
        advanced.addPrerequisite(prerequisite);
        admin.createCourse(prerequisite);
        admin.createCourse(advanced);
        Section advancedSection = new Section("ADV-A", 20, advanced);
        admin.createSection(advancedSection);
        NormalStudent student = new NormalStudent("Pre", "pre@example.test", "", "S-PRE", 0);
        try {
            student.register(advancedSection);
            throw new AssertionError("registration should require prerequisite course");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("prerequisite"), "missing prerequisite should be reported");
        }
    }

    private static String runCli(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CampusCli cli = new CampusCli(new BufferedReader(new StringReader(input)), new PrintStream(output));
        cli.run();
        return output.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

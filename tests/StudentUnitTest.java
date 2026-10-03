public final class StudentUnitTest {
    public static void main(String[] args) throws Exception {
        testStudentRegistrationAndCreditHours();
        testStudentDropSection();
        System.out.println("StudentUnitTest: PASS");
    }

    private static void testStudentRegistrationAndCreditHours() throws Exception {
        NormalStudent student = new NormalStudent("Charlie", "charlie@example.test", "555-0199", "ST-301", 0);
        Course cs = new Course("CS101", "Intro CS", 3);
        Section section = new Section("SEC-CS", 20, cs);

        student.register(section);
        check(student.calculateTotalCreditHours() == 3, "total credit hours should increase to 3");
        check(student.viewCourses().contains(cs), "registered course should appear in viewCourses");

        try {
            student.register(section);
            throw new AssertionError("registering twice for the same section should fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("already enrolled"), "duplicate enrollment exception message");
        }
    }

    private static void testStudentDropSection() throws Exception {
        NormalStudent student = new NormalStudent("Dana", "dana@example.test", "555-0200", "ST-302", 0);
        Course math = new Course("MATH101", "Algebra", 3);
        Section section = new Section("SEC-MATH", 20, math);

        student.register(section);
        check(student.calculateTotalCreditHours() == 3, "credit hours should be 3 before drop");
        student.drop(section);
        check(student.calculateTotalCreditHours() == 0, "credit hours should reset to 0 after drop");
        check(!student.viewCourses().contains(math), "dropped course should no longer be listed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

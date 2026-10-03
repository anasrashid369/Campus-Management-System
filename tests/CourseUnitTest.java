public final class CourseUnitTest {
    public static void main(String[] args) {
        testCourseCreationAndValidation();
        testPrerequisiteManagement();
        System.out.println("CourseUnitTest: PASS");
    }

    private static void testCourseCreationAndValidation() {
        Course course = new Course("CS101", "Intro to CS", 3);
        check(course.getCourseCode().equals("CS101"), "course code should match");
        check(course.getTitle().equals("Intro to CS"), "title should match");
        check(course.getCreditHours() == 3, "credit hours should match");

        try {
            new Course("", "Title", 3);
            throw new AssertionError("blank course code should fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Course code"), "invalid code exception message");
        }
    }

    private static void testPrerequisiteManagement() {
        Course math = new Course("MATH101", "Calculus", 3);
        Course cs = new Course("CS201", "Data Structures", 4);
        cs.addPrerequisite(math);
        check(cs.getPrerequisites().contains(math), "prerequisite should be registered");

        cs.addPrerequisite(cs);
        check(!cs.getPrerequisites().contains(cs), "course cannot be its own prerequisite");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

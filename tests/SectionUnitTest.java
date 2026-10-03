import java.time.LocalTime;

public final class SectionUnitTest {
    public static void main(String[] args) throws Exception {
        testSectionCapacityAndEnrollment();
        testSectionScheduleClash();
        System.out.println("SectionUnitTest: PASS");
    }

    private static void testSectionCapacityAndEnrollment() throws Exception {
        Course course = new Course("CS102", "OOP", 3);
        Section section = new Section("SEC-1", 1, course);
        NormalStudent student1 = new NormalStudent("Alice", "alice@example.test", "", "S-101", 0);
        NormalStudent student2 = new NormalStudent("Bob", "bob@example.test", "", "S-102", 0);

        section.enroll(student1);
        check(section.isFull(), "section should be full");
        check(section.getAvailableSeats() == 0, "available seats should be 0");

        try {
            section.enroll(student2);
            throw new AssertionError("enrolling into full section should throw CourseFullException");
        } catch (CourseFullException expected) {
            check(expected.getMessage().contains("is full"), "exception message should state section full");
        }

        section.drop(student1);
        check(!section.isFull(), "section should no longer be full");
        check(section.getAvailableSeats() == 1, "available seats should be 1");
    }

    private static void testSectionScheduleClash() {
        Course course = new Course("EE101", "Circuits", 3);
        Section s1 = new Section("S1", 30, course);
        Section s2 = new Section("S2", 30, course);

        s1.setSchedule(new Schedule(Day.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Room A"));
        s2.setSchedule(new Schedule(Day.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "Room B"));

        check(s1.hasClash(s2), "overlapping schedules should clash");
        check(s2.hasClash(s1), "clash check should be symmetric");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

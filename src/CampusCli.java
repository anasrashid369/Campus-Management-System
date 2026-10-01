import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

public final class CampusCli {
    private static final List<RoleMenu> ROLE_MENUS = List.of(
            new RoleMenu("Academic Office Admin", List.of(
                    "Create Course",
                    "Update Course",
                    "Search Course",
                    "Create Section",
                    "Update Section",
                    "Set Section Capacity",
                    "Assign Room",
                    "Assign Instructor",
                    "View Requests",
                    "Approve Request",
                    "Reject Request")),
            new RoleMenu("Permanent Instructor", List.of(
                    "View Assigned Courses",
                    "View Assigned Sections",
                    "View Enrolled Students",
                    "Mark Attendance",
                    "Mark Present",
                    "Mark Absent",
                    "Mark Late",
                    "Update Attendance",
                    "Calculate Attendance Percentage",
                    "Assign Teaching Assistant",
                    "View FYP Groups",
                    "View FYP Group Details",
                    "View FYP Members",
                    "Schedule FYP Meeting",
                    "Evaluate FYP Idea",
                    "Provide FYP Feedback")),
            new RoleMenu("Visiting Instructor", List.of(
                    "View Assigned Courses",
                    "View Assigned Sections",
                    "View Enrolled Students",
                    "Mark Attendance",
                    "Mark Present",
                    "Mark Absent",
                    "Mark Late",
                    "Update Attendance",
                    "Calculate Attendance Percentage")),
            new RoleMenu("Teaching Assistant", List.of(
                    "View Enrolled Students",
                    "Create Assignment",
                    "Set Total Marks",
                    "Set Assignment Deadline",
                    "View Submissions",
                    "Check Late Submissions",
                    "Evaluate Submission",
                    "Assign Marks",
                    "Give Feedback",
                    "View Assigned Section")),
            new RoleMenu("Normal Student", List.of(
                    "View Attendance Percentage",
                    "Submit Course Clash Request",
                    "Check Section Clash",
                    "View Requests",
                    "View Available Courses",
                    "View Course Credit Hours",
                    "Register Course",
                    "Calculate Total Credit Hours",
                    "Drop Course",
                    "View Registered Courses",
                    "View Timetable",
                    "View Assignments",
                    "Submit Assignment",
                    "View Attendance")));

    private final BufferedReader input;
    private final PrintStream output;
    private final AcademicOfficeAdmin academicOfficeAdmin = new AcademicOfficeAdmin(
            "Console Admin", "console-admin@localhost", "", "CLI-ADMIN");

    public CampusCli() {
        this(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    CampusCli(BufferedReader input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void run() {
        try {
            runMenus();
        } catch (IOException exception) {
            output.println("Unable to continue reading input. Goodbye.");
        }
    }

    private void runMenus() throws IOException {
        while (true) {
            output.println("\nCampus Management System");
            output.println("0. Exit");
            for (int index = 0; index < ROLE_MENUS.size(); index++) {
                output.printf("%d. %s%n", index + 1, ROLE_MENUS.get(index).role());
            }

            Integer selection = readChoice("Select a role: ", 0, ROLE_MENUS.size());
            if (selection == null || selection == 0) {
                output.println("Goodbye.");
                return;
            }

            runRoleMenu(ROLE_MENUS.get(selection - 1));
        }
    }

    private void runRoleMenu(RoleMenu roleMenu) throws IOException {
        while (true) {
            output.printf("%n%s Menu%n", roleMenu.role());
            output.println("0. Back to role selection");
            for (int index = 0; index < roleMenu.actions().size(); index++) {
                output.printf("%d. %s%n", index + 1, roleMenu.actions().get(index));
            }

            Integer selection = readChoice("Select an option: ", 0, roleMenu.actions().size());
            if (selection == null) {
                output.println("Goodbye.");
                return;
            }
            if (selection == 0) {
                return;
            }

            if (roleMenu.role().equals("Academic Office Admin")) {
                handleAdminAction(selection);
            } else {
                output.println("This workflow is not connected yet.");
            }
        }
    }

    private void handleAdminAction(int selection) throws IOException {
        switch (selection) {
            case 1 -> createCourse();
            case 3 -> searchCourse();
            default -> output.println("This workflow is not connected yet.");
        }
    }

    private void createCourse() throws IOException {
        String courseCode = readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }

        String title = readRequiredText("Course title: ");
        if (title == null) {
            return;
        }

        Integer creditHours = readPositiveInteger("Credit hours: ");
        if (creditHours == null) {
            return;
        }

        if (academicOfficeAdmin.searchCourse(courseCode) != null) {
            output.printf("Course %s already exists; no course was created.%n", courseCode);
            return;
        }

        try {
            Course course = new Course(courseCode, title, creditHours);
            academicOfficeAdmin.createCourse(course);
            if (academicOfficeAdmin.searchCourse(courseCode) == course) {
                output.printf("Course %s created successfully.%n", courseCode);
            } else {
                output.printf("Course %s was not created.%n", courseCode);
            }
        } catch (IllegalArgumentException exception) {
            output.printf("Course was not created: %s%n", exception.getMessage());
        }
    }

    private void searchCourse() throws IOException {
        String courseCode = readRequiredText("Course code to search: ");
        if (courseCode == null) {
            return;
        }

        Course course = academicOfficeAdmin.searchCourse(courseCode);
        if (course == null) {
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        output.printf("Course found: %s | %s | %d credit hour(s)%n",
                course.getCourseCode(), course.getTitle(), course.getCreditHours());
    }

    private String readRequiredText(String prompt) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            String value = line.trim();
            if (!value.isEmpty()) {
                return value;
            }
            output.println("This value cannot be blank.");
        }
    }

    private Integer readPositiveInteger(String prompt) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            try {
                int value = Integer.parseInt(line.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println("Enter a positive whole number of credit hours.");
        }
    }

    private Integer readChoice(String prompt, int minimum, int maximum) throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return null;
            }

            try {
                int selection = Integer.parseInt(line.trim());
                if (selection >= minimum && selection <= maximum) {
                    return selection;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input is reported below and the menu remains active.
            }

            output.printf("Enter a number from %d to %d.%n", minimum, maximum);
        }
    }

    private record RoleMenu(String role, List<String> actions) {
    }
}
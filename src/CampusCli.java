import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private final List<Section> managedSections = new ArrayList<>();
    private final List<Instructor> managedInstructors = new ArrayList<>();

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
            case 2 -> updateCourse();
            case 3 -> searchCourse();
            case 4 -> createSection();
            case 5 -> updateSection();
            case 6 -> setSectionCapacity();
            case 7 -> assignRoom();
            case 8 -> assignInstructor();
            case 9 -> viewRequests();
            case 10 -> processRequest(RequestStatus.APPROVED);
            case 11 -> processRequest(RequestStatus.REJECTED);
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

        Integer creditHours = readPositiveInteger(
            "Credit hours: ", "Enter a positive whole number of credit hours.");
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

    private void updateCourse() throws IOException {
        String courseCode = readRequiredText("Course code to update: ");
        if (courseCode == null) {
            return;
        }
        Course existingCourse = academicOfficeAdmin.searchCourse(courseCode);
        if (existingCourse == null) {
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        String title = readRequiredText("New course title: ");
        if (title == null) {
            return;
        }
        Integer creditHours = readPositiveInteger(
                "New credit hours: ", "Enter a positive whole number of credit hours.");
        if (creditHours == null) {
            return;
        }

        Course updatedDetails = new Course(existingCourse.getCourseCode(), title, creditHours);
        academicOfficeAdmin.updateCourse(updatedDetails);
        if (academicOfficeAdmin.searchCourse(courseCode) == existingCourse
                && existingCourse.getTitle().equals(title)
                && existingCourse.getCreditHours() == creditHours) {
            output.printf("Course %s updated successfully.%n", existingCourse.getCourseCode());
        } else {
            output.printf("Course %s was not updated.%n", courseCode);
        }
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

    private void createSection() throws IOException {
        String sectionId = readRequiredText("Section ID: ");
        if (sectionId == null) {
            return;
        }
        for (Section section : managedSections) {
            if (section.getSectionId().equalsIgnoreCase(sectionId)) {
                output.printf("Section %s already exists.%n", sectionId);
                return;
            }
        }

        String courseCode = readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }
        Course course = academicOfficeAdmin.searchCourse(courseCode);
        if (course == null) {
            output.printf("No course found with code %s.%n", courseCode);
            return;
        }

        Integer capacity = readPositiveInteger(
                "Section capacity: ", "Enter a positive whole number for capacity.");
        if (capacity == null) {
            return;
        }

        Section section = new Section(sectionId, capacity, course);
        academicOfficeAdmin.createSection(section);
        if (course.getSections().contains(section)) {
            managedSections.add(section);
            output.printf("Section %s created for course %s.%n", sectionId, course.getCourseCode());
        } else {
            output.printf("Section %s was not created.%n", sectionId);
        }
    }

    private void setSectionCapacity() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        updateSectionCapacity(section);
    }

    private void updateSectionCapacity(Section section) throws IOException {
        Integer capacity = readPositiveInteger(
                "New capacity: ", "Enter a positive whole number for capacity.");
        if (capacity == null) {
            return;
        }

        try {
            academicOfficeAdmin.setCapacity(section, capacity);
            output.printf("Capacity for section %s set to %d.%n",
                    section.getSectionId(), section.getCapacity());
        } catch (IllegalArgumentException exception) {
            output.printf("Capacity was not changed: %s%n", exception.getMessage());
        }
    }

    private void assignRoom() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        assignRoom(section);
    }

    private void assignRoom(Section section) throws IOException {
        String dayInput = readRequiredText("Day (Monday-Saturday): ");
        if (dayInput == null) {
            return;
        }
        Day day;
        try {
            day = Day.valueOf(dayInput.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            output.println("Enter a day from Monday through Saturday.");
            return;
        }

        LocalTime startTime = readTime("Start time (HH:mm): ");
        if (startTime == null) {
            return;
        }
        LocalTime endTime = readTime("End time (HH:mm): ");
        if (endTime == null) {
            return;
        }
        if (!endTime.isAfter(startTime)) {
            output.println("End time must be after start time.");
            return;
        }

        String room = readRequiredText("Room: ");
        if (room == null) {
            return;
        }

        Schedule schedule = new Schedule(day, startTime, endTime, room);
        academicOfficeAdmin.assignRoom(section, schedule);
        output.printf("Room assigned to section %s: %s%n",
                section.getSectionId(), schedule.getScheduleInfo());
    }

    private void updateSection() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        output.printf("Update Section %s%n", section.getSectionId());
        output.println("0. Cancel");
        output.println("1. Update capacity");
        output.println("2. Update room and schedule");
        Integer selection = readChoice("Select a field to update: ", 0, 2);
        if (selection == null || selection == 0) {
            return;
        }
        if (selection == 1) {
            updateSectionCapacity(section);
        } else {
            assignRoom(section);
        }
    }

    private void assignInstructor() throws IOException {
        Section section = findManagedSection();
        if (section == null) {
            return;
        }

        String teacherId = readRequiredText("Instructor ID: ");
        if (teacherId == null) {
            return;
        }

        Instructor instructor = findManagedInstructor(teacherId);
        if (instructor == null) {
            Integer instructorType = readChoice(
                    "1. Permanent Instructor\n2. Visiting Instructor\nSelect type: ", 1, 2);
            if (instructorType == null) {
                return;
            }
            String name = readRequiredText("Name: ");
            if (name == null) {
                return;
            }
            String email = readRequiredText("Email: ");
            if (email == null) {
                return;
            }
            String phone = readRequiredText("Phone: ");
            if (phone == null) {
                return;
            }

            if (instructorType == 1) {
                instructor = new PermanentInstructor(name, email, phone, teacherId);
            } else {
                instructor = new VisitingInstructor(name, email, phone, teacherId);
            }
            managedInstructors.add(instructor);
        }

        academicOfficeAdmin.assignInstructor(section, instructor);
        if (section.getInstructor() == instructor) {
            output.printf("%s assigned to section %s.%n",
                    instructor.getRole(), section.getSectionId());
        } else {
            output.printf("Instructor was not assigned to section %s.%n", section.getSectionId());
        }
    }

    private Instructor findManagedInstructor(String teacherId) {
        for (Instructor instructor : managedInstructors) {
            if (instructor.getTeacherId().equalsIgnoreCase(teacherId)) {
                return instructor;
            }
        }
        return null;
    }

    private void viewRequests() {
        List<Request> requests = academicOfficeAdmin.viewRequests();
        if (requests.isEmpty()) {
            output.println("No requests are currently available.");
            return;
        }
        for (int index = 0; index < requests.size(); index++) {
            output.printf("%d. %s%n", index + 1, requests.get(index).getDetails());
        }
    }

    private void processRequest(RequestStatus status) throws IOException {
        List<Request> requests = academicOfficeAdmin.viewRequests();
        if (requests.isEmpty()) {
            output.println("No requests are currently available to process.");
            return;
        }
        for (int index = 0; index < requests.size(); index++) {
            output.printf("%d. %s%n", index + 1, requests.get(index).getDetails());
        }

        Integer selection = readChoice("Select a request (0 to cancel): ", 0, requests.size());
        if (selection == null || selection == 0) {
            return;
        }
        Request request = requests.get(selection - 1);
        if (request.getStatus() != RequestStatus.PENDING) {
            output.printf("Request %s has already been processed as %s.%n",
                    request.getRequestId(), request.getStatus());
            return;
        }

        if (status == RequestStatus.APPROVED) {
            academicOfficeAdmin.approveRequest(request);
        } else {
            academicOfficeAdmin.rejectRequest(request);
        }
        if (request.getStatus() == status) {
            output.printf("Request %s %s.%n", request.getRequestId(), status.name().toLowerCase(Locale.ROOT));
        } else {
            output.printf("Request %s was not processed.%n", request.getRequestId());
        }
    }

    private Section findManagedSection() throws IOException {
        String sectionId = readRequiredText("Section ID: ");
        if (sectionId == null) {
            return null;
        }
        for (Section section : managedSections) {
            if (section.getSectionId().equalsIgnoreCase(sectionId)) {
                return section;
            }
        }
        output.printf("No section found with ID %s in this CLI session.%n", sectionId);
        return null;
    }

    private LocalTime readTime(String prompt) throws IOException {
        String value = readRequiredText(prompt);
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException exception) {
            output.println("Enter time in 24-hour HH:mm format.");
            return null;
        }
    }

    private Integer readPositiveInteger(String prompt, String errorMessage) throws IOException {
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
            output.println(errorMessage);
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
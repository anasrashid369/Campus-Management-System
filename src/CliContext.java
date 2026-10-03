import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Shared state for one CLI session: console input/output, the loaded campus records,
 * persistence, and the input/selection helpers every role handler reuses.
 */
final class CliContext {
    static final String CANCEL_SUFFIX = " (0 to cancel): ";

    private final BufferedReader input;
    private final PrintStream output;
    private final CampusPersistence persistence;
    private final AcademicOfficeAdmin admin = new AcademicOfficeAdmin(
            "Console Admin", "console-admin@localhost", "", "CLI-ADMIN");
    private final List<Section> sections = new ArrayList<>();
    private final List<Instructor> instructors = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();

    CliContext(BufferedReader input, PrintStream output, CampusPersistence persistence) {
        this.input = input;
        this.output = output;
        this.persistence = persistence;
    }

    PrintStream out() {
        return output;
    }

    AcademicOfficeAdmin admin() {
        return admin;
    }

    /** All sections in this session (mutable; handlers register newly created sections here). */
    List<Section> sections() {
        return sections;
    }

    /** All instructor profiles in this session (mutable). */
    List<Instructor> instructors() {
        return instructors;
    }

    /** All student and TA profiles in this session (mutable). */
    List<Student> students() {
        return students;
    }

    // ---- Persistence ----

    void loadCatalog() throws IOException {
        sections.addAll(persistence.loadCatalog(admin, students, instructors));
        ApplicationLogger.info("catalog.loaded courses=" + admin.viewCourses().size()
                + " sections=" + sections.size());
    }

    /** Saves every record; returns false (after telling the user) if the file could not be written. */
    boolean saveCatalog(String operation) {
        try {
            persistence.saveCatalog(admin, students, instructors);
            return true;
        } catch (IOException exception) {
            ApplicationLogger.error("catalog.save_failed operation=" + operation, exception);
            output.println("Change is active for this session only; saving the catalog failed.");
            return false;
        }
    }

    // ---- Lookups ----

    Section findSection(String sectionId) {
        for (Section section : sections) {
            if (section.getSectionId().equalsIgnoreCase(sectionId)) {
                return section;
            }
        }
        return null;
    }

    Instructor findInstructor(String teacherId) {
        for (Instructor instructor : instructors) {
            if (instructor.getTeacherId().equalsIgnoreCase(teacherId)) {
                return instructor;
            }
        }
        return null;
    }

    List<NormalStudent> normalStudents() {
        List<NormalStudent> normalStudents = new ArrayList<>();
        for (Student student : students) {
            if (student instanceof NormalStudent normalStudent) {
                normalStudents.add(normalStudent);
            }
        }
        return normalStudents;
    }

    List<TeachingAssistant> teachingAssistants() {
        List<TeachingAssistant> assistants = new ArrayList<>();
        for (Student student : students) {
            if (student instanceof TeachingAssistant assistant) {
                assistants.add(assistant);
            }
        }
        return assistants;
    }

    List<Section> enrolledSections(Student student) {
        List<Section> enrolledSections = new ArrayList<>();
        for (Section section : sections) {
            if (section.getEnrolledStudents().contains(student)) {
                enrolledSections.add(section);
            }
        }
        return enrolledSections;
    }

    // ---- Numbered selection ----

    /**
     * Prints {@code items} as a numbered list and lets the user pick one.
     * Returns null when the list is empty (after printing {@code emptyMessage}), on cancel, or at end of input.
     */
    <T> T choose(List<T> items, String emptyMessage, String prompt, Function<T, String> describe)
            throws IOException {
        if (items.isEmpty()) {
            output.println(emptyMessage);
            return null;
        }
        for (int index = 0; index < items.size(); index++) {
            output.printf("%d. %s%n", index + 1, describe.apply(items.get(index)));
        }
        Integer selection = readChoice(prompt, 0, items.size());
        return selection == null || selection == 0 ? null : items.get(selection - 1);
    }

    Section selectSection(List<Section> candidates, String prompt) throws IOException {
        return choose(candidates, "No sections are available for this operation.", prompt,
                section -> String.format("%s | %s | %d seat(s) available", section.getSectionId(),
                        section.getCourse().getCourseCode(), section.getAvailableSeats()));
    }

    Student selectSectionStudent(Section section) throws IOException {
        return choose(section.getEnrolledStudents(), "Section has no enrolled students.",
                "Select a student" + CANCEL_SUFFIX, CliContext::describeStudent);
    }

    NormalStudent selectNormalStudent(String emptyMessage) throws IOException {
        return choose(normalStudents(), emptyMessage, "Select a student" + CANCEL_SUFFIX,
                CliContext::describeStudent);
    }

    Course selectCourseByCode(String prompt) throws IOException {
        String courseCode = readRequiredText(prompt);
        if (courseCode == null) {
            return null;
        }
        Course course = admin.searchCourse(courseCode);
        if (course == null) {
            output.printf("No course found with code %s.%n", courseCode);
        }
        return course;
    }

    static String describeStudent(Student student) {
        return student.getStudentId() + " | " + student.getName();
    }

    // ---- Input ----

    /** Reads a non-blank line, re-prompting on blank input; null at end of input. */
    String readRequiredText(String prompt) throws IOException {
        while (true) {
            String value = readOptionalText(prompt);
            if (value == null || !value.isEmpty()) {
                return value;
            }
            output.println("This value cannot be blank.");
        }
    }

    /** Reads one trimmed line, which may be blank; null at end of input. */
    String readOptionalText(String prompt) throws IOException {
        output.print(prompt);
        output.flush();
        String line = input.readLine();
        return line == null ? null : line.trim();
    }

    Integer readChoice(String prompt, int minimum, int maximum) throws IOException {
        while (true) {
            String line = readOptionalText(prompt);
            if (line == null) {
                return null;
            }
            try {
                int selection = Integer.parseInt(line);
                if (selection >= minimum && selection <= maximum) {
                    return selection;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input is reported below and the menu remains active.
            }
            output.printf("Enter a number from %d to %d.%n", minimum, maximum);
        }
    }

    Integer readPositiveInteger(String prompt, String errorMessage) throws IOException {
        while (true) {
            String line = readOptionalText(prompt);
            if (line == null) {
                return null;
            }
            try {
                int value = Integer.parseInt(line);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println(errorMessage);
        }
    }

    Double readPositiveDouble(String prompt) throws IOException {
        return readDouble(prompt, false, "Enter a positive finite number.");
    }

    Double readNonNegativeDouble(String prompt) throws IOException {
        return readDouble(prompt, true, "Enter a non-negative finite number.");
    }

    private Double readDouble(String prompt, boolean allowZero, String errorMessage) throws IOException {
        while (true) {
            String value = readRequiredText(prompt);
            if (value == null) {
                return null;
            }
            try {
                double number = Double.parseDouble(value);
                if (Double.isFinite(number) && (number > 0 || (allowZero && number == 0))) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is handled below.
            }
            output.println(errorMessage);
        }
    }

    LocalDate readLocalDate(String prompt) throws IOException {
        while (true) {
            String value = readRequiredText(prompt);
            if (value == null) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeException exception) {
                output.println("Enter a date in YYYY-MM-DD format.");
            }
        }
    }

    /** Reads an HH:mm time once; returns null (after an error message) on invalid input. */
    LocalTime readTime(String prompt) throws IOException {
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

    Boolean readYesNo(String prompt) throws IOException {
        while (true) {
            String answer = readRequiredText(prompt);
            if (answer == null) {
                return null;
            }
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            output.println("Enter y or n.");
        }
    }

    AttendanceStatus readAttendanceStatus(String prompt) throws IOException {
        output.println("1. Present");
        output.println("2. Absent");
        output.println("3. Late");
        Integer selection = readChoice(prompt, 1, 3);
        if (selection == null) {
            return null;
        }
        return switch (selection) {
            case 1 -> AttendanceStatus.PRESENT;
            case 2 -> AttendanceStatus.ABSENT;
            default -> AttendanceStatus.LATE;
        };
    }
}

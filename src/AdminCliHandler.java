import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Academic Office Admin workflows: courses, sections, and request processing. */
final class AdminCliHandler implements RoleCliHandler {
    private enum Option {
        CREATE_COURSE("Create Course"),
        UPDATE_COURSE("Update Course"),
        SEARCH_COURSE("Search Course"),
        CREATE_SECTION("Create Section"),
        UPDATE_SECTION("Update Section"),
        SET_SECTION_CAPACITY("Set Section Capacity"),
        ASSIGN_ROOM("Assign Room"),
        ASSIGN_INSTRUCTOR("Assign Instructor"),
        VIEW_REQUESTS("View Requests"),
        APPROVE_REQUEST("Approve Request"),
        REJECT_REQUEST("Reject Request");

        private final String label;

        Option(String label) {
            this.label = label;
        }
    }

    private static final String CREDIT_HOURS_ERROR = "Enter a positive whole number of credit hours.";
    private static final String CAPACITY_ERROR = "Enter a positive whole number for capacity.";

    private final CliContext context;
    private final AcademicOfficeAdmin admin;

    AdminCliHandler(CliContext context) {
        this.context = context;
        this.admin = context.admin();
    }

    @Override
    public String roleName() {
        return "Academic Office Admin";
    }

    @Override
    public List<String> menuOptions() {
        List<String> labels = new ArrayList<>();
        for (Option option : Option.values()) {
            labels.add(option.label);
        }
        return labels;
    }

    @Override
    public void handle(int option) throws IOException {
        switch (Option.values()[option - 1]) {
            case CREATE_COURSE -> createCourse();
            case UPDATE_COURSE -> updateCourse();
            case SEARCH_COURSE -> searchCourse();
            case CREATE_SECTION -> createSection();
            case UPDATE_SECTION -> updateSection();
            case SET_SECTION_CAPACITY -> withSection(this::updateSectionCapacity);
            case ASSIGN_ROOM -> withSection(this::assignRoom);
            case ASSIGN_INSTRUCTOR -> withSection(this::assignInstructor);
            case VIEW_REQUESTS -> viewRequests();
            case APPROVE_REQUEST -> processRequest(RequestStatus.APPROVED);
            case REJECT_REQUEST -> processRequest(RequestStatus.REJECTED);
        }
    }

    // ---- Courses ----

    private void createCourse() throws IOException {
        String courseCode = context.readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }
        String title = context.readRequiredText("Course title: ");
        if (title == null) {
            return;
        }
        Integer creditHours = context.readPositiveInteger("Credit hours: ", CREDIT_HOURS_ERROR);
        if (creditHours == null) {
            return;
        }
        if (admin.searchCourse(courseCode) != null) {
            ApplicationLogger.info("course.create_rejected duplicate_code=" + courseCode);
            context.out().printf("Course %s already exists; no course was created.%n", courseCode);
            return;
        }

        try {
            Course course = new Course(courseCode, title, creditHours);
            admin.createCourse(course);
            if (admin.searchCourse(courseCode) == course) {
                attachPrerequisitesFromPrompt(course);
                if (context.saveCatalog("course.create")) {
                    ApplicationLogger.info("course.created code=" + courseCode);
                    context.out().printf("Course %s created successfully.%n", courseCode);
                }
            } else {
                ApplicationLogger.info("course.create_failed code=" + courseCode);
                context.out().printf("Course %s was not created.%n", courseCode);
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("course.create_failed code=" + courseCode, exception);
            context.out().printf("Course was not created: %s%n", exception.getMessage());
        }
    }

    private void searchCourse() throws IOException {
        String courseCode = context.readRequiredText("Course code to search: ");
        if (courseCode == null) {
            return;
        }
        Course course = admin.searchCourse(courseCode);
        if (course == null) {
            ApplicationLogger.info("course.search_not_found code=" + courseCode);
            context.out().printf("No course found with code %s.%n", courseCode);
            return;
        }
        ApplicationLogger.info("course.searched code=" + course.getCourseCode());
        context.out().printf("Course found: %s | %s | %d credit hour(s)%n",
                course.getCourseCode(), course.getTitle(), course.getCreditHours());
    }

    private void updateCourse() throws IOException {
        Course existingCourse = context.selectCourseByCode("Course code to update: ");
        if (existingCourse == null) {
            return;
        }
        String title = context.readRequiredText("New course title: ");
        if (title == null) {
            return;
        }
        Integer creditHours = context.readPositiveInteger("New credit hours: ", CREDIT_HOURS_ERROR);
        if (creditHours == null) {
            return;
        }

        String courseCode = existingCourse.getCourseCode();
        admin.updateCourse(new Course(courseCode, title, creditHours));
        if (admin.searchCourse(courseCode) == existingCourse
                && existingCourse.getTitle().equals(title)
                && existingCourse.getCreditHours() == creditHours) {
            attachPrerequisitesFromPrompt(existingCourse);
            if (context.saveCatalog("course.update")) {
                ApplicationLogger.info("course.updated code=" + courseCode);
                context.out().printf("Course %s updated successfully.%n", courseCode);
            }
        } else {
            ApplicationLogger.info("course.update_failed code=" + courseCode);
            context.out().printf("Course %s was not updated.%n", courseCode);
        }
    }

    private void attachPrerequisitesFromPrompt(Course course) throws IOException {
        String raw = context.readOptionalText(
                "Prerequisite course codes (comma-separated, or blank to skip): ");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String token : raw.split(",")) {
            String prerequisiteCode = token.trim();
            if (prerequisiteCode.isEmpty()) {
                continue;
            }
            Course prerequisite = admin.searchCourse(prerequisiteCode);
            if (prerequisite == null) {
                context.out().printf("Prerequisite %s was not found; it was not linked.%n", prerequisiteCode);
            } else if (prerequisite == course) {
                context.out().println("A course cannot be its own prerequisite.");
            } else {
                course.addPrerequisite(prerequisite);
                context.out().printf("Linked prerequisite %s to course %s.%n",
                        prerequisite.getCourseCode(), course.getCourseCode());
            }
        }
    }

    // ---- Sections ----

    private void createSection() throws IOException {
        String sectionId = context.readRequiredText("Section ID: ");
        if (sectionId == null) {
            return;
        }
        if (context.findSection(sectionId) != null) {
            ApplicationLogger.info("section.create_rejected duplicate_id=" + sectionId);
            context.out().printf("Section %s already exists.%n", sectionId);
            return;
        }
        String courseCode = context.readRequiredText("Course code: ");
        if (courseCode == null) {
            return;
        }
        Course course = admin.searchCourse(courseCode);
        if (course == null) {
            ApplicationLogger.info("section.create_rejected unknown_course=" + courseCode);
            context.out().printf("No course found with code %s.%n", courseCode);
            return;
        }
        Integer capacity = context.readPositiveInteger("Section capacity: ", CAPACITY_ERROR);
        if (capacity == null) {
            return;
        }

        Section section = new Section(sectionId, capacity, course);
        admin.createSection(section);
        if (course.getSections().contains(section)) {
            context.sections().add(section);
            if (context.saveCatalog("section.create")) {
                ApplicationLogger.info("section.created id=" + sectionId + " course=" + course.getCourseCode());
                context.out().printf("Section %s created for course %s.%n", sectionId, course.getCourseCode());
            }
        } else {
            ApplicationLogger.info("section.create_failed id=" + sectionId);
            context.out().printf("Section %s was not created.%n", sectionId);
        }
    }

    private void updateSection() throws IOException {
        Section section = findSection();
        if (section == null) {
            return;
        }
        context.out().printf("Update Section %s%n", section.getSectionId());
        context.out().println("0. Cancel");
        context.out().println("1. Update capacity");
        context.out().println("2. Update room and schedule");
        Integer selection = context.readChoice("Select a field to update: ", 0, 2);
        if (selection == null || selection == 0) {
            return;
        }
        if (selection == 1) {
            updateSectionCapacity(section);
        } else {
            assignRoom(section);
        }
    }

    private void updateSectionCapacity(Section section) throws IOException {
        Integer capacity = context.readPositiveInteger("New capacity: ", CAPACITY_ERROR);
        if (capacity == null) {
            return;
        }
        try {
            admin.setCapacity(section, capacity);
            if (context.saveCatalog("section.capacity")) {
                ApplicationLogger.info("section.capacity_changed id=" + section.getSectionId()
                        + " capacity=" + section.getCapacity());
                context.out().printf("Capacity for section %s set to %d.%n",
                        section.getSectionId(), section.getCapacity());
            }
        } catch (IllegalArgumentException exception) {
            ApplicationLogger.error("section.capacity_change_failed id=" + section.getSectionId(), exception);
            context.out().printf("Capacity was not changed: %s%n", exception.getMessage());
        }
    }

    private void assignRoom(Section section) throws IOException {
        String dayInput = context.readRequiredText("Day (Monday-Saturday): ");
        if (dayInput == null) {
            return;
        }
        Day day;
        try {
            day = Day.valueOf(dayInput.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            context.out().println("Enter a day from Monday through Saturday.");
            return;
        }
        LocalTime startTime = context.readTime("Start time (HH:mm): ");
        if (startTime == null) {
            return;
        }
        LocalTime endTime = context.readTime("End time (HH:mm): ");
        if (endTime == null) {
            return;
        }
        if (!endTime.isAfter(startTime)) {
            context.out().println("End time must be after start time.");
            return;
        }
        String room = context.readRequiredText("Room: ");
        if (room == null) {
            return;
        }

        Schedule schedule = new Schedule(day, startTime, endTime, room);
        admin.assignRoom(section, schedule);
        if (context.saveCatalog("section.room")) {
            ApplicationLogger.info("section.room_assigned id=" + section.getSectionId()
                    + " schedule=" + schedule.getScheduleInfo());
            context.out().printf("Room assigned to section %s: %s%n",
                    section.getSectionId(), schedule.getScheduleInfo());
        }
    }

    private void assignInstructor(Section section) throws IOException {
        String teacherId = context.readRequiredText("Instructor ID: ");
        if (teacherId == null) {
            return;
        }
        Instructor instructor = context.findInstructor(teacherId);
        if (instructor == null) {
            instructor = createInstructorProfile(teacherId);
            if (instructor == null) {
                return;
            }
            context.instructors().add(instructor);
        }

        admin.assignInstructor(section, instructor);
        if (section.getInstructor() != instructor) {
            context.out().printf("Instructor was not assigned to section %s.%n", section.getSectionId());
        } else if (context.saveCatalog("instructor.assign")) {
            ApplicationLogger.info("instructor.assigned teacher_id=" + instructor.getTeacherId()
                    + " section=" + section.getSectionId());
            context.out().printf("%s assigned to section %s.%n",
                    instructor.getRole(), section.getSectionId());
        }
    }

    /** Prompts for a new instructor's type and contact details; null if input ends. */
    private Instructor createInstructorProfile(String teacherId) throws IOException {
        Integer instructorType = context.readChoice(
                "1. Permanent Instructor\n2. Visiting Instructor\nSelect type: ", 1, 2);
        if (instructorType == null) {
            return null;
        }
        String name = context.readRequiredText("Name: ");
        if (name == null) {
            return null;
        }
        String email = context.readRequiredText("Email: ");
        if (email == null) {
            return null;
        }
        String phone = context.readRequiredText("Phone: ");
        if (phone == null) {
            return null;
        }
        return instructorType == 1
                ? new PermanentInstructor(name, email, phone, teacherId)
                : new VisitingInstructor(name, email, phone, teacherId);
    }

    private void withSection(SectionAction action) throws IOException {
        Section section = findSection();
        if (section != null) {
            action.run(section);
        }
    }

    private Section findSection() throws IOException {
        String sectionId = context.readRequiredText("Section ID: ");
        if (sectionId == null) {
            return null;
        }
        Section section = context.findSection(sectionId);
        if (section == null) {
            context.out().printf("No section found with ID %s in this CLI session.%n", sectionId);
        }
        return section;
    }

    // ---- Requests ----

    private void viewRequests() {
        List<Request> requests = pendingRequests();
        ApplicationLogger.info("requests.viewed pending_count=" + requests.size());
        if (requests.isEmpty()) {
            context.out().println("No pending requests are currently available.");
            return;
        }
        for (int index = 0; index < requests.size(); index++) {
            context.out().printf("%d. %s%n", index + 1, requests.get(index).getDetails());
        }
    }

    private void processRequest(RequestStatus status) throws IOException {
        List<Request> requests = pendingRequests();
        ApplicationLogger.info("requests.processing_menu status=" + status + " count=" + requests.size());
        Request request = context.choose(requests, "No pending requests are currently available to process.",
                "Select a request" + CliContext.CANCEL_SUFFIX, Request::getDetails);
        if (request == null) {
            return;
        }

        try {
            if (status == RequestStatus.APPROVED) {
                admin.approveRequest(request);
            } else {
                admin.rejectRequest(request);
            }
        } catch (InvalidRequestException exception) {
            ApplicationLogger.error("request.processing_failed id=" + request.getRequestId(), exception);
            context.out().println("Request was not processed: " + exception.getMessage());
            return;
        }
        if (request.getStatus() != status) {
            ApplicationLogger.info("request.processing_failed id=" + request.getRequestId());
            context.out().printf("Request %s was not processed.%n", request.getRequestId());
        } else if (context.saveCatalog("request.process")) {
            ApplicationLogger.info("request.processed id=" + request.getRequestId() + " status=" + status);
            context.out().printf("Request %s %s.%n", request.getRequestId(),
                    status.name().toLowerCase(Locale.ROOT));
        }
    }

    /** Pending requests, highest priority first. */
    private List<Request> pendingRequests() {
        List<Request> pendingRequests = new ArrayList<>();
        for (Request request : admin.viewRequests()) {
            if (request.getStatus() == RequestStatus.PENDING) {
                pendingRequests.add(request);
            }
        }
        pendingRequests.sort(new RequestPriorityComparator());
        return pendingRequests;
    }

    @FunctionalInterface
    private interface SectionAction {
        void run(Section section) throws IOException;
    }
}

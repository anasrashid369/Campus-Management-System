import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.Locale;

public final class CampusPersistence {
    private final Path catalogFile;

    public CampusPersistence() {
        this(Path.of("data", "catalog.txt"));
    }

    CampusPersistence(Path catalogFile) {
        this.catalogFile = catalogFile;
    }

    public List<Section> loadCatalog(AcademicOfficeAdmin admin) throws IOException {
        return loadCatalog(admin, new ArrayList<>(), new ArrayList<>());
    }

    public List<Section> loadCatalog(AcademicOfficeAdmin admin, List<Student> students) throws IOException {
        return loadCatalog(admin, students, new ArrayList<>());
    }

    public List<Section> loadCatalog(AcademicOfficeAdmin admin, List<Student> students,
                                    List<Instructor> instructors) throws IOException {
        if (!Files.exists(catalogFile)) {
            return List.of();
        }
        if (!admin.viewCourses().isEmpty() || !admin.viewSections().isEmpty()
                || !students.isEmpty() || !instructors.isEmpty()) {
            throw new IOException("Catalog can only be loaded into an empty admin session.");
        }

        List<Course> courses = new ArrayList<>();
        List<PrerequisiteRecord> prerequisiteRecords = new ArrayList<>();
        List<SectionRecord> sectionRecords = new ArrayList<>();
        List<StudentRecord> studentRecords = new ArrayList<>();
        List<EnrollmentRecord> enrollmentRecords = new ArrayList<>();
        List<RequestRecord> requestRecords = new ArrayList<>();
        List<AttendanceRecord> attendanceRecords = new ArrayList<>();
        List<InstructorRecord> instructorRecords = new ArrayList<>();
        List<InstructorAssignmentRecord> instructorAssignmentRecords = new ArrayList<>();
        List<AssignmentRecord> assignmentRecords = new ArrayList<>();
        List<SubmissionRecord> submissionRecords = new ArrayList<>();
        List<FypGroupRecord> fypGroupRecords = new ArrayList<>();
        List<FypMemberRecord> fypMemberRecords = new ArrayList<>();
        List<FypMeetingRecord> fypMeetingRecords = new ArrayList<>();
        List<FypEvaluationRecord> fypEvaluationRecords = new ArrayList<>();
        Set<String> courseCodes = new HashSet<>();
        Set<String> prerequisiteKeys = new HashSet<>();
        Set<String> sectionIds = new HashSet<>();
        Set<String> studentIds = new HashSet<>();
        Set<String> enrollmentKeys = new HashSet<>();
        Set<String> requestIds = new HashSet<>();
        Set<String> instructorIds = new HashSet<>();
        Set<String> instructorSectionIds = new HashSet<>();
        Set<String> assignmentIds = new HashSet<>();
        Set<String> submissionIds = new HashSet<>();
        Set<String> fypGroupIds = new HashSet<>();
        Set<String> fypMemberKeys = new HashSet<>();
        Set<String> fypMeetingKeys = new HashSet<>();
        Set<String> fypEvaluationKeys = new HashSet<>();
        List<String> lines = Files.readAllLines(catalogFile, StandardCharsets.UTF_8);

        for (int index = 0; index < lines.size(); index++) {
            String[] fields = lines.get(index).split("\t", -1);
            int lineNumber = index + 1;
            if (fields.length == 0) {
                throw malformed(lineNumber, "empty record");
            }
            try {
                if (fields[0].equals("COURSE") && fields.length == 4) {
                    String code = decode(fields[1]);
                    String title = decode(fields[2]);
                    int creditHours = Integer.parseInt(fields[3]);
                    requireText(code, "course code");
                    requireText(title, "course title");
                    if (creditHours <= 0 || !courseCodes.add(code.toUpperCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("invalid or duplicate course");
                    }
                    courses.add(new Course(code, title, creditHours));
                } else if (fields[0].equals("PREREQUISITE") && fields.length == 3) {
                    String courseCode = decode(fields[1]);
                    String prerequisiteCode = decode(fields[2]);
                    requireText(courseCode, "course code");
                    requireText(prerequisiteCode, "prerequisite course code");
                    String key = courseCode.toUpperCase(Locale.ROOT) + "\u0000"
                            + prerequisiteCode.toUpperCase(Locale.ROOT);
                    if (!prerequisiteKeys.add(key)) {
                        throw new IllegalArgumentException("duplicate prerequisite");
                    }
                    prerequisiteRecords.add(new PrerequisiteRecord(courseCode, prerequisiteCode, lineNumber));
                } else if (fields[0].equals("SECTION") && fields.length == 8) {
                    String sectionId = decode(fields[1]);
                    String courseCode = decode(fields[2]);
                    int capacity = Integer.parseInt(fields[3]);
                    String day = decode(fields[4]);
                    String startTime = decode(fields[5]);
                    String endTime = decode(fields[6]);
                    String room = decode(fields[7]);
                    requireText(sectionId, "section ID");
                    requireText(courseCode, "course code");
                    if (capacity <= 0 || !sectionIds.add(sectionId.toUpperCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("invalid or duplicate section");
                    }
                    validateSchedule(day, startTime, endTime, room);
                    sectionRecords.add(new SectionRecord(sectionId, courseCode, capacity,
                            day, startTime, endTime, room, lineNumber));
                } else if (fields[0].equals("STUDENT")
                        && ((fields.length == 7 && fields[1].equals("NORMAL"))
                        || (fields.length == 8 && fields[1].equals("TA")))) {
                    String type = fields[1];
                    String studentId = decode(fields[2]);
                    String name = decode(fields[3]);
                    String email = decode(fields[4]);
                    String phone = decode(fields[5]);
                    int totalCreditHours = Integer.parseInt(fields[6]);
                    String assignedSectionId = type.equals("TA") ? decode(fields[7]) : "";
                    requireText(studentId, "student ID");
                    requireText(name, "student name");
                    requireText(email, "student email");
                    String profileKey = type + "\u0000" + studentId.toUpperCase(Locale.ROOT);
                    if (totalCreditHours < 0 || !studentIds.add(profileKey)
                            || (type.equals("TA") && assignedSectionId.isBlank())) {
                        throw new IllegalArgumentException("invalid or duplicate student");
                    }
                    studentRecords.add(new StudentRecord(studentId, name, email, phone,
                            totalCreditHours, type, assignedSectionId, lineNumber));
                } else if (fields[0].equals("ENROLLMENT") && fields.length == 3) {
                    String studentId = decode(fields[1]);
                    String sectionId = decode(fields[2]);
                    String key = studentId.toUpperCase(Locale.ROOT) + "\u0000"
                            + sectionId.toUpperCase(Locale.ROOT);
                    if (studentId.isBlank() || sectionId.isBlank() || !enrollmentKeys.add(key)) {
                        throw new IllegalArgumentException("invalid or duplicate enrollment");
                    }
                    enrollmentRecords.add(new EnrollmentRecord(studentId, sectionId, lineNumber));
                    } else if (fields[0].equals("REQUEST") && fields.length == 10
                        && fields[1].equals("CLASH")) {
                        RequestRecord record = new RequestRecord("CLASH", decode(fields[2]),
                            LocalDate.parse(fields[3]).toString(), decode(fields[4]),
                            Integer.parseInt(fields[5]), RequestStatus.valueOf(fields[6]).name(),
                            decode(fields[7]), decode(fields[8]), decode(fields[9]), "", lineNumber);
                        validateRequestIdentity(record, requestIds);
                        requestRecords.add(record);
                    } else if (fields[0].equals("REQUEST") && fields.length == 9
                        && fields[1].equals("GENERIC")) {
                        RequestRecord record = new RequestRecord("GENERIC", decode(fields[2]),
                            LocalDate.parse(fields[3]).toString(), decode(fields[4]),
                            Integer.parseInt(fields[5]), RequestStatus.valueOf(fields[6]).name(),
                            decode(fields[7]), "", "", RequestCategory.valueOf(fields[8]).name(), lineNumber);
                        validateRequestIdentity(record, requestIds);
                        requestRecords.add(record);
                    } else if (fields[0].equals("ATTENDANCE") && fields.length == 5) {
                        String studentId = decode(fields[1]);
                        String sectionId = decode(fields[2]);
                        String date = LocalDate.parse(fields[3]).toString();
                        AttendanceStatus status = AttendanceStatus.valueOf(fields[4]);
                        requireText(studentId, "attendance student ID");
                        requireText(sectionId, "attendance section ID");
                        attendanceRecords.add(new AttendanceRecord(studentId, sectionId,
                                date, status, lineNumber));
                    } else if (fields[0].equals("INSTRUCTOR") && fields.length == 6) {
                        String role = decode(fields[1]);
                        String teacherId = decode(fields[2]);
                        String name = decode(fields[3]);
                        String email = decode(fields[4]);
                        String phone = decode(fields[5]);
                        requireText(teacherId, "instructor ID");
                        requireText(name, "instructor name");
                        if ((!role.equals("Permanent Instructor") && !role.equals("Visiting Instructor"))
                                || !instructorIds.add(teacherId.toUpperCase(Locale.ROOT))) {
                            throw new IllegalArgumentException("invalid or duplicate instructor");
                        }
                        instructorRecords.add(new InstructorRecord(role, teacherId, name, email, phone, lineNumber));
                    } else if (fields[0].equals("INSTRUCTOR_SECTION") && fields.length == 3) {
                        String teacherId = decode(fields[1]);
                        String sectionId = decode(fields[2]);
                        if (teacherId.isBlank() || sectionId.isBlank()
                                || !instructorSectionIds.add(sectionId.toUpperCase(Locale.ROOT))) {
                            throw new IllegalArgumentException("invalid or duplicate instructor-section assignment");
                        }
                        instructorAssignmentRecords.add(
                                new InstructorAssignmentRecord(teacherId, sectionId, lineNumber));
                        } else if (fields[0].equals("ASSIGNMENT") && fields.length == 8) {
                            String id = decode(fields[1]);
                            String title = decode(fields[2]);
                            String description = decode(fields[3]);
                            String deadline = LocalDate.parse(fields[4]).toString();
                            double totalMarks = Double.parseDouble(fields[5]);
                            String sectionId = decode(fields[6]);
                            String createdById = decode(fields[7]);
                            requireText(id, "assignment ID");
                            requireText(title, "assignment title");
                            requireText(sectionId, "assignment section ID");
                            requireText(createdById, "assignment TA ID");
                            if (!Double.isFinite(totalMarks) || totalMarks <= 0
                                || !assignmentIds.add(id.toUpperCase(Locale.ROOT))) {
                            throw new IllegalArgumentException("invalid or duplicate assignment");
                            }
                            assignmentRecords.add(new AssignmentRecord(id, title, description, deadline,
                                totalMarks, sectionId, createdById, lineNumber));
                        } else if (fields[0].equals("SUBMISSION") && fields.length == 13) {
                            String submissionId = decode(fields[1]);
                            String assignmentId = decode(fields[2]);
                            String studentId = decode(fields[3]);
                            String submissionDate = fields[4].isEmpty() ? "" : LocalDate.parse(fields[4]).toString();
                            String content = decode(fields[5]);
                            double marks = Double.parseDouble(fields[6]);
                            SubmissionStatus status = SubmissionStatus.valueOf(fields[7]);
                            String feedbackId = decode(fields[8]);
                            String evaluatorType = fields[9];
                            String evaluatorId = decode(fields[10]);
                            String feedbackComments = decode(fields[11]);
                            String feedbackDate = fields[12].isEmpty() ? "" : LocalDate.parse(fields[12]).toString();
                            requireText(submissionId, "submission ID");
                            requireText(assignmentId, "submission assignment ID");
                            requireText(studentId, "submission student ID");
                            if (!Double.isFinite(marks) || marks < 0
                                || (status != SubmissionStatus.PENDING && submissionDate.isEmpty())
                                || !submissionIds.add(submissionId.toUpperCase(Locale.ROOT))) {
                            throw new IllegalArgumentException("invalid or duplicate submission");
                            }
                            boolean hasFeedback = !feedbackId.isEmpty() || !evaluatorType.isEmpty()
                                || !evaluatorId.isEmpty() || !feedbackComments.isEmpty() || !feedbackDate.isEmpty();
                            if (hasFeedback && (feedbackId.isEmpty() || evaluatorType.isEmpty()
                                || evaluatorId.isEmpty() || feedbackDate.isEmpty())) {
                            throw new IllegalArgumentException("incomplete submission feedback record");
                            }
                            submissionRecords.add(new SubmissionRecord(submissionId, assignmentId, studentId,
                                submissionDate, content, marks, status, feedbackId, evaluatorType,
                                evaluatorId, feedbackComments, feedbackDate, lineNumber));
                } else if (fields[0].equals("FYP_GROUP") && fields.length == 5) {
                    String groupId = decode(fields[1]);
                    String title = decode(fields[2]);
                    String description = decode(fields[3]);
                    String supervisorId = decode(fields[4]);
                    requireText(groupId, "FYP group ID");
                    requireText(title, "FYP group title");
                    if (!fypGroupIds.add(groupId.toUpperCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("duplicate FYP group ID");
                    }
                    fypGroupRecords.add(new FypGroupRecord(groupId, title, description,
                            supervisorId, lineNumber));
                } else if (fields[0].equals("FYP_MEMBER") && fields.length == 3) {
                    String groupId = decode(fields[1]);
                    String studentId = decode(fields[2]);
                    String key = groupId.toUpperCase(Locale.ROOT) + "\u0000"
                            + studentId.toUpperCase(Locale.ROOT);
                    if (groupId.isBlank() || studentId.isBlank() || !fypMemberKeys.add(key)) {
                        throw new IllegalArgumentException("invalid or duplicate FYP member");
                    }
                    fypMemberRecords.add(new FypMemberRecord(groupId, studentId, lineNumber));
                } else if (fields[0].equals("FYP_MEETING") && fields.length == 6) {
                    String groupId = decode(fields[1]);
                    String meetingId = decode(fields[2]);
                    String meetingDate = LocalDate.parse(fields[3]).toString();
                    String agenda = decode(fields[4]);
                    String notes = decode(fields[5]);
                    requireText(groupId, "FYP group ID");
                    requireText(meetingId, "FYP meeting ID");
                    String key = groupId.toUpperCase(Locale.ROOT) + "\u0000"
                            + meetingId.toUpperCase(Locale.ROOT);
                    if (!fypMeetingKeys.add(key)) {
                        throw new IllegalArgumentException("duplicate FYP meeting ID");
                    }
                    fypMeetingRecords.add(new FypMeetingRecord(groupId, meetingId,
                            meetingDate, agenda, notes, lineNumber));
                } else if (fields[0].equals("FYP_EVALUATION") && fields.length == 8) {
                    String groupId = decode(fields[1]);
                    String evaluationId = decode(fields[2]);
                    String evaluationDate = LocalDate.parse(fields[3]).toString();
                    String evaluatorType = fields[4];
                    String evaluatorId = decode(fields[5]);
                    double score = Double.parseDouble(fields[6]);
                    String feedback = decode(fields[7]);
                    requireText(groupId, "FYP group ID");
                    requireText(evaluationId, "FYP evaluation ID");
                    requireText(evaluatorId, "FYP evaluator ID");
                    String key = groupId.toUpperCase(Locale.ROOT) + "\u0000"
                            + evaluationId.toUpperCase(Locale.ROOT);
                    if (!Double.isFinite(score) || score < 0 || !fypEvaluationKeys.add(key)
                            || (!evaluatorType.equals("TA") && !evaluatorType.equals("PERMANENT"))) {
                        throw new IllegalArgumentException("invalid or duplicate FYP evaluation");
                    }
                    fypEvaluationRecords.add(new FypEvaluationRecord(groupId, evaluationId,
                            evaluationDate, evaluatorType, evaluatorId, score, feedback, lineNumber));
                } else {
                    throw new IllegalArgumentException("unknown record or wrong field count");
                }
            } catch (IllegalArgumentException | DateTimeException exception) {
                throw malformed(lineNumber, exception.getMessage(), exception);
            }
        }

        Map<String, Course> coursesByCode = new HashMap<>();
        for (Course course : courses) {
            coursesByCode.put(course.getCourseCode().toUpperCase(Locale.ROOT), course);
        }
        for (SectionRecord record : sectionRecords) {
            if (!coursesByCode.containsKey(record.courseCode().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "section references unknown course " + record.courseCode());
            }
        }
        for (PrerequisiteRecord record : prerequisiteRecords) {
            if (!coursesByCode.containsKey(record.courseCode().toUpperCase(Locale.ROOT))
                    || !coursesByCode.containsKey(record.prerequisiteCode().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "prerequisite references an unknown course");
            }
        }

        Map<String, SectionRecord> sectionsById = new HashMap<>();
        Map<String, Integer> enrollmentsPerSection = new HashMap<>();
        for (SectionRecord record : sectionRecords) {
            sectionsById.put(record.sectionId().toUpperCase(Locale.ROOT), record);
        }
        for (EnrollmentRecord record : enrollmentRecords) {
            if (!studentIds.contains("NORMAL\u0000" + record.studentId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "enrollment references unknown student " + record.studentId());
            }
            SectionRecord section = sectionsById.get(record.sectionId().toUpperCase(Locale.ROOT));
            if (section == null) {
                throw malformed(record.lineNumber(), "enrollment references unknown section " + record.sectionId());
            }
            int count = enrollmentsPerSection.merge(section.sectionId().toUpperCase(Locale.ROOT), 1, Integer::sum);
            if (count > section.capacity()) {
                throw malformed(record.lineNumber(), "section enrollment exceeds capacity");
            }
        }
        for (StudentRecord record : studentRecords) {
            if (record.type().equals("TA")
                    && !sectionsById.containsKey(record.assignedSectionId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "TA references unknown assigned section "
                        + record.assignedSectionId());
            }
        }
        for (RequestRecord record : requestRecords) {
            if (!studentIds.contains("NORMAL\u0000" + record.studentId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "request references unknown student " + record.studentId());
            }
            if (record.type().equals("CLASH")) {
                if (!sectionsById.containsKey(record.conflictingSectionId().toUpperCase(Locale.ROOT))
                        || !sectionsById.containsKey(record.requestedSectionId().toUpperCase(Locale.ROOT))) {
                    throw malformed(record.lineNumber(), "clash request references unknown section");
                }
            }
        }
        for (AttendanceRecord record : attendanceRecords) {
            if (!studentIds.contains("NORMAL\u0000" + record.studentId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "attendance references unknown student " + record.studentId());
            }
            if (!sectionsById.containsKey(record.sectionId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "attendance references unknown section " + record.sectionId());
            }
        }
        Map<String, InstructorRecord> instructorsById = new HashMap<>();
        for (InstructorRecord record : instructorRecords) {
            instructorsById.put(record.teacherId().toUpperCase(Locale.ROOT), record);
        }
        for (InstructorAssignmentRecord record : instructorAssignmentRecords) {
            if (!instructorsById.containsKey(record.teacherId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "assignment references unknown instructor " + record.teacherId());
            }
            if (!sectionsById.containsKey(record.sectionId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "assignment references unknown section " + record.sectionId());
            }
        }
        Map<String, AssignmentRecord> assignmentsById = new HashMap<>();
        for (AssignmentRecord record : assignmentRecords) {
            if (!sectionsById.containsKey(record.sectionId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "assignment references unknown section " + record.sectionId());
            }
            if (!studentIds.contains("TA\u0000" + record.createdById().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "assignment references unknown TA " + record.createdById());
            }
            assignmentsById.put(record.id().toUpperCase(Locale.ROOT), record);
        }
        for (SubmissionRecord record : submissionRecords) {
            AssignmentRecord assignment = assignmentsById.get(record.assignmentId().toUpperCase(Locale.ROOT));
            if (assignment == null) {
                throw malformed(record.lineNumber(), "submission references unknown assignment "
                        + record.assignmentId());
            }
            if (!studentIds.contains("NORMAL\u0000" + record.studentId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "submission references unknown student " + record.studentId());
            }
            if (record.marks() > assignment.totalMarks()) {
                throw malformed(record.lineNumber(), "submission marks exceed assignment total");
            }
            if (!record.feedbackId().isEmpty()) {
                boolean knownEvaluator = record.evaluatorType().equals("TA")
                        ? studentIds.contains("TA\u0000" + record.evaluatorId().toUpperCase(Locale.ROOT))
                        : record.evaluatorType().equals("PERMANENT")
                        && instructorsById.containsKey(record.evaluatorId().toUpperCase(Locale.ROOT))
                        && instructorsById.get(record.evaluatorId().toUpperCase(Locale.ROOT))
                        .role().equals("Permanent Instructor");
                if (!knownEvaluator) {
                    throw malformed(record.lineNumber(), "feedback references an unsupported evaluator");
                }
            }
        }
        for (FypGroupRecord record : fypGroupRecords) {
            if (!record.supervisorId().isEmpty()) {
                InstructorRecord supervisor = instructorsById.get(record.supervisorId().toUpperCase(Locale.ROOT));
                if (supervisor == null || !supervisor.role().equals("Permanent Instructor")) {
                    throw malformed(record.lineNumber(), "FYP group has an invalid supervisor");
                }
            }
        }
        for (FypMemberRecord record : fypMemberRecords) {
            if (!fypGroupIds.contains(record.groupId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "FYP member references unknown group " + record.groupId());
            }
            if (!studentIds.contains("NORMAL\u0000" + record.studentId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "FYP member references unknown student " + record.studentId());
            }
        }
        for (FypMeetingRecord record : fypMeetingRecords) {
            if (!fypGroupIds.contains(record.groupId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "FYP meeting references unknown group " + record.groupId());
            }
        }
        for (FypEvaluationRecord record : fypEvaluationRecords) {
            if (!fypGroupIds.contains(record.groupId().toUpperCase(Locale.ROOT))) {
                throw malformed(record.lineNumber(), "FYP evaluation references unknown group " + record.groupId());
            }
            boolean evaluatorExists = record.evaluatorType().equals("TA")
                    ? studentIds.contains("TA\u0000" + record.evaluatorId().toUpperCase(Locale.ROOT))
                    : studentIds.contains("PERMANENT\u0000" + record.evaluatorId().toUpperCase(Locale.ROOT));
            if (record.evaluatorType().equals("PERMANENT")) {
                InstructorRecord evaluator = instructorsById.get(record.evaluatorId().toUpperCase(Locale.ROOT));
                evaluatorExists = evaluator != null && evaluator.role().equals("Permanent Instructor");
            }
            if (!evaluatorExists) {
                throw malformed(record.lineNumber(), "FYP evaluation references unknown evaluator");
            }
        }

        for (Course course : courses) {
            admin.createCourse(course);
        }
        for (PrerequisiteRecord record : prerequisiteRecords) {
            coursesByCode.get(record.courseCode().toUpperCase(Locale.ROOT)).addPrerequisite(
                    coursesByCode.get(record.prerequisiteCode().toUpperCase(Locale.ROOT)));
        }
        List<Section> sections = new ArrayList<>();
        Map<String, Section> sectionsByIdLoaded = new HashMap<>();
        for (SectionRecord record : sectionRecords) {
            Section section = new Section(record.sectionId(), record.capacity());
            if (!record.courseCode().isEmpty()) {
                Course course = coursesByCode.get(record.courseCode().toUpperCase(Locale.ROOT));
                if (course != null) {
                    section.addCourse(course);
                }
            }
            admin.createSection(section);
            if (!record.day().isEmpty()) {
                section.setSchedule(new Schedule(Day.valueOf(record.day()),
                        LocalTime.parse(record.startTime()), LocalTime.parse(record.endTime()), record.room()));
            }
            sections.add(section);
            sectionsByIdLoaded.put(record.sectionId().toUpperCase(Locale.ROOT), section);
        }
        Map<String, Student> studentsById = new HashMap<>();
        Map<String, TeachingAssistant> teachingAssistantsById = new HashMap<>();
        for (StudentRecord record : studentRecords) {
            Student student;
            if (record.type().equals("NORMAL")) {
                student = new NormalStudent(record.name(), record.email(), record.phone(),
                        record.studentId(), record.totalCreditHours());
                studentsById.put(record.studentId().toUpperCase(Locale.ROOT), student);
            } else {
                TeachingAssistant assistant = new TeachingAssistant(record.name(), record.email(),
                        record.phone(), record.studentId(), record.totalCreditHours());
                sectionsByIdLoaded.get(record.assignedSectionId().toUpperCase(Locale.ROOT)).assignTA(assistant);
                teachingAssistantsById.put(record.studentId().toUpperCase(Locale.ROOT), assistant);
                student = assistant;
            }
            students.add(student);
        }
        for (EnrollmentRecord record : enrollmentRecords) {
            Student student = studentsById.get(record.studentId().toUpperCase(Locale.ROOT));
            Section section = sectionsByIdLoaded.get(record.sectionId().toUpperCase(Locale.ROOT));
            try {
                section.enroll(student);
            } catch (CourseFullException exception) {
                throw new IOException("Persisted enrollment exceeds section capacity: "
                        + section.getSectionId(), exception);
            }
        }
        Map<String, Student> loadedStudentsById = new HashMap<>(studentsById);
        for (RequestRecord record : requestRecords) {
            Student student = loadedStudentsById.get(record.studentId().toUpperCase(Locale.ROOT));
            Request request;
            if (record.type().equals("CLASH")) {
                request = new CourseClashRequest(record.requestId(), LocalDate.parse(record.requestDate()),
                        record.description(), record.priority(), student,
                        sectionsByIdLoaded.get(record.conflictingSectionId().toUpperCase(Locale.ROOT)),
                        sectionsByIdLoaded.get(record.requestedSectionId().toUpperCase(Locale.ROOT)));
            } else {
                request = new GenericRequest(record.requestId(), LocalDate.parse(record.requestDate()),
                        record.description(), record.priority(), student,
                        RequestCategory.valueOf(record.category()));
            }
            RequestStatus status = RequestStatus.valueOf(record.status());
            request.setStatus(status);
            if (status != RequestStatus.PENDING) {
                request.setProcessedBy(admin);
            }
            try {
                admin.addRequest(request);
            } catch (InvalidRequestException exception) {
                throw new IOException("Invalid persisted request: " + request.getRequestId(), exception);
            }
        }
        for (AttendanceRecord record : attendanceRecords) {
            Student student = loadedStudentsById.get(record.studentId().toUpperCase(Locale.ROOT));
            Section section = sectionsByIdLoaded.get(record.sectionId().toUpperCase(Locale.ROOT));
            section.addAttendance(new Attendance(student, section,
                    LocalDate.parse(record.date()), record.status()));
        }
                Map<String, Instructor> loadedInstructorsById = new HashMap<>();
                for (InstructorRecord record : instructorRecords) {
                    Instructor instructor = record.role().equals("Permanent Instructor")
                        ? new PermanentInstructor(record.name(), record.email(), record.phone(), record.teacherId())
                        : new VisitingInstructor(record.name(), record.email(), record.phone(), record.teacherId());
                    instructors.add(instructor);
                    loadedInstructorsById.put(record.teacherId().toUpperCase(Locale.ROOT), instructor);
                }
                for (InstructorAssignmentRecord record : instructorAssignmentRecords) {
                    admin.assignInstructor(sectionsByIdLoaded.get(record.sectionId().toUpperCase(Locale.ROOT)),
                        loadedInstructorsById.get(record.teacherId().toUpperCase(Locale.ROOT)));
                }
            Map<String, Assignment> loadedAssignmentsById = new HashMap<>();
            for (AssignmentRecord record : assignmentRecords) {
                TeachingAssistant assistant = teachingAssistantsById.get(record.createdById().toUpperCase(Locale.ROOT));
                Section section = sectionsByIdLoaded.get(record.sectionId().toUpperCase(Locale.ROOT));
                Assignment assignment = new Assignment(record.id(), record.title(), record.description(),
                    LocalDate.parse(record.deadline()), record.totalMarks(), section, assistant);
                assistant.restoreAssignment(assignment);
                loadedAssignmentsById.put(record.id().toUpperCase(Locale.ROOT), assignment);
            }
            for (SubmissionRecord record : submissionRecords) {
                Assignment assignment = loadedAssignmentsById.get(record.assignmentId().toUpperCase(Locale.ROOT));
                Student student = studentsById.get(record.studentId().toUpperCase(Locale.ROOT));
                Feedback feedback = null;
                if (!record.feedbackId().isEmpty()) {
                    Evaluator evaluator = record.evaluatorType().equals("TA")
                            ? teachingAssistantsById.get(record.evaluatorId().toUpperCase(Locale.ROOT))
                            : (PermanentInstructor) loadedInstructorsById.get(
                                    record.evaluatorId().toUpperCase(Locale.ROOT));
                    feedback = new Feedback(record.feedbackId(), evaluator, record.feedbackComments(),
                            LocalDate.parse(record.feedbackDate()));
                }
                Submission submission = new Submission(record.submissionId(), assignment, student, record.content());
                submission.restoreState(record.submissionDate().isEmpty() ? null
                        : LocalDate.parse(record.submissionDate()),
                    record.content(), record.marks(), record.status(), feedback);
                assignment.addSubmission(submission);
            }
        Map<String, FYPGroup> loadedFypGroupsById = new HashMap<>();
        for (FypGroupRecord record : fypGroupRecords) {
            FYPGroup group = new FYPGroup(record.groupId(), record.title(), record.description());
            try {
                if (!record.supervisorId().isEmpty()) {
                    group.assignSupervisor((PermanentInstructor) loadedInstructorsById.get(
                            record.supervisorId().toUpperCase(Locale.ROOT)));
                }
            } catch (InvalidFYPGroupException exception) {
                throw new IOException("Invalid FYP supervisor for group " + record.groupId(), exception);
            }
            loadedFypGroupsById.put(record.groupId().toUpperCase(Locale.ROOT), group);
        }
        for (FypMemberRecord record : fypMemberRecords) {
            try {
                loadedFypGroupsById.get(record.groupId().toUpperCase(Locale.ROOT)).addMember(
                        studentsById.get(record.studentId().toUpperCase(Locale.ROOT)));
            } catch (InvalidFYPGroupException exception) {
                throw new IOException("Invalid FYP member for group " + record.groupId(), exception);
            }
        }
        for (FypMeetingRecord record : fypMeetingRecords) {
            FYPMeeting meeting = new FYPMeeting(record.meetingId(), LocalDate.parse(record.meetingDate()),
                    record.agenda());
            meeting.updateNotes(record.notes());
            try {
                loadedFypGroupsById.get(record.groupId().toUpperCase(Locale.ROOT)).addMeeting(meeting);
            } catch (InvalidFYPGroupException exception) {
                throw new IOException("Invalid FYP meeting for group " + record.groupId(), exception);
            }
        }
        for (FypEvaluationRecord record : fypEvaluationRecords) {
            Evaluator evaluator = record.evaluatorType().equals("TA")
                    ? teachingAssistantsById.get(record.evaluatorId().toUpperCase(Locale.ROOT))
                    : (PermanentInstructor) loadedInstructorsById.get(record.evaluatorId().toUpperCase(Locale.ROOT));
            FYPEvaluation evaluation = new FYPEvaluation(record.evaluationId(),
                    LocalDate.parse(record.evaluationDate()), evaluator);
            try {
                evaluation.evaluate(record.score());
            } catch (InvalidFYPEvaluationException exception) {
                throw new IOException("Invalid FYP score for evaluation " + record.evaluationId(), exception);
            }
            evaluation.addFeedback(record.feedback());
            try {
                loadedFypGroupsById.get(record.groupId().toUpperCase(Locale.ROOT)).addEvaluation(evaluation);
            } catch (InvalidFYPEvaluationException exception) {
                throw new IOException("Invalid FYP evaluation " + record.evaluationId(), exception);
            }
        }
        return sections;
    }

    public void saveCatalog(AcademicOfficeAdmin admin) throws IOException {
        saveCatalog(admin, List.of(), List.of());
    }

    public void saveCatalog(AcademicOfficeAdmin admin, List<Student> students) throws IOException {
        saveCatalog(admin, students, List.of());
    }

    public void saveCatalog(AcademicOfficeAdmin admin, List<Student> students,
                            List<Instructor> instructors) throws IOException {
        Path parent = catalogFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = catalogFile.resolveSibling(catalogFile.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        for (Course course : admin.viewCourses()) {
            lines.add(String.join("\t", "COURSE", encode(course.getCourseCode()),
                    encode(course.getTitle()), Integer.toString(course.getCreditHours())));
        }
        for (Course course : admin.viewCourses()) {
            for (Course prerequisite : course.getPrerequisites()) {
                lines.add(String.join("\t", "PREREQUISITE", encode(course.getCourseCode()),
                        encode(prerequisite.getCourseCode())));
            }
        }
        for (Section section : admin.viewSections()) {
            Schedule schedule = section.getSchedule();
            Course primaryCourse = section.getPrimaryCourse();
            String courseCode = primaryCourse != null ? primaryCourse.getCourseCode() : "";
            lines.add(String.join("\t", "SECTION", encode(section.getSectionId()),
                    encode(courseCode), Integer.toString(section.getCapacity()),
                    schedule == null ? "" : encode(schedule.getDay().name()),
                    schedule == null ? "" : encode(schedule.getStartTime().toString()),
                    schedule == null ? "" : encode(schedule.getEndTime().toString()),
                    schedule == null ? "" : encode(schedule.getRoom())));
        }
        Map<String, Student> studentsById = new HashMap<>();
        for (Student student : students) {
            String studentId = student.getStudentId();
            String type;
            String assignedSectionId = "";
            if (student instanceof TeachingAssistant assistant) {
                type = "TA";
                if (assistant.getAssignedSection() == null) {
                    throw new IOException("TA has no assigned section: " + studentId);
                }
                assignedSectionId = assistant.getAssignedSection().getSectionId();
            } else if (student instanceof NormalStudent) {
                type = "NORMAL";
            } else {
                throw new IOException("Unsupported student role: " + student.getRole());
            }
            String profileKey = type + "\u0000" + studentId.toUpperCase(Locale.ROOT);
            if (studentsById.putIfAbsent(profileKey, student) != null) {
                throw new IOException("Duplicate student profile in catalog: " + studentId);
            }
            List<String> studentFields = new ArrayList<>(List.of("STUDENT", type, encode(studentId),
                    encode(student.getName()), encode(student.getEmail()), encode(student.getPhone()),
                    Integer.toString(student.calculateTotalCreditHours())));
            if (type.equals("TA")) {
                studentFields.add(encode(assignedSectionId));
            }
            lines.add(String.join("\t", studentFields));
        }
        Set<String> savedEnrollments = new HashSet<>();
        for (Section section : admin.viewSections()) {
            for (Student student : section.getEnrolledStudents()) {
                String studentId = student.getStudentId();
                String studentKey = studentId.toUpperCase(Locale.ROOT);
                if (!studentsById.containsKey("NORMAL\u0000" + studentKey)) {
                    throw new IOException("Enrolled student is missing from the student registry: " + studentId);
                }
                String enrollmentKey = studentKey + "\u0000" + section.getSectionId().toUpperCase(Locale.ROOT);
                if (savedEnrollments.add(enrollmentKey)) {
                    lines.add(String.join("\t", "ENROLLMENT", encode(studentId),
                            encode(section.getSectionId())));
                }
            }
        }
        Map<String, Instructor> instructorsById = new HashMap<>();
        for (Instructor instructor : instructors) {
            String teacherId = instructor.getTeacherId();
            if (instructorsById.putIfAbsent(teacherId.toUpperCase(Locale.ROOT), instructor) != null) {
                throw new IOException("Duplicate instructor ID in catalog: " + teacherId);
            }
            lines.add(String.join("\t", "INSTRUCTOR", encode(instructor.getRole()), encode(teacherId),
                    encode(instructor.getName()), encode(instructor.getEmail()), encode(instructor.getPhone())));
        }
        Set<String> savedAssignments = new HashSet<>();
        for (Section section : admin.viewSections()) {
            Instructor instructor = section.getInstructor();
            if (instructor == null) {
                continue;
            }
            String instructorKey = instructor.getTeacherId().toUpperCase(Locale.ROOT);
            Instructor registeredInstructor = instructorsById.get(instructorKey);
            if (registeredInstructor != instructor) {
                throw new IOException("Assigned instructor is missing from the instructor registry: "
                        + instructor.getTeacherId());
            }
            if (savedAssignments.add(instructorKey + "\u0000" + section.getSectionId().toUpperCase(Locale.ROOT))) {
                lines.add(String.join("\t", "INSTRUCTOR_SECTION", encode(instructor.getTeacherId()),
                        encode(section.getSectionId())));
            }
        }
        Map<String, Assignment> assignmentsById = new HashMap<>();
        Set<String> submissionIds = new HashSet<>();
        for (Student student : students) {
            if (!(student instanceof TeachingAssistant assistant)) {
                continue;
            }
            for (Assignment assignment : assistant.getCreatedAssignments()) {
                String assignmentKey = assignment.getId().toUpperCase(Locale.ROOT);
                if (assignmentsById.putIfAbsent(assignmentKey, assignment) != null) {
                    throw new IOException("Duplicate assignment ID in catalog: " + assignment.getId());
                }
                if (assignment.getCreatedBy() != assistant
                        || !admin.viewSections().contains(assignment.getSection())) {
                    throw new IOException("Assignment has an invalid TA or section relationship: "
                            + assignment.getId());
                }
                lines.add(String.join("\t", "ASSIGNMENT", encode(assignment.getId()),
                        encode(assignment.getTitle()), encode(assignment.getDescription()),
                        assignment.getDeadline().toString(), Double.toString(assignment.getTotalMarks()),
                        encode(assignment.getSection().getSectionId()), encode(assistant.getStudentId())));
            }
        }
        for (Assignment assignment : assignmentsById.values()) {
            for (Submission submission : assignment.getSubmissions()) {
                if (submission.getAssignment() != assignment || submission.getStudent() == null
                        || !studentsById.containsKey("NORMAL\u0000"
                        + submission.getStudent().getStudentId().toUpperCase(Locale.ROOT))) {
                    throw new IOException("Submission has an invalid student/assignment relationship: "
                            + submission.getSubmissionId());
                }
                if (!submissionIds.add(submission.getSubmissionId().toUpperCase(Locale.ROOT))) {
                    throw new IOException("Duplicate submission ID in catalog: " + submission.getSubmissionId());
                }
                Feedback feedback = submission.getFeedback();
                String feedbackId = "";
                String evaluatorType = "";
                String evaluatorId = "";
                String feedbackComments = "";
                String feedbackDate = "";
                if (feedback != null) {
                    feedbackId = encode(feedback.getFeedbackId());
                    feedbackComments = encode(feedback.getComments());
                    feedbackDate = feedback.getDate().toString();
                    if (feedback.getEvaluator() instanceof TeachingAssistant evaluator) {
                        evaluatorType = "TA";
                        evaluatorId = encode(evaluator.getStudentId());
                    } else if (feedback.getEvaluator() instanceof PermanentInstructor evaluator) {
                        evaluatorType = "PERMANENT";
                        evaluatorId = encode(evaluator.getTeacherId());
                    } else {
                        throw new IOException("Feedback has an unsupported evaluator: "
                                + submission.getSubmissionId());
                    }
                }
                LocalDate submissionDate = submission.getSubmissionDate();
                lines.add(String.join("\t", "SUBMISSION", encode(submission.getSubmissionId()),
                        encode(assignment.getId()), encode(submission.getStudent().getStudentId()),
                        submissionDate == null ? "" : submissionDate.toString(),
                        encode(submission.getContent()), Double.toString(submission.getMarks()),
                        submission.getStatus().name(), feedbackId, evaluatorType, evaluatorId,
                        feedbackComments, feedbackDate));
            }
        }
        Set<String> savedFypGroupIds = new HashSet<>();
        for (Instructor instructor : instructors) {
            if (!(instructor instanceof PermanentInstructor permanentInstructor)) {
                continue;
            }
            for (FYPGroup group : permanentInstructor.viewFYPGroups()) {
                if (group.getSupervisor() != permanentInstructor
                        || !savedFypGroupIds.add(group.getGroupId().toUpperCase(Locale.ROOT))) {
                    throw new IOException("FYP group has an invalid supervisor or duplicate ID: "
                            + group.getGroupId());
                }
                lines.add(String.join("\t", "FYP_GROUP", encode(group.getGroupId()),
                        encode(group.getTitle()), encode(group.getDescription()),
                        encode(permanentInstructor.getTeacherId())));
                for (Student member : group.getMembers()) {
                    if (!(member instanceof NormalStudent)
                            || !studentsById.containsKey("NORMAL\u0000"
                            + member.getStudentId().toUpperCase(Locale.ROOT))) {
                        throw new IOException("FYP member is missing from the Normal Student registry: "
                                + member.getStudentId());
                    }
                    lines.add(String.join("\t", "FYP_MEMBER", encode(group.getGroupId()),
                            encode(member.getStudentId())));
                }
                for (FYPMeeting meeting : group.getMeetings()) {
                    lines.add(String.join("\t", "FYP_MEETING", encode(group.getGroupId()),
                            encode(meeting.getMeetingId()), meeting.getMeetingDate().toString(),
                            encode(meeting.getAgenda()), encode(meeting.getNotes())));
                }
                for (FYPEvaluation evaluation : group.getEvaluations()) {
                    String evaluatorType;
                    String evaluatorId;
                    if (evaluation.getEvaluator() instanceof TeachingAssistant evaluator) {
                        evaluatorType = "TA";
                        evaluatorId = evaluator.getStudentId();
                        if (!studentsById.containsKey("TA\u0000" + evaluatorId.toUpperCase(Locale.ROOT))) {
                            throw new IOException("FYP evaluator TA is missing from the registry: " + evaluatorId);
                        }
                    } else if (evaluation.getEvaluator() instanceof PermanentInstructor evaluator) {
                        evaluatorType = "PERMANENT";
                        evaluatorId = evaluator.getTeacherId();
                        if (!instructorsById.containsKey(evaluatorId.toUpperCase(Locale.ROOT))) {
                            throw new IOException("FYP evaluator instructor is missing from the registry: "
                                    + evaluatorId);
                        }
                    } else {
                        throw new IOException("FYP evaluation has an unsupported evaluator: "
                                + evaluation.getEvaluationId());
                    }
                    lines.add(String.join("\t", "FYP_EVALUATION", encode(group.getGroupId()),
                            encode(evaluation.getEvaluationId()), evaluation.getEvaluationDate().toString(),
                            evaluatorType, encode(evaluatorId), Double.toString(evaluation.getScore()),
                            encode(evaluation.getFeedback())));
                }
            }
        }
        for (Request request : admin.viewRequests()) {
            Student student = request.getStudent();
                if (student == null || !studentsById.containsKey(
                    "NORMAL\u0000" + student.getStudentId().toUpperCase(Locale.ROOT))) {
                throw new IOException("Request references a student missing from the student registry: "
                        + request.getRequestId());
            }
            String commonFields = String.join("\t", "REQUEST", "", encode(request.getRequestId()),
                    encode(request.getRequestDate().toString()), encode(request.getDescription()),
                    Integer.toString(request.getPriority()), request.getStatus().name(),
                    encode(student.getStudentId()));
            if (request instanceof CourseClashRequest clashRequest) {
                lines.add(commonFields.replace("REQUEST\t\t", "REQUEST\tCLASH\t") + "\t"
                        + encode(clashRequest.getConflictingSection().getSectionId()) + "\t"
                        + encode(clashRequest.getRequestedSection().getSectionId()));
            } else if (request instanceof GenericRequest genericRequest) {
                lines.add(commonFields.replace("REQUEST\t\t", "REQUEST\tGENERIC\t") + "\t"
                        + genericRequest.getCategory().name());
            } else {
                throw new IOException("Unsupported request type: " + request.getClass().getSimpleName());
            }
        }
        for (Section section : admin.viewSections()) {
            for (Attendance attendance : section.getAttendanceRecords()) {
                if (!studentsById.containsKey("NORMAL\u0000"
                    + attendance.getStudent().getStudentId().toUpperCase(Locale.ROOT))) {
                    throw new IOException("Attendance references a student missing from the student registry.");
                }
                lines.add(String.join("\t", "ATTENDANCE",
                        encode(attendance.getStudent().getStudentId()),
                        encode(section.getSectionId()), encode(attendance.getDate().toString()),
                        attendance.getStatus().name()));
            }
        }
        Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporaryFile, catalogFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, catalogFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String encode(String value) {
        return value.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String decode(String value) {
        StringBuilder decoded = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!escaped && character == '\\') {
                escaped = true;
            } else if (escaped) {
                switch (character) {
                    case '\\' -> decoded.append('\\');
                    case 't' -> decoded.append('\t');
                    case 'r' -> decoded.append('\r');
                    case 'n' -> decoded.append('\n');
                    default -> throw new IllegalArgumentException("invalid escape sequence");
                }
                escaped = false;
            } else {
                decoded.append(character);
            }
        }
        if (escaped) {
            throw new IllegalArgumentException("unfinished escape sequence");
        }
        return decoded.toString();
    }

    private static void requireText(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
    }

    private static void validateRequestIdentity(RequestRecord record, Set<String> requestIds) {
        requireText(record.requestId(), "request ID");
        requireText(record.description(), "request description");
        requireText(record.studentId(), "request student ID");
        if (!requestIds.add(record.requestId().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("duplicate request ID");
        }
    }

    private static void validateSchedule(String day, String startTime, String endTime, String room) {
        if (day.isEmpty() && startTime.isEmpty() && endTime.isEmpty() && room.isEmpty()) {
            return;
        }
        if (day.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || room.isBlank()) {
            throw new IllegalArgumentException("incomplete schedule record");
        }
        Day.valueOf(day);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("schedule end must be after start");
        }
    }

    private static IOException malformed(int lineNumber, String message) {
        return malformed(lineNumber, message, null);
    }

    private static IOException malformed(int lineNumber, String message, Exception cause) {
        return new IOException("Invalid catalog record at line " + lineNumber + ": " + message, cause);
    }

    private record SectionRecord(String sectionId, String courseCode, int capacity,
                                 String day, String startTime, String endTime, String room,
                                 int lineNumber) { }

    private record PrerequisiteRecord(String courseCode, String prerequisiteCode, int lineNumber) { }

    private record StudentRecord(String studentId, String name, String email, String phone,
                                 int totalCreditHours, String type, String assignedSectionId,
                                 int lineNumber) { }

    private record EnrollmentRecord(String studentId, String sectionId, int lineNumber) { }

    private record RequestRecord(String type, String requestId, String requestDate,
                                 String description, int priority, String status, String studentId,
                                 String conflictingSectionId, String requestedSectionId,
                                 String category, int lineNumber) { }

    private record AttendanceRecord(String studentId, String sectionId, String date,
                                    AttendanceStatus status, int lineNumber) { }

    private record InstructorRecord(String role, String teacherId, String name, String email,
                                    String phone, int lineNumber) { }

    private record InstructorAssignmentRecord(String teacherId, String sectionId, int lineNumber) { }

    private record AssignmentRecord(String id, String title, String description, String deadline,
                                   double totalMarks, String sectionId, String createdById,
                                   int lineNumber) { }

    private record SubmissionRecord(String submissionId, String assignmentId, String studentId,
                                    String submissionDate, String content, double marks,
                                    SubmissionStatus status, String feedbackId, String evaluatorType,
                                    String evaluatorId, String feedbackComments, String feedbackDate,
                                    int lineNumber) { }

    private record FypGroupRecord(String groupId, String title, String description,
                                  String supervisorId, int lineNumber) { }

    private record FypMemberRecord(String groupId, String studentId, int lineNumber) { }

    private record FypMeetingRecord(String groupId, String meetingId, String meetingDate,
                                    String agenda, String notes, int lineNumber) { }

    private record FypEvaluationRecord(String groupId, String evaluationId, String evaluationDate,
                                       String evaluatorType, String evaluatorId, double score,
                                       String feedback, int lineNumber) { }
}
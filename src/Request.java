import java.time.LocalDate;

/**
 * Abstract base class for academic requests submitted by students.
 */
public abstract class Request {
    private String requestId;
    private LocalDate requestDate;
    private String description;
    private RequestStatus status;
    private int priority;
    private Student student;
    private AcademicOfficeAdmin processedBy;

    Request(String requestId, LocalDate requestDate, String description, int priority, Student student) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID cannot be null or blank");
        }
        if (requestDate == null) {
            throw new IllegalArgumentException("Request date cannot be null");
        }
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be positive");
        }
        this.requestId = requestId;
        this.requestDate = requestDate;
        this.description = description == null ? "" : description;
        this.priority = priority;
        this.student = student;
        this.status = RequestStatus.PENDING;
    }


    public void submit() {
        this.status = RequestStatus.PENDING;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public String getDetails() {
        return "Request " + requestId + " | " + requestDate + " | " + description
                + " | Priority: " + priority + " | Status: " + status;
    }

    // Added getters
    public String getRequestId() {
        return requestId;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public String getDescription() {
        return description;
    }

    public Student getStudent() {
        return student;
    }

    public AcademicOfficeAdmin getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(AcademicOfficeAdmin admin) {
        this.processedBy = admin;
    }
}
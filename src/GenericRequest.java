import java.time.LocalDate;

public class GenericRequest extends Request {
    private RequestCategory category;

    GenericRequest(String requestId, LocalDate requestDate, String description, int priority,
                   Student student, RequestCategory category) {
        super(requestId, requestDate, description, priority, student);
        this.category = category;
    }

    public RequestCategory getCategory() {
        return category;
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Category: " + category;
    }
}
import java.time.LocalDate;

public final class RequestUnitTest {
    public static void main(String[] args) {
        testRequestCreationAndPriority();
        testRequestComparatorSorting();
        System.out.println("RequestUnitTest: PASS");
    }

    private static void testRequestCreationAndPriority() {
        NormalStudent student = new NormalStudent("Eve", "eve@example.test", "", "ST-401", 0);
        GenericRequest request = new GenericRequest("REQ-101", LocalDate.now(),
                "Professor concern", 5, student, RequestCategory.PROFESSOR);

        check(request.getRequestId().equals("REQ-101"), "request ID should match");
        check(request.getPriority() == 5, "priority should match");
        check(request.getStatus() == RequestStatus.PENDING, "initial status should be PENDING");
    }

    private static void testRequestComparatorSorting() {
        NormalStudent student = new NormalStudent("Frank", "frank@example.test", "", "ST-402", 0);
        Request highPriority = new GenericRequest("REQ-1", LocalDate.of(2026, 5, 1), "High", 10, student, RequestCategory.OTHER);
        Request lowPriority = new GenericRequest("REQ-2", LocalDate.of(2026, 4, 1), "Low", 2, student, RequestCategory.OTHER);

        RequestPriorityComparator priorityComparator = new RequestPriorityComparator();
        check(priorityComparator.compare(highPriority, lowPriority) < 0, "higher priority request should come first");

        RequestDateComparator dateComparator = new RequestDateComparator();
        check(dateComparator.compare(lowPriority, highPriority) < 0, "earlier request date should come first");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

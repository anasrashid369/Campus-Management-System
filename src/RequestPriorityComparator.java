import java.util.Comparator;

public class RequestPriorityComparator implements Comparator<Request> {
    @Override
    public int compare(Request first, Request second) {
        int priorityOrder = Integer.compare(second.getPriority(), first.getPriority());
        if (priorityOrder != 0) {
            return priorityOrder;
        }
        return first.getRequestDate().compareTo(second.getRequestDate());
    }
}
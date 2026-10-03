import java.util.Comparator;

public class RequestDateComparator implements Comparator<Request> {
    @Override
    public int compare(Request first, Request second) {
        return first.getRequestDate().compareTo(second.getRequestDate());
    }
}
import java.util.Comparator;

public class AssignmentDeadlineComparator implements Comparator<Assignment> {
    @Override
    public int compare(Assignment first, Assignment second) {
        return first.getDeadline().compareTo(second.getDeadline());
    }
}
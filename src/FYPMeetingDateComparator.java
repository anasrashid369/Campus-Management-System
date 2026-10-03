import java.util.Comparator;

public class FYPMeetingDateComparator implements Comparator<FYPMeeting> {
    @Override
    public int compare(FYPMeeting first, FYPMeeting second) {
        return first.getMeetingDate().compareTo(second.getMeetingDate());
    }
}
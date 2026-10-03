import java.time.LocalTime;

public class Schedule {
    private Day day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;

    Schedule(Day day, LocalTime startTime, LocalTime endTime, String room) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public boolean hasClash(Schedule schedule) {
        if (this.day != schedule.day) {
            return false;
        }
        return this.startTime.isBefore(schedule.endTime)
                && schedule.startTime.isBefore(this.endTime);
    }

    public Day getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }

    public String getScheduleInfo() {
        return day + " " + startTime + "-" + endTime + " (" + room + ")";
    }
}
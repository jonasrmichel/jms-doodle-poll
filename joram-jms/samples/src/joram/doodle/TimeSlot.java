package doodle;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

/**
 * A poll is made up of two or more time slots, which are voted on by poll
 * invitees. A time slot is defined by a start datetime and optionally an end
 * datetime.
 * 
 * @author Jonas Michel
 * 
 */
public class TimeSlot implements Serializable {
	private static final long serialVersionUID = -8690469227753138784L;

	/** The time slot's start time. */
	private Date start = null;

	/** The time slot's end time (optional). */
	private Date end = null;

	public TimeSlot(Date start, Date end) {
		this.start = start;
		this.end = end;
	}

	public TimeSlot(Date start) {
		this.start = start;
	}

	public TimeSlot(Date day, String timeStr) throws NumberFormatException {
		if (timeStr.contains("-"))
			initDoubleTime(day, timeStr);
		else
			initSingleTime(day, timeStr);
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	private void initSingleTime(Date day, String timeStr)
			throws NumberFormatException {
		start = parseTimeString(day, timeStr);
	}

	private void initDoubleTime(Date day, String timeStr)
			throws NumberFormatException {
		String[] timeStrArr = timeStr.split("-");
		start = parseTimeString(day, timeStrArr[0]);
		end = parseTimeString(day, timeStrArr[1]);
	}

	private Date parseTimeString(Date day, String timeStr)
			throws NumberFormatException {
		int hour = 0, minute = 0;
		if (timeStr.contains(":")) {
			hour = Integer.parseInt(timeStr.split(":")[0]);
			minute = Integer.parseInt(timeStr.split(":")[1]);
		} else {
			hour = Integer.parseInt(timeStr);
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(day);
		cal.add(Calendar.HOUR_OF_DAY, hour);
		cal.add(Calendar.MINUTE, minute);

		return cal.getTime();
	}

	public String toDayString() {
		SimpleDateFormat day = new SimpleDateFormat("MM/dd/yyyy");
		return day.format(start);
	}

	public String toTimeString() {
		SimpleDateFormat time = new SimpleDateFormat("HH:mm");

		StringBuilder sb = new StringBuilder();
		sb.append(time.format(start));

		if (end == null)
			return sb.toString();

		sb.append("-" + time.format(end));

		return sb.toString();
	}

	@Override
	public String toString() {
		return toDayString() + " " + toTimeString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TimeSlot))
			return false;
		TimeSlot other = (TimeSlot) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	public static class TimeSlotComparator implements Comparator<TimeSlot> {
		@Override
		public int compare(TimeSlot ts1, TimeSlot ts2) {
			if (ts1.getStart().before(ts2.getStart()))
				return -1;
			else if (ts1.getStart().after(ts2.getStart()))
				return 1;
			else
				return 0;
		}
	}
}

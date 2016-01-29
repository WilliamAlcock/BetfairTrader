import org.joda.time.DateTime

val marketStartTime = DateTime.now().plusMinutes(20)

val minutesToStart = new DateTime(marketStartTime.getMillis() - DateTime.now().getMillis())

minutesToStart.toString("YYYY_MM_DD")

minutesToStart.getYear

minutesToStart.getMonthOfYear

minutesToStart.getDayOfMonth

minutesToStart.getHourOfDay

minutesToStart.getMinuteOfHour

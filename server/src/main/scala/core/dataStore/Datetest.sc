import org.joda.time.{Interval, DateTime}

val marketStartTime = DateTime.now().plusMinutes(120)

val minutesToStart = new DateTime(marketStartTime.getMillis() - DateTime.now().getMillis())

minutesToStart.toString("YYYY_MM_DD")

minutesToStart.getYear

minutesToStart.getMonthOfYear

minutesToStart.getDayOfMonth

minutesToStart.getHourOfDay

minutesToStart.getMinuteOfHour

new Interval(marketStartTime, marketStartTime).toPeriod().toStandardMinutes.getMinutes

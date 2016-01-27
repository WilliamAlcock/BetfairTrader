import domain.MarketBook
import org.joda.time.DateTime

val marketStartTime = DateTime.now().plusMinutes(20)

val minutesToStart = new DateTime(marketStartTime.getMillis() - DateTime.now().getMillis())

minutesToStart.getYear

minutesToStart.getMonthOfYear

minutesToStart.getDayOfMonth

minutesToStart.getHourOfDay

minutesToStart.getMinuteOfHour

val marketBookA = MarketBook()
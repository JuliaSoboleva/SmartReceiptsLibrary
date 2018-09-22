package co.smartreceipts.android.model

import android.content.Context
import android.os.Parcelable
import co.smartreceipts.android.currency.PriceCurrency
import co.smartreceipts.android.model.factory.PriceBuilderFactory
import co.smartreceipts.android.model.utils.ModelUtils
import co.smartreceipts.android.sync.model.SyncState
import co.smartreceipts.android.sync.model.Syncable
import co.smartreceipts.android.sync.model.impl.DefaultSyncState
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.sql.Date
import java.util.*

@Parcelize
class Trip @JvmOverloads constructor(
    override val id: Int,
    override val uuid: UUID,
    /**
     * The directory in which all this trip's images are stored
     */
    val directory: File,
    /**
     * The [Date] upon which this trip began
     */
    val startDate: Date,
    /**
     * The [TimeZone] in which the start date was set
     */
    val startTimeZone: TimeZone,
    /**
     * The [Date] upon which this trip will end
     */
    val endDate: Date,
    /**
     * The [TimeZone] in which the end date was set
     */
    val endTimeZone: TimeZone,
    /**
     * The [PriceCurrency] which this trip is tracked in
     */
    val tripCurrency: PriceCurrency,
    /**
     * The user defined comment [String] for this trip
     */
    val comment: String,
    /**
     * The cost center for this particular trip
     */
    val costCenter: String,
    /**
     * The [Source] from which this trip was built for debugging purposes
     */
    val source: Source = Source.Undefined,
    override val syncState: SyncState = DefaultSyncState(),
    /**
     * As the price of a trip exists as a function of it's receipt children (and not itself), [Price] must be var
     */
    override var price: Price = PriceBuilderFactory().setPrice(0.0).setCurrency(tripCurrency).build(),
    /**
     * The daily sub-total [Price] (i.e. all expenditures that occurred today) for this trip,
     * daily sub-total of a trip exists as a function of it's receipt children (and not itself)
     */
    var dailySubTotal: Price = PriceBuilderFactory().setPrice(0.0).setCurrency(tripCurrency).build()

) : Parcelable, Priceable, Comparable<Trip>, Syncable {

    /**
     * The name of this trip (this will be the name of [.getDirectory]
     */
    val name: String
        get() = directory.name

    /**
     * The absolute path of this Trip's directory from [.getDirectory] and [File.getAbsolutePath]
     */
    val directoryPath: String
        get() = directory.absolutePath

    /**
     * The default currency code representation for this trip or [PriceCurrency.MISSING_CURRENCY]
     * if it cannot be found
     */
    val defaultCurrencyCode: String
        get() = tripCurrency.currencyCode

    /**
     * Gets a formatted version of the start date based on the timezone and locale for a given separator. In the US,
     * we might expect to see a result like "10/23/2014" returned if we set the separator as "/"
     *
     * @param context   - the current [Context]
     * @param separator - the date separator (e.g. "/", "-", ".")
     * @return the formatted date string for the start date
     */
    fun getFormattedStartDate(context: Context, separator: String): String =
        ModelUtils.getFormattedDate(startDate, startTimeZone, context, separator)

    /**
     * Gets a formatted version of the end date based on the timezone and locale for a given separator. In the US,
     * we might expect to see a result like "10/23/2014" returned if we set the separator as "/"
     *
     * @param context   - the current [Context]
     * @param separator - the date separator (e.g. "/", "-", ".")
     * @return the formatted date string for the end date
     */
    fun getFormattedEndDate(context: Context, separator: String): String =
        ModelUtils.getFormattedDate(endDate, endTimeZone, context, separator)

    /**
     * Tests if a particular date is included with the bounds of this particular trip When performing the test, it uses
     * the local time zone for the date, and the defined time zones for the start and end date bounds. The start date
     * time is assumed to occur at 00:01 of the start day and the end date is assumed to occur at 23:59 of the end day.
     * The reasoning behind this is to ensure that it appears properly from a UI perspective. Since the initial date
     * only shows the day itself, it may include an arbitrary time that is never shown to the user. Setting the time
     * aspect manually accounts for this. This returns false if the date is null.
     *
     * @param date - the date to test
     * @return true if it is contained within. false otherwise
     */
    fun isDateInsideTripBounds(date: Date?): Boolean {
        if (date == null) {
            return false
        }

        // Build a calendar for the date we intend to test
        val testCalendar = Calendar.getInstance().apply {
            time = date
            timeZone = TimeZone.getDefault()
        }

        // Build a calendar for the start date
        val startCalendar = Calendar.getInstance().apply {
            time = startDate
            timeZone = startTimeZone
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Build a calendar for the end date
        val endCalendar = Calendar.getInstance().apply {
            time = endDate
            timeZone = endTimeZone
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return testCalendar in startCalendar..endCalendar
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Trip) return false

        val that = o as Trip?

        if (id != that!!.id) return false
        if (uuid != that.uuid) return false
        if (directory != that.directory) return false
        if (comment != that.comment) return false
        if (startDate != that.startDate) return false
        if (startTimeZone != that.startTimeZone) return false
        if (endDate != that.endDate) return false
        if (endTimeZone != that.endTimeZone) return false
        return if (tripCurrency != that.tripCurrency) false else costCenter == that.costCenter

    }


    override fun hashCode(): Int {
        var result = id
        result = 31 * result + uuid.hashCode()
        result = 31 * result + directory.hashCode()
        result = 31 * result + comment.hashCode()
        result = 31 * result + startDate.hashCode()
        result = 31 * result + startTimeZone.hashCode()
        result = 31 * result + endDate.hashCode()
        result = 31 * result + endTimeZone.hashCode()
        result = 31 * result + tripCurrency.hashCode()
        result = 31 * result + costCenter.hashCode()
        return result
    }

    override fun toString(): String {
        return "DefaultTripImpl{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", mReportDirectory=" + directory +
                ", mComment='" + comment + '\''.toString() +
                ", mCostCenter='" + costCenter + '\''.toString() +
                ", mPrice=" + price +
                ", mDailySubTotal=" + dailySubTotal +
                ", mStartDate=" + startDate +
                ", mEndDate=" + endDate +
                ", mStartTimeZone=" + startTimeZone +
                ", mEndTimeZone=" + endTimeZone +
                ", mDefaultCurrency=" + tripCurrency +
                ", mSource=" + source +
                '}'.toString()
    }

    override fun compareTo(trip: Trip): Int {
        return trip.endDate.compareTo(endDate)
    }

    companion object {
        val PARCEL_KEY: String = Trip::class.java.name
    }
}

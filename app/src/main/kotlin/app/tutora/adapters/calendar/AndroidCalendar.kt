package app.tutora.adapters.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import app.tutora.R
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.BusyEvent
import app.tutora.domain.CalendarWords
import app.tutora.domain.ClassEvent
import app.tutora.domain.Language
import app.tutora.domain.MarkedEvent
import app.tutora.usecases.PhoneCalendar
import app.tutora.usecases.PhoneCalendarInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

// The phone's calendar store (spec 012). Android syncs each account's calendars with Google or
// Microsoft, so Tutora reaches the cloud with no network of its own. Tutora's mark on an event is
// its custom-app fields (its package and tutora://lesson/<id>): hidden in calendar apps, and
// writable by an app in an account's calendar, unlike extended properties (AC-012-5).

private const val OWN_ACCOUNT = "Tutora"
private const val LESSON_URI = "tutora://lesson/"

class AndroidPhoneCalendar(
    private val context: Context,
) : PhoneCalendar {
    private val resolver get() = context.contentResolver
    private val pkg get() = context.packageName

    override fun hasPermission() =
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** As the calendar's own account: how an app makes a calendar kept on the phone. */
    private fun asOwnAccount(uri: Uri) =
        uri
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, OWN_ACCOUNT)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

    private fun ownId(): Long? =
        resolver
            .query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
                arrayOf(OWN_ACCOUNT, CalendarContract.ACCOUNT_TYPE_LOCAL),
                null,
            )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    override fun calendars(): List<PhoneCalendarInfo> {
        val own = ownId()
        val columns =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
            )
        return resolver
            .query(
                CalendarContract.Calendars.CONTENT_URI,
                columns,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null,
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val type = c.getString(3).orEmpty()
                        add(
                            PhoneCalendarInfo(
                                id = c.getLong(0),
                                name = c.getString(1).orEmpty(),
                                account = if (c.getLong(0) == own) "" else c.getString(2).orEmpty(),
                                provider =
                                    when {
                                        type.contains("google") -> "Google"
                                        type.contains("exchange") || type.contains("outlook") || type.contains("microsoft") -> "Outlook"
                                        else -> ""
                                    },
                                own = c.getLong(0) == own,
                            ),
                        )
                    }
                }
            }.orEmpty()
    }

    override fun ownCalendar(
        name: String,
        color: Int,
    ): Long {
        ownId()?.let { return it }
        val values =
            ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, OWN_ACCOUNT)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(CalendarContract.Calendars.NAME, name)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, name)
                put(CalendarContract.Calendars.CALENDAR_COLOR, color)
                put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                put(CalendarContract.Calendars.OWNER_ACCOUNT, OWN_ACCOUNT)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            }
        val uri = checkNotNull(resolver.insert(asOwnAccount(CalendarContract.Calendars.CONTENT_URI), values)) { "No calendar" }
        return ContentUris.parseId(uri)
    }

    private val eventColumns =
        arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
        )

    private fun query(
        where: String,
        args: Array<String>,
    ): List<MarkedEvent> =
        resolver
            .query(
                CalendarContract.Events.CONTENT_URI,
                eventColumns,
                "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.DELETED} = 0$where",
                arrayOf(pkg) + args,
                null,
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val lesson = c.getString(1)?.removePrefix(LESSON_URI) ?: continue
                        add(
                            MarkedEvent(
                                c.getLong(0),
                                ClassEvent(
                                    lesson,
                                    c.getLong(2),
                                    c.getLong(3),
                                    c.getString(4).orEmpty(),
                                    c.getString(5).orEmpty(),
                                    c.getString(6).orEmpty(),
                                ),
                            ),
                        )
                    }
                }
            }.orEmpty()

    override fun marked(calendarId: Long) = query(" AND ${CalendarContract.Events.CALENDAR_ID} = ?", arrayOf(calendarId.toString()))

    override fun markedEverywhere() = query("", emptyArray()).map { it.eventId }

    private fun values(
        e: ClassEvent,
        zone: String,
    ) = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, e.startMs)
        put(CalendarContract.Events.DTEND, e.endMs)
        put(CalendarContract.Events.TITLE, e.title)
        put(CalendarContract.Events.EVENT_LOCATION, e.location)
        put(CalendarContract.Events.DESCRIPTION, e.description)
        put(CalendarContract.Events.EVENT_TIMEZONE, zone)
        put(CalendarContract.Events.CUSTOM_APP_PACKAGE, pkg)
        put(CalendarContract.Events.CUSTOM_APP_URI, LESSON_URI + e.lessonId)
    }

    override fun insert(
        calendarId: Long,
        event: ClassEvent,
        zone: String,
    ): Long {
        val v = values(event, zone).apply { put(CalendarContract.Events.CALENDAR_ID, calendarId) }
        return ContentUris.parseId(checkNotNull(resolver.insert(CalendarContract.Events.CONTENT_URI, v)) { "Event refused" })
    }

    private val onlyMarked = "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ?"

    override fun update(
        eventId: Long,
        event: ClassEvent,
        zone: String,
    ) {
        resolver.update(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            values(event, zone),
            onlyMarked,
            arrayOf(pkg),
        )
    }

    override fun delete(eventId: Long) {
        resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), onlyMarked, arrayOf(pkg))
    }

    override fun deleteOwnCalendar() {
        val id = ownId() ?: return
        resolver.delete(ContentUris.withAppendedId(asOwnAccount(CalendarContract.Calendars.CONTENT_URI), id), null, null)
    }

    override fun busy(
        fromMs: Long,
        toMs: Long,
    ): Flow<List<BusyEvent>> =
        callbackFlow {
            fun read() {
                val uri =
                    CalendarContract.Instances.CONTENT_URI
                        .buildUpon()
                        .also {
                            ContentUris.appendId(it, fromMs)
                            ContentUris.appendId(it, toMs)
                        }.build()
                val own = ownId()
                val list =
                    runCatching {
                        resolver
                            .query(
                                uri,
                                arrayOf(
                                    CalendarContract.Instances.TITLE,
                                    CalendarContract.Instances.BEGIN,
                                    CalendarContract.Instances.END,
                                    CalendarContract.Instances.ALL_DAY,
                                    CalendarContract.Instances.CUSTOM_APP_PACKAGE,
                                    CalendarContract.Instances.CALENDAR_ID,
                                ),
                                "${CalendarContract.Instances.VISIBLE} = 1",
                                null,
                                null,
                            )?.use { c ->
                                buildList {
                                    while (c.moveToNext()) {
                                        add(
                                            BusyEvent(
                                                c.getString(0).orEmpty(),
                                                c.getLong(1),
                                                c.getLong(2),
                                                c.getInt(3) == 1,
                                                marked = c.getString(4) == pkg || c.getLong(5) == own,
                                            ),
                                        )
                                    }
                                }
                            }.orEmpty()
                    }.getOrDefault(emptyList())
                trySend(list)
            }
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) = read()
                }
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            read()
            awaitClose { resolver.unregisterContentObserver(observer) }
        }
}

/** The event texts from string resources, in the app language rather than the phone's (AC-012-22). */
fun androidCalendarWords(context: Context): (Language) -> CalendarWords =
    { language ->
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) }
        val res = context.createConfigurationContext(config).resources
        CalendarWords(
            cancelledPrefix = res.getString(R.string.cal_cancelledPrefix),
            missedPrefix = res.getString(R.string.cal_missedPrefix),
            value = res.getString(R.string.cal_value),
            footer = res.getString(R.string.cal_footer),
            builtinName = { b ->
                res.getString(
                    when (b) {
                        BuiltinPlace.STUDENT_HOME -> R.string.places_builtin_student_home
                        BuiltinPlace.ONLINE -> R.string.places_builtin_online
                    },
                )
            },
            money = {
                app.tutora.designsystem.components
                    .formatMoney(it, language)
            },
        )
    }

package ke.co.smartroundclinic.common

import kotlin.time.Clock

/**
 * ISO-8601 UTC "now" with a fixed 9-digit fractional-second width. Plain `Clock.System.now().toString()`
 * trims trailing zero groups (emitting 3, 6, or 9 fractional digits, or none at all on a whole second),
 * so lexicographic string comparison — which is how Mongo sorts/filters our String-typed timestamp
 * fields (createdAt, lastReadAt, lastDeliveredAt, lastSeenAt) — can disagree with real chronological
 * order. A fixed width keeps string ordering monotonic with time.
 */
fun sortableNowIso(): String {
    val now = Clock.System.now()
    val whole = now.toString().substringBefore('.').removeSuffix("Z")
    val nanos = now.nanosecondsOfSecond.toString().padStart(9, '0')
    return "$whole.${nanos}Z"
}

package smartstay;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorting algorithm used to list reservations in check-in order.
 *
 * <p>Merge sort is implemented by hand (instead of calling a library sort) to show the algorithm
 * clearly. It runs in O(n log n) time in every case and is stable, so bookings with the same
 * check-in date keep their booking-ID order.
 */
public final class EUK00361749_BookingSorter {

    private EUK00361749_BookingSorter() {
        // Utility class: no objects are needed.
    }

    /**
     * Returns a new list sorted by check-in date, then booking ID. The input list is not changed.
     *
     * <pre>
     * mergeSort(list):
     *   if size &lt;= 1 return list
     *   split list into left half and right half
     *   return merge(mergeSort(left), mergeSort(right))
     * </pre>
     */
    public static List<EUK00361749_Booking> sortByCheckInDate(List<EUK00361749_Booking> bookings) {
        if (bookings.size() <= 1) {
            return new ArrayList<>(bookings);
        }
        int middle = bookings.size() / 2;
        List<EUK00361749_Booking> sortedLeft = sortByCheckInDate(bookings.subList(0, middle));
        List<EUK00361749_Booking> sortedRight = sortByCheckInDate(bookings.subList(middle, bookings.size()));
        return merge(sortedLeft, sortedRight);
    }

    /** Merges two already-sorted lists into one sorted list. */
    private static List<EUK00361749_Booking> merge(List<EUK00361749_Booking> left,
                                                    List<EUK00361749_Booking> right) {
        List<EUK00361749_Booking> merged = new ArrayList<>(left.size() + right.size());
        int leftIndex = 0;
        int rightIndex = 0;
        // Repeatedly take the smaller head element; "<=" keeps the sort stable.
        while (leftIndex < left.size() && rightIndex < right.size()) {
            if (compare(left.get(leftIndex), right.get(rightIndex)) <= 0) {
                merged.add(left.get(leftIndex++));
            } else {
                merged.add(right.get(rightIndex++));
            }
        }
        // Copy whatever remains in either half.
        while (leftIndex < left.size()) {
            merged.add(left.get(leftIndex++));
        }
        while (rightIndex < right.size()) {
            merged.add(right.get(rightIndex++));
        }
        return merged;
    }

    /** Orders by check-in date first, then by booking ID. */
    private static int compare(EUK00361749_Booking first, EUK00361749_Booking second) {
        int byDate = first.getCheckInDate().compareTo(second.getCheckInDate());
        return byDate != 0 ? byDate : first.getBookingId().compareTo(second.getBookingId());
    }
}

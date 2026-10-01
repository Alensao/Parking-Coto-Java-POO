package pricing;

import java.time.Duration;
import java.util.Objects;

/**
 * Hourly pricing with a daily maximum.
 * Rules implemented (and ONLY here):
 * 1. Every started hour is charged as a full hour (minimum 1 hour).
 * 2. The stay is split into 24-hour periods counted from the entry.
 * 3. Each full 24-hour period is charged at the daily maximum.
 * 4. The remaining hours are charged per hour, unless they reach the
 *    threshold (10 hours by default); then the daily maximum applies.
 * Instances are immutable.
 */
public final class HourlyPricingPolicy implements PricingPolicy {

    public static final int DEFAULT_DAILY_CAP_THRESHOLD_HOURS = 10;

    private static final long MINUTES_PER_HOUR = 60;
    private static final long HOURS_PER_DAY = 24;

    private final long hourlyRate;
    private final long dailyMaximum;
    private final int dailyCapThresholdHours;

    public HourlyPricingPolicy(long hourlyRate, long dailyMaximum) {
        this(hourlyRate, dailyMaximum, DEFAULT_DAILY_CAP_THRESHOLD_HOURS);
    }

    public HourlyPricingPolicy(long hourlyRate, long dailyMaximum, int dailyCapThresholdHours) {
        if (hourlyRate <= 0) {
            throw new IllegalArgumentException("Hourly rate must be greater than zero.");
        }
        if (dailyMaximum <= 0) {
            throw new IllegalArgumentException("Daily maximum must be greater than zero.");
        }
        if (dailyCapThresholdHours <= 0 || dailyCapThresholdHours > HOURS_PER_DAY) {
            throw new IllegalArgumentException("Daily cap threshold must be between 1 and 24 hours.");
        }
        this.hourlyRate = hourlyRate;
        this.dailyMaximum = dailyMaximum;
        this.dailyCapThresholdHours = dailyCapThresholdHours;
    }

    @Override
    public long calculateFee(Duration stay) {
        long chargeableHours = calculateChargeableHours(stay);
        long fullDays = chargeableHours / HOURS_PER_DAY;
        long remainingHours = chargeableHours % HOURS_PER_DAY;
        return fullDays * dailyMaximum + feeForPartialDay(remainingHours);
    }

    /**
     * Rounds the stay up to whole hours, working with minute precision.
     * Examples: 1 min -> 1 h, 60 min -> 1 h, 61 min -> 2 h.
     */
    @Override
    public long calculateChargeableHours(Duration stay) {
        Objects.requireNonNull(stay, "Stay must not be null.");
        if (stay.isNegative()) {
            throw new IllegalArgumentException("Stay must not be negative.");
        }
        long minutes = stay.toMinutes();
        long hours = (minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR; // integer ceiling
        return Math.max(1, hours);
    }

    public long getHourlyRate() {
        return hourlyRate;
    }

    public long getDailyMaximum() {
        return dailyMaximum;
    }

    public int getDailyCapThresholdHours() {
        return dailyCapThresholdHours;
    }

    private long feeForPartialDay(long hours) {
        if (hours >= dailyCapThresholdHours) {
            return dailyMaximum;
        }
        return hours * hourlyRate;
    }

    @Override
    public String toString() {
        return "CRC " + hourlyRate + "/hour, daily maximum CRC " + dailyMaximum
                + " from " + dailyCapThresholdHours + " hours";
    }
}
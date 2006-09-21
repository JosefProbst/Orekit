package fr.cs.aerospace.orekit.time;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.iers.IERSData;
import fr.cs.aerospace.orekit.iers.Leap;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. This
 * adjustments require introduction of leap seconds.</p>
 * <p>The handling of time <em>during</em> the leap seconds insertion has
 * been adapted from the standard in order to compensate for the lack of
 * support for leap seconds in the standard java {@link java.util.Date}
 * class. We consider the leap is introduced as one clock reset at the
 * end of the leap. For example when a one second leap was introduced
 * between 2005-12-31T23:59:59 UTC and 2006-01-01T00:00:00 UTC, we
 * consider time flowed continuously for one second in UTC time scale
 * from 23:59:59 to 00:00:00 and <em>then</em> a -1 second leap reset
 * the clock to 23:59:59 again, leading to have to wait one second more
 * before 00:00:00 was reached. The standard would have required to have
 * introduced a second corresponding to location 23:59:60, i.e. the
 * last minute of 2005 was 61 seconds long instead of 60 seconds.</p>
 * <p>The OREKIT library retrieves time steps data thanks to the {@link
 * fr.cs.aerospace.orekit.iers.IERSData IERSData} class.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @see fr.cs.aerospace.orekit.iers.IERSData
 */
public class UTCScale extends TimeScale {

  /** Private constructor for the singleton.
   * @exception OrekitException if the time steps cannot be read
   */
  private UTCScale()
    throws OrekitException {
    super("UTC");

    // put the most recent leap first,
    // as it will often be the only one really used
    leaps = IERSData.getInstance().getTimeSteps();
    for (int i = 0, j = leaps.length - 1; i < j; ++i, --j) {
      Leap l   = leaps[i];
      leaps[i] = leaps[j];
      leaps[j] = l;
    }

  }

  /** Get the uniq instance of this class.
   * @return the uniq instance
   * @exception OrekitException if the time steps cannot be read
   */
  public static TimeScale getInstance()
    throws OrekitException {
    if (instance == null) {
      instance = new UTCScale();
    }
    return instance;
  }

  /** Get the offset to convert locations from {@link TAIScale}  to instance.
   * @param taiTime location of an event in the {@link TAIScale}  time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to taiTime to get a location
   * in instance time scale
   */
  public double offsetFromTAI(double taiTime) {
    for (int i = 0; i < leaps.length; ++i) {
      Leap leap = leaps[i];
      if ((taiTime  + (leap.offsetAfter - leap.step)) >= leap.utcTime) {
        return leap.offsetAfter;
      }
    }
    return 0;
  }

  /** Get the offset to convert locations from instance to {@link TAIScale} .
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to instanceTime to get a location
   * in {@link TAIScale}  time scale
   */
  public double offsetToTAI(double instanceTime) {
    for (int i = 0; i < leaps.length; ++i) {
      Leap leap = leaps[i];
      if (instanceTime >= leap.utcTime) {
        return -leap.offsetAfter;
      }
    }
    return 0;
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

  /** Time steps. */
  private Leap[] leaps;

}

package smartstay;

import java.util.Objects;

/** Immutable inventory item. Manual status and dated reservations are separate. */
public final class EUK00361749_Room {
  private final String id;
  private final EUK00361749_RoomType type;
  private final EUK00361749_BedType bed;
  private final EUK00361749_RoomStatus status;

  public EUK00361749_Room(
      String id,
      EUK00361749_RoomType type,
      EUK00361749_BedType bed,
      EUK00361749_RoomStatus status) {
    if (id == null || !id.matches("[A-Za-z0-9-]{1,20}"))
      throw new IllegalArgumentException("Room ID: 1-20 letters, digits or hyphens.");
    this.id = id.toUpperCase(java.util.Locale.ROOT);
    this.type = Objects.requireNonNull(type);
    this.bed = Objects.requireNonNull(bed);
    this.status = Objects.requireNonNull(status);
  }

  public String getId() {
    return id;
  }

  public EUK00361749_RoomType getType() {
    return type;
  }

  public EUK00361749_BedType getBed() {
    return bed;
  }

  public EUK00361749_RoomStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return id
        + " | "
        + type
        + " | "
        + bed
        + " | LKR "
        + type.dailyRate()
        + " | included guests "
        + type.capacity()
        + " | breakfast "
        + type.breakfast()
        + " | "
        + status;
  }
}

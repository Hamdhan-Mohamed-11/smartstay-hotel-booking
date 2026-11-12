package smartstay;

/** Immutable registered guest; document identifier is the unique key. */
public final class EUK00361749_Guest {
  private final String document, name, phone, email;

  public EUK00361749_Guest(String document, String name, String phone, String email) {
    if (document == null || !document.matches("[A-Za-z0-9]{5,20}"))
      throw new IllegalArgumentException("NIC/passport: 5-20 letters or digits.");
    if (name == null
        || name.isBlank()
        || name.length() > 100
        || name.chars().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Full name must contain 1-100 printable characters.");
    if (phone == null || !phone.matches("[+]?[0-9]{7,15}"))
      throw new IllegalArgumentException("Phone: 7-15 digits, optional leading +.");
    if (email == null || !email.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+") || email.length() > 254)
      throw new IllegalArgumentException("Enter a valid email address.");
    this.document = document.toUpperCase(java.util.Locale.ROOT);
    this.name = name.trim();
    this.phone = phone;
    this.email = email;
  }

  public String getDocument() {
    return document;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return document + " | " + name + " | " + phone + " | " + email;
  }
}

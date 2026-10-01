package io.github.kriolos.opos.model;

/**
 * Represents a contact in the KriolOS POS system.
 * Can be a customer, supplier, or employee.
 */
public record Contact(
    String id,
    String name,
    String email,
    String phone,
    String company,
    String type,    // CUSTOMER, SUPPLIER, EMPLOYEE
    String status   // ACTIVE, INACTIVE
) {

  /** Convenience factory for a customer contact. */
  public static Contact customer(String id, String name, String email, String phone, String company) {
    return new Contact(id, name, email, phone, company, "CUSTOMER", "ACTIVE");
  }

  /** Convenience factory for a supplier contact. */
  public static Contact supplier(String id, String name, String email, String phone, String company) {
    return new Contact(id, name, email, phone, company, "SUPPLIER", "ACTIVE");
  }

  /** Convenience factory for an employee contact. */
  public static Contact employee(String id, String name, String email, String phone, String company) {
    return new Contact(id, name, email, phone, company, "EMPLOYEE", "ACTIVE");
  }

  /** Returns the initials (up to 2 chars) derived from the name. */
  public String initials() {
    if (name == null || name.isBlank()) return "?";
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) {
      return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
  }
}

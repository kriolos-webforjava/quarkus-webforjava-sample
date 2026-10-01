package io.github.kriolos.opos.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import io.github.kriolos.opos.model.Contact;
import io.quarkus.arc.Unremovable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI repository for {@link Contact} entities.
 *
 * <p>Backed by an in-memory {@link CopyOnWriteArrayList} so it is thread-safe
 * for concurrent webforJ sessions without introducing a database dependency.
 * Swap the backing store for a Panache/JDBC implementation whenever a real DB
 * is wired up.</p>
 */
@ApplicationScoped
@Unremovable
public class ContactRepository {

  private final List<Contact> store = new CopyOnWriteArrayList<>();

  @PostConstruct
  void seed() {
    List<Contact> seed = new ArrayList<>();

    // --- Customers ---
    seed.add(Contact.customer("c1", "Ana Tavares",      "ana.tavares@email.cv",     "+238 991 2345", "Mercado Sucupira"));
    seed.add(Contact.customer("c2", "João Fonseca",     "joao.fonseca@cabo.cv",     "+238 995 6789", "Casa Fonseca Lda."));
    seed.add(Contact.customer("c3", "Maria Delgado",    "mdelgado@praia.cv",        "+238 993 1122", "Loja da Maria"));
    seed.add(Contact.customer("c4", "Carlos Santos",    "csantos@barlavento.cv",    "+238 992 3344", "Bar Barlavento"));
    seed.add(Contact.customer("c5", "Sofia Almeida",    "sofia.almeida@cv.net",     "+238 994 5566", "Restaurante Sodade"));
    seed.add(Contact.customer("c6", "Tomé Évora",       "tome.evora@sal.cv",        "+238 996 7788", "Hotel Atlantico"));
    seed.add(Contact.customer("c7", "Filomena Cruz",    "fcruz@email.cv",           "+238 998 9900", "Cruz & Filhos"));
    seed.add(new Contact("c8", "Pedro Brito",  "pedro@cabo.cv", "+238 997 1100", "PB Invest", "CUSTOMER", "INACTIVE"));

    // --- Suppliers ---
    seed.add(Contact.supplier("s1", "Importações Silva",  "silva@importacao.cv",   "+238 261 1234", "Silva Importações Lda."));
    seed.add(Contact.supplier("s2", "Distribuidora Norte", "norte@distrib.cv",    "+238 232 5678", "Norte Dist. S.A."));
    seed.add(Contact.supplier("s3", "Agro Fogo",           "agro@fogo.cv",        "+238 285 4321", "Cooperativa Agro Fogo"));
    seed.add(new Contact("s4", "Global Tech CV", "gtcv@globaltech.cv", "+238 261 9090", "Global Tech Cape Verde", "SUPPLIER", "INACTIVE"));

    // --- Employees ---
    seed.add(Contact.employee("e1", "Admin User",  "admin@kriolos.io",  "+238 999 0001", "KriolOS"));
    seed.add(Contact.employee("e2", "Luísa Pina",  "luisa@kriolos.io",  "+238 999 0002", "KriolOS"));
    seed.add(Contact.employee("e3", "Rui Andrade", "rui@kriolos.io",    "+238 999 0003", "KriolOS"));

    store.addAll(seed);
  }

  /** Returns all contacts. */
  public List<Contact> findAll() {
    return List.copyOf(store);
  }

  /** Returns contacts matching a text query on name, email, phone, or company. */
  public List<Contact> search(String query) {
    if (query == null || query.isBlank()) return findAll();
    String lower = query.strip().toLowerCase();
    return store.stream()
        .filter(c -> c.name().toLowerCase().contains(lower)
            || c.email().toLowerCase().contains(lower)
            || c.phone().toLowerCase().contains(lower)
            || c.company().toLowerCase().contains(lower))
        .collect(Collectors.toList());
  }

  /** Finds a contact by its ID. */
  public Optional<Contact> findById(String id) {
    return store.stream().filter(c -> c.id().equals(id)).findFirst();
  }

  /** Saves (upserts) a contact. If id is null/blank a new UUID is generated. */
  public Contact save(Contact contact) {
    Contact toSave = (contact.id() == null || contact.id().isBlank())
        ? new Contact(UUID.randomUUID().toString(), contact.name(), contact.email(),
            contact.phone(), contact.company(), contact.type(), contact.status())
        : contact;

    store.removeIf(c -> c.id().equals(toSave.id()));
    store.add(toSave);
    return toSave;
  }

  /** Removes a contact by ID. Returns {@code true} if it was found and removed. */
  public boolean deleteById(String id) {
    return store.removeIf(c -> c.id().equals(id));
  }

  /** Total count of all contacts. */
  public int count() {
    return store.size();
  }

  /** Count of contacts with the given type (CUSTOMER, SUPPLIER, EMPLOYEE). */
  public long countByType(String type) {
    return store.stream().filter(c -> c.type().equalsIgnoreCase(type)).count();
  }
}
